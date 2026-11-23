package com.appnotas.app.ui.theme

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

data class AppPalettePreset(
    val id: String,
    val label: String,
    val tagline: String,
    val previewColors: List<Color>,
    val lightScheme: ColorScheme?,
    val darkScheme: ColorScheme?
)

private val AuroraLight = lightColorScheme(
    primary = Color(0xFF0F766E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCCFBF1),
    onPrimaryContainer = Color(0xFF042F2E),
    secondary = Color(0xFF2563EB),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDBEAFE),
    onSecondaryContainer = Color(0xFF0F172A),
    tertiary = Color(0xFFD97706),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF451A03),
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF450A0A),
    background = Color(0xFFF3F8F7),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFF3F8F7),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE0ECE9),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFF64748B),
    outlineVariant = Color(0xFFCBD5E1),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF8FBFA),
    surfaceContainer = Color(0xFFECF3F1),
    surfaceContainerHigh = Color(0xFFE2ECE9),
    surfaceContainerHighest = Color(0xFFD7E5E1)
)

private val AuroraDark = darkColorScheme(
    primary = Color(0xFF5EEAD4),
    onPrimary = Color(0xFF042F2E),
    primaryContainer = Color(0xFF115E59),
    onPrimaryContainer = Color(0xFFCCFBF1),
    secondary = Color(0xFF93C5FD),
    onSecondary = Color(0xFF0F172A),
    secondaryContainer = Color(0xFF1E3A8A),
    onSecondaryContainer = Color(0xFFDBEAFE),
    tertiary = Color(0xFFFCD34D),
    onTertiary = Color(0xFF451A03),
    tertiaryContainer = Color(0xFF78350F),
    onTertiaryContainer = Color(0xFFFEF3C7),
    error = Color(0xFFFCA5A5),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2),
    background = Color(0xFF0A1214),
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF0A1214),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF64748B),
    outlineVariant = Color(0xFF283548),
    surfaceContainerLowest = Color(0xFF060B0D),
    surfaceContainerLow = Color(0xFF101A1D),
    surfaceContainer = Color(0xFF152226),
    surfaceContainerHigh = Color(0xFF1D2D32),
    surfaceContainerHighest = Color(0xFF263940)
)

private val EmeraldLight = lightColorScheme(
    primary = Color(0xFF047857),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF022C22),
    secondary = Color(0xFF0284C7),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF082F49),
    tertiary = Color(0xFF16A34A),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDCFCE7),
    onTertiaryContainer = Color(0xFF052E16),
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF450A0A),
    background = Color(0xFFF2FAF6),
    onBackground = Color(0xFF0B1F17),
    surface = Color(0xFFF2FAF6),
    onSurface = Color(0xFF0B1F17),
    surfaceVariant = Color(0xFFDCF0E6),
    onSurfaceVariant = Color(0xFF3E564B),
    outline = Color(0xFF647D70),
    outlineVariant = Color(0xFFC3DFD1),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7FCF9),
    surfaceContainer = Color(0xFFE9F5EE),
    surfaceContainerHigh = Color(0xFFDFF0E7),
    surfaceContainerHighest = Color(0xFFD3E9DD)
)

private val EmeraldDark = darkColorScheme(
    primary = Color(0xFF34D399),
    onPrimary = Color(0xFF022C22),
    primaryContainer = Color(0xFF065F46),
    onPrimaryContainer = Color(0xFFD1FAE5),
    secondary = Color(0xFF7DD3FC),
    onSecondary = Color(0xFF082F49),
    secondaryContainer = Color(0xFF075985),
    onSecondaryContainer = Color(0xFFE0F2FE),
    tertiary = Color(0xFF86EFAC),
    onTertiary = Color(0xFF052E16),
    tertiaryContainer = Color(0xFF166534),
    onTertiaryContainer = Color(0xFFDCFCE7),
    error = Color(0xFFFCA5A5),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2),
    background = Color(0xFF08130F),
    onBackground = Color(0xFFECFDF5),
    surface = Color(0xFF08130F),
    onSurface = Color(0xFFECFDF5),
    surfaceVariant = Color(0xFF1B2E26),
    onSurfaceVariant = Color(0xFF94B4A5),
    outline = Color(0xFF5D7A6C),
    outlineVariant = Color(0xFF233A30),
    surfaceContainerLowest = Color(0xFF050C09),
    surfaceContainerLow = Color(0xFF0E1B16),
    surfaceContainer = Color(0xFF13241D),
    surfaceContainerHigh = Color(0xFF1A2F26),
    surfaceContainerHighest = Color(0xFF223C31)
)

