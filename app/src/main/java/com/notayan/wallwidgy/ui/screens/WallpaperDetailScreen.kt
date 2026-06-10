package com.notayan.wallwidgy.ui.screens

import android.app.Activity
import android.app.DownloadManager
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.notayan.wallwidgy.data.Wallpaper
import com.notayan.wallwidgy.data.cacheUrl
import com.notayan.wallwidgy.data.mainUrl
import com.notayan.wallwidgy.network.WallpaperApi
import com.notayan.wallwidgy.ui.viewmodel.UiState
import com.notayan.wallwidgy.ui.viewmodel.WallpaperViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun WallpaperDetailScreen(
    wallpaper: Wallpaper,
    viewModel: WallpaperViewModel,
    onBack: () -> Unit,
    onNavigateToWallpaper: (Wallpaper) -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp

    // ── System Theme Configurations ──
    val isSystemDark = isSystemInDarkTheme()
    val contentColorOnBlur = if (isSystemDark) Color.White else Color.Black
    val glassBgColor = if (isSystemDark) Color.Black.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.6f)
    val glassBorderColor = if (isSystemDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f)

    val sheetBgColor = if (isSystemDark) Color(0xFF0E100D) else Color(0xFFFAFBF9)
    val sheetBorderColor = if (isSystemDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f)
    val sheetTextColor = if (isSystemDark) Color.White else Color.Black
    val sheetSecondaryTextColor = if (isSystemDark) Color.White.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.5f)

    // ── ViewModel State & Cycling ──
    val favorites by viewModel.favorites.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val wallpapers = remember(uiState) {
        (uiState as? UiState.Success)?.wallpapers ?: emptyList()
    }

    var currentIndex by remember(wallpaper, wallpapers) {
        mutableIntStateOf(
            wallpapers.indexOfFirst { it.fileName == wallpaper.fileName }.coerceAtLeast(0)
        )
    }
    val currentWallpaper = wallpapers.getOrNull(currentIndex) ?: wallpaper
    val isFavorite = favorites.contains(currentWallpaper.fileName)
    val isDesktop = currentWallpaper.orientation.equals("Desktop", ignoreCase = true)

    // ── Dialog, Fullscreen & Scroll UI States ──
    var showSetWallpaperDialog by remember { mutableStateOf(false) }
    var isHdLoaded by remember { mutableStateOf(false) }
    var isSheetExpanded by remember { mutableStateOf(false) }
    var isFullScreen by remember { mutableStateOf(false) }
    var showAllTags by remember { mutableStateOf(false) }
    var showLikeAnimTrigger by remember { mutableStateOf(false) }

    // Intercept system back gestures to collapse expanded bottom sheet
    BackHandler(enabled = isSheetExpanded) {
        isSheetExpanded = false
    }

    // ── Pinch to Zoom State ──
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        offset = if (scale > 1.05f) offset + offsetChange else Offset.Zero
    }

    // Reset zoom & panel states on swipe/navigation
    LaunchedEffect(currentIndex) {
        isHdLoaded = false
        isSheetExpanded = false
        showAllTags = false
        scale = 1f
        offset = Offset.Zero
    }

    val statusBarsPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navigationBarsPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val topHeaderHeight = statusBarsPadding + 44.dp + 16.dp
    val collapsedSheetHeight = 140.dp
    val expandedSheetHeight = remember(screenHeight) {
        (screenHeight * 0.75f).coerceIn(350.dp, 580.dp).coerceAtMost(screenHeight - 80.dp)
    }
    val bottomPanelHeight = navigationBarsPadding + collapsedSheetHeight + 16.dp

    val targetCardOffset = if (isFullScreen) {
        0.dp
    } else {
        (topHeaderHeight - bottomPanelHeight) / 2
    }

    // ── Vibrant Color Extraction & Accent Coloring ──
    val metaAccentColor = remember(currentWallpaper) {
        findVibrantColor(currentWallpaper.data?.primaryColors, null)
    }
    var extractedColor by remember { mutableStateOf<Color?>(null) }

    LaunchedEffect(currentWallpaper.fileName, metaAccentColor) {
        withContext(Dispatchers.IO) {
            try {
                val loader = ImageLoader(context)
                val req = ImageRequest.Builder(context)
                    .data(currentWallpaper.cacheUrl)
                    .allowHardware(false)
                    .size(100)
                    .build()
                val res = loader.execute(req)
                if (res is SuccessResult) {
                    val bmp = (res.drawable as? BitmapDrawable)?.bitmap
                    if (bmp != null) {
                        val c = extractVibrantAccent(bmp)
                        withContext(Dispatchers.Main) { extractedColor = c }
                        return@withContext
                    }
                }
            } catch (_: Exception) {}
            
            // Fallback to metadata accent color if bitmap loading fails
            withContext(Dispatchers.Main) {
                extractedColor = metaAccentColor ?: Color(0xFFC85A17)
            }
        }
    }

    val activeAccentColor by animateColorAsState(
        targetValue = extractedColor ?: Color(0xFFC85A17),
        animationSpec = tween(500), label = "accent"
    )

    val displayAccentColor = remember(activeAccentColor) {
        if (activeAccentColor.luminance() < 0.05f) Color.White else activeAccentColor
    }

    // ── Animations & Transitions ──
    val isVisible = !isFullScreen
    val topBarOffset by animateDpAsState(
        if (isVisible) 0.dp else (-90).dp,
        spring(stiffness = Spring.StiffnessLow), label = "topOff"
    )
    val bottomCardOffset by animateDpAsState(
        if (isVisible) 0.dp else (screenHeight + 100.dp),
        spring(stiffness = Spring.StiffnessLow), label = "botOff"
    )
    val cardVerticalOffset by animateDpAsState(
        targetCardOffset,
        spring(stiffness = Spring.StiffnessLow), label = "cardOffset"
    )
    val controlsAlpha by animateFloatAsState(
        if (isVisible) 1f else 0f, tween(300), label = "ctrlA"
    )

    // Heart popup animation
    val likeScale by animateFloatAsState(
        if (showLikeAnimTrigger) 1.5f else 0f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "likeS"
    )
    val likeAlpha by animateFloatAsState(
        if (showLikeAnimTrigger) 1f else 0f,
        tween(if (showLikeAnimTrigger) 100 else 400), label = "likeA"
    )
    LaunchedEffect(showLikeAnimTrigger) {
        if (showLikeAnimTrigger) { delay(600); showLikeAnimTrigger = false }
    }

    val bottomSheetHeight by animateDpAsState(
        if (isSheetExpanded) expandedSheetHeight else collapsedSheetHeight,
        spring(stiffness = Spring.StiffnessLow), label = "sheetH"
    )
    val fullScreenProgress by animateFloatAsState(
        targetValue = if (isFullScreen) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "fullscreen"
    )

    // ── Fullscreen immersive sticky system bars toggle ──
    DisposableEffect(isFullScreen) {
        val window = (context as? Activity)?.window
        if (window != null) {
            val ctrl = WindowCompat.getInsetsController(window, view)
            if (isFullScreen) {
                ctrl.hide(WindowInsetsCompat.Type.systemBars())
                ctrl.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                ctrl.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            val w = (context as? Activity)?.window
            if (w != null) {
                val ctrl = WindowCompat.getInsetsController(w, view)
                ctrl.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    val imageUrl = if (isHdLoaded)
        currentWallpaper.mainUrl
    else
        currentWallpaper.cacheUrl

    // ══════════════════════════ UI ══════════════════════════
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isSystemDark) Color(0xFF0A0A0A) else Color(0xFFFAF9F6))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (isSheetExpanded) {
                    isSheetExpanded = false
                } else {
                    isFullScreen = !isFullScreen
                }
            }
    ) {
        // ── Blurred backdrop ──
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data("${WallpaperApi.CACHE_BASE_URL}${currentWallpaper.fileCacheName}")
                .build(),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = 1.3f
                    scaleY = 1.3f
                    alpha = 0.5f
                }
                .blur(40.dp),
            contentScale = ContentScale.Crop
        )

        val overlayBrush = Brush.verticalGradient(
            colors = listOf(
                activeAccentColor.copy(alpha = if (isSystemDark) 0.35f else 0.15f),
                Color.Black.copy(alpha = if (isSystemDark) 0.9f else 0.65f)
            )
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(overlayBrush)
        )

        // ── Wallpaper Preview Card ──
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val cardAspect = if (isDesktop) 1.6f else 0.56f
            val cardW: Dp
            val cardH: Dp

            val usableWidth = maxWidth - 64.dp
            val usableHeight = maxHeight - collapsedSheetHeight - 100.dp
            if (usableWidth / usableHeight <= cardAspect) {
                cardW = usableWidth; cardH = usableWidth / cardAspect
            } else {
                cardH = usableHeight; cardW = usableHeight * cardAspect
            }

            val currentWidth = lerp(cardW, maxWidth, fullScreenProgress)
            val currentHeight = lerp(cardH, maxHeight, fullScreenProgress)
            val currentCornerRadius = lerp(28.dp, 0.dp, fullScreenProgress)

            // Gallery Card Preview Surface
            Box(
                modifier = Modifier
                    .size(currentWidth, currentHeight)
                    .offset(y = cardVerticalOffset),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(currentCornerRadius),
                    color = if (isSystemDark) Color.Black else Color.White,
                    border = BorderStroke(
                        1.dp * (1f - fullScreenProgress),
                        Color.White.copy(alpha = 0.15f * (1f - fullScreenProgress))
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(
                                1.dp * (1f - fullScreenProgress),
                                Color.Black.copy(alpha = 0.1f * (1f - fullScreenProgress)),
                                RoundedCornerShape(currentCornerRadius)
                            )
                            .transformable(transformState)
                            .pointerInput(currentWallpaper.fileName, isFavorite) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        if (!isFavorite) {
                                            viewModel.toggleFavorite(currentWallpaper)
                                        }
                                        showLikeAnimTrigger = true
                                    },
                                    onTap = {
                                        if (isSheetExpanded) {
                                            isSheetExpanded = false
                                        } else {
                                            isFullScreen = !isFullScreen
                                        }
                                    }
                                )
                            }
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(imageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = currentWallpaper.data?.title,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = scale; scaleY = scale
                                    translationX = offset.x; translationY = offset.y
                                },
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                // Heart Pop Feedback
                Icon(
                    Icons.Default.Favorite, null,
                    tint = Color.Red,
                    modifier = Modifier
                        .size(84.dp)
                        .graphicsLayer {
                            scaleX = likeScale; scaleY = likeScale; alpha = likeAlpha
                        }
                )

                // Navigation Buttons (Left/Right Arrows) - Circular overlapping style matching the screenshot
                if (wallpapers.size > 1 && !isFullScreen) {
                    GlassNavigationButton(
                        icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Previous",
                        isSystemDark = isSystemDark,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .offset(x = (-22).dp)
                            .graphicsLayer { alpha = controlsAlpha },
                        onClick = {
                            currentIndex = (currentIndex - 1 + wallpapers.size) % wallpapers.size
                        }
                    )
                    GlassNavigationButton(
                        icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next",
                        isSystemDark = isSystemDark,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .offset(x = 22.dp)
                            .graphicsLayer { alpha = controlsAlpha },
                        onClick = {
                            currentIndex = (currentIndex + 1) % wallpapers.size
                        }
                    )
                }
            }
        }

        // ── Top Header Bar (Redesigned Floating Elements) ──
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .offset(y = topBarOffset)
                .graphicsLayer { alpha = controlsAlpha }
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassCircleButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                isSystemDark = isSystemDark,
                onClick = onBack
            )

            val formattedResolution = remember(currentWallpaper.resolution) {
                val res = currentWallpaper.resolution.lowercase()
                if (res.contains("3840") || res.contains("4k")) "4K"
                else if (res.contains("1920") || res.contains("1080") || res.contains("2k")) "2K"
                else currentWallpaper.resolution.uppercase()
            }

            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color.Black.copy(alpha = 0.6f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                shadowElevation = 0.dp,
                modifier = Modifier.height(44.dp)
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = formattedResolution,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium.copy(letterSpacing = 1.sp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            GlassCircleButton(
                icon = Icons.Default.HighQuality,
                contentDescription = "Toggle HD",
                isSystemDark = isSystemDark,
                tint = if (isHdLoaded) displayAccentColor else null,
                onClick = { isHdLoaded = !isHdLoaded }
            )
        }

        // ── Floating macOS-style Bottom Dock Panel ──
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .height(bottomSheetHeight)
                .padding(start = 24.dp, end = 24.dp, bottom = 16.dp)
                .offset(y = bottomCardOffset)
                .graphicsLayer { alpha = controlsAlpha },
            shape = RoundedCornerShape(32.dp),
            color = sheetBgColor,
            border = BorderStroke(1.dp, sheetBorderColor),
            shadowElevation = 0.dp
        ) {
            Box(Modifier.fillMaxSize()) {
                if (isSheetExpanded) {
                    // ── Expanded Panel Scroll Content ──
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Title and Close Row
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = getCleanTitle(currentWallpaper),
                                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = sheetTextColor
                                )
                                val author = currentWallpaper.data?.author
                                if (!author.isNullOrBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "by $author",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = sheetSecondaryTextColor,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = displayAccentColor.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, displayAccentColor.copy(alpha = 0.3f))
                                ) {
                                    Text(
                                        text = currentWallpaper.category.uppercase(),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.5.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = displayAccentColor
                                    )
                                }
                            }
                            
                            GlassCircleButton(
                                icon = Icons.Default.Close,
                                contentDescription = "Close",
                                isSystemDark = isSystemDark,
                                onClick = { isSheetExpanded = false }
                            )
                        }

                        // Consolidated Info & Palette Details Card
                        val colors = currentWallpaper.data?.primaryColors ?: emptyList()
                        WallpaperInfoCard(
                            wallpaper = currentWallpaper,
                            colors = colors,
                            context = context,
                            isSystemDark = isSystemDark
                        )

                        // Tags flow
                        val tags = currentWallpaper.data?.tags ?: emptyList()
                        if (tags.isNotEmpty()) {
                            Column(Modifier.fillMaxWidth()) {
                                Text(
                                    "TAGS",
                                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp, letterSpacing = 0.8.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = sheetSecondaryTextColor,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                val displayTags = if (showAllTags || tags.size <= 4) tags else tags.take(4)
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    displayTags.forEach { tag ->
                                        TagChip(tag, isSystemDark) {
                                            viewModel.setSearchQuery(tag)
                                            viewModel.clearCategories()
                                            onBack()
                                        }
                                    }
                                    if (!showAllTags && tags.size > 4) {
                                        TagChip("+${tags.size - 4} more", isSystemDark, textColor = displayAccentColor) { showAllTags = true }
                                    } else if (showAllTags && tags.size > 4) {
                                        TagChip("show less", isSystemDark, textColor = displayAccentColor) { showAllTags = false }
                                    }
                                }
                            }
                        }

                        // Similar Wallpapers Horizontal List
                        val similar = remember(currentWallpaper) { viewModel.getSimilarWallpapers(currentWallpaper) }
                        if (similar.isNotEmpty()) {
                            Column(Modifier.fillMaxWidth()) {
                                Text(
                                    "MORE LIKE THIS",
                                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp, letterSpacing = 0.8.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = sheetSecondaryTextColor,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(similar) { s ->
                                        SimilarWallpaperCard(s, isSystemDark) { onNavigateToWallpaper(s) }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // ── Collapsed Dock capsule ──
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Row 1: Title and Expand Details Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = getCleanTitle(currentWallpaper),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = sheetTextColor,
                                maxLines = 1,
                                modifier = Modifier.weight(1f).padding(end = 12.dp)
                            )

                            // Expand details button: small dark translucent circle with arrow up
                            GlassCircleButton(
                                icon = Icons.Default.KeyboardArrowUp,
                                contentDescription = "Details",
                                isSystemDark = isSystemDark,
                                size = 38.dp,
                                onClick = { isSheetExpanded = true }
                            )
                        }

                        // Row 2: Favorite, Set Wallpaper, Share, Download
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Favorite
                            GlassCircleButton(
                                icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                isSystemDark = isSystemDark,
                                tint = if (isFavorite) Color.Red else null,
                                onClick = { viewModel.toggleFavorite(currentWallpaper) }
                            )

                            // Set Wallpaper Button (accent color background, image/wallpaper icon)
                            val textAndIconColor = if (displayAccentColor.luminance() > 0.5f) Color.Black else Color.White
                            val applyInteractionSource = remember { MutableInteractionSource() }
                            val isApplyPressed by applyInteractionSource.collectIsPressedAsState()
                            val applyScale by animateFloatAsState(if (isApplyPressed) 0.92f else 1f, label = "scale")

                            val showButtonIcon = screenWidth >= 350.dp
                            val buttonTextSize = if (screenWidth < 360.dp) 12.sp else 14.sp
                            val buttonText = if (screenWidth < 340.dp) "Apply" else "Set Wallpaper"

                            Button(
                                onClick = { showSetWallpaperDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = displayAccentColor,
                                    contentColor = textAndIconColor
                                ),
                                shape = RoundedCornerShape(24.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                interactionSource = applyInteractionSource,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .graphicsLayer {
                                        scaleX = applyScale
                                        scaleY = applyScale
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    if (showButtonIcon) {
                                        Icon(Icons.Outlined.Wallpaper, null, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(6.dp))
                                    }
                                    Text(
                                        text = buttonText,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false,
                                        style = MaterialTheme.typography.labelLarge.copy(fontSize = buttonTextSize)
                                    )
                                }
                            }

                            // Share
                            GlassCircleButton(
                                icon = Icons.Default.Share,
                                contentDescription = "Share",
                                isSystemDark = isSystemDark,
                                onClick = { shareWallpaper(context, currentWallpaper) }
                            )

                            // Download
                            GlassCircleButton(
                                icon = Icons.Default.Download,
                                contentDescription = "Download",
                                isSystemDark = isSystemDark,
                                onClick = { downloadWallpaper(context, currentWallpaper) }
                            )
                        }
                    }
                }
            }
        }

        // ══════════ Set Wallpaper Dialog (Redesigned) ══════════
        if (showSetWallpaperDialog) {
            Dialog(onDismissRequest = { showSetWallpaperDialog = false }) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = sheetBgColor,
                    border = BorderStroke(1.dp, sheetBorderColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Apply Wallpaper",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = sheetTextColor
                        )
                        Text(
                            text = "Choose how you would like to set this wallpaper:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = sheetSecondaryTextColor,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )

                        WallpaperOptionCard(
                            title = "Home Screen",
                            subtext = "Apply to home workspace",
                            icon = Icons.Default.Home,
                            isSystemDark = isSystemDark,
                            activeAccentColor = displayAccentColor
                        ) {
                            showSetWallpaperDialog = false
                            applyWallpaper(context, scope, currentWallpaper, WallpaperManager.FLAG_SYSTEM)
                        }

                        WallpaperOptionCard(
                            title = "Lock Screen",
                            subtext = "Apply to lock workspace",
                            icon = Icons.Default.Lock,
                            isSystemDark = isSystemDark,
                            activeAccentColor = displayAccentColor
                        ) {
                            showSetWallpaperDialog = false
                            applyWallpaper(context, scope, currentWallpaper, WallpaperManager.FLAG_LOCK)
                        }

                        WallpaperOptionCard(
                            title = "Both Screens",
                            subtext = "Apply to home & lock",
                            icon = Icons.Default.StayCurrentPortrait,
                            isSystemDark = isSystemDark,
                            activeAccentColor = displayAccentColor
                        ) {
                            showSetWallpaperDialog = false
                            applyWallpaper(context, scope, currentWallpaper, WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)
                        }

                        WallpaperOptionCard(
                            title = "Using System Picker",
                            subtext = "Set using system app",
                            icon = Icons.AutoMirrored.Filled.OpenInNew,
                            isSystemDark = isSystemDark,
                            activeAccentColor = displayAccentColor
                        ) {
                            showSetWallpaperDialog = false
                            applyUsingSystem(context, scope, currentWallpaper)
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { showSetWallpaperDialog = false }) {
                                Text("Cancel", color = displayAccentColor, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════
//  Helper Composables
// ══════════════════════════════════════════════════════════

@Composable
private fun GlassCircleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    isSystemDark: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    tint: Color? = null,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.9f else 1f, label = "scale")

    val bgColor = Color.Black.copy(alpha = 0.6f)
    val borderColor = Color.White.copy(alpha = 0.08f)
    val iconColor = tint ?: Color.White

    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
        interactionSource = interactionSource,
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = iconColor,
                modifier = Modifier.size(if (size > 44.dp) 22.dp else 20.dp)
            )
        }
    }
}

