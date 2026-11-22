package com.appnotas.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.appnotas.app.ui.FolderSheetState
import com.appnotas.app.ui.motion.AppExpressiveMotion
import com.appnotas.app.ui.motion.performConfirmHaptic
import com.appnotas.app.ui.motion.performTickHaptic
import com.appnotas.app.ui.motion.pressableScale
import com.appnotas.app.ui.motion.rememberReducedMotion

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class
)
@Composable
fun FolderEditorSheet(
    state: FolderSheetState,
    onDismiss: () -> Unit,
    onSave: (name: String, color: String?, icon: String?) -> Unit
) {
    val initialFolder = (state as? FolderSheetState.Edit)?.folder
    var name by remember(state) { mutableStateOf(initialFolder?.name.orEmpty()) }
    var selectedColor by remember(state) {
        mutableStateOf(initialFolder?.color ?: folderColorOptions.first().hex)
    }
    var selectedIcon by remember(state) {
        mutableStateOf(initialFolder?.icon ?: folderIconOptions.first().key)
    }

    val focusRequester = remember { FocusRequester() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val view = LocalView.current
    val reducedMotion = rememberReducedMotion()

    LaunchedEffect(state) {
        focusRequester.requestFocus()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (state is FolderSheetState.Create) "Nueva carpeta" else "Editar carpeta",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre de la carpeta") },
                placeholder = { Text("Ej. Frutería, Ventas, Gastos") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (name.isNotBlank()) {
                            performConfirmHaptic(view)
                            onSave(name, selectedColor, selectedIcon)
                        }
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Color",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    folderColorOptions.forEach { option ->
                        val isSelected = selectedColor == option.hex
                        val swatchInteraction = remember { MutableInteractionSource() }
                        val selectionScale by animateFloatAsState(
                            targetValue = if (isSelected) 1.08f else 1f,
                            animationSpec = AppExpressiveMotion.fastSpatialSpec(),
                            label = "swatchScale"
                        )
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .graphicsLayer {
                                    if (!reducedMotion) {
                                        scaleX = selectionScale
                                        scaleY = selectionScale
                                    }
                                }
                                .pressableScale(swatchInteraction, pressedScale = 0.90f)
                                .clip(CircleShape)
                                .background(option.color)
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
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
                            if (isSelected) {
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

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Icono",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    folderIconOptions.forEach { option ->
                        val isSelected = selectedIcon == option.key
                        val chipInteraction = remember { MutableInteractionSource() }
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                performTickHaptic(view)
                                selectedIcon = option.key
                            },
                            interactionSource = chipInteraction,
                            label = { Text(option.label) },
                            leadingIcon = {
                                Icon(
                                    imageVector = option.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            modifier = Modifier.pressableScale(chipInteraction, pressedScale = 0.96f)
                        )
                    }
                }
            }

            val saveInteraction = remember { MutableInteractionSource() }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        performConfirmHaptic(view)
                        onSave(name, selectedColor, selectedIcon)
                    },
                    enabled = name.isNotBlank(),
                    interactionSource = saveInteraction,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.pressableScale(
                        saveInteraction,
                        pressedScale = 0.96f,
                        enabled = name.isNotBlank()
                    )
                ) {
                    Text("Guardar")
                }
            }
        }
    }
}
