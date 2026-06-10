package com.notayan.wallwidgy.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.luminance
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar

import kotlinx.coroutines.launch
import com.notayan.wallwidgy.ui.viewmodel.WallpaperViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AboutScreen(
    viewModel: WallpaperViewModel,
    onNavigateHome: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val isSystemDark = isSystemInDarkTheme()

    val currentDefaultIndexEnabled by viewModel.defaultIndexEnabled.collectAsState()
    val currentCustomIndices by viewModel.customIndices.collectAsState()
    val currentEnabledCustomIndices by viewModel.enabledCustomIndices.collectAsState()

    var localDefaultIndexEnabled by remember(currentDefaultIndexEnabled) { mutableStateOf(currentDefaultIndexEnabled) }
    var localCustomIndices by remember(currentCustomIndices) { mutableStateOf(currentCustomIndices) }
    var localEnabledCustomIndices by remember(currentEnabledCustomIndices) { mutableStateOf(currentEnabledCustomIndices) }
    
    var newIndexUrl by remember { mutableStateOf("") }

    val currentMonetEnabled by viewModel.monetEnabled.collectAsState()
    val currentAccentColor by viewModel.customAccentColor.collectAsState()
    val recentColors by viewModel.recentColors.collectAsState()

    var localMonetEnabled by remember(currentMonetEnabled) { mutableStateOf(currentMonetEnabled) }
    var localAccentColor by remember(currentAccentColor) { mutableStateOf(currentAccentColor) }
    
    // Check for Birthday (October 31st)
    val isBirthday = remember { isTodayBirthday(10, 31) }
    // Calculate exact age from birthDate Oct 31, 2002
    val creatorAge = remember { calculateAge(2002, 10, 31) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isSystemDark) Color(0xFF0A0A0A) else Color(0xFFFAF9F6))
    ) {
        // 1. Subtle top gradient glow (dynamic Monet primary color)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = if (isSystemDark) 0.08f else 0.05f),
                            Color.Transparent
                        )
                    )
                )
        )

        // 2. Main Scrollable Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Header navigation row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onNavigateBack,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (isSystemDark) 0.15f else 0.25f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = if (isSystemDark) 0.25f else 0.4f)),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Birthday Indicator Card
            if (isBirthday) {
                BirthdayIndicatorCard(isSystemDark)
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Outer Space-y Content sections
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(48.dp)
            ) {
                // SECTION: About Wallwidgy
                SectionContainer(
                    icon = Icons.Default.Info,
                    title = "About Wallwidgy",
                    subtitle = "Learn more about our platform and mission",
                    isSystemDark = isSystemDark
                ) {
                    GlassyCard(isSystemDark = isSystemDark) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "The Platform",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Normal,
                                color = if (isSystemDark) Color.White else Color.Black
                            )
                        }
                        Text(
                            text = "Wallwidgy is a curated collection of high-quality wallpapers designed for enthusiasts who appreciate clean, minimalist, and artistic designs. Our platform focuses on providing a seamless experience for discovering and downloading beautiful wallpapers.",
                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                            fontWeight = FontWeight.Light,
                            color = (if (isSystemDark) Color.White else Color.Black).copy(alpha = 0.8f),
                            modifier = Modifier.padding(bottom = 20.dp)
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            BadgeChip("Minimalist Design", isSystemDark)
                            BadgeChip("High Quality", isSystemDark)
                            BadgeChip("Free to Use", isSystemDark)
                        }
                    }
                }

                // SECTION: About the Creator
                SectionContainer(
                    icon = Icons.Default.Person,
                    title = "About the Creator",
                    subtitle = "The mind behind the pixels",
                    isSystemDark = isSystemDark
                ) {
                    GlassyCard(isSystemDark = isSystemDark) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Ayan",
                                style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
                                fontWeight = FontWeight.Bold,
                                color = if (isSystemDark) Color.White else Color.Black
                            )
                        }

                        Text(
                            text = "A designer from Assam, India trying to make cool stuff that works well.",
                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                            fontWeight = FontWeight.Light,
                            color = (if (isSystemDark) Color.White else Color.Black).copy(alpha = 0.75f),
                            modifier = Modifier.padding(bottom = 20.dp)
                        )

                        Text(
                            text = "CONNECT",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // Connect Links & Info row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ConnectIconButton(
                                    icon = getGithubIcon(MaterialTheme.colorScheme.secondary),
                                    contentDescription = "GitHub",
                                    isSystemDark = isSystemDark
                                ) {
                                    openUrl(context, "https://github.com/not-ayan")
                                }
                                ConnectIconButton(
                                    icon = Icons.Default.Mail,
                                    contentDescription = "Email",
                                    isSystemDark = isSystemDark
                                ) {
                                    openEmail(context, "notayan99@gmail.com")
                                }
                                ConnectIconButton(
                                    icon = Icons.AutoMirrored.Filled.Send, // Using Send as Telegram indicator
                                    contentDescription = "Telegram",
                                    isSystemDark = isSystemDark
                                ) {
                                    openUrl(context, "https://t.me/Not_ayan99")
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                InfoTag(icon = Icons.Outlined.Person, label = "Male", isSystemDark = isSystemDark)
                                InfoTag(icon = Icons.Outlined.CalendarToday, label = "$creatorAge years", isSystemDark = isSystemDark)
                            }
                        }
                    }
                }

                // SECTION: API Development
                SectionContainer(
                    icon = Icons.Default.Code,
                    title = "API Development",
                    subtitle = "Developer tools and integration details",
                    isSystemDark = isSystemDark
                ) {
                    GlassyCard(isSystemDark = isSystemDark) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Developer API",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Normal,
                                color = if (isSystemDark) Color.White else Color.Black
                            )
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (isSystemDark) 0.15f else 0.25f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = if (isSystemDark) 0.25f else 0.4f))
                            ) {
                                Text(
                                    text = "✨ LIVE",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Text(
                            text = "Our comprehensive API is now live! Developers can integrate Wallwidgy's collection into their applications with simple REST endpoints. Get random wallpapers, filter by category or device type - no authentication required.",
                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                            fontWeight = FontWeight.Light,
                            color = (if (isSystemDark) Color.White else Color.Black).copy(alpha = 0.8f),
                            modifier = Modifier.padding(bottom = 20.dp)
                        )

                        // API Doc Button
                        Button(
                            onClick = { openUrl(context, "https://wallwidgy.vercel.app/api") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = (if (isSystemDark) Color.White else Color.Black).copy(alpha = if (isSystemDark) 0.08f else 0.06f),
                                contentColor = if (isSystemDark) Color.White else Color.Black
                            ),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, (if (isSystemDark) Color.White else Color.Black).copy(alpha = if (isSystemDark) 0.2f else 0.15f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Code, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "View API Documentation",
                                    fontWeight = FontWeight.Normal,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Features & Status Cards
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Subcard: Planned Features
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = (if (isSystemDark) Color.White else Color.Black).copy(alpha = 0.02f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, (if (isSystemDark) Color.White else Color.Black).copy(alpha = 0.05f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "Planned Features",
                                            fontWeight = FontWeight.Normal,
                                            color = if (isSystemDark) Color.White else Color.Black,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    
                                    BulletRow("RESTful API endpoints", true, isSystemDark)
                                    BulletRow("Random wallpaper selection", true, isSystemDark)
                                    BulletRow("Category & device filtering", true, isSystemDark)
                                    BulletRow("CORS enabled & documentation", true, isSystemDark)
                                }
                            }

                            // Subcard: Status
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = (if (isSystemDark) Color.White else Color.Black).copy(alpha = 0.02f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, (if (isSystemDark) Color.White else Color.Black).copy(alpha = 0.05f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "Status",
                                            fontWeight = FontWeight.Normal,
                                            color = if (isSystemDark) Color.White else Color.Black,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    
                                    BulletRow("API live: October 2025", true, isSystemDark)
                                    BulletRow("Documentation: Complete", true, isSystemDark)
                                    BulletRow("Rate limiting: Planned", false, isSystemDark)
                                    BulletRow("Authentication: Future", false, isSystemDark)
                                }
                            }
                        }
                    }
                }

                // SECTION: Wallpaper Index
                SectionContainer(
                    icon = Icons.Default.Storage,
                    title = "Wallpaper Index",
                    subtitle = "Manage index repositories",
                    isSystemDark = isSystemDark
                ) {
                    GlassyCard(isSystemDark = isSystemDark) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Section Description
                            Text(
                                text = "Wallwidgy fetches wallpapers from JSON index files. You can toggle the default collection or add your own repositories.",
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                                fontWeight = FontWeight.Light,
                                color = (if (isSystemDark) Color.White else Color.Black).copy(alpha = 0.8f)
                            )

                            // Warning Card
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF57C00).copy(alpha = if (isSystemDark) 0.1f else 0.05f),
                                border = BorderStroke(1.dp, Color(0xFFF57C00).copy(alpha = if (isSystemDark) 0.25f else 0.15f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Warning",
                                        tint = Color(0xFFF57C00),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Turning off or removing an index will clear the favorites from that index.",
                                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 16.sp),
                                        fontWeight = FontWeight.Medium,
                                        color = if (isSystemDark) Color(0xFFFFB74D) else Color(0xFFE65100)
                                    )
                                }
                            }

                            // Default Index Link & Toggle
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isSystemDark) 0.1f else 0.2f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isSystemDark) 0.15f else 0.3f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Default Index",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSystemDark) Color.White else Color.Black
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                        ) {
                                            Text(
                                                text = "SYSTEM",
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "https://raw.githubusercontent.com/not-ayan/storage/main/index.json",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = (if (isSystemDark) Color.White else Color.Black).copy(alpha = 0.5f),
                                        maxLines = 1,
                                        modifier = Modifier.fillMaxWidth().padding(end = 8.dp)
                                    )
                                }
                                val isDefaultIndexToggleEnabled = localCustomIndices.isNotEmpty()
                                Switch(
                                    checked = localDefaultIndexEnabled,
                                    onCheckedChange = { localDefaultIndexEnabled = it },
                                    enabled = isDefaultIndexToggleEnabled,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                    )
                                )
                            }

                            // Custom Index List Section
                            HorizontalDivider(color = (if (isSystemDark) Color.White else Color.Black).copy(alpha = if (isSystemDark) 0.06f else 0.08f))
                            
                            Text(
                                text = "CUSTOM SOURCES",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                letterSpacing = 1.sp
                            )

                            if (localCustomIndices.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No custom sources added yet.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Light,
                                        color = (if (isSystemDark) Color.White else Color.Black).copy(alpha = 0.4f)
                                    )
                                }
                            } else {
                                localCustomIndices.forEach { url ->
                                    val isEnabled = localEnabledCustomIndices.contains(url)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isSystemDark) 0.1f else 0.2f),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isSystemDark) 0.15f else 0.3f),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = url.substringBeforeLast("/").substringAfterLast("/"),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSystemDark) Color.White else Color.Black
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = url,
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                color = (if (isSystemDark) Color.White else Color.Black).copy(alpha = 0.5f),
                                                maxLines = 1,
                                                modifier = Modifier.fillMaxWidth().padding(end = 8.dp)
                                            )
                                        }
                                        
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Switch(
                                                checked = isEnabled,
                                                onCheckedChange = { isChecked ->
                                                    localEnabledCustomIndices = if (isChecked) {
                                                        localEnabledCustomIndices + url
                                                    } else {
                                                        localEnabledCustomIndices - url
                                                    }
                                                },
                                                colors = SwitchDefaults.colors(
                                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                                )
                                            )
                                            
                                            Spacer(modifier = Modifier.width(4.dp))
                                            
                                            IconButton(
                                                onClick = {
                                                    localCustomIndices = localCustomIndices - url
                                                    localEnabledCustomIndices = localEnabledCustomIndices - url
                                                    if (localCustomIndices.isEmpty()) {
                                                        localDefaultIndexEnabled = true
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Remove Index",
                                                    tint = Color.Red.copy(alpha = 0.7f),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Add New Custom Index Section
                            HorizontalDivider(color = (if (isSystemDark) Color.White else Color.Black).copy(alpha = if (isSystemDark) 0.06f else 0.08f))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = newIndexUrl,
                                    onValueChange = { newIndexUrl = it },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp),
                                    placeholder = {
                                        Text(
                                            text = "Paste index.json URL...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Light,
                                            color = (if (isSystemDark) Color.White else Color.Black).copy(alpha = 0.4f)
                                        )
                                    },
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Normal),
                                    shape = RoundedCornerShape(14.dp),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isSystemDark) 0.25f else 0.4f),
                                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isSystemDark) 0.1f else 0.2f),
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isSystemDark) 0.05f else 0.1f),
                                    )
                                )

                                Button(
                                    onClick = {
                                        if (newIndexUrl.isNotBlank()) {
                                            val trimmed = newIndexUrl.trim()
                                            localCustomIndices = localCustomIndices + trimmed
                                            localEnabledCustomIndices = localEnabledCustomIndices + trimmed
                                            newIndexUrl = ""
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.height(52.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Add", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // GitHub Documentation / How to guide link (frontend design styling)
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                onClick = { openUrl(context, "https://github.com/not-ayan/custom_index") },
                                shape = RoundedCornerShape(12.dp),
                                color = (if (isSystemDark) Color.White else Color.Black).copy(alpha = 0.04f),
                                border = BorderStroke(1.dp, (if (isSystemDark) Color.White else Color.Black).copy(alpha = 0.08f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "How to make your own index",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            // Save & Restart Button for Index Changes
                            val indexChanges = localDefaultIndexEnabled != currentDefaultIndexEnabled ||
                                    localCustomIndices != currentCustomIndices ||
                                    localEnabledCustomIndices != currentEnabledCustomIndices
                            if (indexChanges) {
                                HorizontalDivider(color = (if (isSystemDark) Color.White else Color.Black).copy(alpha = if (isSystemDark) 0.06f else 0.08f))

                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            viewModel.saveIndexSettings(localDefaultIndexEnabled, localCustomIndices, localEnabledCustomIndices)
                                            restartApp(context)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Save & Restart", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // SECTION: Accent Color
                SectionContainer(
                    icon = Icons.Default.Palette,
                    title = "Accent Color",
                    subtitle = "Customize the theme accent color",
                    isSystemDark = isSystemDark
                ) {
                    GlassyCard(isSystemDark = isSystemDark) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Description
                            Text(
                                text = "Personalize your app's theme. You can use dynamic Monet colors extracted from your system wallpaper, select a preset color, or pick a custom hue.",
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                                fontWeight = FontWeight.Light,
                                color = (if (isSystemDark) Color.White else Color.Black).copy(alpha = 0.8f)
                            )

                            // 1. Monet Engine Toggle Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isSystemDark) 0.1f else 0.2f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isSystemDark) 0.15f else 0.3f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Monet Theme (Material You)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSystemDark) Color.White else Color.Black
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Extract colors from device wallpaper (Android 12+). Turning it off will let you select and customize accent colors below.",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = (if (isSystemDark) Color.White else Color.Black).copy(alpha = 0.5f)
                                    )
                                }
                                Switch(
                                    checked = localMonetEnabled,
                                    onCheckedChange = { localMonetEnabled = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                    )
                                )
                            }

                            // If Monet is disabled, show Custom Accent selectors
                            if (!localMonetEnabled) {
                                HorizontalDivider(color = (if (isSystemDark) Color.White else Color.Black).copy(alpha = if (isSystemDark) 0.06f else 0.08f))

                                // 2. Presets / Recently Used Colors Row
                                Text(
                                    text = "RECENT & PRESET COLORS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                    letterSpacing = 1.sp
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    recentColors.forEach { colorInt ->
                                        val color = Color(colorInt)
                                        val isSelected = localAccentColor == colorInt
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(color)
                                                .border(
                                                    width = 2.dp,
                                                    color = if (isSelected) (if (isSystemDark) Color.White else Color.Black) else Color.Transparent,
                                                    shape = CircleShape
                                                )
                                                .clickable { localAccentColor = colorInt },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = if (color.luminance() > 0.5f) Color.Black else Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                HorizontalDivider(color = (if (isSystemDark) Color.White else Color.Black).copy(alpha = if (isSystemDark) 0.06f else 0.08f))

                                // 3. Custom Color Picker (Hue Slider)
                                Text(
                                    text = "CUSTOM COLOR PICKER",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                    letterSpacing = 1.sp
                                )                                // We extract the current HSV parameters of localAccentColor
                                val hsvArray = remember(localAccentColor) {
                                    val hsv = FloatArray(3)
                                    android.graphics.Color.colorToHSV(localAccentColor, hsv)
                                    hsv
                                }
                                var hueValue by remember(localAccentColor) { mutableStateOf(hsvArray[0]) }
                                var saturationValue by remember(localAccentColor) { mutableStateOf(hsvArray[1]) }
                                var brightnessValue by remember(localAccentColor) { mutableStateOf(hsvArray[2]) }

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // Row showing the Hex Text and a large color preview rectangle
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color(localAccentColor),
                                            border = BorderStroke(1.dp, (if (isSystemDark) Color.White else Color.Black).copy(alpha = 0.15f)),
                                            modifier = Modifier
                                                .height(56.dp)
                                                .weight(1f)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = String.format("#%06X", 0xFFFFFF and localAccentColor),
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (Color(localAccentColor).luminance() > 0.5f) Color.Black else Color.White
                                                )
                                            }
                                        }
                                    }

                                    // Hue Spectrum Slider Row
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            text = "Hue",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = (if (isSystemDark) Color.White else Color.Black).copy(alpha = 0.6f),
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(28.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val hueGradient = remember {
                                                listOf(
                                                    Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(14.dp)
                                                    .clip(RoundedCornerShape(7.dp))
                                                    .background(Brush.horizontalGradient(hueGradient))
                                            )
                                            Slider(
                                                value = hueValue,
                                                onValueChange = { newHue ->
                                                    hueValue = newHue
                                                    localAccentColor = android.graphics.Color.HSVToColor(floatArrayOf(newHue, saturationValue, brightnessValue))
                                                },
                                                valueRange = 0f..360f,
                                                colors = SliderDefaults.colors(
                                                    activeTrackColor = Color.Transparent,
                                                    inactiveTrackColor = Color.Transparent,
                                                    thumbColor = Color.White
                                                ),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }

                                    // Saturation Slider Row
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            text = "Saturation (Vibrancy)",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = (if (isSystemDark) Color.White else Color.Black).copy(alpha = 0.6f),
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(28.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val saturationGradient = remember(hueValue, brightnessValue) {
                                                listOf(
                                                    Color.hsv(hueValue, 0f, brightnessValue),
                                                    Color.hsv(hueValue, 1f, brightnessValue)
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(14.dp)
                                                    .clip(RoundedCornerShape(7.dp))
                                                    .background(Brush.horizontalGradient(saturationGradient))
                                            )
                                            Slider(
                                                value = saturationValue,
                                                onValueChange = { newSat ->
                                                    saturationValue = newSat
                                                    localAccentColor = android.graphics.Color.HSVToColor(floatArrayOf(hueValue, newSat, brightnessValue))
                                                },
                                                valueRange = 0f..1f,
                                                colors = SliderDefaults.colors(
                                                    activeTrackColor = Color.Transparent,
                                                    inactiveTrackColor = Color.Transparent,
                                                    thumbColor = Color.White
                                                ),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }

                                    // Brightness / Value Slider Row
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            text = "Brightness (Lightness)",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = (if (isSystemDark) Color.White else Color.Black).copy(alpha = 0.6f),
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(28.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val brightnessGradient = remember(hueValue, saturationValue) {
                                                listOf(
                                                    Color.Black,
                                                    Color.hsv(hueValue, saturationValue, 1f)
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(14.dp)
                                                    .clip(RoundedCornerShape(7.dp))
                                                    .background(Brush.horizontalGradient(brightnessGradient))
                                            )
                                            Slider(
                                                value = brightnessValue,
                                                onValueChange = { newVal ->
                                                    brightnessValue = newVal
                                                    localAccentColor = android.graphics.Color.HSVToColor(floatArrayOf(hueValue, saturationValue, newVal))
                                                },
                                                valueRange = 0f..1f,
                                                colors = SliderDefaults.colors(
                                                    activeTrackColor = Color.Transparent,
                                                    inactiveTrackColor = Color.Transparent,
                                                    thumbColor = Color.White
                                                ),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                    }
                                }
                            }
                        }

                        // 4. Save & Restart Button (always shown if values differ from saved values)
                            val hasChanges = localMonetEnabled != currentMonetEnabled || localAccentColor != currentAccentColor
                            if (hasChanges) {
                                HorizontalDivider(color = (if (isSystemDark) Color.White else Color.Black).copy(alpha = if (isSystemDark) 0.06f else 0.08f))

                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            viewModel.saveThemeSettings(localMonetEnabled, localAccentColor)
                                            restartApp(context)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Save & Restart", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // SECTION: Legal & Privacy
                SectionContainer(
                    icon = Icons.Default.Shield,
                    title = "Legal & Privacy",
                    subtitle = "Important information about our terms and policies",
                    isSystemDark = isSystemDark
                ) {
                    GlassyCard(isSystemDark = isSystemDark) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            LegalItem(
                                icon = Icons.Default.Shield,
                                title = "Copyright & Usage",
                                description = "We respect intellectual property rights. All wallpapers are provided for personal use and are taken from Pinterest or similar sources. If you believe any content infringes on your copyright, please contact us for prompt removal.",
                                isSystemDark = isSystemDark
                            )
                            HorizontalDivider(color = (if (isSystemDark) Color.White else Color.Black).copy(alpha = if (isSystemDark) 0.06f else 0.08f))
                            LegalItem(
                                icon = Icons.Default.Info,
                                title = "Privacy Policy",
                                description = "We dont collect any data other than user analytics.",
                                isSystemDark = isSystemDark
                            )
                            HorizontalDivider(color = (if (isSystemDark) Color.White else Color.Black).copy(alpha = if (isSystemDark) 0.06f else 0.08f))
                            LegalItem(
                                icon = Icons.Default.Link,
                                title = "Service Terms",
                                description = "This service is provided \"as is\" without warranties. We strive for reliability but are not responsible for any damages arising from platform use.",
                                isSystemDark = isSystemDark
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
private fun SectionContainer(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isSystemDark: Boolean,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (isSystemDark) 0.15f else 0.25f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = if (isSystemDark) 0.25f else 0.4f)),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSystemDark) Color.White else Color.Black
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Light,
                    color = (if (isSystemDark) Color.White else Color.Black).copy(alpha = 0.6f)
                )
            }
        }
        content()
    }
}

@Composable
private fun GlassyCard(
    isSystemDark: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = if (isSystemDark) 0.15f else 0.25f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isSystemDark) 0.15f else 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            content = content
        )
    }
}