@Composable
private fun GlassNavigationButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    isSystemDark: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.88f else 1f, label = "scale")

    val bgColor = Color.Black.copy(alpha = 0.6f)
    val borderColor = Color.White.copy(alpha = 0.08f)
    val tintColor = Color.White

    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
        interactionSource = interactionSource,
        modifier = modifier
            .size(44.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tintColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun StatGridCard(
    title: String,
    items: List<Pair<String, String>>,
    isSystemDark: Boolean,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isSystemDark) Color.White.copy(alpha = 0.02f) else Color.Black.copy(alpha = 0.02f)
    val borderColor = if (isSystemDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)
    val textColor = if (isSystemDark) Color.White else Color.Black
    val secColor = if (isSystemDark) Color.White.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.5f)

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp, letterSpacing = 0.8.sp),
                fontWeight = FontWeight.Bold,
                color = secColor
            )
            Spacer(modifier = Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items.forEach { (label, value) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(label, style = MaterialTheme.typography.bodySmall, color = secColor)
                        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = textColor)
                    }
                }
            }
        }
    }
}

@Composable
private fun WallpaperInfoCard(
    wallpaper: Wallpaper,
    colors: List<String>,
    context: android.content.Context,
    isSystemDark: Boolean,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isSystemDark) Color.White.copy(alpha = 0.02f) else Color.Black.copy(alpha = 0.02f)
    val borderColor = if (isSystemDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)
    val secColor = if (isSystemDark) Color.White.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.5f)
    val textColor = if (isSystemDark) Color.White else Color.Black

    val details = buildList {
        add("Resolution" to wallpaper.resolution)
        add("Orientation" to wallpaper.orientation)
        val style = wallpaper.data?.artStyle
        if (!style.isNullOrBlank()) add("Art Style" to style)
        val series = wallpaper.data?.series
        if (!series.isNullOrBlank()) add("Series" to series)
        val mood = wallpaper.data?.mood
        if (!mood.isNullOrBlank()) add("Mood Theme" to mood)
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "DETAILS",
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp, letterSpacing = 0.8.sp),
                fontWeight = FontWeight.Bold,
                color = secColor
            )
            Spacer(modifier = Modifier.height(14.dp))
            
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                details.forEach { (label, value) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(label, style = MaterialTheme.typography.bodySmall, color = secColor)
                        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = textColor)
                    }
                }
            }

            if (colors.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(borderColor))
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "COLOR PALETTE",
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp, letterSpacing = 0.8.sp),
                    fontWeight = FontWeight.Bold,
                    color = secColor,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy((-12).dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    colors.forEach { hex ->
                        val swatchColor = remember(hex) { parseHexColor(hex, Color.Gray) }
                        val displayHex = remember(hex) { getDisplayHex(hex) }
                        var isPressed by remember { mutableStateOf(false) }
                        val scale by animateFloatAsState(if (isPressed) 0.85f else 1f, label = "scale")

                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                }
                                .clip(CircleShape)
                                .background(swatchColor)
                                .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = {
                                            isPressed = true
                                            tryAwaitRelease()
                                            isPressed = false
                                        },
                                        onTap = {
                                            copyToClipboard(context, "Hex Code", "#$displayHex")
                                        }
                                    )
                                }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WallpaperOptionCard(
    title: String,
    subtext: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSystemDark: Boolean,
    activeAccentColor: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.95f else 1f, label = "scale")

    val bgColor = if (isSystemDark) Color.White.copy(alpha = 0.03f) else Color.Black.copy(alpha = 0.03f)
    val borderColor = if (isSystemDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f)
    val textColor = if (isSystemDark) Color.White else Color.Black
    val secColor = if (isSystemDark) Color.White.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.5f)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
        interactionSource = interactionSource,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(activeAccentColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = activeAccentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = textColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtext,
                    style = MaterialTheme.typography.bodySmall,
                    color = secColor
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = secColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun TagChip(
    label: String,
    isSystemDark: Boolean,
    textColor: Color? = null,
    onClick: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.92f else 1f, label = "scale")

    val bgColor = if (isSystemDark) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.04f)
    val borderColor = if (isSystemDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f)
    val defaultTextColor = if (isSystemDark) Color.White.copy(alpha = 0.75f) else Color.Black.copy(alpha = 0.75f)
    val finalTextColor = textColor ?: defaultTextColor

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
        interactionSource = interactionSource,
        modifier = Modifier
            .height(32.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        Box(Modifier.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
            Text(
                label,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                fontWeight = FontWeight.Medium,
                color = finalTextColor
            )
        }
    }
}