private val IndigoLight = lightColorScheme(
    primary = Color(0xFF4338CA),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = Color(0xFF1E1B4B),
    secondary = Color(0xFF0EA5E9),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF0C4A6E),
    tertiary = Color(0xFFDB2777),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFCE7F3),
    onTertiaryContainer = Color(0xFF500724),
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF450A0A),
    background = Color(0xFFF5F6FF),
    onBackground = Color(0xFF11142B),
    surface = Color(0xFFF5F6FF),
    onSurface = Color(0xFF11142B),
    surfaceVariant = Color(0xFFE4E6FA),
    onSurfaceVariant = Color(0xFF494D6E),
    outline = Color(0xFF6B7094),
    outlineVariant = Color(0xFFCCD0EE),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF9FAFF),
    surfaceContainer = Color(0xFFECEEFD),
    surfaceContainerHigh = Color(0xFFE3E6FA),
    surfaceContainerHighest = Color(0xFFD8DCF7)
)

private val IndigoDark = darkColorScheme(
    primary = Color(0xFFA5B4FC),
    onPrimary = Color(0xFF1E1B4B),
    primaryContainer = Color(0xFF3730A3),
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = Color(0xFF7DD3FC),
    onSecondary = Color(0xFF0C4A6E),
    secondaryContainer = Color(0xFF0369A1),
    onSecondaryContainer = Color(0xFFE0F2FE),
    tertiary = Color(0xFFF9A8D4),
    onTertiary = Color(0xFF500724),
    tertiaryContainer = Color(0xFF9D174D),
    onTertiaryContainer = Color(0xFFFCE7F3),
    error = Color(0xFFFCA5A5),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2),
    background = Color(0xFF0B0C1A),
    onBackground = Color(0xFFEEF2FF),
    surface = Color(0xFF0B0C1A),
    onSurface = Color(0xFFEEF2FF),
    surfaceVariant = Color(0xFF20233D),
    onSurfaceVariant = Color(0xFFA5AACF),
    outline = Color(0xFF6A6F96),
    outlineVariant = Color(0xFF2A2E4F),
    surfaceContainerLowest = Color(0xFF070812),
    surfaceContainerLow = Color(0xFF121426),
    surfaceContainer = Color(0xFF171A30),
    surfaceContainerHigh = Color(0xFF1E223D),
    surfaceContainerHighest = Color(0xFF272B4B)
)

private val CoralLight = lightColorScheme(
    primary = Color(0xFFC2410C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFEDD5),
    onPrimaryContainer = Color(0xFF431407),
    secondary = Color(0xFFE11D48),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE4E6),
    onSecondaryContainer = Color(0xFF4C0519),
    tertiary = Color(0xFF0F766E),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFCCFBF1),
    onTertiaryContainer = Color(0xFF042F2E),
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF450A0A),
    background = Color(0xFFFFF7F4),
    onBackground = Color(0xFF29140E),
    surface = Color(0xFFFFF7F4),
    onSurface = Color(0xFF29140E),
    surfaceVariant = Color(0xFFFBE5DC),
    onSurfaceVariant = Color(0xFF6D4C41),
    outline = Color(0xFF8C695E),
    outlineVariant = Color(0xFFEED0C4),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFBF9),
    surfaceContainer = Color(0xFFFCEEE8),
    surfaceContainerHigh = Color(0xFFF9E5DD),
    surfaceContainerHighest = Color(0xFFF4DCD2)
)

