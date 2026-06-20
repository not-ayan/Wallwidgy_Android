package com.notayan.wallwidgy.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.notayan.wallwidgy.data.Wallpaper
import com.notayan.wallwidgy.data.mainUrl
import com.notayan.wallwidgy.network.NetworkModule
import com.notayan.wallwidgy.network.WallpaperApi
import com.notayan.wallwidgy.repository.FavoritesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.lang.Exception
import kotlin.random.Random

class AutoWallpaperWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val favoritesRepository = FavoritesRepository(applicationContext)
        val enabled = favoritesRepository.rotationEnabled.first()
        if (!enabled) {
            return@withContext Result.success()
        }

        val mode = favoritesRepository.rotationMode.first()
        val rotationVal = favoritesRepository.rotationValue.first()
        val target = favoritesRepository.rotationTarget.first()

        val defaultEnabled = favoritesRepository.defaultIndexEnabled.first()
        val custom = favoritesRepository.customIndices.first()
        val enabledCustom = favoritesRepository.enabledCustomIndices.first()

        val allFetched = mutableListOf<Wallpaper>()
        if (defaultEnabled) {
            try {
                val response = NetworkModule.api.getWallpapers(WallpaperApi.BASE_URL + "index.json")
                allFetched.addAll(response.map { it.copy(baseUrl = WallpaperApi.BASE_URL) })
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        for (url in custom) {
            if (enabledCustom.contains(url)) {
                try {
                    val indexUrl = if (url.endsWith("index.json")) url else {
                        if (url.endsWith("/")) "${url}index.json" else "$url/index.json"
                    }
                    val baseUrl = indexUrl.substringBeforeLast("index.json")
                    val response = NetworkModule.api.getWallpapers(indexUrl)
                    allFetched.addAll(response.map { it.copy(baseUrl = baseUrl) })
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        if (allFetched.isEmpty()) {
            return@withContext Result.retry()
        }

        // Filter: only phone (not desktop)
        val phoneWallpapers = allFetched.filter { !it.orientation.equals("Desktop", ignoreCase = true) }
        if (phoneWallpapers.isEmpty()) {
            return@withContext Result.success()
        }

        // Apply mode filters
        var filtered = when (mode) {
            "favorites" -> {
                val favs = favoritesRepository.favorites.first()
                phoneWallpapers.filter { favs.contains(it.fileName) }
            }
            "category" -> {
                val targetCategory = rotationVal.removePrefix("#").trim().lowercase()
                phoneWallpapers.filter { 
                    it.category.removePrefix("#").trim().lowercase() == targetCategory 
                }
            }
            "color" -> {
                val targetColor = rotationVal.removePrefix("#").trim().lowercase()
                phoneWallpapers.filter { wp ->
                    wp.data?.primaryColors?.any { 
                        it.removePrefix("#").trim().lowercase() == targetColor 
                    } == true || wp.data?.colorPalette?.removePrefix("#")?.trim()?.lowercase() == targetColor
                }
            }
            else -> phoneWallpapers
        }

        // Fallback if filtered list is empty
        if (filtered.isEmpty()) {
            filtered = phoneWallpapers
        }

        val selectedWp = filtered[Random.nextInt(filtered.size)]
        
        // Download and apply
        try {
            val loader = ImageLoader(applicationContext)
            val req = ImageRequest.Builder(applicationContext)
                .data(selectedWp.mainUrl)
                .allowHardware(false)
                .build()
            val res = loader.execute(req)
            if (res is SuccessResult) {
                val bmp = (res.drawable as? BitmapDrawable)?.bitmap
                if (bmp != null) {
                    val wm = WallpaperManager.getInstance(applicationContext)
                    val flags = when (target) {
                        "home" -> WallpaperManager.FLAG_SYSTEM
                        "lock" -> WallpaperManager.FLAG_LOCK
                        "both" -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
                        else -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        wm.setBitmap(bmp, null, true, flags)
                    } else {
                        wm.setBitmap(bmp)
                    }
                    favoritesRepository.setRotationLastTime(System.currentTimeMillis())
                    
                    // Show a simple notification to inform the user
                    showNotification(selectedWp.data?.title ?: selectedWp.fileName)
                    return@withContext Result.success()
                }
            }
            return@withContext Result.retry()
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext Result.retry()
        }
    }

    private fun showNotification(title: String) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "auto_wallpaper_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Auto Wallpaper Rotation",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("Wallpaper Updated")
            .setContentText("Set new wallpaper: $title")
            .setSmallIcon(android.R.drawable.ic_menu_gallery)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(4242, notification)
    }
}