@Composable
private fun BadgeChip(label: String, isSystemDark: Boolean) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = if (isSystemDark) 0.15f else 0.3f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = if (isSystemDark) 0.25f else 0.4f))
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Light,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
private fun InfoTag(icon: ImageVector, label: String, isSystemDark: Boolean) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = if (isSystemDark) 0.12f else 0.2f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = if (isSystemDark) 0.3f else 0.5f))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = icon, 
                contentDescription = null, 
                tint = MaterialTheme.colorScheme.tertiary, 
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = if (isSystemDark) Color.White else Color.Black
            )
        }
    }
}

@Composable
private fun ConnectIconButton(
    icon: ImageVector,
    contentDescription: String,
    isSystemDark: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = if (isSystemDark) 0.15f else 0.3f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = if (isSystemDark) 0.25f else 0.4f)),
        modifier = Modifier.size(38.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun PulsingDot() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulsingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutLinearInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    
    Box(
        modifier = Modifier
            .size(8.dp)
            .graphicsLayer { alpha = pulsingAlpha }
            .clip(CircleShape)
            .background(Color(0xFFC7F33C)) // Glowing NeonLime
    )
}

@Composable
private fun BirthdayIndicatorCard(isSystemDark: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "birthday")
    val indicatorAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = if (isSystemDark) 0.1f else 0.2f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = if (isSystemDark) 0.2f else 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = indicatorAlpha }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text("🎉", fontSize = 20.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "It's my birthday today!",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (isSystemDark) Color.White else Color.Black
            )
        }
    }
}

