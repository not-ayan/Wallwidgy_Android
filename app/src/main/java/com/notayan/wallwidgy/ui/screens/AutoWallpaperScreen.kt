package com.notayan.wallwidgy.ui.screens

import android.Manifest
import android.app.WallpaperManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.compose.ui.graphics.nativeCanvas
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.notayan.wallwidgy.ui.viewmodel.WallpaperViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AutoWallpaperScreen(
    viewModel: WallpaperViewModel
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    // Rotation Settings from ViewModel
    val enabled by viewModel.rotationEnabled.collectAsState()
    val mode by viewModel.rotationMode.collectAsState()
    val rotationVal by viewModel.rotationValue.collectAsState()
    val duration by viewModel.rotationDuration.collectAsState()
    val target by viewModel.rotationTarget.collectAsState()
    val lastTime by viewModel.rotationLastTime.collectAsState()

    val categories by viewModel.availableCategories.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current

    // Permission state
    var hasStoragePermission by remember {
        mutableStateOf(false)
    }

    fun checkStoragePermission() {
        hasStoragePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            android.os.Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkStoragePermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasStoragePermission = isGranted
        if (isGranted) {
            Toast.makeText(context, "Permission granted. Loading previews...", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Permission denied. Using fallback previews.", Toast.LENGTH_SHORT).show()
        }
    }

    // Wallpaper Drawables
    var homeWallpaperDrawable by remember { mutableStateOf<Drawable?>(null) }
    var lockWallpaperDrawable by remember { mutableStateOf<Drawable?>(null) }
    var isWpLoading by remember { mutableStateOf(false) }

    LaunchedEffect(hasStoragePermission) {
        if (hasStoragePermission) {
            isWpLoading = true
            withContext(Dispatchers.IO) {
                try {
                    val wm = WallpaperManager.getInstance(context)
                    val home = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        wm.getDrawable(WallpaperManager.FLAG_SYSTEM)
                    } else {
                        wm.drawable
                    }
                    val lock = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        wm.getFastDrawable(WallpaperManager.FLAG_LOCK) ?: home
                    } else {
                        home
                    }
                    withContext(Dispatchers.Main) {
                        homeWallpaperDrawable = home
                        lockWallpaperDrawable = lock
                        isWpLoading = false
                    }
                } catch (e: SecurityException) {
                    withContext(Dispatchers.Main) {
                        isWpLoading = false
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        isWpLoading = false
                    }
                }
            }
        }
    }

    // Scroll state
    val scrollState = rememberScrollState()

    // Color Palette Selection Options
    val colorOptions = remember {
        listOf(
            "red" to Color(0xFFE53935),
            "blue" to Color(0xFF1E88E5),
            "green" to Color(0xFF43A047),
            "yellow" to Color(0xFFFFEB3B),
            "orange" to Color(0xFFFF9800),
            "purple" to Color(0xFF8E24AA),
            "pink" to Color(0xFFE91E63),
            "teal" to Color(0xFF008080),
            "cyan" to Color(0xFF00ACC1),
            "black" to Color(0xFF1A1A1A),
            "white" to Color(0xFFFFFFFF)
        )
    }

    // Duration State
    val durationPreset = remember(duration) {
        when (duration) {
            12 -> "12h"
            24 -> "24h"
            48 -> "48h"
            else -> "custom"
        }
    }
    var customHoursText by remember(duration) {
        mutableStateOf(
            if (duration != 12 && duration != 24 && duration != 48) duration.toString() else "6"
        )
    }

    val activeAccentColor = MaterialTheme.colorScheme.primary
    val isSystemDark = isSystemInDarkTheme()
    val bgColor = MaterialTheme.colorScheme.background
    val cardBgColor = MaterialTheme.colorScheme.surfaceVariant
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    val textColor = MaterialTheme.colorScheme.onBackground
    val secTextColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)

    Scaffold(
        containerColor = bgColor,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Auto Rotate",
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                        fontWeight = FontWeight.ExtraLight,
                        color = textColor,
                        letterSpacing = 2.sp
                    )

                    // Enable/Disable Switch on top right
                    Switch(
                        checked = enabled,
                        onCheckedChange = { viewModel.setRotationEnabled(context, it) },
                        modifier = Modifier.align(Alignment.CenterEnd),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = activeAccentColor,
                            checkedTrackColor = activeAccentColor.copy(alpha = 0.4f)
                        )
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(borderColor)
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Live Previews Container (above all)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "TARGET SCREENS",
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp, letterSpacing = 1.sp),
                    fontWeight = FontWeight.Bold,
                    color = secTextColor
                )

                if (!hasStoragePermission) {
                    Surface(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                try {
                                    val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                        data = android.net.Uri.parse("package:${context.packageName}")
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                                    context.startActivity(intent)
                                }
                            } else {
                                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = cardBgColor,
                        border = BorderStroke(1.dp, borderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = activeAccentColor
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Tap to show current wallpaper previews",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = textColor
                                )
                                Text(
                                    "Requires storage access permission.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = secTextColor
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Home Screen Preview
                    val homeSelected = target == "home" || target == "both"
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                val newTarget = when (target) {
                                    "both" -> "lock"
                                    "home" -> "both" // Toggle off but wait, if home selected and we toggle, target becomes empty? Users must select at least one.
                                    "lock" -> "both"
                                    else -> "home"
                                }
                                viewModel.setRotationTarget(newTarget)
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PhoneMockup(
                            isLockscreen = false,
                            wallpaper = homeWallpaperDrawable,
                            selected = homeSelected,
                            accentColor = activeAccentColor,
                            isLoading = isWpLoading
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (homeSelected) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                                contentDescription = null,
                                tint = if (homeSelected) activeAccentColor else secTextColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "Home Screen",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = if (homeSelected) textColor else secTextColor
                            )
                        }
                    }

                    // Lock Screen Preview
                    val lockSelected = target == "lock" || target == "both"
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                val newTarget = when (target) {
                                    "both" -> "home"
                                    "lock" -> "both"
                                    "home" -> "both"
                                    else -> "lock"
                                }
                                viewModel.setRotationTarget(newTarget)
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PhoneMockup(
                            isLockscreen = true,
                            wallpaper = lockWallpaperDrawable,
                            selected = lockSelected,
                            accentColor = activeAccentColor,
                            isLoading = isWpLoading
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (lockSelected) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                                contentDescription = null,
                                tint = if (lockSelected) activeAccentColor else secTextColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "Lock Screen",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = if (lockSelected) textColor else secTextColor
                            )
                        }
                    }
                }
            }

            // Selection Source Options
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "ROTATION SOURCE",
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp, letterSpacing = 1.sp),
                    fontWeight = FontWeight.Bold,
                    color = secTextColor
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SourceCard(
                        title = "Random",
                        description = "Switch completely random wallpapers",
                        selected = mode == "random",
                        icon = Icons.Default.Casino,
                        accentColor = activeAccentColor,
                        modifier = Modifier.fillMaxWidth(0.47f),
                        onClick = {
                            viewModel.setRotationMode("random")
                            viewModel.setRotationValue("")
                        }
                    )

                    SourceCard(
                        title = "Favorites",
                        description = "Only rotate from favorited items",
                        selected = mode == "favorites",
                        icon = Icons.Default.Favorite,
                        accentColor = activeAccentColor,
                        modifier = Modifier.fillMaxWidth(0.47f),
                        onClick = {
                            viewModel.setRotationMode("favorites")
                            viewModel.setRotationValue("")
                        }
                    )

                    SourceCard(
                        title = "Category",
                        description = "Select a specific style category",
                        selected = mode == "category",
                        icon = Icons.Default.Category,
                        accentColor = activeAccentColor,
                        modifier = Modifier.fillMaxWidth(0.47f),
                        onClick = {
                            viewModel.setRotationMode("category")
                            val defaultCategory = categories.firstOrNull() ?: ""
                            viewModel.setRotationValue(defaultCategory)
                        }
                    )

                    SourceCard(
                        title = "Color",
                        description = "Limit rotating wallpapers by color",
                        selected = mode == "color",
                        icon = Icons.Default.Palette,
                        accentColor = activeAccentColor,
                        modifier = Modifier.fillMaxWidth(0.47f),
                        onClick = {
                            viewModel.setRotationMode("color")
                            viewModel.setRotationValue("blue")
                        }
                    )
                }

                // Category Selection Panel
                AnimatedVisibility(
                    visible = mode == "category",
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Text(
                            text = "Select Category",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = textColor,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        var expanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = !expanded }
                            ) {
                                OutlinedTextField(
                                    value = rotationVal.removePrefix("#").uppercase(Locale.ROOT),
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = cardBgColor,
                                        unfocusedContainerColor = cardBgColor,
                                        focusedTextColor = textColor,
                                        unfocusedTextColor = textColor
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false },
                                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                                ) {
                                    categories.forEach { cat ->
                                        val displayCat = cat.removePrefix("#").trim()
                                        DropdownMenuItem(
                                            text = { Text(displayCat.uppercase(Locale.ROOT)) },
                                            onClick = {
                                                viewModel.setRotationValue(cat)
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Color Selection Panel
                AnimatedVisibility(
                    visible = mode == "color",
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Select Color",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = textColor
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            colorOptions.forEach { (name, color) ->
                                val isColorSelected = rotationVal.lowercase(Locale.ROOT) == name.lowercase(Locale.ROOT)
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (isColorSelected) 3.dp else 1.dp,
                                            color = if (isColorSelected) activeAccentColor else Color.White.copy(alpha = 0.2f),
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            viewModel.setRotationValue(name)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isColorSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = if (color == Color.White) Color.Black else Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Duration Interval Selection
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "TIME DURATION",
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp, letterSpacing = 1.sp),
                    fontWeight = FontWeight.Bold,
                    color = secTextColor
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf("12h" to 12, "24h" to 24, "48h" to 48, "custom" to 0).forEach { (label, hrs) ->
                        val isPresetSelected = (label == "custom" && durationPreset == "custom") ||
                                (label != "custom" && durationPreset != "custom" && duration == hrs)

                        Surface(
                            onClick = {
                                if (label != "custom") {
                                    viewModel.setRotationDuration(context, hrs)
                                } else {
                                    val customHrs = customHoursText.toIntOrNull() ?: 6
                                    viewModel.setRotationDuration(context, customHrs)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isPresetSelected) activeAccentColor.copy(alpha = 0.15f) else cardBgColor,
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isPresetSelected) activeAccentColor else borderColor
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (label == "custom") "Custom" else label.uppercase(Locale.ROOT),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isPresetSelected) activeAccentColor else textColor
                                )
                            }
                        }
                    }
                }

                // Custom hours text field
                AnimatedVisibility(
                    visible = durationPreset == "custom",
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    OutlinedTextField(
                        value = customHoursText,
                        onValueChange = { text ->
                            val cleanText = text.filter { it.isDigit() }
                            customHoursText = cleanText
                            val hoursVal = cleanText.toIntOrNull()
                            if (hoursVal != null && hoursVal > 0) {
                                viewModel.setRotationDuration(context, hoursVal)
                            }
                        },
                        label = { Text("Hours Interval") },
                        placeholder = { Text("e.g. 6") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.clearFocus() }
                        ),
                        suffix = { Text("hours") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = cardBgColor,
                            unfocusedContainerColor = cardBgColor,
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor
                        )
                    )
                }
            }

            // Sync Stats / Info
            if (enabled) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = cardBgColor,
                    border = BorderStroke(1.dp, borderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Last rotated:",
                                style = MaterialTheme.typography.bodySmall,
                                color = secTextColor
                            )
                            Text(
                                text = if (lastTime == 0L) "Never" else {
                                    val diff = System.currentTimeMillis() - lastTime
                                    val diffMins = diff / (60 * 1000)
                                    val diffHours = diffMins / 60
                                    when {
                                        diffMins < 1 -> "Just now"
                                        diffHours < 1 -> "$diffMins minutes ago"
                                        else -> "$diffHours hours ago"
                                    }
                                },
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = textColor
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Next rotation in:",
                                style = MaterialTheme.typography.bodySmall,
                                color = secTextColor
                            )
                            Text(
                                text = if (lastTime == 0L) "Pending first run" else {
                                    val elapsed = System.currentTimeMillis() - lastTime
                                    val limit = duration.toLong() * 60 * 60 * 1000
                                    val remaining = limit - elapsed
                                    if (remaining <= 0) "Any moment" else {
                                        val remHours = remaining / (60 * 60 * 1000)
                                        val remMins = (remaining % (60 * 60 * 1000)) / (60 * 1000)
                                        if (remHours > 0) "$remHours hours $remMins mins" else "$remMins minutes"
                                    }
                                },
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = textColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action: Rotate Now
            Button(
                onClick = {
                    viewModel.rotateNow(context)
                    Toast.makeText(context, "Queued background wallpaper update...", Toast.LENGTH_SHORT).show()
                },
                enabled = enabled,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = activeAccentColor,
                    disabledContainerColor = cardBgColor
                ),
                contentPadding = PaddingValues(vertical = 14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = if (enabled) Color.White else secTextColor
                    )
                    Text(
                        "Rotate Now",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (enabled) Color.White else secTextColor
                    )
                }
            }
        }
    }
}

