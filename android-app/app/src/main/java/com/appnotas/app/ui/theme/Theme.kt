package com.appnotas.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightFallbackColors = lightColorScheme(
    primary = Color(0xFF196B37),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFA2F3B1),
    onPrimaryContainer = Color(0xFF00210B),
    secondary = Color(0xFF4D6352),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD0E8D2),
    onSecondaryContainer = Color(0xFF0B1F11),
    tertiary = Color(0xFF366470),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFBAEAF7),
    onTertiaryContainer = Color(0xFF001F27),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF6FBF2),
    onBackground = Color(0xFF181D18),
    surface = Color(0xFFF6FBF2),
    onSurface = Color(0xFF181D18),
    surfaceVariant = Color(0xFFDDE5DA),
    onSurfaceVariant = Color(0xFF414941),
    outline = Color(0xFF717970),
    outlineVariant = Color(0xFFC1C9BE),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF0F5EC),
    surfaceContainer = Color(0xFFEAEFE6),
    surfaceContainerHigh = Color(0xFFE5EAE1),
    surfaceContainerHighest = Color(0xFFDFE4DB)
)

private val DarkFallbackColors = darkColorScheme(
    primary = Color(0xFF87D697),
    onPrimary = Color(0xFF003917),
    primaryContainer = Color(0xFF005224),
    onPrimaryContainer = Color(0xFFA2F3B1),
    secondary = Color(0xFFB4CCB7),
    onSecondary = Color(0xFF203525),
    secondaryContainer = Color(0xFF364B3B),
    onSecondaryContainer = Color(0xFFD0E8D2),
    tertiary = Color(0xFF9ECEDB),
    onTertiary = Color(0xFF003641),
    tertiaryContainer = Color(0xFF1C4C58),
    onTertiaryContainer = Color(0xFFBAEAF7),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF101510),
    onBackground = Color(0xFFE0E4DC),
    surface = Color(0xFF101510),
    onSurface = Color(0xFFE0E4DC),
    surfaceVariant = Color(0xFF414941),
    onSurfaceVariant = Color(0xFFC1C9BE),
    outline = Color(0xFF8B9389),
    outlineVariant = Color(0xFF414941),
    surfaceContainerLowest = Color(0xFF0B0F0B),
    surfaceContainerLow = Color(0xFF181D18),
    surfaceContainer = Color(0xFF1C211C),
    surfaceContainerHigh = Color(0xFF262B26),
    surfaceContainerHighest = Color(0xFF313630)
)

@Composable
fun AppNotasTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkFallbackColors
        else -> LightFallbackColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = AppNotasShapes,
        typography = AppNotasTypography,
        content = content
    )
}
