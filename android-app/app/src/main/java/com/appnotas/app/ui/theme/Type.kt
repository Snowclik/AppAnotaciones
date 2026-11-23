package com.appnotas.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.appnotas.app.R

const val TabularFeatureSettings = "tnum"

@OptIn(ExperimentalTextApi::class)
private fun alloFont(weight: FontWeight, roundness: Float = 56f): Font {
    return Font(
        resId = R.font.allo_variable,
        weight = weight,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(weight.weight),
            FontVariation.Setting("ROND", roundness)
        )
    )
}

val AlloFontFamily = FontFamily(
    alloFont(FontWeight.Normal, roundness = 48f),
    alloFont(FontWeight.Medium, roundness = 56f),
    alloFont(FontWeight.SemiBold, roundness = 64f),
    alloFont(FontWeight.Bold, roundness = 72f),
    alloFont(FontWeight.ExtraBold, roundness = 76f)
)

val AppNotasTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = AlloFontFamily,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.7).sp,
        fontFeatureSettings = TabularFeatureSettings
    ),
    headlineLarge = TextStyle(
        fontFamily = AlloFontFamily,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.6).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = AlloFontFamily,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.35).sp
    ),
    titleLarge = TextStyle(
        fontFamily = AlloFontFamily,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.2).sp
    ),
    titleMedium = TextStyle(
        fontFamily = AlloFontFamily,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = AlloFontFamily,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Normal
    ),
    bodyMedium = TextStyle(
        fontFamily = AlloFontFamily,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Normal
    ),
    bodySmall = TextStyle(
        fontFamily = AlloFontFamily,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Normal
    ),
    labelLarge = TextStyle(
        fontFamily = AlloFontFamily,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = AlloFontFamily,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.25.sp
    ),
    labelSmall = TextStyle(
        fontFamily = AlloFontFamily,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.3.sp
    )
)
