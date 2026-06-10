package com.notayan.wallwidgy.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.notayan.wallwidgy.R

val Aspekta = FontFamily(
    Font(R.font.aspekta_vf, FontWeight.Thin),
    Font(R.font.aspekta_vf, FontWeight.Light),
    Font(R.font.aspekta_vf, FontWeight.Normal),
    Font(R.font.aspekta_vf, FontWeight.Medium),
    Font(R.font.aspekta_vf, FontWeight.SemiBold),
    Font(R.font.aspekta_vf, FontWeight.Bold),
    Font(R.font.aspekta_vf, FontWeight.ExtraBold)
)

private val defaultTypography = Typography()

// Set of Material typography styles using Aspekta font
val Typography = Typography(
    displayLarge = defaultTypography.displayLarge.copy(fontFamily = Aspekta),
    displayMedium = defaultTypography.displayMedium.copy(fontFamily = Aspekta),
    displaySmall = defaultTypography.displaySmall.copy(fontFamily = Aspekta),
    headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = Aspekta),
    headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = Aspekta),
    headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = Aspekta),
    titleLarge = defaultTypography.titleLarge.copy(fontFamily = Aspekta),
    titleMedium = defaultTypography.titleMedium.copy(fontFamily = Aspekta),
    titleSmall = defaultTypography.titleSmall.copy(fontFamily = Aspekta),
    bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = Aspekta),
    bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = Aspekta),
    bodySmall = defaultTypography.bodySmall.copy(fontFamily = Aspekta),
    labelLarge = defaultTypography.labelLarge.copy(fontFamily = Aspekta),
    labelMedium = defaultTypography.labelMedium.copy(fontFamily = Aspekta),
    labelSmall = defaultTypography.labelSmall.copy(fontFamily = Aspekta)
)