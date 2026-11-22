package com.appnotas.app.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.appnotas.app.ui.RecordSheetState
import com.appnotas.app.ui.motion.performConfirmHaptic
import com.appnotas.app.ui.motion.performTickHaptic
import com.appnotas.app.ui.motion.pressableScale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecordEditorSheet(
    state: RecordSheetState,
    onDismiss: () -> Unit,
    onSave: (
        targetFolderId: String,
        title: String,
        numericRaw: String,
        unit: String?,
        textValue: String?
    ) -> Unit
) {
    val existingRecord = (state as? RecordSheetState.Edit)?.record
    val initialFolderId = when (state) {
        is RecordSheetState.Create -> state.folderId
        is RecordSheetState.Edit -> state.record.folderId
    }
    val availableFolders = (state as? RecordSheetState.Create)?.availableFolders.orEmpty()

    var selectedFolderId by remember(state) { mutableStateOf(initialFolderId) }
    var title by remember(state) { mutableStateOf(existingRecord?.title.orEmpty()) }
    var numericRaw by remember(state) {
        mutableStateOf(
            existingRecord?.numericValue?.let { value ->
                if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
            }.orEmpty()
        )
    }
    var unit by remember(state) { mutableStateOf(existingRecord?.unit.orEmpty()) }
    var textValue by remember(state) { mutableStateOf(existingRecord?.textValue.orEmpty()) }

    val focusRequester = remember { FocusRequester() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val view = LocalView.current

    val normalizedNumber = numericRaw.trim().replace(',', '.')
    val isNumericInvalid = normalizedNumber.isNotEmpty() && normalizedNumber.toDoubleOrNull() == null
    val canSave = title.isNotBlank() && !isNumericInvalid

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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (state is RecordSheetState.Create) "Nuevo registro" else "Editar registro",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            if (availableFolders.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Carpeta destino",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        availableFolders.forEach { folder ->
                            val chipInteraction = remember { MutableInteractionSource() }
                            FilterChip(
                                selected = selectedFolderId == folder.id,
                                onClick = {
                                    performTickHaptic(view)
                                    selectedFolderId = folder.id
                                },
                                interactionSource = chipInteraction,
                                label = { Text(folder.name) },
                                modifier = Modifier.pressableScale(chipInteraction, pressedScale = 0.96f)
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Título *") },
                placeholder = { Text("Ej. Venta manzanas, Pago luz, Nota") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = numericRaw,
                    onValueChange = { numericRaw = it },
                    label = { Text("Valor numérico") },
                    placeholder = { Text("0.00") },
                    singleLine = true,
                    isError = isNumericInvalid,
                    supportingText = if (isNumericInvalid) {
                        { Text("Número inválido") }
                    } else null,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.weight(1.3f)
                )

                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text("Unidad") },
                    placeholder = { Text("$, kg, pz") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.weight(1f)
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                quickUnitPresets.forEach { preset ->
                    val chipInteraction = remember { MutableInteractionSource() }
                    FilterChip(
                        selected = unit.trim() == preset,
                        onClick = {
                            performTickHaptic(view)
                            unit = if (unit.trim() == preset) "" else preset
                        },
                        interactionSource = chipInteraction,
                        label = { Text(preset) },
                        modifier = Modifier.pressableScale(chipInteraction, pressedScale = 0.95f)
                    )
                }
            }

            OutlinedTextField(
                value = textValue,
                onValueChange = { textValue = it },
                label = { Text("Notas (opcional)") },
                placeholder = { Text("Detalles adicionales, proveedor, observaciones...") },
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Default
                ),
                modifier = Modifier.fillMaxWidth()
            )

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
                        onSave(
                            selectedFolderId,
                            title,
                            numericRaw,
                            unit.ifBlank { null },
                            textValue.ifBlank { null }
                        )
                    },
                    enabled = canSave,
                    interactionSource = saveInteraction,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.pressableScale(
                        saveInteraction,
                        pressedScale = 0.96f,
                        enabled = canSave
                    )
                ) {
                    Text("Guardar")
                }
            }
        }
    }
}
