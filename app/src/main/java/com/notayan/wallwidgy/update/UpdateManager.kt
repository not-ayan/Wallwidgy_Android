package com.notayan.wallwidgy.update

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.notayan.wallwidgy.MainActivity
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException

object UpdateManager {
    private const val CHANNEL_ID = "update_channel"
    private const val NOTIFICATION_ID = 1001

    fun getCurrentVersion(context: Context): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0"
        } catch (e: Exception) {
            "1.0"
        }
    }

    fun isNewerVersion(current: String, target: String): Boolean {
        val cleanCurrent = current.trim().removePrefix("v").removePrefix("V")
        val cleanTarget = target.trim().removePrefix("v").removePrefix("V")
        
        val currentParts = cleanCurrent.split(".").mapNotNull { it.toIntOrNull() }
        val targetParts = cleanTarget.split(".").mapNotNull { it.toIntOrNull() }
        
        val maxLength = maxOf(currentParts.size, targetParts.size)
        for (i in 0 until maxLength) {
            val currVal = currentParts.getOrNull(i) ?: 0
            val targetVal = targetParts.getOrNull(i) ?: 0
            if (targetVal > currVal) return true
            if (currVal > targetVal) return false
        }
        return false
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "App Updates"
            val descriptionText = "Notifications about new versions and updates"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showUpdateNotification(context: Context, latestVersion: String, downloadUrl: String) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "about")
            putExtra("update_version", latestVersion)
            putExtra("update_url", downloadUrl)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Wallwidgy Update Available")
            .setContentText("Version $latestVersion is available. Tap to download and install.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            ) {
                val notificationManager = NotificationManagerCompat.from(context)
                notificationManager.notify(NOTIFICATION_ID, builder.build())
            }
        } catch (e: SecurityException) {
            // Handled when notification permission is denied
        }
    }

    fun downloadApk(context: Context, downloadUrl: String, onProgress: (Int) -> Unit): File {
        val request = Request.Builder().url(downloadUrl).build()
        val client = OkHttpClient()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw IOException("Failed to download file: $response")
        val body = response.body ?: throw IOException("Response body is null")

        val file = File(context.cacheDir, "update.apk")
        if (file.exists()) file.delete()

        val totalBytes = body.contentLength()
        var bytesDownloaded = 0L

        body.byteStream().use { input ->
            file.outputStream().use { output ->
                val buffer = ByteArray(8192)
                var bytesRead = input.read(buffer)
                while (bytesRead != -1) {
                    output.write(buffer, 0, bytesRead)
                    bytesDownloaded += bytesRead
                    val progress = if (totalBytes > 0) (bytesDownloaded * 100 / totalBytes).toInt() else -1
                    onProgress(progress)
                    bytesRead = input.read(buffer)
                }
            }
        }
        return file
    }

    fun installApk(context: Context, file: File) {
        val apkUri = FileProvider.getUriForFile(
            context,
            "com.notayan.wallwidgy.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
