package com.appnotas.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

const val TabularFeatureSettings = "tnum"

val AppNotasTypography = Typography(
    displaySmall = TextStyle(
        fontSize = 34.sp,
        lineHeight = 40.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.6).sp,
        fontFeatureSettings = TabularFeatureSettings
    ),
    headlineLarge = TextStyle(
        fontSize = 30.sp,
        lineHeight = 36.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontSize = 24.sp,
        lineHeight = 30.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.3).sp
    ),
    titleLarge = TextStyle(
        fontSize = 20.sp,
        lineHeight = 26.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.15).sp
    ),
    titleMedium = TextStyle(
        fontSize = 16.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Normal
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Normal
    ),
    bodySmall = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Normal
    ),
    labelLarge = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold,
        fontFeatureSettings = TabularFeatureSettings
    ),
    labelMedium = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium,
        fontFeatureSettings = TabularFeatureSettings
    ),
    labelSmall = TextStyle(
        fontSize = 11.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.Medium
    )
)
