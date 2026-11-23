package com.appnotas.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.appnotas.app.ui.motion.AppExpressiveMotion
import com.appnotas.app.ui.motion.rememberReducedMotion
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Iconos vectoriales animados nativos inspirados en morphicons.com.
 * Interpolan vértices, arcos y trazos con resortes expresivos sobre Compose Canvas.
 */
@Composable
fun MorphPlusCloseIcon(
    isExpanded: Boolean,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp
) {
    val reducedMotion = rememberReducedMotion()
    val progress by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        animationSpec = if (reducedMotion) snap() else AppExpressiveMotion.fastSpatialSpec(),
        label = "morphPlusClose"
    )

    Canvas(modifier = modifier.size(size)) {
        val strokeWidth = this.size.minDimension * 0.095f
        val radius = this.size.minDimension * 0.34f
        val center = Offset(this.size.width / 2f, this.size.height / 2f)

        val baseAngleRad = lerp(0f, (PI / 4.0).toFloat(), progress)
        val cosA = cos(baseAngleRad)
        val sinA = sin(baseAngleRad)

        val hStart = Offset(center.x - radius * cosA, center.y - radius * sinA)
        val hEnd = Offset(center.x + radius * cosA, center.y + radius * sinA)
        val vStart = Offset(center.x + radius * sinA, center.y - radius * cosA)
        val vEnd = Offset(center.x - radius * sinA, center.y + radius * cosA)

        drawLine(
            color = tint,
            start = hStart,
            end = hEnd,
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = vStart,
            end = vEnd,
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun MorphSearchBackIcon(
    isBack: Boolean,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp
) {
    val reducedMotion = rememberReducedMotion()
    val progress by animateFloatAsState(
        targetValue = if (isBack) 1f else 0f,
        animationSpec = if (reducedMotion) snap() else AppExpressiveMotion.fastSpatialSpec(),
        label = "morphSearchBack"
    )

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeWidth = this.size.minDimension * 0.095f

        // Círculo de lupa que se contrae mientras emergen las alas de flecha hacia atrás
        val circleScale = (1f - progress * 1.15f).coerceIn(0f, 1f)
        if (circleScale > 0.05f) {
            drawCircle(
                color = tint.copy(alpha = tint.alpha * circleScale),
                radius = w * 0.25f * circleScale,
                center = Offset(w * lerp(0.43f, 0.26f, progress), h * 0.44f),
                style = Stroke(width = strokeWidth)
            )
        }

        // Mango de lupa -> cuerpo horizontal de la flecha
        val shaftStart = Offset(
            x = w * lerp(0.61f, 0.20f, progress),
            y = h * lerp(0.61f, 0.50f, progress)
        )
        val shaftEnd = Offset(
            x = w * lerp(0.83f, 0.80f, progress),
            y = h * lerp(0.83f, 0.50f, progress)
        )
        drawLine(
            color = tint,
            start = shaftStart,
            end = shaftEnd,
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        // Alas de flecha apareciendo al transformar a estado Back
        if (progress > 0.08f) {
            val wingSpread = progress
            val tip = Offset(w * 0.20f, h * 0.50f)
            val topWing = Offset(
                x = w * lerp(0.20f, 0.45f, wingSpread),
                y = h * lerp(0.50f, 0.26f, wingSpread)
            )
            val bottomWing = Offset(
                x = w * lerp(0.20f, 0.45f, wingSpread),
                y = h * lerp(0.50f, 0.74f, wingSpread)
            )
            val chevron = Path().apply {
                moveTo(topWing.x, topWing.y)
                lineTo(tip.x, tip.y)
                lineTo(bottomWing.x, bottomWing.y)
            }
            drawPath(
                path = chevron,
                color = tint.copy(alpha = tint.alpha * wingSpread),
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}

@Composable
fun MorphLockIcon(
    isLocked: Boolean,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp
) {
    val reducedMotion = rememberReducedMotion()
    val progress by animateFloatAsState(
        targetValue = if (isLocked) 1f else 0f,
        animationSpec = if (reducedMotion) snap() else AppExpressiveMotion.defaultSpatialSpec(),
        label = "morphLock"
    )

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeWidth = this.size.minDimension * 0.085f

        // Cuerpo redondeado del candado
        val bodyPath = Path().apply {
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    left = w * 0.21f,
                    top = h * 0.45f,
                    right = w * 0.79f,
                    bottom = h * 0.87f,
                    radiusX = w * 0.10f,
                    radiusY = w * 0.10f
                )
            )
        }
        drawPath(
            path = bodyPath,
            color = tint,
            style = Stroke(width = strokeWidth, join = StrokeJoin.Round)
        )

        // Arco superior que se eleva al estar desbloqueado (progress -> 0)
        val liftY = lerp(h * 0.11f, 0f, progress)
        val rightDropY = lerp(h * 0.31f, h * 0.45f, progress)
        val shackle = Path().apply {
            moveTo(w * 0.33f, h * 0.45f)
            lineTo(w * 0.33f, h * 0.32f - liftY)
            cubicTo(
                w * 0.33f, h * 0.12f - liftY,
                w * 0.67f, h * 0.12f - liftY,
                w * 0.67f, h * 0.32f - liftY
            )
            lineTo(w * 0.67f, rightDropY)
        }
        drawPath(
            path = shackle,
            color = tint,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Punto de cerradura
        drawCircle(
            color = tint,
            radius = strokeWidth * 0.75f,
            center = Offset(w * 0.50f, h * 0.66f)
        )
    }
}

@Composable
fun MorphVisibilityIcon(
    isVisible: Boolean,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp
) {
    val reducedMotion = rememberReducedMotion()
    val slashProgress by animateFloatAsState(
        targetValue = if (isVisible) 0f else 1f,
        animationSpec = if (reducedMotion) snap() else AppExpressiveMotion.fastSpatialSpec(),
        label = "morphVisibility"
    )

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeWidth = this.size.minDimension * 0.088f

        val eyeContour = Path().apply {
            moveTo(w * 0.12f, h * 0.50f)
            cubicTo(w * 0.30f, h * 0.22f, w * 0.70f, h * 0.22f, w * 0.88f, h * 0.50f)
            cubicTo(w * 0.70f, h * 0.78f, w * 0.30f, h * 0.78f, w * 0.12f, h * 0.50f)
            close()
        }
        drawPath(
            path = eyeContour,
            color = tint,
            style = Stroke(width = strokeWidth, join = StrokeJoin.Round)
        )

        drawCircle(
            color = tint,
            radius = lerp(w * 0.13f, w * 0.08f, slashProgress),
            center = Offset(w * 0.50f, h * 0.50f)
        )

        if (slashProgress > 0.04f) {
            val start = Offset(w * 0.19f, h * 0.81f)
            val end = Offset(
                x = lerp(w * 0.19f, w * 0.81f, slashProgress),
                y = lerp(h * 0.81f, h * 0.19f, slashProgress)
            )
            drawLine(
                color = tint,
                start = start,
                end = end,
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
    }
}
