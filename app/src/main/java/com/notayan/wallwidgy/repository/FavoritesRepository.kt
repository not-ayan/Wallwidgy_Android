package com.notayan.wallwidgy.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "favorites")

class FavoritesRepository(val context: Context) {
    private val FAVORITES_KEY = stringSetPreferencesKey("favorites_list")
    private val DEFAULT_INDEX_ENABLED_KEY = booleanPreferencesKey("default_index_enabled")
    private val CUSTOM_INDICES_KEY = stringSetPreferencesKey("custom_indices")
    private val ENABLED_CUSTOM_INDICES_KEY = stringSetPreferencesKey("enabled_custom_indices")
    private val MONET_ENABLED_KEY = booleanPreferencesKey("monet_enabled")
    private val CUSTOM_ACCENT_COLOR_KEY = androidx.datastore.preferences.core.intPreferencesKey("custom_accent_color")
    private val RECENT_COLORS_KEY = androidx.datastore.preferences.core.stringPreferencesKey("recent_colors_csv")
    private val ROTATION_ENABLED_KEY = booleanPreferencesKey("rotation_enabled")
    private val ROTATION_MODE_KEY = androidx.datastore.preferences.core.stringPreferencesKey("rotation_mode")
    private val ROTATION_VALUE_KEY = androidx.datastore.preferences.core.stringPreferencesKey("rotation_value")
    private val ROTATION_DURATION_KEY = androidx.datastore.preferences.core.intPreferencesKey("rotation_duration")
    private val ROTATION_TARGET_KEY = androidx.datastore.preferences.core.stringPreferencesKey("rotation_target")
    private val ROTATION_LAST_TIME_KEY = androidx.datastore.preferences.core.longPreferencesKey("rotation_last_time")
    private val SEMANTIC_SEARCH_ENABLED_KEY = booleanPreferencesKey("semantic_search_enabled")

    val favorites: Flow<Set<String>> = context.dataStore.data
        .map { preferences ->
            preferences[FAVORITES_KEY] ?: emptySet()
        }
        .distinctUntilChanged()

