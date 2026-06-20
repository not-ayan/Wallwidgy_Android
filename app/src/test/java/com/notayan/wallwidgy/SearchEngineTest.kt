package com.notayan.wallwidgy

import com.notayan.wallwidgy.search.SearchEngine
import com.notayan.wallwidgy.data.Wallpaper
import com.notayan.wallwidgy.data.WallpaperMetadata
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchEngineTest {

    @Test
    fun testCosineSimilarityPerfectMatch() {
        val vec = floatArrayOf(0.5f, 0.5f, 0.5f, 0.5f)
        val similarity = SearchEngine.cosineSimilarity(vec, vec)
        assertEquals(1.0, similarity, 0.0001)
    }

    @Test
    fun testCosineSimilarityOpposite() {
        val vec1 = floatArrayOf(1.0f, 0.0f)
        val vec2 = floatArrayOf(-1.0f, 0.0f)
        val similarity = SearchEngine.cosineSimilarity(vec1, vec2)
        assertEquals(-1.0, similarity, 0.0001)
    }

    @Test
    fun testCosineSimilarityOrthogonal() {
        val vec1 = floatArrayOf(1.0f, 0.0f)
        val vec2 = floatArrayOf(0.0f, 1.0f)
        val similarity = SearchEngine.cosineSimilarity(vec1, vec2)
        assertEquals(0.0, similarity, 0.0001)
    }

    @Test
    fun testSearchTextPreparation() {
        val metadata = WallpaperMetadata(
            title = "Samurai Hill",
            artStyle = "anime",
            series = "original",
            characterNames = listOf("ronin"),
            primaryColors = listOf("red", "blue"),
            tags = listOf("sunset", "dramatic"),
            sceneDescription = "A warrior with a sword stands in a field of red grass."
        )
        val wallpaper = Wallpaper(
            fileName = "samurai-hill",
            fileCacheName = "samurai-hill.webp",
            fileMainName = "samurai-hill.png",
            category = "#art",
            data = metadata
        )

        val searchText = getSearchTextForWallpaper(wallpaper)
        assertTrue(searchText.contains("Samurai Hill"))
        assertTrue(searchText.contains("art"))
        assertTrue(searchText.contains("original"))
        assertTrue(searchText.contains("anime"))
        assertTrue(searchText.contains("ronin"))
        assertTrue(searchText.contains("red"))
        assertTrue(searchText.contains("blue"))
        assertTrue(searchText.contains("sunset"))
        assertTrue(searchText.contains("dramatic"))
        assertTrue(searchText.contains("A warrior with a sword"))
    }

    private fun getSearchTextForWallpaper(wallpaper: Wallpaper): String {
        val sb = StringBuilder()
        wallpaper.data?.title?.let { sb.append(it).append(" ") }
        sb.append(wallpaper.category.removePrefix("#")).append(" ")
        wallpaper.data?.series?.let { sb.append(it).append(" ") }
        wallpaper.data?.artStyle?.let { sb.append(it).append(" ") }
        wallpaper.data?.mood?.let { sb.append(it).append(" ") }
        wallpaper.data?.characterNames?.forEach { sb.append(it).append(" ") }
        wallpaper.data?.primaryColors?.forEach { sb.append(it).append(" ") }
        wallpaper.data?.tags?.forEach { sb.append(it).append(" ") }
        wallpaper.data?.sceneDescription?.let { sb.append(it).append(" ") }
        return sb.toString().trim()
    }
}
