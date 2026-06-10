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

class FavoritesRepository(private val context: Context) {
    private val FAVORITES_KEY = stringSetPreferencesKey("favorites_list")
    private val DEFAULT_INDEX_ENABLED_KEY = booleanPreferencesKey("default_index_enabled")
    private val CUSTOM_INDICES_KEY = stringSetPreferencesKey("custom_indices")
    private val ENABLED_CUSTOM_INDICES_KEY = stringSetPreferencesKey("enabled_custom_indices")
    private val MONET_ENABLED_KEY = booleanPreferencesKey("monet_enabled")
    private val CUSTOM_ACCENT_COLOR_KEY = androidx.datastore.preferences.core.intPreferencesKey("custom_accent_color")
    private val RECENT_COLORS_KEY = androidx.datastore.preferences.core.stringPreferencesKey("recent_colors_csv")

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
}