@Composable
fun PhoneMockup(
    isLockscreen: Boolean,
    wallpaper: Drawable?,
    selected: Boolean,
    accentColor: Color,
    isLoading: Boolean
) {
    val frameBorderColor = if (selected) accentColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    val frameBgColor = MaterialTheme.colorScheme.surface

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = frameBgColor,
        border = BorderStroke(
            width = if (selected) 3.dp else 1.5.dp,
            color = frameBorderColor
        ),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.56f)
            .shadow(
                elevation = if (selected) 8.dp else 2.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = if (selected) accentColor else Color.Black,
                spotColor = if (selected) accentColor else Color.Black
            )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background Wallpaper Image
            if (wallpaper != null) {
                // Use a custom drawing method to scale and center-crop the drawable perfectly
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val drawableWidth = wallpaper.intrinsicWidth.toFloat()
                    val drawableHeight = wallpaper.intrinsicHeight.toFloat()

                    if (drawableWidth > 0 && drawableHeight > 0) {
                        val scale = maxOf(width / drawableWidth, height / drawableHeight)
                        val newWidth = drawableWidth * scale
                        val newHeight = drawableHeight * scale
                        val left = (width - newWidth) / 2
                        val top = (height - newHeight) / 2

                        drawContext.canvas.save()
                        drawContext.canvas.translate(left, top)
                        wallpaper.setBounds(0, 0, newWidth.toInt(), newHeight.toInt())
                        wallpaper.draw(drawContext.canvas.nativeCanvas)
                        drawContext.canvas.restore()
                    } else {
                        wallpaper.setBounds(0, 0, width.toInt(), height.toInt())
                        wallpaper.draw(drawContext.canvas.nativeCanvas)
                    }
                }
            } else {
                // Fallback gradient background if permission is not granted or drawable is null
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF2C3E50),
                                    Color(0xFF000000)
                                )
                            )
                        )
                )
            }

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(24.dp),
                    color = accentColor,
                    strokeWidth = 2.dp
                )
            }
        }
    }
}

@Composable
fun SourceCard(
    title: String,
    description: String,
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.95f else 1f, label = "scale")

    val cardBg = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
    val cardBorder = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    val titleColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
    val descColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        interactionSource = interactionSource,
        modifier = modifier
            .height(115.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) accentColor else titleColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = titleColor
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = descColor,
                    maxLines = 2
                )
            }
        }
    }
}