@Composable
private fun BulletRow(text: String, completed: Boolean, isSystemDark: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Icon(
            imageVector = if (completed) Icons.Default.CheckCircle else Icons.Default.Schedule,
            contentDescription = null,
            tint = if (completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Light,
            color = (if (isSystemDark) Color.White else Color.Black).copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun LegalItem(
    icon: ImageVector,
    title: String,
    description: String,
    isSystemDark: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (isSystemDark) Color.White else Color.Black
            )
        }
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 18.sp),
            fontWeight = FontWeight.Light,
            color = (if (isSystemDark) Color.White else Color.Black).copy(alpha = 0.7f),
            modifier = Modifier.padding(start = 26.dp)
        )
    }
}

private fun openUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Could not open browser", Toast.LENGTH_SHORT).show()
    }
}

private fun openEmail(context: Context, email: String) {
    try {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email"))
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Could not open email app", Toast.LENGTH_SHORT).show()
    }
}

private fun calculateAge(birthYear: Int, birthMonth: Int, birthDay: Int): Int {
    val today = Calendar.getInstance()
    val birth = Calendar.getInstance().apply {
        set(birthYear, birthMonth - 1, birthDay)
    }
    var age = today.get(Calendar.YEAR) - birth.get(Calendar.YEAR)
    if (today.get(Calendar.MONTH) < birth.get(Calendar.MONTH) || 
        (today.get(Calendar.MONTH) == birth.get(Calendar.MONTH) && today.get(Calendar.DAY_OF_MONTH) < birth.get(Calendar.DAY_OF_MONTH))) {
        age--
    }
    return age
}

