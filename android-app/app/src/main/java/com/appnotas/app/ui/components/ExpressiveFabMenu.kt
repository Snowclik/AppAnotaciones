package com.appnotas.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.NoteAdd
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.appnotas.app.ui.motion.AppExpressiveMotion
import com.appnotas.app.ui.motion.EmilEaseOut
import com.appnotas.app.ui.motion.performConfirmHaptic
import com.appnotas.app.ui.motion.performTickHaptic
import com.appnotas.app.ui.motion.pressableScale
import com.appnotas.app.ui.motion.rememberReducedMotion
import kotlinx.coroutines.delay

@Composable
fun ExpressiveFabMenuOverlay(
    expanded: Boolean,
    visible: Boolean,
    onToggle: () -> Unit,
    onDismiss: () -> Unit,
    onCreateFolder: () -> Unit,
    onCreateRecord: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!visible) return

    val view = LocalView.current
    val reducedMotion = rememberReducedMotion()
    val scrimAlpha by animateFloatAsState(
        targetValue = if (expanded) 0.34f else 0f,
        animationSpec = tween(durationMillis = if (reducedMotion) 80 else 180, easing = EmilEaseOut),
        label = "fabScrimAlpha"
    )

    Box(modifier = modifier.fillMaxSize()) {
        if (expanded || scrimAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = scrimAlpha))
                    .then(
                        if (expanded) {
                            Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onDismiss
                            )
                        } else {
                            Modifier
                        }
                    )
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 16.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ExpressiveFabActionPill(
                visible = expanded,
                enterDelayMs = 35,
                icon = Icons.Rounded.CreateNewFolder,
                label = "Nueva Carpeta",
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                reducedMotion = reducedMotion,
                onClick = {
                    performConfirmHaptic(view)
                    onCreateFolder()
                }
            )

            ExpressiveFabActionPill(
                visible = expanded,
                enterDelayMs = 0,
                icon = Icons.AutoMirrored.Rounded.NoteAdd,
                label = "Nuevo Registro",
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                reducedMotion = reducedMotion,
                onClick = {
                    performConfirmHaptic(view)
                    onCreateRecord()
                }
            )

            val fabInteraction = remember { MutableInteractionSource() }
            val cornerRadius by animateDpAsState(
                targetValue = if (expanded) 28.dp else 18.dp,
                animationSpec = if (reducedMotion) tween(0) else AppExpressiveMotion.fastSpatialSpec(),
                label = "fabCorner"
            )
            val iconRotation by animateFloatAsState(
                targetValue = if (expanded) 45f else 0f,
                animationSpec = if (reducedMotion) tween(0) else AppExpressiveMotion.fastSpatialSpec(),
                label = "fabRotation"
            )
            val containerColor by animateColorAsState(
                targetValue = if (expanded) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                },
                animationSpec = tween(durationMillis = 160, easing = EmilEaseOut),
                label = "fabContainerColor"
            )
            val contentColor by animateColorAsState(
                targetValue = if (expanded) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer
                },
                animationSpec = tween(durationMillis = 160, easing = EmilEaseOut),
                label = "fabContentColor"
            )

            val fabShape = RoundedCornerShape(cornerRadius)
            Surface(
                shape = fabShape,
                color = containerColor,
                contentColor = contentColor,
                shadowElevation = 6.dp,
                tonalElevation = 4.dp,
                modifier = Modifier
                    .size(60.dp)
                    .pressableScale(fabInteraction, pressedScale = 0.93f)
                    .clip(fabShape)
                    .clickable(
                        interactionSource = fabInteraction,
                        indication = LocalIndication.current,
                        onClick = {
                            performTickHaptic(view)
                            onToggle()
                        }
                    )
                    .semantics {
                        contentDescription = if (expanded) "Cerrar menú crear" else "Agregar carpeta o registro"
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier
                            .size(28.dp)
                            .graphicsLayer { rotationZ = iconRotation }
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpressiveFabActionPill(
    visible: Boolean,
    enterDelayMs: Int,
    icon: ImageVector,
    label: String,
    containerColor: Color,
    contentColor: Color,
    reducedMotion: Boolean,
    onClick: () -> Unit
) {
    val progress = remember { Animatable(0f) }
    val density = LocalDensity.current
    val slideOffsetPx = with(density) { 14.dp.toPx() }

    LaunchedEffect(visible, reducedMotion) {
        if (visible) {
            if (!reducedMotion && enterDelayMs > 0) {
                delay(enterDelayMs.toLong())
            }
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = if (reducedMotion) 80 else 185,
                    easing = EmilEaseOut
                )
            )
        } else {
            progress.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = if (reducedMotion) 60 else 120,
                    easing = EmilEaseOut
                )
            )
        }
    }

    if (progress.value <= 0.01f && !visible) return

    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(18.dp)

    Surface(
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        shadowElevation = 4.dp,
        modifier = Modifier
            .graphicsLayer {
                val t = progress.value
                alpha = t
                if (!reducedMotion) {
                    val scale = 0.92f + (0.08f * t)
                    scaleX = scale
                    scaleY = scale
                    translationY = (1f - t) * slideOffsetPx
                    transformOrigin = TransformOrigin(1f, 1f)
                }
            }
            .pressableScale(interaction, pressedScale = 0.96f, enabled = visible)
            .clip(shape)
            .clickable(
                enabled = visible,
                interactionSource = interaction,
                indication = LocalIndication.current,
                onClick = onClick
            )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
