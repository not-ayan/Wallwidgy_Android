package com.notayan.wallwidgy.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.notayan.wallwidgy.data.Wallpaper
import com.notayan.wallwidgy.data.cacheUrl
import com.notayan.wallwidgy.network.WallpaperApi

@Composable
fun WallpaperCard(
    wallpaper: Wallpaper,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Dynamic aspect ratio: 1.77 for Desktop (16:9), 0.7 for Mobile (~9:13)
    val aspectRatio = if (wallpaper.orientation.equals("Desktop", ignoreCase = true)) 1.77f else 0.7f
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(wallpaper.cacheUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = wallpaper.fileName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            ) {
                val state = painter.state
                if (state is AsyncImagePainter.State.Loading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .shimmerEffect()
                    )
                } else {
                    SubcomposeAsyncImageContent()
                }
            }
            
            // Simplified Favorite Button (Bottom Right)
            Surface(
                onClick = { onToggleFavorite() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .size(36.dp),
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.4f),
                contentColor = Color.White
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) Color.Red else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