private fun isTodayBirthday(birthMonth: Int, birthDay: Int): Boolean {
    val today = Calendar.getInstance()
    return today.get(Calendar.MONTH) == (birthMonth - 1) && 
           today.get(Calendar.DAY_OF_MONTH) == birthDay
}

private fun restartApp(context: Context) {
    val packageManager = context.packageManager
    val intent = packageManager.getLaunchIntentForPackage(context.packageName)
    if (intent != null) {
        val mainIntent = Intent.makeRestartActivityTask(intent.component)
        context.startActivity(mainIntent)
        Runtime.getRuntime().exit(0)
    }
}

private fun getGithubIcon(color: Color): ImageVector {
    return ImageVector.Builder(
        name = "Github",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(
        fill = SolidColor(color),
        pathFillType = PathFillType.NonZero
    ) {
        moveTo(12f, 2f)
        curveTo(6.477f, 2f, 2f, 6.484f, 2f, 12.017f)
        curveTo(2f, 16.446f, 4.87f, 20.203f, 8.866f, 21.54f)
        curveTo(9.366f, 21.634f, 9.549f, 21.321f, 9.549f, 21.054f)
        curveTo(9.549f, 20.819f, 9.54f, 20.196f, 9.535f, 19.373f)
        curveTo(6.753f, 19.977f, 6.167f, 18.03f, 6.167f, 18.03f)
        curveTo(5.713f, 16.877f, 5.057f, 16.57f, 5.057f, 16.57f)
        curveTo(4.148f, 15.948f, 5.126f, 15.961f, 5.126f, 15.961f)
        curveTo(6.13f, 16.032f, 6.658f, 16.995f, 6.658f, 16.995f)
        curveTo(7.55f, 18.528f, 8.998f, 18.084f, 9.569f, 17.828f)
        curveTo(9.66f, 17.181f, 9.919f, 16.74f, 10.206f, 16.489f)
        curveTo(7.985f, 16.237f, 5.65f, 15.377f, 5.65f, 11.537f)
        curveTo(5.65f, 10.443f, 6.04f, 9.548f, 6.682f, 8.85f)
        curveTo(6.579f, 8.597f, 6.236f, 7.577f, 6.78f, 6.197f)
        curveTo(6.78f, 6.197f, 7.62f, 5.927f, 9.53f, 7.222f)
        curveTo(10.328f, 7.001f, 11.183f, 6.89f, 12.033f, 6.886f)
        curveTo(12.882f, 6.89f, 13.737f, 7.001f, 14.538f, 7.222f)
        curveTo(16.446f, 5.927f, 17.284f, 6.197f, 17.284f, 6.197f)
        curveTo(17.83f, 7.577f, 17.487f, 8.597f, 17.385f, 8.85f)
        curveTo(18.029f, 9.548f, 18.417f, 10.443f, 18.417f, 11.537f)
        curveTo(18.417f, 15.387f, 16.078f, 16.234f, 13.85f, 16.48f)
        curveTo(14.21f, 16.79f, 14.53f, 17.4f, 14.53f, 18.327f)
        curveTo(14.53f, 19.645f, 14.518f, 20.707f, 14.518f, 21.025f)
        curveTo(14.518f, 21.295f, 14.699f, 21.61f, 15.207f, 21.51f)
        curveTo(19.201f, 20.177f, 22f, 16.429f, 22f, 12.017f)
        curveTo(22f, 6.484f, 17.522f, 2f, 12f, 2f)
        close()
    }.build()
}
