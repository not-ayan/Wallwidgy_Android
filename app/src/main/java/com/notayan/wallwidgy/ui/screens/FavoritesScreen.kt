package com.notayan.wallwidgy.ui.screens

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.notayan.wallwidgy.data.Wallpaper
import com.notayan.wallwidgy.ui.components.WallpaperCard
import com.notayan.wallwidgy.ui.viewmodel.WallpaperViewModel

@Composable
fun FavoritesScreen(
    viewModel: WallpaperViewModel,
    onWallpaperClick: (Wallpaper) -> Unit
) {
    val favorites by viewModel.favorites.collectAsState()
    val wallpapers = viewModel.getFavoriteWallpapers()
    val isSystemDark = isSystemInDarkTheme()
    val bgColor = if (isSystemDark) Color(0xFF0A0A0A) else Color(0xFFFAF9F6)

    Scaffold(
        containerColor = bgColor,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            // Header for Favorites
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Favorites",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            }
        }
    ) { innerPadding ->
        if (wallpapers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "No favorites yet", style = MaterialTheme.typography.titleMedium)
            }
        } else {
            LazyVerticalStaggeredGrid(
                state = rememberLazyStaggeredGridState(),
                columns = StaggeredGridCells.Adaptive(minSize = 160.dp),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 16.dp,
                    bottom = 34.dp,
                    top = innerPadding.calculateTopPadding()
                ),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalItemSpacing = 8.dp,
                modifier = Modifier.fillMaxSize()
            ) {
                items(wallpapers, key = { it.fileName }) { wallpaper ->
                    WallpaperCard(
                        wallpaper = wallpaper,
                        isFavorite = true,
                        onToggleFavorite = { viewModel.toggleFavorite(wallpaper) },
                        onClick = { onWallpaperClick(wallpaper) }
                    )
                }
            }
        }
    }
}