private val CoralDark = darkColorScheme(
    primary = Color(0xFFFDBA74),
    onPrimary = Color(0xFF431407),
    primaryContainer = Color(0xFF9A3412),
    onPrimaryContainer = Color(0xFFFFEDD5),
    secondary = Color(0xFFFDA4AF),
    onSecondary = Color(0xFF4C0519),
    secondaryContainer = Color(0xFF9F1239),
    onSecondaryContainer = Color(0xFFFFE4E6),
    tertiary = Color(0xFF5EEAD4),
    onTertiary = Color(0xFF042F2E),
    tertiaryContainer = Color(0xFF115E59),
    onTertiaryContainer = Color(0xFFCCFBF1),
    error = Color(0xFFFCA5A5),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2),
    background = Color(0xFF160D0B),
    onBackground = Color(0xFFFFF1EC),
    surface = Color(0xFF160D0B),
    onSurface = Color(0xFFFFF1EC),
    surfaceVariant = Color(0xFF33201B),
    onSurfaceVariant = Color(0xFFD4B2A7),
    outline = Color(0xFF8C6D63),
    outlineVariant = Color(0xFF422A23),
    surfaceContainerLowest = Color(0xFF0F0806),
    surfaceContainerLow = Color(0xFF1E1310),
    surfaceContainer = Color(0xFF261814),
    surfaceContainerHigh = Color(0xFF301F1A),
    surfaceContainerHighest = Color(0xFF3C2721)
)

private val AmberLight = lightColorScheme(
    primary = Color(0xFFB45309),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFEF3C7),
    onPrimaryContainer = Color(0xFF451A03),
    secondary = Color(0xFF4F46E5),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0E7FF),
    onSecondaryContainer = Color(0xFF1E1B4B),
    tertiary = Color(0xFF059669),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD1FAE5),
    onTertiaryContainer = Color(0xFF022C22),
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF450A0A),
    background = Color(0xFFFFFBF2),
    onBackground = Color(0xFF241B08),
    surface = Color(0xFFFFFBF2),
    onSurface = Color(0xFF241B08),
    surfaceVariant = Color(0xFFF7ECCD),
    onSurfaceVariant = Color(0xFF665632),
    outline = Color(0xFF87754D),
    outlineVariant = Color(0xFFE6D6AE),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFDF7),
    surfaceContainer = Color(0xFFFAF2DC),
    surfaceContainerHigh = Color(0xFFF5EAD0),
    surfaceContainerHighest = Color(0xFFEFE1C1)
)

private val AmberDark = darkColorScheme(
    primary = Color(0xFFFCD34D),
    onPrimary = Color(0xFF451A03),
    primaryContainer = Color(0xFF92400E),
    onPrimaryContainer = Color(0xFFFEF3C7),
    secondary = Color(0xFFA5B4FC),
    onSecondary = Color(0xFF1E1B4B),
    secondaryContainer = Color(0xFF3730A3),
    onSecondaryContainer = Color(0xFFE0E7FF),
    tertiary = Color(0xFF34D399),
    onTertiary = Color(0xFF022C22),
    tertiaryContainer = Color(0xFF065F46),
    onTertiaryContainer = Color(0xFFD1FAE5),
    error = Color(0xFFFCA5A5),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2),
    background = Color(0xFF141007),
    onBackground = Color(0xFFFFFBEB),
    surface = Color(0xFF141007),
    onSurface = Color(0xFFFFFBEB),
    surfaceVariant = Color(0xFF302715),
    onSurfaceVariant = Color(0xFFCCC09E),
    outline = Color(0xFF877A5A),
    outlineVariant = Color(0xFF3D321B),
    surfaceContainerLowest = Color(0xFF0D0A04),
    surfaceContainerLow = Color(0xFF1C170B),
    surfaceContainer = Color(0xFF241D0F),
    surfaceContainerHigh = Color(0xFF2E2514),
    surfaceContainerHighest = Color(0xFF382E19)
)

