package com.appnotas.app.ui.components

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxDefaults
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.appnotas.app.ui.motion.performConfirmHaptic
import com.appnotas.app.ui.motion.performRejectHaptic
import com.appnotas.app.ui.motion.performTickHaptic
import com.appnotas.app.ui.motion.pressableScale
import com.appnotas.core.FolderItem
import com.appnotas.core.RecordItem
import kotlinx.coroutines.launch

@Composable
fun FolderRowCard(
    folder: FolderItem,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    subtitleOverride: String? = null
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val tintColor = resolveFolderColor(folder.color, MaterialTheme.colorScheme.primary)
    val iconVector = resolveFolderIcon(folder.icon)
    val interactionSource = remember { MutableInteractionSource() }
    val view = LocalView.current

    SwipeActionRowContainer(
        shape = shape,
        onSwipeEdit = onEdit,
        onSwipeDelete = onDelete,
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .pressableScale(interactionSource, pressedScale = 0.98f)
                .clip(shape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current
                ) {
                    performTickHaptic(view)
                    onClick()
                },
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = shape
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(tintColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        tint = tintColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = folder.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val defaultSubtitle = buildString {
                        if (folder.subfolderCount > 0UL) {
                            append("${folder.subfolderCount} ")
                            append(if (folder.subfolderCount == 1UL) "subcarpeta" else "subcarpetas")
                        }
                        if (folder.recordCount > 0UL) {
                            if (isNotEmpty()) append(" · ")
                            append("${folder.recordCount} ")
                            append(if (folder.recordCount == 1UL) "registro" else "registros")
                        }
                        if (isEmpty()) {
                            append("Carpeta vacía")
                        }
                    }
                    Text(
                        text = subtitleOverride ?: defaultSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.size(20.dp)
                )

                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "Opciones de carpeta",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Editar") },
                            leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Eliminar") },
                            leadingIcon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RecordRowCard(
    record: RecordItem,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    pathSubtitle: String? = null
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val view = LocalView.current

    SwipeActionRowContainer(
        shape = shape,
        onSwipeEdit = onClick,
        onSwipeDelete = onDelete,
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .pressableScale(interactionSource, pressedScale = 0.98f)
                .clip(shape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current
                ) {
                    performTickHaptic(view)
                    onClick()
                },
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = shape
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = record.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!pathSubtitle.isNullOrBlank()) {
                        Text(
                            text = pathSubtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (!record.textValue.isNullOrBlank()) {
                        Text(
                            text = record.textValue ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                val numeric = record.numericValue
                if (numeric != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Text(
                            text = formatValueWithUnit(numeric, record.unit),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                        )
                    }
                }

                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "Opciones de registro",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Editar") },
                            leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Eliminar") },
                            leadingIcon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SwipeActionRowContainer(
    shape: Shape,
    onSwipeEdit: () -> Unit,
    onSwipeDelete: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = SwipeToDismissBoxDefaults.positionalThreshold
    )
    val scope = rememberCoroutineScope()
    val view = LocalView.current

    LaunchedEffect(dismissState.targetValue) {
        if (dismissState.targetValue != SwipeToDismissBoxValue.Settled) {
            performTickHaptic(view)
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier.fillMaxWidth(),
        onDismiss = { direction ->
            when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    performConfirmHaptic(view)
                    onSwipeEdit()
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    performRejectHaptic(view)
                    onSwipeDelete()
                }
                SwipeToDismissBoxValue.Settled -> Unit
            }
            scope.launch {
                dismissState.reset()
            }
        },
        backgroundContent = {
            SwipeActionBackground(
                dismissState = dismissState,
                shape = shape
            )
        }
    ) {
        content()
    }
}

@Composable
private fun SwipeActionBackground(
    dismissState: SwipeToDismissBoxState,
    shape: Shape
) {
    val direction = dismissState.dismissDirection
    val progress = dismissState.progress.coerceIn(0f, 1f)

    val backgroundColor = when (direction) {
        SwipeToDismissBoxValue.StartToEnd ->
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = (0.35f + 0.65f * progress).coerceIn(0f, 1f))
        SwipeToDismissBoxValue.EndToStart ->
            MaterialTheme.colorScheme.errorContainer.copy(alpha = (0.35f + 0.65f * progress).coerceIn(0f, 1f))
        SwipeToDismissBoxValue.Settled ->
            MaterialTheme.colorScheme.surfaceContainerLow
    }

    val contentColor = when (direction) {
        SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.onSecondaryContainer
        SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.onErrorContainer
        SwipeToDismissBoxValue.Settled -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val alignment = when (direction) {
        SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
        else -> Alignment.CenterEnd
    }

    val icon = when (direction) {
        SwipeToDismissBoxValue.StartToEnd -> Icons.Rounded.Edit
        else -> Icons.Rounded.DeleteOutline
    }

    val label = when (direction) {
        SwipeToDismissBoxValue.StartToEnd -> "Editar"
        else -> "Eliminar"
    }

    val iconScale = 0.84f + (0.20f * progress)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(shape)
            .background(backgroundColor)
            .padding(horizontal = 20.dp),
        contentAlignment = alignment
    ) {
        if (direction != SwipeToDismissBoxValue.Settled) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.graphicsLayer {
                    alpha = (0.3f + 0.7f * progress).coerceIn(0f, 1f)
                    scaleX = iconScale
                    scaleY = iconScale
                }
            ) {
                if (direction == SwipeToDismissBoxValue.EndToStart) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = contentColor
                    )
                }
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = contentColor
                )
                if (direction == SwipeToDismissBoxValue.StartToEnd) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = contentColor
                    )
                }
            }
        }
    }
}
