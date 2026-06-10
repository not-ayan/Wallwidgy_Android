package com.notayan.wallwidgy.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.notayan.wallwidgy.repository.FavoritesRepository

class WallpaperViewModelFactory(private val repository: FavoritesRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WallpaperViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WallpaperViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
