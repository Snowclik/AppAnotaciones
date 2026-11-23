package com.appnotas.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.appnotas.app.ui.RecordSheetState
import com.appnotas.app.ui.motion.EmilEaseOut
import com.appnotas.app.ui.motion.performConfirmHaptic
import com.appnotas.app.ui.motion.performTickHaptic
import com.appnotas.app.ui.motion.pressableScale
import com.appnotas.app.ui.motion.rememberReducedMotion
import com.appnotas.core.CustomField

private data class DraftCustomField(
    val id: Int,
    var name: String,
    var value: String
)

private data class InitialNotebookValues(
    val quantityRaw: String,
    val measureUnit: String,
    val customMeasureActive: Boolean,
    val priceRaw: String,
    val priceIsPerUnit: Boolean
)

private fun reconstructNotebookValues(state: RecordSheetState): InitialNotebookValues {
    val existing = (state as? RecordSheetState.Edit)?.record
        ?: return InitialNotebookValues("", "u", false, "", true)

    val isKnownMeasure: (String) -> Boolean = { code ->
        measureUnitOptions.any { it.code.equals(code, ignoreCase = true) }
    }

    val existingQuantity = existing.quantity
    val existingUnitMultiplier = existing.unitMultiplier
    val existingUnit = existing.unit

    if (existingQuantity != null && existingUnitMultiplier != null && existingUnit == "$") {
        val measure = existing.multiplierLabel?.takeIf { it.isNotBlank() && it != "$/u" } ?: "u"
        return InitialNotebookValues(
            quantityRaw = formatEditableDecimal(existingQuantity),
            measureUnit = measure,
            customMeasureActive = !isKnownMeasure(measure),
            priceRaw = formatEditableDecimal(existingUnitMultiplier),
            priceIsPerUnit = true
        )
    }

    if (existingQuantity != null && existingUnitMultiplier == null && existingUnit == "$") {
        val measure = existing.multiplierLabel?.takeIf { it.isNotBlank() } ?: "u"
        return InitialNotebookValues(
            quantityRaw = formatEditableDecimal(existingQuantity),
            measureUnit = measure,
            customMeasureActive = !isKnownMeasure(measure),
            priceRaw = existing.numericValue?.let(::formatEditableDecimal).orEmpty(),
            priceIsPerUnit = false
        )
    }

    if (existingQuantity == null && existingUnit == "$") {
        return InitialNotebookValues(
            quantityRaw = "",
            measureUnit = "u",
            customMeasureActive = false,
            priceRaw = existing.numericValue?.let(::formatEditableDecimal).orEmpty(),
            priceIsPerUnit = false
        )
    }

    val qtyFallback = existing.numericValue
    val measure = existing.unit?.takeIf { it.isNotBlank() } ?: "u"
    return InitialNotebookValues(
        quantityRaw = qtyFallback?.let(::formatEditableDecimal).orEmpty(),
        measureUnit = measure,
        customMeasureActive = qtyFallback != null && !isKnownMeasure(measure),
        priceRaw = "",
        priceIsPerUnit = true
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordEditorSheet(
    state: RecordSheetState,
    onDismiss: () -> Unit,
    onSave: (
        targetFolderId: String,
        title: String,
        quantityRaw: String,
        measureUnit: String?,
        priceRaw: String,
        priceIsPerUnit: Boolean,
        textValue: String?,
        customFields: List<CustomField>
    ) -> Unit
) {
    val existing = (state as? RecordSheetState.Edit)?.record
    val initialFolderId = when (state) {
        is RecordSheetState.Create -> state.folderId
        is RecordSheetState.Edit -> state.record.folderId
    }
    val availableFolders = (state as? RecordSheetState.Create)?.availableFolders.orEmpty()
    val initialValues = remember(state) { reconstructNotebookValues(state) }

    var selectedFolderId by remember(state) { mutableStateOf(initialFolderId) }
    var title by remember(state) { mutableStateOf(existing?.title.orEmpty()) }
    var quantityRaw by remember(state) { mutableStateOf(initialValues.quantityRaw) }
    var measureUnit by remember(state) { mutableStateOf(initialValues.measureUnit) }
    var useCustomMeasure by remember(state) { mutableStateOf(initialValues.customMeasureActive) }
    var measureMenuOpen by remember { mutableStateOf(false) }
    var priceRaw by remember(state) { mutableStateOf(initialValues.priceRaw) }
    var priceIsPerUnit by remember(state) { mutableStateOf(initialValues.priceIsPerUnit) }
    var textValue by remember(state) { mutableStateOf(existing?.textValue.orEmpty()) }

    var nextFieldKey by remember(state) {
        mutableStateOf(existing?.customFields?.size ?: 0)
    }
    val customFields = remember(state) {
        mutableStateListOf<DraftCustomField>().apply {
            existing?.customFields?.forEachIndexed { index, field ->
                add(DraftCustomField(id = index, name = field.name, value = field.value))
            }
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val reducedMotion = rememberReducedMotion()
    val view = LocalView.current

    val parsedQty = remember(quantityRaw) {
        quantityRaw.trim().replace(',', '.').toDoubleOrNull()
    }
    val parsedPrice = remember(priceRaw) {
        priceRaw.trim().replace(',', '.').toDoubleOrNull()
    }
    val hasBothQtyAndPrice = parsedQty != null && parsedPrice != null

    val hasIncompleteCustomField = customFields.any { draft ->
        val nEmpty = draft.name.trim().isEmpty()
        val vEmpty = draft.value.trim().isEmpty()
        nEmpty != vEmpty
    }
    val canSubmit = title.trim().isNotEmpty() && !hasIncompleteCustomField

    val submit = {
        if (canSubmit) {
            performConfirmHaptic(view)
            val fieldsPayload = customFields.mapNotNull { draft ->
                val n = draft.name.trim()
                val v = draft.value.trim()
                if (n.isEmpty() && v.isEmpty()) null else CustomField(name = n, value = v)
            }
            onSave(
                selectedFolderId,
                title.trim(),
                quantityRaw,
                measureUnit.trim().takeIf { it.isNotEmpty() },
                priceRaw,
                priceIsPerUnit,
                textValue.trim().takeIf { it.isNotEmpty() },
                fieldsPayload
            )
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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = if (existing == null) "Nuevo registro" else "Editar registro",
                    style = MaterialTheme.typography.headlineMedium
                )

                if (availableFolders.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Guardar en negocio",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(availableFolders, key = { it.id }) { folder ->
                                val selected = selectedFolderId == folder.id
                                val shape = RoundedCornerShape(14.dp)
                                val emoji = extractEmoji(folder.icon)
                                Surface(
                                    shape = shape,
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerHigh
                                    },
                                    modifier = Modifier
                                        .clip(shape)
                                        .then(
                                            if (selected) {
                                                Modifier.border(
                                                    1.5.dp,
                                                    MaterialTheme.colorScheme.primary,
                                                    shape
                                                )
                                            } else {
                                                Modifier
                                            }
                                        )
                                        .clickable {
                                            performTickHaptic(view)
                                            selectedFolderId = folder.id
                                        }
                                ) {
                                    Text(
                                        text = if (emoji != null) "$emoji ${folder.name}" else folder.name,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // CAMPO 1: ¿Qué anotas?
                ExpressiveTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = "¿Qué anotas? *",
                    placeholder = "Ej. Huevos, Soda, Aspirina, Pago de luz",
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next
                    )
                )

                // CAMPO 2: Cantidad + Medida legible en español
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ExpressiveTextField(
                        value = quantityRaw,
                        onValueChange = { quantityRaw = it },
                        label = "Cantidad",
                        placeholder = "Ej. 2, 12, 600",
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    Box(modifier = Modifier.weight(1f)) {
                        val selectorShape = RoundedCornerShape(20.dp)
                        Surface(
                            shape = selectorShape,
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clip(selectorShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = LocalIndication.current
                                ) {
                                    performTickHaptic(view)
                                    measureMenuOpen = true
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Medida",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = measureDisplayName(measureUnit),
                                        style = MaterialTheme.typography.bodyLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = "Elegir medida",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = measureMenuOpen,
                            onDismissRequest = { measureMenuOpen = false }
                        ) {
                            measureUnitOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.menuLabel) },
                                    onClick = {
                                        performTickHaptic(view)
                                        measureUnit = option.code
                                        useCustomMeasure = false
                                        measureMenuOpen = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "Otra medida...",
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                },
                                onClick = {
                                    performTickHaptic(view)
                                    useCustomMeasure = true
                                    if (measureUnitOptions.any { it.code == measureUnit }) {
                                        measureUnit = ""
                                    }
                                    measureMenuOpen = false
                                }
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = useCustomMeasure,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    ExpressiveTextField(
                        value = measureUnit,
                        onValueChange = { measureUnit = it },
                        label = "Nombre de tu medida",
                        placeholder = "Ej. costales, rollos, porciones",
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                }

                // CAMPO 3: Precio ($) + explicación viva solo cuando hay Cantidad Y Precio
                ExpressiveTextField(
                    value = priceRaw,
                    onValueChange = { priceRaw = it },
                    label = "Precio (\$)",
                    placeholder = "Ej. 3.00 (opcional)",
                    singleLine = true,
                    leadingIcon = {
                        Text(
                            text = "$",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    )
                )

                AnimatedVisibility(
                    visible = hasBothQtyAndPrice,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "¿Este precio es por cada uno o por todo?",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    PriceScopePill(
                                        selected = priceIsPerUnit,
                                        label = "Cada uno",
                                        onClick = {
                                            performTickHaptic(view)
                                            priceIsPerUnit = true
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                    PriceScopePill(
                                        selected = !priceIsPerUnit,
                                        label = "Total",
                                        onClick = {
                                            performTickHaptic(view)
                                            priceIsPerUnit = false
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            val qVal = parsedQty ?: 0.0
                            val pVal = parsedPrice ?: 0.0
                            val qTxt = formatNumberDisplay(qVal)
                            val measureLabel = measureDisplayName(measureUnit)
                            val sentence = if (priceIsPerUnit) {
                                val product = qVal * pVal
                                "$qTxt $measureLabel × $${formatNumberDisplay(pVal)} c/u = $${formatNumberDisplay(product)} en total"
                            } else {
                                "Total a guardar: $${formatNumberDisplay(pVal)} (por $qTxt $measureLabel)"
                            }

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.78f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = sentence,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }
                }

                // CAMPO 4: Notas libres
                ExpressiveTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    label = "Notas (opcional)",
                    placeholder = "Ej. Proveedor, marca, observación",
                    singleLine = false,
                    minLines = 2,
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { submit() })
                )

                // Detalles adicionales ocultos por defecto para mantener limpio el formulario
                customFields.forEachIndexed { idx, field ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ExpressiveTextField(
                            value = field.name,
                            onValueChange = { newName ->
                                customFields[idx] = field.copy(name = newName)
                            },
                            label = "Dato",
                            placeholder = "Ej. Marca",
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                imeAction = ImeAction.Next
                            ),
                            modifier = Modifier.weight(0.95f)
                        )
                        ExpressiveTextField(
                            value = field.value,
                            onValueChange = { newVal ->
                                customFields[idx] = field.copy(value = newVal)
                            },
                            label = "Detalle",
                            placeholder = "Ej. Mediana",
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                imeAction = ImeAction.Next
                            ),
                            modifier = Modifier.weight(1.1f)
                        )
                        IconButton(
                            onClick = {
                                performTickHaptic(view)
                                customFields.removeAt(idx)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Quitar detalle",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (hasIncompleteCustomField) {
                    Text(
                        text = "Completa ambos campos del detalle o quítalo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                TextButton(
                    onClick = {
                        performTickHaptic(view)
                        customFields.add(DraftCustomField(id = nextFieldKey++, name = "", value = ""))
                    },
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("Añadir otro detalle")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            val saveInteraction = remember { MutableInteractionSource() }
            Button(
                onClick = submit,
                enabled = canSubmit,
                interactionSource = saveInteraction,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .pressableScale(saveInteraction, pressedScale = 0.97f, enabled = canSubmit)
            ) {
                Text(
                    text = if (existing == null) "Guardar registro" else "Actualizar registro",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun PriceScopePill(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(10.dp)
    Surface(
        shape = shape,
        color = if (selected) MaterialTheme.colorScheme.surfaceContainerLowest else Color.Transparent,
        tonalElevation = if (selected) 2.dp else 0.dp,
        modifier = modifier
            .clip(shape)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.padding(vertical = 9.dp, horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
