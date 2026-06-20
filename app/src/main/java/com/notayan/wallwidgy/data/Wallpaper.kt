package com.notayan.wallwidgy.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class Wallpaper(
    @SerialName("file_name") val fileName: String,
    @SerialName("file_cache_name") val fileCacheName: String,
    @SerialName("file_main_name") val fileMainName: String,
    val width: Int = 0,
    val height: Int = 0,
    val resolution: String = "",
    val orientation: String = "",
    val timestamp: String = "",
    val category: String = "",
    val data: WallpaperMetadata? = null,
    val baseUrl: String? = null
)

val Wallpaper.cacheUrl: String
    get() = "${baseUrl ?: com.notayan.wallwidgy.network.WallpaperApi.BASE_URL}cache/$fileCacheName"

val Wallpaper.mainUrl: String
    get() = "${baseUrl ?: com.notayan.wallwidgy.network.WallpaperApi.BASE_URL}main/$fileMainName"

@Serializable
data class WallpaperMetadata(
    val title: String? = null,
    val author: String? = null,
    @SerialName("art_style") val artStyle: String? = null,
    val series: String? = null,
    @SerialName("character_names") val characterNames: List<String>? = null,
    @SerialName("primary_colors") val primaryColors: List<String>? = null,
    @SerialName("color_palette") val colorPalette: String? = null,
    val mood: String? = null,
    val category: String? = null,
    val tags: List<String>? = null,
    @SerialName("scene_description") val sceneDescription: String? = null
)