@Composable
private fun SimilarWallpaperCard(
    wallpaper: Wallpaper,
    isSystemDark: Boolean,
    onClick: () -> Unit
) {
    val isDesktop = wallpaper.orientation.equals("Desktop", ignoreCase = true)
    val aspectRatio = if (isDesktop) 1.6f else 0.56f
    val borderColor = if (isSystemDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f)
    val textColor = if (isSystemDark) Color.White.copy(alpha = 0.65f) else Color.Black.copy(alpha = 0.65f)

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.94f else 1f, label = "scale")

    Column(
        Modifier
            .width(if (isDesktop) 170.dp else 105.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.Start
    ) {
        Card(
            Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isSystemDark) Color.White.copy(alpha = 0.03f) else Color.Black.copy(alpha = 0.03f)
            ),
            border = BorderStroke(1.dp, borderColor)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(wallpaper.cacheUrl)
                    .crossfade(true).build(),
                contentDescription = wallpaper.fileName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            getCleanTitle(wallpaper),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = textColor,
            maxLines = 1,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@Composable
private fun WallpaperSelectionOption(
    label: String,
    isSystemDark: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.95f else 1f, label = "scale")

    val bgColor = if (isSystemDark) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.04f)
    val borderColor = if (isSystemDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f)
    val textColor = if (isSystemDark) Color.White else Color.Black

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
        interactionSource = interactionSource,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        Text(
            label, color = textColor,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
        )
    }
}