    val defaultIndexEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[DEFAULT_INDEX_ENABLED_KEY] ?: true
        }
        .distinctUntilChanged()

    val customIndices: Flow<Set<String>> = context.dataStore.data
        .map { preferences ->
            preferences[CUSTOM_INDICES_KEY] ?: emptySet()
        }
        .distinctUntilChanged()

    val enabledCustomIndices: Flow<Set<String>> = context.dataStore.data
        .map { preferences ->
            preferences[ENABLED_CUSTOM_INDICES_KEY] ?: emptySet()
        }
        .distinctUntilChanged()

    val monetEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[MONET_ENABLED_KEY] ?: true
        }
        .distinctUntilChanged()

    val customAccentColor: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[CUSTOM_ACCENT_COLOR_KEY] ?: 0xFF4C663B.toInt()
        }
        .distinctUntilChanged()

    val recentColors: Flow<List<Int>> = context.dataStore.data
        .map { preferences ->
            val csv = preferences[RECENT_COLORS_KEY] ?: "0xFF4C663B,0xFF2196F3,0xFFE91E63,0xFF9C27B0,0xFFFF9800"
            csv.split(",")
                .mapNotNull { it.trim().substringAfter("0x").toLongOrNull(16)?.toInt() }
        }
        .distinctUntilChanged()

    val rotationEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[ROTATION_ENABLED_KEY] ?: false
        }
        .distinctUntilChanged()

    val rotationMode: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[ROTATION_MODE_KEY] ?: "random"
        }
        .distinctUntilChanged()

    val rotationValue: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[ROTATION_VALUE_KEY] ?: ""
        }
        .distinctUntilChanged()

    val rotationDuration: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[ROTATION_DURATION_KEY] ?: 24
        }
        .distinctUntilChanged()

    val rotationTarget: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[ROTATION_TARGET_KEY] ?: "both"
        }
        .distinctUntilChanged()

    val rotationLastTime: Flow<Long> = context.dataStore.data
        .map { preferences ->
            preferences[ROTATION_LAST_TIME_KEY] ?: 0L
        }
        .distinctUntilChanged()

    val semanticSearchEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[SEMANTIC_SEARCH_ENABLED_KEY] ?: false
        }
        .distinctUntilChanged()

    suspend fun toggleFavorite(fileName: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[FAVORITES_KEY] ?: emptySet()
            if (current.contains(fileName)) {
                preferences[FAVORITES_KEY] = current - fileName
            } else {
                preferences[FAVORITES_KEY] = current + fileName
            }
        }
    }

    suspend fun removeFavorites(fileNames: Set<String>) {
        context.dataStore.edit { preferences ->
            val current = preferences[FAVORITES_KEY] ?: emptySet()
            preferences[FAVORITES_KEY] = current - fileNames
        }
    }

    suspend fun isFavorite(fileName: String): Boolean {
        var isFav = false
        context.dataStore.edit { preferences ->
            val current = preferences[FAVORITES_KEY] ?: emptySet()
            isFav = current.contains(fileName)
        }
        return isFav
    }

    suspend fun setDefaultIndexEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DEFAULT_INDEX_ENABLED_KEY] = enabled
        }
    }

    suspend fun addCustomIndex(url: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[CUSTOM_INDICES_KEY] ?: emptySet()
            preferences[CUSTOM_INDICES_KEY] = current + url
            
            // Auto-enable newly added index
            val currentEnabled = preferences[ENABLED_CUSTOM_INDICES_KEY] ?: emptySet()
            preferences[ENABLED_CUSTOM_INDICES_KEY] = currentEnabled + url
        }
    }

    suspend fun removeCustomIndex(url: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[CUSTOM_INDICES_KEY] ?: emptySet()
            val newCustom = current - url
            preferences[CUSTOM_INDICES_KEY] = newCustom
            
            val currentEnabled = preferences[ENABLED_CUSTOM_INDICES_KEY] ?: emptySet()
            preferences[ENABLED_CUSTOM_INDICES_KEY] = currentEnabled - url

            if (newCustom.isEmpty()) {
                preferences[DEFAULT_INDEX_ENABLED_KEY] = true
            }
        }
    }

    suspend fun saveIndexSettings(defaultEnabled: Boolean, custom: Set<String>, enabledCustom: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[DEFAULT_INDEX_ENABLED_KEY] = defaultEnabled
            preferences[CUSTOM_INDICES_KEY] = custom
            preferences[ENABLED_CUSTOM_INDICES_KEY] = enabledCustom
        }
    }

    suspend fun toggleCustomIndexEnabled(url: String) {
        context.dataStore.edit { preferences ->
            val currentEnabled = preferences[ENABLED_CUSTOM_INDICES_KEY] ?: emptySet()
            if (currentEnabled.contains(url)) {
                preferences[ENABLED_CUSTOM_INDICES_KEY] = currentEnabled - url
            } else {
                preferences[ENABLED_CUSTOM_INDICES_KEY] = currentEnabled + url
            }
        }
    }

    suspend fun setMonetEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[MONET_ENABLED_KEY] = enabled
        }
    }

    suspend fun setCustomAccentColor(color: Int) {
        context.dataStore.edit { preferences ->
            preferences[CUSTOM_ACCENT_COLOR_KEY] = color
            
            // Add to recent colors list
            val csv = preferences[RECENT_COLORS_KEY] ?: "0xFF4C663B,0xFF2196F3,0xFFE91E63,0xFF9C27B0,0xFFFF9800"
            val list = csv.split(",").map { it.trim() }.toMutableList()
            
            val hexString = "0x" + Integer.toHexString(color).uppercase()
            list.remove(hexString) // Remove if already exists to push it to the top
            list.add(0, hexString) // Add at start
            
            // Keep maximum 5 recent colors
            val updatedList = list.take(5)
            preferences[RECENT_COLORS_KEY] = updatedList.joinToString(",")
        }
    }

    suspend fun setRotationEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ROTATION_ENABLED_KEY] = enabled
        }
    }

    suspend fun setRotationMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[ROTATION_MODE_KEY] = mode
        }
    }

    suspend fun setRotationValue(value: String) {
        context.dataStore.edit { preferences ->
            preferences[ROTATION_VALUE_KEY] = value
        }
    }

    suspend fun setRotationDuration(duration: Int) {
        context.dataStore.edit { preferences ->
            preferences[ROTATION_DURATION_KEY] = duration
        }
    }

    suspend fun setRotationTarget(target: String) {
        context.dataStore.edit { preferences ->
            preferences[ROTATION_TARGET_KEY] = target
        }
    }

    suspend fun setRotationLastTime(time: Long) {
        context.dataStore.edit { preferences ->
            preferences[ROTATION_LAST_TIME_KEY] = time
        }
    }

    suspend fun setSemanticSearchEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SEMANTIC_SEARCH_ENABLED_KEY] = enabled
        }
    }
}
