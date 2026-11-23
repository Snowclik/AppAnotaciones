package com.appnotas.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appnotas.app.ui.FolderSheetState
import com.appnotas.app.ui.motion.EmilEaseOut
import com.appnotas.app.ui.motion.performConfirmHaptic
import com.appnotas.app.ui.motion.performTickHaptic
import com.appnotas.app.ui.motion.pressableScale
import com.appnotas.app.ui.motion.rememberReducedMotion
import java.text.BreakIterator

private enum class FolderVisualTab {
    EMOJI,
    ICON
}

private fun firstGrapheme(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return ""
    val iterator = BreakIterator.getCharacterInstance()
    iterator.setText(trimmed)
    val start = iterator.first()
    val end = iterator.next()
    return if (end != BreakIterator.DONE) trimmed.substring(start, end) else trimmed.take(2)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderEditorSheet(
    state: FolderSheetState,
    onDismiss: () -> Unit,
    onSave: (name: String, color: String?, icon: String?) -> Unit
) {
    val existing = (state as? FolderSheetState.Edit)?.folder
    var name by remember(state) { mutableStateOf(existing?.name.orEmpty()) }
    var selectedColor by remember(state) {
        mutableStateOf(existing?.color ?: folderColorOptions.first().hex)
    }

    val initialEmoji = remember(state) { extractEmoji(existing?.icon) }
    var activeTab by remember(state) {
        mutableStateOf(
            if (existing == null || initialEmoji != null) FolderVisualTab.EMOJI else FolderVisualTab.ICON
        )
    }
    var selectedEmoji by remember(state) {
        mutableStateOf(initialEmoji ?: folderEmojiOptions.first().emoji)
    }
    var customEmojiInput by remember(state) {
        mutableStateOf(
            if (initialEmoji != null && folderEmojiOptions.none { it.emoji == initialEmoji }) {
                initialEmoji
            } else {
                ""
            }
        )
    }
    var showCustomEmojiField by remember(state) {
        mutableStateOf(
            initialEmoji != null && folderEmojiOptions.none { it.emoji == initialEmoji }
        )
    }
    var selectedIconKey by remember(state) {
        mutableStateOf(
            if (existing != null && !isEmojiIcon(existing.icon)) {
                existing.icon ?: "store"
            } else {
                "store"
            }
        )
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val reducedMotion = rememberReducedMotion()
    val view = LocalView.current

    val effectiveIconValue = remember(activeTab, selectedEmoji, selectedIconKey) {
        if (activeTab == FolderVisualTab.EMOJI) {
            encodeEmojiIcon(selectedEmoji)
        } else {
            selectedIconKey
        }
    }
    val previewAccent by animateColorAsState(
        targetValue = resolveFolderColor(selectedColor, MaterialTheme.colorScheme.primary),
        animationSpec = tween(durationMillis = 200, easing = EmilEaseOut),
        label = "folderEditorPreviewAccent"
    )

    val submit = {
        if (name.trim().isNotEmpty()) {
            performConfirmHaptic(view)
            onSave(name.trim(), selectedColor, effectiveIconValue)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.statusBarsPadding(),
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .animateContentSize(
                    animationSpec = if (reducedMotion) snap() else tween(220, easing = EmilEaseOut)
                )
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Vista previa en vivo del negocio / carpeta
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        previewAccent.copy(alpha = 0.16f),
                                        Color.Transparent
                                    )
                                )
                            )
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        FolderAvatarBadge(
                            iconValue = effectiveIconValue,
                            colorHex = selectedColor,
                            fallbackColor = MaterialTheme.colorScheme.primary,
                            size = 54.dp,
                            cornerRadius = 17.dp,
                            modifier = Modifier.clickable {
                                performTickHaptic(view)
                                activeTab = FolderVisualTab.EMOJI
                                showCustomEmojiField = !showCustomEmojiField
                            }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (existing == null) "Nueva carpeta" else "Editar carpeta",
                                style = MaterialTheme.typography.labelMedium,
                                color = previewAccent,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = name.trim().ifEmpty { "Ej. Pizzería, Farmacia, Agricultura" },
                                style = MaterialTheme.typography.titleLarge,
                                color = if (name.trim().isEmpty()) {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                ExpressiveTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Nombre de la carpeta *",
                    placeholder = "Ej. Pizzería, Farmacia, Ventas",
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { submit() })
                )

                // Paleta de 7 colores en 1 sola fila uniforme
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Color distintivo",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        folderColorOptions.forEach { option ->
                            val selected = selectedColor.equals(option.hex, ignoreCase = true)
                            val swatchInteraction = remember { MutableInteractionSource() }
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .pressableScale(swatchInteraction, pressedScale = 0.90f)
                                    .clip(CircleShape)
                                    .background(option.color)
                                    .border(
                                        width = if (selected) 3.dp else 0.dp,
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.surface
                                        } else {
                                            Color.Transparent
                                        },
                                        shape = CircleShape
                                    )
                                    .clickable(
                                        interactionSource = swatchInteraction,
                                        indication = LocalIndication.current
                                    ) {
                                        performTickHaptic(view)
                                        selectedColor = option.hex
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (selected) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = option.label,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Selector Emoji vs Ícono en 1 sola fila horizontal limpia
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Símbolo",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Row(
                                modifier = Modifier.padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                VisualModeChip(
                                    selected = activeTab == FolderVisualTab.EMOJI,
                                    icon = Icons.Rounded.EmojiEmotions,
                                    label = "Emoji",
                                    onClick = {
                                        performTickHaptic(view)
                                        activeTab = FolderVisualTab.EMOJI
                                    }
                                )
                                VisualModeChip(
                                    selected = activeTab == FolderVisualTab.ICON,
                                    icon = Icons.Rounded.GridView,
                                    label = "Ícono",
                                    onClick = {
                                        performTickHaptic(view)
                                        activeTab = FolderVisualTab.ICON
                                    }
                                )
                            }
                        }
                    }

                    AnimatedContent(
                        targetState = activeTab,
                        label = "folderVisualTab"
                    ) { tab ->
                        if (tab == FolderVisualTab.EMOJI) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    items(folderEmojiOptions, key = { it.emoji }) { option ->
                                        val selected = selectedEmoji == option.emoji
                                        val emojiInteraction = remember { MutableInteractionSource() }
                                        val shape = RoundedCornerShape(14.dp)
                                        Surface(
                                            shape = shape,
                                            color = if (selected) {
                                                previewAccent.copy(alpha = 0.22f)
                                            } else {
                                                MaterialTheme.colorScheme.surfaceContainerHigh
                                            },
                                            modifier = Modifier
                                                .size(46.dp)
                                                .pressableScale(emojiInteraction, pressedScale = 0.90f)
                                                .clip(shape)
                                                .then(
                                                    if (selected) {
                                                        Modifier.border(
                                                            width = 1.5.dp,
                                                            color = previewAccent,
                                                            shape = shape
                                                        )
                                                    } else {
                                                        Modifier
                                                    }
                                                )
                                                .clickable(
                                                    interactionSource = emojiInteraction,
                                                    indication = LocalIndication.current
                                                ) {
                                                    performTickHaptic(view)
                                                    selectedEmoji = option.emoji
                                                    customEmojiInput = ""
                                                }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(text = option.emoji, fontSize = 21.sp)
                                            }
                                        }
                                    }

                                    item(key = "custom_emoji_toggle") {
                                        val kbShape = RoundedCornerShape(14.dp)
                                        Surface(
                                            shape = kbShape,
                                            color = if (showCustomEmojiField) {
                                                previewAccent.copy(alpha = 0.20f)
                                            } else {
                                                MaterialTheme.colorScheme.surfaceContainerHigh
                                            },
                                            modifier = Modifier
                                                .height(46.dp)
                                                .clip(kbShape)
                                                .clickable {
                                                    performTickHaptic(view)
                                                    showCustomEmojiField = !showCustomEmojiField
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Keyboard,
                                                    contentDescription = "Otro emoji",
                                                    tint = if (showCustomEmojiField) {
                                                        previewAccent
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                    },
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Text(
                                                    text = "Otro",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = if (showCustomEmojiField) {
                                                        previewAccent
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                AnimatedVisibility(
                                    visible = showCustomEmojiField,
                                    enter = fadeIn() + expandVertically(),
                                    exit = fadeOut() + shrinkVertically()
                                ) {
                                    ExpressiveTextField(
                                        value = customEmojiInput,
                                        onValueChange = { typed ->
                                            if (typed.isBlank()) {
                                                customEmojiInput = ""
                                            } else {
                                                val cluster = firstGrapheme(typed)
                                                customEmojiInput = cluster
                                                if (cluster.isNotEmpty()) {
                                                    selectedEmoji = cluster
                                                }
                                            }
                                        },
                                        label = "Emoji personalizado",
                                        placeholder = "Toca aquí y elige uno de tu teclado",
                                        singleLine = true,
                                        leadingIcon = {
                                            Text(text = selectedEmoji, fontSize = 20.sp)
                                        }
                                    )
                                }
                            }
                        } else {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(vertical = 2.dp)
                            ) {
                                items(folderIconOptions, key = { it.key }) { option ->
                                    val selected = selectedIconKey == option.key
                                    val iconInteraction = remember { MutableInteractionSource() }
                                    val chipShape = RoundedCornerShape(14.dp)
                                    Surface(
                                        shape = chipShape,
                                        color = if (selected) {
                                            previewAccent.copy(alpha = 0.20f)
                                        } else {
                                            MaterialTheme.colorScheme.surfaceContainerHigh
                                        },
                                        modifier = Modifier
                                            .pressableScale(iconInteraction, pressedScale = 0.94f)
                                            .clip(chipShape)
                                            .then(
                                                if (selected) {
                                                    Modifier.border(
                                                        width = 1.5.dp,
                                                        color = previewAccent,
                                                        shape = chipShape
                                                    )
                                                } else {
                                                    Modifier
                                                }
                                            )
                                            .clickable(
                                                interactionSource = iconInteraction,
                                                indication = LocalIndication.current
                                            ) {
                                                performTickHaptic(view)
                                                selectedIconKey = option.key
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = option.icon,
                                                contentDescription = null,
                                                tint = if (selected) previewAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = option.label,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = if (selected) {
                                                    MaterialTheme.colorScheme.onSurface
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            val saveInteraction = remember { MutableInteractionSource() }
            Button(
                onClick = submit,
                enabled = name.trim().isNotEmpty(),
                interactionSource = saveInteraction,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = previewAccent,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .pressableScale(
                        saveInteraction,
                        pressedScale = 0.97f,
                        enabled = name.trim().isNotEmpty()
                    )
            ) {
                Text(
                    text = if (existing == null) "Crear carpeta" else "Guardar cambios",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
private fun VisualModeChip(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    Surface(
        shape = shape,
        color = if (selected) MaterialTheme.colorScheme.surfaceContainerLowest else Color.Transparent,
        tonalElevation = if (selected) 2.dp else 0.dp,
        modifier = Modifier
            .clip(shape)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}
