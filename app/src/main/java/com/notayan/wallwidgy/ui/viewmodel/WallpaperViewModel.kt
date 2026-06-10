package com.notayan.wallwidgy.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notayan.wallwidgy.data.Wallpaper
import com.notayan.wallwidgy.network.NetworkModule
import com.notayan.wallwidgy.repository.FavoritesRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

sealed class UiState {
    object Loading : UiState()
    data class Success(val wallpapers: List<Wallpaper>) : UiState()
    data class Error(val message: String) : UiState()
}

sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    data class UpdateAvailable(val latestVersion: String, val downloadUrl: String, val releaseNotes: String?) : UpdateState()
    object UpToDate : UpdateState()
    data class Downloading(val progress: Int) : UpdateState()
    data class ReadyToInstall(val apkFile: File) : UpdateState()
    data class Error(val message: String) : UpdateState()
}

class WallpaperViewModel(private val favoritesRepository: FavoritesRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _favorites = MutableStateFlow<Set<String>>(emptySet())
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategories = MutableStateFlow<Set<String>>(emptySet())
    val selectedCategories: StateFlow<Set<String>> = _selectedCategories.asStateFlow()

    private val _selectedOrientation = MutableStateFlow<String?>(null)
    val selectedOrientation: StateFlow<String?> = _selectedOrientation.asStateFlow()

    val defaultIndexEnabled = favoritesRepository.defaultIndexEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val customIndices = favoritesRepository.customIndices
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val enabledCustomIndices = favoritesRepository.enabledCustomIndices
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val monetEnabled = favoritesRepository.monetEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val customAccentColor = favoritesRepository.customAccentColor
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0xFF4C663B.toInt())

    val recentColors = favoritesRepository.recentColors
        .stateIn(viewModelScope, SharingStarted.Eagerly, listOf(0xFF4C663B.toInt(), 0xFF2196F3.toInt(), 0xFFE91E63.toInt(), 0xFF9C27B0.toInt(), 0xFFFF9800.toInt()))

    private val _allWallpapers = MutableStateFlow<List<Wallpaper>>(emptyList())

    val availableCategories: StateFlow<List<String>> = _allWallpapers.map { wallpapers ->
        wallpapers.map { it.category.removePrefix("#").trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sortedWith(String.CASE_INSENSITIVE_ORDER)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredWallpapers: StateFlow<List<Wallpaper>> = combine(
        _allWallpapers,
        _searchQuery,
        _selectedCategories,
        _selectedOrientation
    ) { all, query, categories, orientation ->
        all.filter { wallpaper ->
            val matchesSearch = if (query.isEmpty()) true else {
                wallpaper.category.contains(query, ignoreCase = true) ||
                wallpaper.data?.tags?.any { it.contains(query, ignoreCase = true) } == true ||
                wallpaper.data?.title?.contains(query, ignoreCase = true) == true
            }
            val matchesCategory = if (categories.isEmpty()) true else {
                val cleanWallpaperCategory = wallpaper.category.removePrefix("#").trim().lowercase()
                categories.any { selected ->
                    selected.removePrefix("#").trim().lowercase() == cleanWallpaperCategory
                }
            }
            val matchesOrientation = if (orientation == null) true else {
                wallpaper.orientation.equals(orientation, ignoreCase = true)
            }
            matchesSearch && matchesCategory && matchesOrientation
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        observeIndicesAndFetch()
        observeFavorites()
    }

    private fun observeIndicesAndFetch() {
        viewModelScope.launch {
            combine(
                favoritesRepository.defaultIndexEnabled,
                favoritesRepository.customIndices,
                favoritesRepository.enabledCustomIndices
            ) { defaultEnabled, custom, enabledCustom ->
                Triple(defaultEnabled, custom, enabledCustom)
            }.collectLatest { (defaultEnabled, custom, enabledCustom) ->
                fetchWallpapers(defaultEnabled, custom, enabledCustom)
            }
        }
    }

    private fun fetchWallpapers(defaultEnabled: Boolean, custom: Set<String>, enabledCustom: Set<String>) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val allFetched = mutableListOf<Wallpaper>()
                
                // Fetch from default index if enabled
                if (defaultEnabled) {
                    try {
                        val response = NetworkModule.api.getWallpapers(com.notayan.wallwidgy.network.WallpaperApi.BASE_URL + "index.json")
                        allFetched.addAll(response.map { it.copy(baseUrl = com.notayan.wallwidgy.network.WallpaperApi.BASE_URL) })
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                
                // Fetch from enabled custom indices
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
                
                val sorted = allFetched.sortedByDescending { it.timestamp }
                _allWallpapers.value = sorted
                _uiState.value = UiState.Success(sorted)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun observeFavorites() {
        viewModelScope.launch {
            favoritesRepository.favorites.collectLatest {
                _favorites.value = it
            }
        }
    }

    fun toggleFavorite(wallpaper: Wallpaper) {
        viewModelScope.launch {
            favoritesRepository.toggleFavorite(wallpaper.fileName)
        }
    }

    fun setDefaultIndexEnabled(enabled: Boolean) {
        viewModelScope.launch {
            if (!enabled) {
                val defaultBaseUrl = com.notayan.wallwidgy.network.WallpaperApi.BASE_URL
                val toRemove = _allWallpapers.value
                    .filter { it.baseUrl == defaultBaseUrl && _favorites.value.contains(it.fileName) }
                    .map { it.fileName }
                    .toSet()
                if (toRemove.isNotEmpty()) {
                    favoritesRepository.removeFavorites(toRemove)
                }
            }
            favoritesRepository.setDefaultIndexEnabled(enabled)
        }
    }

    fun addCustomIndex(url: String) {
        viewModelScope.launch {
            favoritesRepository.addCustomIndex(url)
        }
    }

    fun removeCustomIndex(url: String) {
        viewModelScope.launch {
            val indexUrl = if (url.endsWith("index.json")) url else {
                if (url.endsWith("/")) "${url}index.json" else "$url/index.json"
            }
            val baseUrl = indexUrl.substringBeforeLast("index.json")
            val toRemove = _allWallpapers.value
                .filter { it.baseUrl == baseUrl && _favorites.value.contains(it.fileName) }
                .map { it.fileName }
                .toSet()
            if (toRemove.isNotEmpty()) {
                favoritesRepository.removeFavorites(toRemove)
            }
            favoritesRepository.removeCustomIndex(url)
        }
    }

    fun toggleCustomIndexEnabled(url: String) {
        viewModelScope.launch {
            val isCurrentlyEnabled = enabledCustomIndices.value.contains(url)
            if (isCurrentlyEnabled) {
                val indexUrl = if (url.endsWith("index.json")) url else {
                    if (url.endsWith("/")) "${url}index.json" else "$url/index.json"
                }
                val baseUrl = indexUrl.substringBeforeLast("index.json")
                val toRemove = _allWallpapers.value
                    .filter { it.baseUrl == baseUrl && _favorites.value.contains(it.fileName) }
                    .map { it.fileName }
                    .toSet()
                if (toRemove.isNotEmpty()) {
                    favoritesRepository.removeFavorites(toRemove)
                }
            }
            favoritesRepository.toggleCustomIndexEnabled(url)
        }
    }

    suspend fun saveIndexSettings(defaultEnabled: Boolean, custom: Set<String>, enabledCustom: Set<String>) {
        val toRemove = mutableSetOf<String>()

        if (!defaultEnabled && defaultIndexEnabled.value) {
            val defaultBaseUrl = com.notayan.wallwidgy.network.WallpaperApi.BASE_URL
            val defaultFavorites = _allWallpapers.value
                .filter { it.baseUrl == defaultBaseUrl && _favorites.value.contains(it.fileName) }
                .map { it.fileName }
            toRemove.addAll(defaultFavorites)
        }

        val prevCustom = customIndices.value
        val prevEnabledCustom = enabledCustomIndices.value

        for (url in prevCustom) {
            val indexUrl = if (url.endsWith("index.json")) url else {
                if (url.endsWith("/")) "${url}index.json" else "$url/index.json"
            }
            val baseUrl = indexUrl.substringBeforeLast("index.json")

            val wasEnabled = prevEnabledCustom.contains(url)
            val isNowEnabled = custom.contains(url) && enabledCustom.contains(url)

            if (wasEnabled && !isNowEnabled) {
                val indexFavorites = _allWallpapers.value
                    .filter { it.baseUrl == baseUrl && _favorites.value.contains(it.fileName) }
                    .map { it.fileName }
                toRemove.addAll(indexFavorites)
            }
        }

        if (toRemove.isNotEmpty()) {
            favoritesRepository.removeFavorites(toRemove)
        }

        favoritesRepository.saveIndexSettings(defaultEnabled, custom, enabledCustom)
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleCategory(category: String) {
        val target = if (category.startsWith("#")) category else "#$category"
        val current = _selectedCategories.value.toMutableSet()
        if (current.contains(target)) {
            current.remove(target)
        } else {
            current.add(target)
        }
        _selectedCategories.value = current
    }

    fun clearCategories() {
        _selectedCategories.value = emptySet()
    }

    fun setOrientation(orientation: String?) {
        _selectedOrientation.value = orientation
    }

    fun getFavoriteWallpapers(): List<Wallpaper> {
        return _allWallpapers.value.filter { _favorites.value.contains(it.fileName) }
    }

    fun getSimilarWallpapers(wallpaper: Wallpaper): List<Wallpaper> {
        val curData = wallpaper.data ?: return emptyList()
        
        return _allWallpapers.value
            .filter { it.fileName != wallpaper.fileName && it.data != null }
            .map { other ->
                var score = 0
                val otherData = other.data!!
                
                if (curData.artStyle == otherData.artStyle) score += 5
                if (curData.series != null && otherData.series != null && curData.series == otherData.series) score += 10
                
                curData.characterNames?.let { curChars ->
                    otherData.characterNames?.let { otherChars ->
                        val common = curChars.intersect(otherChars.toSet())
                        score += common.size * 3
                    }
                }
                
                curData.primaryColors?.let { curCols ->
                    otherData.primaryColors?.let { otherCols ->
                        val common = curCols.intersect(otherCols.toSet())
                        score += (common.size * 1.5).toInt()
                    }
                }
                
                if (curData.colorPalette == otherData.colorPalette) score += 2
                if (curData.mood == otherData.mood) score += 2
                if (curData.category == otherData.category) score += 4
                
                other to score
            }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .map { it.first }
            .take(10)
    }

    // Scroll positions storage
    private val scrollPositions = mutableMapOf<String, Pair<Int, Int>>()

    fun saveScrollPosition(key: String, index: Int, offset: Int) {
        scrollPositions[key] = Pair(index, offset)
    }

    fun getScrollPosition(key: String): Pair<Int, Int>? {
        return scrollPositions[key]
    }

    fun setMonetEnabled(enabled: Boolean) {
        viewModelScope.launch {
            favoritesRepository.setMonetEnabled(enabled)
        }
    }

    fun setCustomAccentColor(color: Int) {
        viewModelScope.launch {
            favoritesRepository.setCustomAccentColor(color)
        }
    }

    suspend fun saveThemeSettings(monetEnabled: Boolean, accentColor: Int) {
        favoritesRepository.setMonetEnabled(monetEnabled)
        favoritesRepository.setCustomAccentColor(accentColor)
    }

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    fun checkForUpdates(context: Context, showNotification: Boolean = false) {
        viewModelScope.launch {
            _updateState.value = UpdateState.Checking
            try {
                val release = NetworkModule.api.getLatestRelease("https://api.github.com/repos/not-ayan/Wallwidgy_Android/releases/tags/release")
                val currentVersion = com.notayan.wallwidgy.update.UpdateManager.getCurrentVersion(context)
                val targetVersion = release.name ?: release.tagName
                
                val isNew = com.notayan.wallwidgy.update.UpdateManager.isNewerVersion(currentVersion, targetVersion)
                if (isNew) {
                    val apkAsset = release.assets.find { it.name.endsWith(".apk") }
                    val downloadUrl = apkAsset?.browserDownloadUrl ?: release.assets.firstOrNull()?.browserDownloadUrl
                    
                    if (downloadUrl != null) {
                        _updateState.value = UpdateState.UpdateAvailable(
                            latestVersion = targetVersion,
                            downloadUrl = downloadUrl,
                            releaseNotes = release.body
                        )
                        if (showNotification) {
                            com.notayan.wallwidgy.update.UpdateManager.showUpdateNotification(context, targetVersion, downloadUrl)
                        }
                    } else {
                        _updateState.value = UpdateState.Error("No APK file found in the latest release assets.")
                    }
                } else {
                    _updateState.value = UpdateState.UpToDate
                }
            } catch (e: Exception) {
                _updateState.value = UpdateState.Error(e.message ?: "Failed to check for updates")
            }
        }
    }

    fun startDownload(context: Context, downloadUrl: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _updateState.value = UpdateState.Downloading(0)
            try {
                val file = com.notayan.wallwidgy.update.UpdateManager.downloadApk(context, downloadUrl) { progress ->
                    _updateState.value = UpdateState.Downloading(progress)
                }
                _updateState.value = UpdateState.ReadyToInstall(file)
            } catch (e: Exception) {
                _updateState.value = UpdateState.Error(e.message ?: "Download failed")
            }
        }
    }

    fun installUpdate(context: Context, file: File) {
        try {
            com.notayan.wallwidgy.update.UpdateManager.installApk(context, file)
        } catch (e: Exception) {
            _updateState.value = UpdateState.Error(e.message ?: "Failed to start installation")
        }
    }

    fun resetUpdateState() {
        _updateState.value = UpdateState.Idle
    }
}
