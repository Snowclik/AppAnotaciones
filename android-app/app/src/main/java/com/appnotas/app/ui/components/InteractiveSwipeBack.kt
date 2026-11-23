package com.appnotas.app.ui.components

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.dp
import com.appnotas.app.ui.NavigationDirection
import com.appnotas.app.ui.motion.AppExpressiveMotion
import com.appnotas.app.ui.motion.EmilDrawerEase
import com.appnotas.app.ui.motion.performConfirmHaptic
import com.appnotas.app.ui.motion.performTickHaptic
import com.appnotas.app.ui.motion.rememberReducedMotion
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Contenedor físico unificado para Swipe-Back 1:1 y entrada Push (`FORWARD`) sin `AnimatedContent`.
 * Evita flashes de 1 frame al terminar el arrastre y sincroniza el parallax de la vista previa.
 */
@Composable
fun InteractiveSwipeBackContainer(
    enabled: Boolean,
    fullWidth: Boolean,
    contentKey: Any?,
    underlayContent: @Composable () -> Unit,
    onSwipeBack: () -> Unit,
    modifier: Modifier = Modifier,
    direction: NavigationDirection = NavigationDirection.NEUTRAL,
    enterFromRightOnAppear: Boolean = false,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val viewConfig = LocalViewConfiguration.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val reducedMotion = rememberReducedMotion()
    val fallbackWidthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }

    val latestEnabled by rememberUpdatedState(enabled)
    val latestOnSwipeBack by rememberUpdatedState(onSwipeBack)

    val edgeZonePx = with(density) { 72.dp.toPx() }
    val shadowWidthPx = with(density) { 18.dp.toPx() }
    val fallbackThresholdPx = with(density) { 104.dp.toPx() }
    val minFlickOffsetPx = with(density) { 40.dp.toPx() }

    var containerWidthPx by remember { mutableFloatStateOf(fallbackWidthPx) }
    var previousKey by remember { mutableStateOf(contentKey) }
    var didInitialAppear by remember { mutableStateOf(false) }

    var offsetPx by remember(contentKey) {
        mutableFloatStateOf(
            if (!didInitialAppear && enterFromRightOnAppear && !reducedMotion) {
                containerWidthPx
            } else {
                0f
            }
        )
    }
    var isCommitting by remember(contentKey) { mutableStateOf(false) }
    var settleJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(Unit) {
        if (!didInitialAppear) {
            didInitialAppear = true
            if (enterFromRightOnAppear && !reducedMotion && offsetPx > 0.5f) {
                val startOffset = offsetPx
                settleJob?.cancel()
                settleJob = scope.launch {
                    animate(
                        initialValue = startOffset,
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = 250, easing = EmilDrawerEase)
                    ) { value, _ ->
                        offsetPx = value
                    }
                }
            } else {
                offsetPx = 0f
            }
        }
    }

    if (contentKey != previousKey) {
        previousKey = contentKey
        settleJob?.cancel()
        settleJob = null
        if (direction == NavigationDirection.FORWARD && !reducedMotion && containerWidthPx > 0f) {
            val startOffset = containerWidthPx
            offsetPx = startOffset
            settleJob = scope.launch {
                animate(
                    initialValue = startOffset,
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 250, easing = EmilDrawerEase)
                ) { value, _ ->
                    offsetPx = value
                }
            }
        } else {
            offsetPx = 0f
        }
    }

    val commitThresholdPx = if (containerWidthPx > 0f) {
        (containerWidthPx * 0.28f).coerceAtLeast(fallbackThresholdPx)
    } else {
        fallbackThresholdPx
    }
    val progress = if (containerWidthPx > 0f) {
        (offsetPx / containerWidthPx).coerceIn(0f, 1f)
    } else {
        (offsetPx / (fallbackThresholdPx * 3f)).coerceIn(0f, 1f)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .onSizeChanged {
                if (it.width > 0) containerWidthPx = it.width.toFloat()
            }
            .pointerInput(fullWidth, contentKey) {
                awaitEachGesture {
                    val down = awaitFirstDown(
                        requireUnconsumed = false,
                        pass = PointerEventPass.Initial
                    )
                    if (!latestEnabled || isCommitting) return@awaitEachGesture

                    settleJob?.cancel()
                    settleJob = null

                    val fromLeftEdge = down.position.x <= edgeZonePx
                    val primaryPass = if (fullWidth || fromLeftEdge) PointerEventPass.Initial else PointerEventPass.Main
                    val touchSlop = viewConfig.touchSlop
                    val velocityTracker = VelocityTracker()
                    velocityTracker.addPosition(down.uptimeMillis, down.position)

                    var active = false
                    var cancelled = false
                    var totalDx = 0f
                    var totalDy = 0f
                    var dragOriginDx = 0f
                    val dragStartOffset = offsetPx
                    var wasPast = dragStartOffset >= commitThresholdPx

                    while (!cancelled) {
                        val event = awaitPointerEvent(pass = primaryPass)
                        val change = event.changes.firstOrNull { it.id == down.id }
                            ?: event.changes.firstOrNull()
                            ?: break

                        if (change.changedToUpIgnoreConsumed() || !change.pressed) {
                            break
                        }

                        if (!active && !fromLeftEdge && change.isConsumed) {
                            cancelled = true
                            break
                        }

                        val delta = change.positionChange()
                        totalDx += delta.x
                        totalDy += delta.y

                        if (!active) {
                            if (totalDx > touchSlop && totalDx > abs(totalDy) * 1.15f) {
                                if (!fullWidth && !fromLeftEdge && change.isConsumed) {
                                    cancelled = true
                                    break
                                }
                                active = true
                                dragOriginDx = totalDx
                                change.consume()
                            } else if (
                                (abs(totalDy) > touchSlop * 1.25f && abs(totalDy) >= abs(totalDx)) ||
                                totalDx < -touchSlop
                            ) {
                                cancelled = true
                                break
                            }
                        } else {
                            change.consume()
                            velocityTracker.addPosition(change.uptimeMillis, change.position)
                            val clamped = (dragStartOffset + (totalDx - dragOriginDx)).coerceAtLeast(0f)
                            val nowPast = clamped >= commitThresholdPx
                            if (nowPast != wasPast) {
                                wasPast = nowPast
                                performTickHaptic(view)
                            }
                            offsetPx = clamped
                        }
                    }

                    if (active) {
                        val velocityX = velocityTracker.calculateVelocity().x
                        val shouldCommit = offsetPx >= commitThresholdPx ||
                            (velocityX > 550f && offsetPx >= minFlickOffsetPx)

                        if (shouldCommit) {
                            performConfirmHaptic(view)
                            isCommitting = true
                            settleJob = scope.launch {
                                try {
                                    val exitTarget = containerWidthPx.coerceAtLeast(commitThresholdPx * 2f)
                                    val remainingFraction = ((exitTarget - offsetPx) / exitTarget).coerceIn(0.15f, 1f)
                                    val exitDurationMs = (190f * remainingFraction).toInt().coerceAtLeast(35)
                                    if (!reducedMotion && offsetPx < exitTarget) {
                                        animate(
                                            initialValue = offsetPx,
                                            targetValue = exitTarget,
                                            animationSpec = tween(durationMillis = exitDurationMs, easing = EmilDrawerEase)
                                        ) { value, _ ->
                                            offsetPx = value
                                        }
                                    }
                                    latestOnSwipeBack()
                                } finally {
                                    isCommitting = false
                                }
                            }
                        } else {
                            settleJob = scope.launch {
                                if (reducedMotion) {
                                    offsetPx = 0f
                                } else {
                                    animate(
                                        initialValue = offsetPx,
                                        targetValue = 0f,
                                        animationSpec = AppExpressiveMotion.defaultSpatialSpec()
                                    ) { value, _ ->
                                        offsetPx = value
                                    }
                                }
                            }
                        }
                    } else if (offsetPx > 0f && !isCommitting) {
                        settleJob = scope.launch {
                            if (reducedMotion) {
                                offsetPx = 0f
                            } else {
                                animate(
                                    initialValue = offsetPx,
                                    targetValue = 0f,
                                    animationSpec = AppExpressiveMotion.defaultSpatialSpec()
                                ) { value, _ ->
                                    offsetPx = value
                                }
                            }
                        }
                    }
                }
            }
    ) {
        // Capa trasera: vista previa real con parallax suave 12%
        if (offsetPx > 0.5f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .graphicsLayer {
                        val parallaxShift = -containerWidthPx * 0.12f * (1f - progress)
                        translationX = if (reducedMotion) 0f else parallaxShift
                    }
            ) {
                underlayContent()
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.08f * (1f - progress)))
                )
            }

            // Sombra exterior limpia a la izquierda de la pantalla activa
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(18.dp)
                    .graphicsLayer {
                        val shift = if (reducedMotion) 0f else offsetPx
                        translationX = shift - shadowWidthPx
                    }
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.09f * (1f - progress * 0.5f))
                            )
                        )
                    )
            )
        }

        // Capa frontal: pantalla actual (o entrando desde +width en FORWARD)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = if (reducedMotion) 0f else offsetPx
                }
        ) {
            content()

            if (offsetPx > 1f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxHeight()
                        .width(0.5.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                )
            }
        }
    }
}
