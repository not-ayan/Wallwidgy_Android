package com.notayan.wallwidgy.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notayan.wallwidgy.data.Wallpaper
import com.notayan.wallwidgy.ui.components.CategoryBar
import com.notayan.wallwidgy.ui.components.WallpaperCard
import com.notayan.wallwidgy.ui.components.WallpaperGridSkeleton
import com.notayan.wallwidgy.ui.viewmodel.UiState
import com.notayan.wallwidgy.ui.viewmodel.WallpaperViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: WallpaperViewModel,
    onWallpaperClick: (Wallpaper) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val filteredWallpapers by viewModel.filteredWallpapers.collectAsState()
    val selectedCategories by viewModel.selectedCategories.collectAsState()
    val selectedOrientation by viewModel.selectedOrientation.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val availableCategories by viewModel.availableCategories.collectAsState()

    var showSearch by remember { mutableStateOf(false) }
    
    // LazyGridState is remembered across recompositions
    val gridState = rememberLazyStaggeredGridState()

    val filterKey = remember(searchQuery, selectedCategories, selectedOrientation) {
        val cats = selectedCategories.sorted().joinToString(",")
        "q:${searchQuery}|c:${cats}|o:${selectedOrientation ?: ""}"
    }

    var lastAppliedKey by remember { mutableStateOf(filterKey) }
    var isRestoring by remember { mutableStateOf(false) }

    // Observe scroll state and save it for the current filter key
    LaunchedEffect(gridState, filterKey) {
        snapshotFlow { 
            Pair(gridState.firstVisibleItemIndex, gridState.firstVisibleItemScrollOffset)
        }.collect { (index, offset) ->
            if (lastAppliedKey == filterKey && !isRestoring) {
                viewModel.saveScrollPosition(filterKey, index, offset)
            }
        }
    }

    // Restore scroll position when filter key changes or new filtered list loaded
    LaunchedEffect(filterKey, filteredWallpapers) {
        if (lastAppliedKey != filterKey) {
            val saved = viewModel.getScrollPosition(filterKey)
            if (saved != null) {
                if (filteredWallpapers.size > saved.first) {
                    isRestoring = true
                    gridState.scrollToItem(saved.first, saved.second)
                    kotlinx.coroutines.delay(100)
                    lastAppliedKey = filterKey
                    isRestoring = false
                } else if (uiState is UiState.Success) {
                    isRestoring = true
                    val targetIndex = maxOf(0, filteredWallpapers.size - 1)
                    gridState.scrollToItem(targetIndex, 0)
                    kotlinx.coroutines.delay(100)
                    lastAppliedKey = filterKey
                    isRestoring = false
                }
            } else {
                isRestoring = true
                gridState.scrollToItem(0, 0)
                kotlinx.coroutines.delay(100)
                lastAppliedKey = filterKey
                isRestoring = false
            }
        }
    }

    val isSystemDark = isSystemInDarkTheme()
    val bgColor = if (isSystemDark) Color(0xFF0A0A0A) else Color(0xFFFAF9F6)

    Scaffold(
        containerColor = bgColor,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 0.dp) // Removed extra gap
            ) {
                // Top Action Bar with Centered Title
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Orientation Switcher (Left) - styled as a beautiful round-corner pill
                    Surface(
                        onClick = { 
                            val next = when(selectedOrientation) {
                                null -> "Mobile"
                                "Mobile" -> "Desktop"
                                else -> null
                            }
                            viewModel.setOrientation(next)
                        },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .height(40.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        ) {
                            Icon(
                                imageVector = when(selectedOrientation) {
                                    "Desktop" -> Icons.Default.Laptop
                                    "Mobile" -> Icons.Default.Smartphone
                                    else -> Icons.Default.Apps
                                },
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = selectedOrientation ?: "All",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Light,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    // Fixed Centered Title with premium thin typography
                    Text(
                        text = "#wallwidgy",
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                        fontWeight = FontWeight.ExtraLight,
                        color = MaterialTheme.colorScheme.onBackground,
                        letterSpacing = 2.sp
                    )

                    // Search Toggle (Right) - styled as a glass circle
                    Surface(
                        onClick = { 
                            showSearch = !showSearch
                            if (!showSearch) {
                                viewModel.setSearchQuery("")
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(40.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                if (showSearch) Icons.Default.Close else Icons.Default.Search, 
                                contentDescription = "Search",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = showSearch,
                    enter = fadeIn(animationSpec = tween(300)) + slideInVertically(animationSpec = tween(300)),
                    exit = fadeOut(animationSpec = tween(300)) + slideOutVertically(animationSpec = tween(300))
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .height(48.dp),
                        placeholder = { 
                            Text(
                                "Search wallpapers...",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Light,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Light),
                        shape = RoundedCornerShape(24.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        singleLine = true
                    )
                }

                // Filter Chips
                CategoryBar(
                    categories = availableCategories,
                    selectedCategories = selectedCategories,
                    onCategoryToggled = { viewModel.toggleCategory(it) },
                    onClearAll = { viewModel.clearCategories() }
                )
                
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            if (uiState is UiState.Loading && filteredWallpapers.isEmpty()) {
                WallpaperGridSkeleton()
            } else {
                LazyVerticalStaggeredGrid(
                    state = gridState, // Maintains scroll position
                    columns = StaggeredGridCells.Fixed(2),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalItemSpacing = 16.dp,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredWallpapers, key = { it.fileName }) { wallpaper ->
                        WallpaperCard(
                            wallpaper = wallpaper,
                            isFavorite = favorites.contains(wallpaper.fileName),
                            onToggleFavorite = { viewModel.toggleFavorite(wallpaper) },
                            onClick = { onWallpaperClick(wallpaper) }
                        )
                    }
                }
            }
        }
    }
}