// ══════════════════════════════════════════════════════════
//  Utility Functions
// ══════════════════════════════════════════════════════════

private fun lerp(start: Dp, end: Dp, fraction: Float): Dp {
    return start + (end - start) * fraction
}

private fun getCleanTitle(wallpaper: Wallpaper): String {
    val metaTitle = wallpaper.data?.title
    if (!metaTitle.isNullOrBlank()) return metaTitle
    return wallpaper.fileName
        .substringBeforeLast(".")
        .replace("_", " ")
        .replace("-", " ")
        .split(" ")
        .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    try {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "$text copied to clipboard!", Toast.LENGTH_SHORT).show()
    } catch (_: Exception) {}
}

private fun parseHexColor(hex: String?, fallback: Color): Color {
    if (hex.isNullOrBlank()) return fallback
    val trimmed = hex.trim().lowercase()
    
    val namedColor = when (trimmed) {
        "red" -> Color(0xFFE53935)
        "blue" -> Color(0xFF1E88E5)
        "green" -> Color(0xFF43A047)
        "black" -> Color(0xFF1A1A1A)
        "white" -> Color(0xFFFFFFFF)
        "gray", "grey" -> Color(0xFF757575)
        "yellow" -> Color(0xFFFFEB3B)
        "orange" -> Color(0xFFFF9800)
        "purple" -> Color(0xFF8E24AA)
        "pink" -> Color(0xFFE91E63)
        "brown" -> Color(0xFF6D4C41)
        "cyan" -> Color(0xFF00ACC1)
        "magenta" -> Color(0xFFD81B60)
        "gold" -> Color(0xFFFFD700)
        "beige" -> Color(0xFFF5F5DC)
        "tan" -> Color(0xFFD2B48C)
        "charcoal" -> Color(0xFF2E2E2E)
        "navy" -> Color(0xFF0D47A1)
        "teal" -> Color(0xFF008080)
        "maroon" -> Color(0xFF800000)
        "amber" -> Color(0xFFFFC107)
        "silver" -> Color(0xFFB0BEC5)
        else -> null
    }
    if (namedColor != null) return namedColor
    
    return try {
        val cleaned = if (trimmed.startsWith("#")) trimmed else "#$trimmed"
        Color(android.graphics.Color.parseColor(cleaned))
    } catch (e: Exception) {
        fallback
    }
}

