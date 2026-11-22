package com.appnotas.app.ui.motion

import android.os.Build
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext

/**
 * Curvas de movimiento alineadas a los principios de Emil Kowalski:
 * - EmilEaseOut: respuesta instantánea en pointer-down, entradas y menús (nunca ease-in).
 * - EmilEaseInOut: transiciones de estado en pantalla.
 * - EmilDrawerEase: hojas inferiores y overlays.
 */
val EmilEaseOut = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)
val EmilEaseInOut = CubicBezierEasing(0.77f, 0f, 0.175f, 1f)
val EmilDrawerEase = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)

/**
 * Resortes expresivos basados en los valores oficiales de ExpressiveMotionTokens
 * (material3:1.4.0 expone MotionScheme como internal en metadata Kotlin).
 */
object AppExpressiveMotion {
    fun <T> fastSpatialSpec(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.6f, stiffness = 800f)

    fun <T> defaultSpatialSpec(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.8f, stiffness = 380f)

    fun <T> fastEffectsSpec(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 1.0f, stiffness = 3800f)

    fun <T> defaultEffectsSpec(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 1.0f, stiffness = 1600f)
}

@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) == 0f
        }.getOrDefault(false)
    }
}

/**
 * Retroalimentación física inmediata al presionar (scale 0.97f en GPU graphicsLayer, sin relayout).
 */
@Composable
fun Modifier.pressableScale(
    interactionSource: InteractionSource,
    pressedScale: Float = 0.97f,
    enabled: Boolean = true
): Modifier {
    val reducedMotion = rememberReducedMotion()
    val isPressed by interactionSource.collectIsPressedAsState()
    val targetScale = if (enabled && !reducedMotion && isPressed) pressedScale else 1f
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(durationMillis = 125, easing = EmilEaseOut),
        label = "pressableScale"
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

fun performTickHaptic(view: View) {
    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
}

fun performConfirmHaptic(view: View) {
    val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        HapticFeedbackConstants.CONFIRM
    } else {
        HapticFeedbackConstants.CONTEXT_CLICK
    }
    view.performHapticFeedback(constant)
}

fun performRejectHaptic(view: View) {
    val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        HapticFeedbackConstants.REJECT
    } else {
        HapticFeedbackConstants.LONG_PRESS
    }
    view.performHapticFeedback(constant)
}