val AppPalettes: List<AppPalettePreset> = listOf(
    AppPalettePreset(
        id = "aurora",
        label = "Aurora",
        tagline = "Turquesa profundo + dorado",
        previewColors = listOf(Color(0xFF0F766E), Color(0xFF5EEAD4), Color(0xFFD97706)),
        lightScheme = AuroraLight,
        darkScheme = AuroraDark
    ),
    AppPalettePreset(
        id = "emerald",
        label = "Esmeralda",
        tagline = "Verde caja y comercio",
        previewColors = listOf(Color(0xFF047857), Color(0xFF34D399), Color(0xFF0284C7)),
        lightScheme = EmeraldLight,
        darkScheme = EmeraldDark
    ),
    AppPalettePreset(
        id = "indigo",
        label = "Índigo",
        tagline = "Estudio nocturno nítido",
        previewColors = listOf(Color(0xFF4338CA), Color(0xFFA5B4FC), Color(0xFFDB2777)),
        lightScheme = IndigoLight,
        darkScheme = IndigoDark
    ),
    AppPalettePreset(
        id = "coral",
        label = "Terracota",
        tagline = "Cálido artesanal vibrante",
        previewColors = listOf(Color(0xFFC2410C), Color(0xFFFDBA74), Color(0xFF0F766E)),
        lightScheme = CoralLight,
        darkScheme = CoralDark
    ),
    AppPalettePreset(
        id = "amber",
        label = "Ámbar",
        tagline = "Cítrico luminoso de ventas",
        previewColors = listOf(Color(0xFFB45309), Color(0xFFFCD34D), Color(0xFF4F46E5)),
        lightScheme = AmberLight,
        darkScheme = AmberDark
    ),
    AppPalettePreset(
        id = "dynamic",
        label = "Material You",
        tagline = "Colores de tu fondo Android",
        previewColors = listOf(Color(0xFF6750A4), Color(0xFF625B71), Color(0xFF7D5260)),
        lightScheme = null,
        darkScheme = null
    )
)

private fun applyPureBlack(scheme: ColorScheme): ColorScheme = scheme.copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF07080A),
    surfaceContainer = Color(0xFF0E1014),
    surfaceContainerHigh = Color(0xFF15181E),
    surfaceContainerHighest = Color(0xFF1E222A)
)

@Composable
private fun rememberAnimatedColor(target: Color): Color {
    val animated by animateColorAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 240),
        label = "themeColorTransition"
    )
    return animated
}

@Composable
fun AppNotasTheme(
    paletteId: String = "aurora",
    themeMode: String = "system",
    pureBlack: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> systemDark
    }

    val preset = AppPalettes.firstOrNull { it.id == paletteId } ?: AppPalettes.first()
    val baseScheme = when {
        preset.id == "dynamic" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (useDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        preset.id == "dynamic" -> if (useDark) AuroraDark else AuroraLight
        useDark -> preset.darkScheme ?: AuroraDark
        else -> preset.lightScheme ?: AuroraLight
    }

    val resolvedScheme = if (useDark && pureBlack) applyPureBlack(baseScheme) else baseScheme

    val smoothScheme = resolvedScheme.copy(
        primary = rememberAnimatedColor(resolvedScheme.primary),
        primaryContainer = rememberAnimatedColor(resolvedScheme.primaryContainer),
        secondary = rememberAnimatedColor(resolvedScheme.secondary),
        secondaryContainer = rememberAnimatedColor(resolvedScheme.secondaryContainer),
        tertiary = rememberAnimatedColor(resolvedScheme.tertiary),
        tertiaryContainer = rememberAnimatedColor(resolvedScheme.tertiaryContainer),
        background = rememberAnimatedColor(resolvedScheme.background),
        surface = rememberAnimatedColor(resolvedScheme.surface),
        surfaceContainerLowest = rememberAnimatedColor(resolvedScheme.surfaceContainerLowest),
        surfaceContainerLow = rememberAnimatedColor(resolvedScheme.surfaceContainerLow),
        surfaceContainer = rememberAnimatedColor(resolvedScheme.surfaceContainer),
        surfaceContainerHigh = rememberAnimatedColor(resolvedScheme.surfaceContainerHigh),
        surfaceContainerHighest = rememberAnimatedColor(resolvedScheme.surfaceContainerHighest)
    )

    MaterialTheme(
        colorScheme = smoothScheme,
        shapes = AppNotasShapes,
        typography = AppNotasTypography
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = smoothScheme.background,
            contentColor = smoothScheme.onBackground,
            content = content
        )
    }
}