private fun getDisplayHex(hex: String): String {
    val trimmed = hex.trim().lowercase()
    return when (trimmed) {
        "red" -> "E53935"
        "blue" -> "1E88E5"
        "green" -> "43A047"
        "black" -> "1A1A1A"
        "white" -> "FFFFFF"
        "gray", "grey" -> "757575"
        "yellow" -> "FFEB3B"
        "orange" -> "FF9800"
        "purple" -> "8E24AA"
        "pink" -> "E91E63"
        "brown" -> "6D4C41"
        "cyan" -> "00ACC1"
        "magenta" -> "D81B60"
        "gold" -> "FFD700"
        "beige" -> "F5F5DC"
        "tan" -> "D2B48C"
        "charcoal" -> "2E2E2E"
        "navy" -> "0D47A1"
        "teal" -> "008080"
        "maroon" -> "800000"
        "amber" -> "FFC107"
        "silver" -> "B0BEC5"
        else -> hex.trim().removePrefix("#").uppercase()
    }
}

private fun findVibrantColor(colors: List<String>?, fallback: Color?): Color? {
    if (colors.isNullOrEmpty()) return fallback
    var maxScore = -1f
    var bestColor: Color? = null
    val hsl = FloatArray(3)
    for (hex in colors) {
        try {
            val parsedColor = parseHexColor(hex, Color.Transparent)
            if (parsedColor == Color.Transparent) continue
            val parsed = parsedColor.toArgb()
            ColorUtils.colorToHSL(parsed, hsl)
            val sat = hsl[1]
            val lgt = hsl[2]
            
            // Score based on saturation and lightness
            if (sat < 0.15f || lgt < 0.1f || lgt > 0.9f) continue
            val lgtFactor = if (lgt in 0.35f..0.7f) 1f else 1f - Math.abs(lgt - 0.525f) * 2.2f
            val score = sat * lgtFactor
            
            if (score > maxScore) {
                maxScore = score
                bestColor = parsedColor
            }
        } catch (_: Exception) {}
    }
    return bestColor ?: fallback
}

