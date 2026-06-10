package com.notayan.wallwidgy.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.material3.ColorScheme

private val DarkColorScheme = darkColorScheme(
    primary = WallPrimary,
    onPrimary = WallOnPrimary,
    background = WallBg,
    onBackground = WallText,
    surface = WallSurface,
    onSurface = WallText,
    surfaceVariant = WallSurface,
    onSurfaceVariant = WallTextSecondary,
    outline = WallBorder,
    secondary = WallAccent
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF4C663B),
    onPrimary = Color.White,
    background = Color(0xFFFAF9F6),
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFE2E3D8),
    onSurfaceVariant = Color(0xFF5C5F56),
    outline = Color(0xFF72796D),
    secondary = Color(0xFF54624D)
)

@Composable
fun WallwidgyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    monetEnabled: Boolean = true,
    customAccentColor: Int = 0xFF4C663B.toInt(),
    content: @Composable () -> Unit
) {
    val customColor = Color(customAccentColor)
    val baseColorScheme = when {
        monetEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> getCustomColorScheme(customColor, darkTheme)
    }

    // Adjusting colors to be a bit lighter than the darkest system shade, forcing deep black background in dark mode
    val colorScheme = baseColorScheme.copy(
        background = if (darkTheme) Color(0xFF0A0A0A) else Color(0xFFFAF9F6), // Force exact background color
        surface = baseColorScheme.surfaceVariant, // Lighter than 'surface'
        surfaceVariant = baseColorScheme.secondaryContainer.copy(alpha = 0.5f) // Even lighter for secondary elements
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

private fun getCustomColorScheme(
    seedColor: Color,
    darkTheme: Boolean
): ColorScheme {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(seedColor.toArgb(), hsv)
    val h = hsv[0]
    val s = hsv[1]
    val v = hsv[2]

    val fromHsv = { hue: Float, sat: Float, valVal: Float ->
        Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat.coerceIn(0f, 1f), valVal.coerceIn(0f, 1f))))
    }

    return if (darkTheme) {
        // Dark theme dynamic colors
        val primaryColor = seedColor
        val secondaryColor = fromHsv(h, s * 0.5f, v * 0.8f)
        val tertiaryColor = fromHsv((h + 60f) % 360f, s * 0.6f, v * 0.9f)
        
        DarkColorScheme.copy(
            primary = primaryColor,
            onPrimary = if (primaryColor.luminance() > 0.5f) Color.Black else Color.White,
            secondary = secondaryColor,
            onSecondary = Color.White,
            tertiary = tertiaryColor,
            onTertiary = Color.White,
            primaryContainer = fromHsv(h, s * 0.3f, v * 0.25f),
            onPrimaryContainer = fromHsv(h, s * 0.3f, 0.9f),
            secondaryContainer = fromHsv(h, s * 0.2f, v * 0.2f),
            onSecondaryContainer = fromHsv(h, s * 0.3f, 0.85f),
            tertiaryContainer = fromHsv((h + 60f) % 360f, s * 0.3f, v * 0.25f),
            onTertiaryContainer = fromHsv((h + 60f) % 360f, s * 0.3f, 0.9f),
            surface = Color(0xFF141414), // Premium dark surface
            surfaceVariant = fromHsv(h, s * 0.12f, 0.14f),
            onSurfaceVariant = fromHsv(h, s * 0.15f, 0.85f),
            outline = fromHsv(h, s * 0.25f, 0.45f)
        )
    } else {
        // Light theme dynamic colors
        val primaryColor = seedColor
        val secondaryColor = fromHsv(h, s * 0.4f, v * 0.6f)
        val tertiaryColor = fromHsv((h + 60f) % 360f, s * 0.5f, v * 0.7f)
        
        LightColorScheme.copy(
            primary = primaryColor,
            onPrimary = if (primaryColor.luminance() > 0.5f) Color.Black else Color.White,
            secondary = secondaryColor,
            onSecondary = Color.White,
            tertiary = tertiaryColor,
            onTertiary = Color.White,
            primaryContainer = fromHsv(h, s * 0.15f, 0.96f),
            onPrimaryContainer = fromHsv(h, s * 0.8f, 0.25f),
            secondaryContainer = fromHsv(h, s * 0.1f, 0.94f),
            onSecondaryContainer = fromHsv(h, s * 0.6f, 0.2f),
            tertiaryContainer = fromHsv((h + 60f) % 360f, s * 0.15f, 0.96f),
            onTertiaryContainer = fromHsv((h + 60f) % 360f, s * 0.7f, 0.25f),
            surface = Color.White,
            surfaceVariant = fromHsv(h, s * 0.08f, 0.92f),
            onSurfaceVariant = fromHsv(h, s * 0.6f, 0.25f),
            outline = fromHsv(h, s * 0.3f, 0.5f)
        )
    }
}
