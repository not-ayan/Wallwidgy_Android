package com.notayan.wallwidgy.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun WallpaperEditor(
    bitmap: Bitmap,
    onCancel: () -> Unit,
    onDone: (Bitmap) -> Unit,
    isSystemDark: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var isProcessing by remember { mutableStateOf(false) }

    var activeTab by remember { mutableStateOf("Brightness") }

    var brightness by remember { mutableFloatStateOf(0f) }
    var contrast by remember { mutableFloatStateOf(1f) }
    var saturation by remember { mutableFloatStateOf(1f) }

    var cropLeft by remember { mutableFloatStateOf(0f) }
    var cropTop by remember { mutableFloatStateOf(0f) }
    var cropRight by remember { mutableFloatStateOf(1f) }
    var cropBottom by remember { mutableFloatStateOf(1f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isSystemDark) Color(0xFF0A0A0A) else Color(0xFFFAF9F6))
            // Consume all pointer/click events to prevent them from bubbling up to details screen full screen click listeners
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { /* Consume clicks */ }
    ) {
        if (isProcessing) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = accentColor)
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Processing image...",
                    color = if (isSystemDark) Color.White else Color.Black,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Edit Wallpaper",
                        color = if (isSystemDark) Color.White else Color.Black,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel",
                            tint = if (isSystemDark) Color.White else Color.Black
                        )
                    }
                }

                // Preview Box (Custom View)
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val bitmapRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
                    val maxAvailableWidth = maxWidth - 24.dp
                    val maxAvailableHeight = maxHeight - 24.dp
                    
                    val containerRatio = maxAvailableWidth.value / maxAvailableHeight.value
                    val (imageWidth, imageHeight) = if (bitmapRatio > containerRatio) {
                        Pair(maxAvailableWidth, maxAvailableWidth / bitmapRatio)
                    } else {
                        Pair(maxAvailableHeight * bitmapRatio, maxAvailableHeight)
                    }

                    Box(
                        modifier = Modifier.size(imageWidth + 24.dp, imageHeight + 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(imageWidth, imageHeight)
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop,
                            colorFilter = ColorFilter.colorMatrix(
                                getAdjustedColorMatrix(brightness, contrast, saturation)
                            )
                        )

                        CropOverlay(
                            left = cropLeft,
                            top = cropTop,
                            right = cropRight,
                            bottom = cropBottom,
                            onCropChanged = { l, t, r, b ->
                                cropLeft = l
                                cropTop = t
                                cropRight = r
                                cropBottom = b
                            },
                            modifier = Modifier
                                .size(imageWidth, imageHeight)
                                .align(Alignment.Center)
                        )
                    }
                }

                // Sliders & Actions Panel
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    color = if (isSystemDark) Color(0xFF141414) else Color(0xFFF2F1EC),
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                    border = BorderStroke(1.dp, if (isSystemDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Tab selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val tabs = listOf("Brightness", "Contrast", "Saturation")
                            tabs.forEach { tab ->
                                val isSelected = activeTab == tab
                                Text(
                                    text = tab,
                                    color = if (isSelected) {
                                        if (isSystemDark) Color.White else Color.Black
                                    } else {
                                        if (isSystemDark) Color.White.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.4f)
                                    },
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp,
                                    modifier = Modifier
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) { activeTab = tab }
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }

                        val activeDisplayValue = remember(activeTab, brightness, contrast, saturation) {
                            getDisplayValue(activeTab, when (activeTab) {
                                "Brightness" -> brightness
                                "Contrast" -> contrast
                                "Saturation" -> saturation
                                else -> 0f
                            })
                        }

                        WheelSlider(
                            value = activeDisplayValue,
                            onValueChange = { newValue ->
                                val floatVal = getValueFromDisplay(activeTab, newValue)
                                when (activeTab) {
                                    "Brightness" -> brightness = floatVal
                                    "Contrast" -> contrast = floatVal
                                    "Saturation" -> saturation = floatVal
                                }
                            },
                            onReset = {
                                when (activeTab) {
                                    "Brightness" -> brightness = 0f
                                    "Contrast" -> contrast = 1f
                                    "Saturation" -> saturation = 1f
                                }
                            },
                            isSystemDark = isSystemDark,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(8.dp))

                        Button(
                            onClick = {
                                isProcessing = true
                                scope.launch {
                                    val edited = withContext(Dispatchers.IO) {
                                        processBitmap(
                                            bitmap,
                                            cropLeft,
                                            cropTop,
                                            cropRight,
                                            cropBottom,
                                            brightness,
                                            contrast,
                                            saturation
                                        )
                                    }
                                    isProcessing = false
                                    onDone(edited)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentColor,
                                contentColor = if (accentColor.luminance() > 0.5f) Color.Black else Color.White
                            ),
                            shape = RoundedCornerShape(28.dp),
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .width(180.dp)
                                .height(56.dp)
                        ) {
                            Text("Done", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WheelSlider(
    value: Int,
    onValueChange: (Int) -> Unit,
    onReset: () -> Unit,
    isSystemDark: Boolean,
    modifier: Modifier = Modifier
) {
    var lastValue by remember { mutableIntStateOf(value) }
    var floatValue by remember { mutableFloatStateOf(value.toFloat()) }

    if (value != lastValue) {
        lastValue = value
        if (value != floatValue.roundToInt()) {
            floatValue = value.toFloat()
        }
    }

    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentValue by rememberUpdatedState(value)

    val containerBg = if (isSystemDark) Color(0xFF1A1C18) else Color(0xFFE2E4DC)
    val buttonBg = if (isSystemDark) Color(0xFF282A24) else Color(0xFFD2D4CA)
    val textColor = if (isSystemDark) Color.White else Color.Black
    val tickColor = if (isSystemDark) Color.White else Color.Black

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(containerBg, RoundedCornerShape(36.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Value Badge
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(buttonBg, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = (if (value > 0) "+$value" else value.toString()),
                color = textColor,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        // Center: Draggable tick marks
        Box(
            modifier = Modifier
                .weight(1f)
                .height(60.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            // No accumulator needed, floatValue holds continuous state
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val tickSpacingPx = 10.dp.toPx()
                            // Natural scroll: dragging left increases value, dragging right decreases it
                            val delta = -dragAmount.x / tickSpacingPx
                            val newFloatValue = (floatValue + delta).coerceIn(-100f, 100f)
                            if (newFloatValue != floatValue) {
                                floatValue = newFloatValue
                                val newIntValue = newFloatValue.roundToInt()
                                if (newIntValue != currentValue) {
                                    currentOnValueChange(newIntValue)
                                }
                            }
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val center = width / 2
                
                val tickSpacing = 10.dp.toPx()
                
                val maxTicks = (center / tickSpacing).toInt() + 2
                val startV = (floatValue - maxTicks).roundToInt().coerceIn(-100, 100)
                val endV = (floatValue + maxTicks).roundToInt().coerceIn(-100, 100)
                
                for (v in startV..endV) {
                    val x = center + (v - floatValue) * tickSpacing
                    if (x in 0f..width) {
                        val isMajor = v % 10 == 0
                        val isMedium = v % 5 == 0
                        val tickHeight = when {
                            isMajor -> height * 0.45f
                            isMedium -> height * 0.3f
                            else -> height * 0.18f
                        }
                        
                        val alpha = when {
                            x < width * 0.1f -> x / (width * 0.1f)
                            x > width * 0.9f -> (width - x) / (width * 0.1f)
                            else -> 1f
                        }
                        
                        val color = if (isMajor) {
                            tickColor.copy(alpha = 0.7f * alpha)
                        } else {
                            tickColor.copy(alpha = 0.35f * alpha)
                        }
                        val strokeWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx()
                        
                        drawLine(
                            color = color,
                            start = Offset(x, (height - tickHeight) / 2),
                            end = Offset(x, (height + tickHeight) / 2),
                            strokeWidth = strokeWidth
                        )
                    }
                }
                
                // Center pointer line
                drawLine(
                    color = tickColor,
                    start = Offset(center, (height - height * 0.65f) / 2),
                    end = Offset(center, (height + height * 0.65f) / 2),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }

        // Right: Reset button
        IconButton(
            onClick = onReset,
            modifier = Modifier
                .size(48.dp)
                .background(buttonBg, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Reset",
                tint = textColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun CropOverlay(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    onCropChanged: (Float, Float, Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentLeft by rememberUpdatedState(left)
    val currentTop by rememberUpdatedState(top)
    val currentRight by rememberUpdatedState(right)
    val currentBottom by rememberUpdatedState(bottom)
    val currentOnCropChanged by rememberUpdatedState(onCropChanged)

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val w = maxWidth
        val h = maxHeight
        
        val density = LocalDensity.current
        val wPx = with(density) { w.toPx() }
        val hPx = with(density) { h.toPx() }
        
        val l = left * wPx
        val t = top * hPx
        val r = right * wPx
        val b = bottom * hPx
        
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Semi-transparent overlay outside the crop rect
            drawRect(Color.Black.copy(alpha = 0.6f), size = Size(wPx, t))
            drawRect(Color.Black.copy(alpha = 0.6f), topLeft = Offset(0f, b), size = Size(wPx, hPx - b))
            drawRect(Color.Black.copy(alpha = 0.6f), topLeft = Offset(0f, t), size = Size(l, b - t))
            drawRect(Color.Black.copy(alpha = 0.6f), topLeft = Offset(r, t), size = Size(wPx - r, b - t))
            
            // Crop rect outline
            drawRect(
                color = Color.White.copy(alpha = 0.8f),
                topLeft = Offset(l, t),
                size = Size(r - l, b - t),
                style = Stroke(width = 1.dp.toPx())
            )
            
            // Draw L-shaped corner handles
            val lLen = 16.dp.toPx()
            val lThick = 3.dp.toPx()
            
            // Top-Left
            drawLine(Color.White, Offset(l, t), Offset(l + lLen, t), lThick)
            drawLine(Color.White, Offset(l, t), Offset(l, t + lLen), lThick)
            
            // Top-Right
            drawLine(Color.White, Offset(r, t), Offset(r - lLen, t), lThick)
            drawLine(Color.White, Offset(r, t), Offset(r, t + lLen), lThick)
            
            // Bottom-Left
            drawLine(Color.White, Offset(l, b), Offset(l + lLen, b), lThick)
            drawLine(Color.White, Offset(l, b), Offset(l, b - lLen), lThick)
            
            // Bottom-Right
            drawLine(Color.White, Offset(r, b), Offset(r - lLen, b), lThick)
            drawLine(Color.White, Offset(r, b), Offset(r, b - lLen), lThick)
        }
        
        val touchSize = 44.dp
        
        // Top-Left Target
        Box(
            modifier = Modifier
                .size(touchSize)
                .offset(w * left - touchSize/2, h * top - touchSize/2)
                .pointerInput(wPx, hPx) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val newL = ((currentLeft * wPx + dragAmount.x) / wPx).coerceIn(0f, currentRight - 0.15f)
                        val newT = ((currentTop * hPx + dragAmount.y) / hPx).coerceIn(0f, currentBottom - 0.15f)
                        currentOnCropChanged(newL, newT, currentRight, currentBottom)
                    }
                }
        )
        
        // Top-Right Target
        Box(
            modifier = Modifier
                .size(touchSize)
                .offset(w * right - touchSize/2, h * top - touchSize/2)
                .pointerInput(wPx, hPx) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val newR = ((currentRight * wPx + dragAmount.x) / wPx).coerceIn(currentLeft + 0.15f, 1f)
                        val newT = ((currentTop * hPx + dragAmount.y) / hPx).coerceIn(0f, currentBottom - 0.15f)
                        currentOnCropChanged(currentLeft, newT, newR, currentBottom)
                    }
                }
        )
        
        // Bottom-Left Target
        Box(
            modifier = Modifier
                .size(touchSize)
                .offset(w * left - touchSize/2, h * bottom - touchSize/2)
                .pointerInput(wPx, hPx) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val newL = ((currentLeft * wPx + dragAmount.x) / wPx).coerceIn(0f, currentRight - 0.15f)
                        val newB = ((currentBottom * hPx + dragAmount.y) / hPx).coerceIn(currentTop + 0.15f, 1f)
                        currentOnCropChanged(newL, currentTop, currentRight, newB)
                    }
                }
        )
        
        // Bottom-Right Target
        Box(
            modifier = Modifier
                .size(touchSize)
                .offset(w * right - touchSize/2, h * bottom - touchSize/2)
                .pointerInput(wPx, hPx) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val newR = ((currentRight * wPx + dragAmount.x) / wPx).coerceIn(currentLeft + 0.15f, 1f)
                        val newB = ((currentBottom * hPx + dragAmount.y) / hPx).coerceIn(currentTop + 0.15f, 1f)
                        currentOnCropChanged(currentLeft, currentTop, newR, newB)
                    }
                }
        )
    }
}

private fun getDisplayValue(tab: String, value: Float): Int {
    return when (tab) {
        "Brightness" -> (value * 200f).toInt().coerceIn(-100, 100)
        "Contrast" -> ((value - 1.0f) * 200f).toInt().coerceIn(-100, 100)
        "Saturation" -> ((value - 1.0f) * 100f).toInt().coerceIn(-100, 100)
        else -> 0
    }
}

private fun getValueFromDisplay(tab: String, displayVal: Int): Float {
    return when (tab) {
        "Brightness" -> (displayVal / 200f).coerceIn(-0.5f, 0.5f)
        "Contrast" -> (1.0f + displayVal / 200f).coerceIn(0.5f, 1.5f)
        "Saturation" -> (1.0f + displayVal / 100f).coerceIn(0.0f, 2.0f)
        else -> 0f
    }
}

private fun getAdjustedColorMatrix(brightness: Float, contrast: Float, saturation: Float): ColorMatrix {
    val s = saturation
    val c = contrast
    val b = brightness
    
    val lumR = 0.213f
    val lumG = 0.715f
    val lumB = 0.072f
    
    val sr = lumR * (1f - s) + s
    val sg = lumG * (1f - s)
    val sb = lumB * (1f - s)
    
    val srG = lumR * (1f - s)
    val sgG = lumG * (1f - s) + s
    val sbG = lumB * (1f - s)
    
    val srB = lumR * (1f - s)
    val sgB = lumG * (1f - s)
    val sbB = lumB * (1f - s) + s
    
    val o = b * 255f + 128f * (1f - c)
    
    return ColorMatrix(
        floatArrayOf(
            c * sr,  c * sg,  c * sb,  0f, o,
            c * srG, c * sgG, c * sbG, 0f, o,
            c * srB, c * sgB, c * sbB, 0f, o,
            0f,      0f,      0f,      1f, 0f
        )
    )
}

private fun processBitmap(
    original: Bitmap,
    cropLeft: Float,
    cropTop: Float,
    cropRight: Float,
    cropBottom: Float,
    brightness: Float,
    contrast: Float,
    saturation: Float
): Bitmap {
    val width = original.width
    val height = original.height
    
    val x = (cropLeft * width).toInt().coerceIn(0, width - 1)
    val y = (cropTop * height).toInt().coerceIn(0, height - 1)
    val w = ((cropRight - cropLeft) * width).toInt().coerceIn(1, width - x)
    val h = ((cropBottom - cropTop) * height).toInt().coerceIn(1, height - y)
    
    val cropped = Bitmap.createBitmap(original, x, y, w, h)
    
    val adjusted = Bitmap.createBitmap(cropped.width, cropped.height, cropped.config ?: Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(adjusted)
    val paint = android.graphics.Paint()
    
    val s = saturation
    val c = contrast
    val b = brightness
    
    val lumR = 0.213f
    val lumG = 0.715f
    val lumB = 0.072f
    
    val sr = lumR * (1f - s) + s
    val sg = lumG * (1f - s)
    val sb = lumB * (1f - s)
    
    val srG = lumR * (1f - s)
    val sgG = lumG * (1f - s) + s
    val sbG = lumB * (1f - s)
    
    val srB = lumR * (1f - s)
    val sgB = lumG * (1f - s)
    val sbB = lumB * (1f - s) + s
    
    val o = b * 255f + 128f * (1f - c)
    
    val androidMatrix = android.graphics.ColorMatrix(
        floatArrayOf(
            c * sr,  c * sg,  c * sb,  0f, o,
            c * srG, c * sgG, c * sbG, 0f, o,
            c * srB, c * sgB, c * sbB, 0f, o,
            0f,      0f,      0f,      1f, 0f
        )
    )
    paint.colorFilter = android.graphics.ColorMatrixColorFilter(androidMatrix)
    canvas.drawBitmap(cropped, 0f, 0f, paint)
    
    if (cropped != original && cropped != adjusted) {
        cropped.recycle()
    }
    
    return adjusted
}