private fun extractVibrantAccent(bitmap: Bitmap): Color {
    val w = bitmap.width; val h = bitmap.height
    var maxScore = -1f
    var best = android.graphics.Color.parseColor("#C85A17")
    val step = maxOf(1, minOf(w, h) / 30)
    val hsl = FloatArray(3)
    
    for (x in 0 until w step step) {
        for (y in 0 until h step step) {
            val px = bitmap.getPixel(x, y)
            ColorUtils.colorToHSL(px, hsl)
            val sat = hsl[1]
            val lgt = hsl[2]
            
            if (sat < 0.15f || lgt < 0.12f || lgt > 0.88f) continue
            val lgtFactor = 1.0f - Math.abs(lgt - 0.5f) * 1.2f
            val score = sat * lgtFactor
            
            if (score > maxScore) {
                maxScore = score
                best = px
            }
        }
    }
    
    // If no colorful vibrant pixels found, use the average color as a fallback
    if (maxScore < 0.05f) {
        var rSum = 0L; var gSum = 0L; var bSum = 0L; var count = 0
        for (x in 0 until w step step) {
            for (y in 0 until h step step) {
                val px = bitmap.getPixel(x, y)
                rSum += android.graphics.Color.red(px)
                gSum += android.graphics.Color.green(px)
                bSum += android.graphics.Color.blue(px)
                count++
            }
        }
        if (count > 0) {
            best = android.graphics.Color.rgb((rSum / count).toInt(), (gSum / count).toInt(), (bSum / count).toInt())
        }
    }
    
    return Color(best)
}

