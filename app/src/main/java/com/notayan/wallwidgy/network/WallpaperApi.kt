package com.notayan.wallwidgy.network

import com.notayan.wallwidgy.data.Wallpaper
import retrofit2.http.GET
import retrofit2.http.Url

interface WallpaperApi {
    @GET
    suspend fun getWallpapers(@Url url: String): List<Wallpaper>

    companion object {
        const val BASE_URL = "https://raw.githubusercontent.com/not-ayan/storage/main/"
        const val CACHE_BASE_URL = "${BASE_URL}cache/"
        const val MAIN_BASE_URL = "${BASE_URL}main/"
    }
}