private fun downloadWallpaper(context: Context, wallpaper: Wallpaper) {
    try {
        val url = wallpaper.mainUrl
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle(wallpaper.data?.title ?: wallpaper.fileName)
            .setDescription("Downloading from Wallwidgy")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES, "Wallwidgy/${wallpaper.fileMainName}")
        (context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
        Toast.makeText(context, "Download started", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun shareWallpaper(context: Context, wallpaper: Wallpaper) {
    try {
        val url = wallpaper.mainUrl
        context.startActivity(Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "Check out this wallpaper: $url")
            }, "Share Wallpaper"
        ))
    } catch (e: Exception) {
        Toast.makeText(context, "Share failed", Toast.LENGTH_SHORT).show()
    }
}

private fun applyWallpaper(
    context: Context,
    scope: kotlinx.coroutines.CoroutineScope,
    wallpaper: Wallpaper,
    flags: Int
) {
    scope.launch(Dispatchers.IO) {
        try {
            val url = wallpaper.mainUrl
            val loader = ImageLoader(context)
            val req = ImageRequest.Builder(context).data(url).allowHardware(false).build()
            val res = loader.execute(req)
            if (res is SuccessResult) {
                val bmp = (res.drawable as? BitmapDrawable)?.bitmap
                if (bmp != null) {
                    val wm = WallpaperManager.getInstance(context)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) wm.setBitmap(bmp, null, true, flags)
                    else wm.setBitmap(bmp)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Wallpaper applied!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

private fun applyUsingSystem(
    context: Context,
    scope: kotlinx.coroutines.CoroutineScope,
    wallpaper: Wallpaper
) {
    Toast.makeText(context, "Preparing system picker...", Toast.LENGTH_SHORT).show()
    scope.launch(Dispatchers.IO) {
        try {
            val url = wallpaper.mainUrl
            val loader = ImageLoader(context)
            val req = ImageRequest.Builder(context).data(url).allowHardware(false).build()
            val res = loader.execute(req)
            if (res is SuccessResult) {
                val bmp = (res.drawable as? BitmapDrawable)?.bitmap
                if (bmp != null) {
                    val cacheFile = java.io.File(context.cacheDir, "temp_wallpaper.jpg")
                    java.io.FileOutputStream(cacheFile).use { out ->
                        bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, out)
                    }
                    
                    val uri = androidx.core.content.FileProvider.getUriForFile(
                        context,
                        "com.notayan.wallwidgy.fileprovider",
                        cacheFile
                    )
                    
                    val intent = Intent(Intent.ACTION_ATTACH_DATA).apply {
                        setDataAndType(uri, "image/*")
                        putExtra("mimeType", "image/*")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    
                    withContext(Dispatchers.Main) {
                        context.startActivity(Intent.createChooser(intent, "Set as Wallpaper"))
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to load wallpaper image", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
