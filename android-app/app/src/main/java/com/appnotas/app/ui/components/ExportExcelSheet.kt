package com.appnotas.app.ui.components

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.appnotas.app.ui.ExportDialogState
import com.appnotas.app.ui.motion.EmilEaseOut
import com.appnotas.app.ui.motion.performConfirmHaptic
import com.appnotas.app.ui.motion.performTickHaptic
import com.appnotas.app.ui.motion.pressableScale
import com.appnotas.app.ui.motion.rememberReducedMotion
import com.appnotas.core.ExportPeriod
import com.appnotas.core.ExportPreview
import com.appnotas.core.ExportResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val EXCEL_MIME =
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportExcelSheet(
    state: ExportDialogState,
    preview: ExportPreview?,
    isLoadingPreview: Boolean,
    isExporting: Boolean,
    lastResult: ExportResult?,
    onRequestPreview: (folderId: String?, period: ExportPeriod) -> Unit,
    onDismiss: () -> Unit,
    onExport: (folderId: String?, period: ExportPeriod) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val reducedMotion = rememberReducedMotion()
    val context = LocalContext.current
    val view = LocalView.current

    var selectedPeriod by remember(state) { mutableStateOf(ExportPeriod.WEEK) }
    var exportWholeApp by remember(state) { mutableStateOf(state.folderId == null) }
    var downloadedToDevice by remember(lastResult) { mutableStateOf(false) }
    var isSavingDownload by remember(lastResult) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val effectiveFolderId = if (exportWholeApp) null else state.folderId

    LaunchedEffect(state, effectiveFolderId, selectedPeriod) {
        onRequestPreview(effectiveFolderId, selectedPeriod)
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.TableChart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Exportar a Excel (.xlsx)",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = if (effectiveFolderId == null) {
                                "Alcance: Todos los negocios y carpetas"
                            } else {
                                "Alcance: ${state.folderName ?: "Esta carpeta"} y subcarpetas"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (state.folderId != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "¿Qué deseas exportar?",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ExportOptionChip(
                                label = state.folderName ?: "Esta carpeta",
                                selected = !exportWholeApp,
                                onClick = {
                                    performTickHaptic(view)
                                    exportWholeApp = false
                                },
                                modifier = Modifier.weight(1f)
                            )
                            ExportOptionChip(
                                label = "Todos los negocios",
                                selected = exportWholeApp,
                                onClick = {
                                    performTickHaptic(view)
                                    exportWholeApp = true
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "¿De qué fechas quieres exportar?",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Filtra solo las anotaciones de esta semana, de este mes o todo el historial.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ExportOptionChip(
                            label = "Esta semana",
                            selected = selectedPeriod == ExportPeriod.WEEK,
                            onClick = {
                                performTickHaptic(view)
                                selectedPeriod = ExportPeriod.WEEK
                            },
                            modifier = Modifier.weight(1f)
                        )
                        ExportOptionChip(
                            label = "Este mes",
                            selected = selectedPeriod == ExportPeriod.MONTH,
                            onClick = {
                                performTickHaptic(view)
                                selectedPeriod = ExportPeriod.MONTH
                            },
                            modifier = Modifier.weight(1f)
                        )
                        ExportOptionChip(
                            label = "Todo",
                            selected = selectedPeriod == ExportPeriod.ALL,
                            onClick = {
                                performTickHaptic(view)
                                selectedPeriod = ExportPeriod.ALL
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                ExcelPreviewCard(
                    preview = preview,
                    isLoading = isLoadingPreview
                )

                AnimatedVisibility(visible = lastResult != null) {
                    lastResult?.let { result ->
                        ExcelGeneratedResultCard(
                            result = result,
                            downloadedToDevice = downloadedToDevice,
                            onDownload = {
                                if (!isSavingDownload) {
                                    performTickHaptic(view)
                                    isSavingDownload = true
                                    scope.launch {
                                        try {
                                            if (saveExportToDownloads(context, result)) {
                                                downloadedToDevice = true
                                            }
                                        } finally {
                                            isSavingDownload = false
                                        }
                                    }
                                }
                            },
                            onShare = {
                                performConfirmHaptic(view)
                                shareExcelFile(context, result)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            val generateInteraction = remember { MutableInteractionSource() }
            Button(
                onClick = {
                    performConfirmHaptic(view)
                    onExport(effectiveFolderId, selectedPeriod)
                },
                enabled = !isExporting,
                interactionSource = generateInteraction,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .pressableScale(generateInteraction, pressedScale = 0.97f, enabled = !isExporting)
            ) {
                if (isExporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Generando Excel...")
                } else {
                    Text(
                        text = if (lastResult == null) "Generar Excel (.xlsx)" else "Generar otro reporte",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

@Composable
fun ExcelPreviewCard(
    preview: ExportPreview?,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(20.dp)
    Surface(
        shape = cardShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Encabezado tipo Excel en tono esmeralda
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F766E))
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Vista previa de la hoja Excel",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White
                )
                if (preview != null) {
                    Text(
                        text = "${preview.recordCount} reg · ${preview.periodLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }

            if (isLoading && preview == null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Preparando vista previa...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (preview != null) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = preview.scopeLabel,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Suma: ${formatNumberDisplay(preview.numericSum)}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (preview.sampleRows.isEmpty()) {
                        Text(
                            text = "No hay registros en este alcance y período todavía. Se generará el archivo con encabezados vacíos.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        // Mini tabla desplazable horizontalmente simulando columnas Excel
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLowest
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                            ) {
                                Row(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    PreviewCell("Fecha", 72.dp, bold = true)
                                    PreviewCell("Concepto", 120.dp, bold = true)
                                    PreviewCell("Valor", 76.dp, bold = true)
                                    PreviewCell("Cálculo", 92.dp, bold = true)
                                    PreviewCell("Datos extras", 130.dp, bold = true)
                                }

                                preview.sampleRows.forEachIndexed { index, row ->
                                    if (index > 0) {
                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        PreviewCell(row.dateLabel, 72.dp)
                                        PreviewCell(row.title, 120.dp, bold = true)
                                        PreviewCell(row.valueDisplay, 76.dp)
                                        PreviewCell(row.calculationDisplay ?: "—", 92.dp)
                                        PreviewCell(row.extraSummary, 130.dp)
                                    }
                                }
                            }
                        }

                        if (preview.recordCount > preview.sampleRows.size.toULong()) {
                            val remaining = preview.recordCount - preview.sampleRows.size.toULong()
                            Text(
                                text = "+ $remaining registros más y ${preview.columns.size} columnas totales al exportar.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "Columnas: ${preview.columns.joinToString(" · ")}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewCell(
    text: String,
    width: androidx.compose.ui.unit.Dp,
    bold: Boolean = false
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.width(width)
    )
}

@Composable
fun ExcelGeneratedResultCard(
    result: ExportResult,
    downloadedToDevice: Boolean,
    onDownload: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = result.fileName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${result.periodLabel} · ${result.recordCount} registros · Suma ${formatNumberDisplay(result.numericSum)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilledTonalButton(
                    onClick = onDownload,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(if (downloadedToDevice) "En Descargas" else "Descargar")
                }

                Button(
                    onClick = onShare,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("Compartir")
                }
            }
        }
    }
}

@Composable
fun ExportOptionChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(14.dp)
    Surface(
        shape = shape,
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        modifier = modifier
            .clip(shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = LocalIndication.current,
                onClick = onClick
            )
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 11.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

suspend fun saveExportToDownloads(context: Context, result: ExportResult): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        withContext(Dispatchers.Main) {
            Toast.makeText(
                context,
                "Usa 'Compartir' para guardar el archivo en esta versión de Android.",
                Toast.LENGTH_LONG
            ).show()
        }
        return false
    }

    val outcome = withContext(Dispatchers.IO) {
        val sourceFile = File(result.filePath)
        if (!sourceFile.exists()) {
            return@withContext Result.failure(IllegalStateException("No se encontró el archivo generado."))
        }
        val resolver = context.contentResolver
        var insertedUri: android.net.Uri? = null
        runCatching {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, result.fileName)
                put(MediaStore.Downloads.MIME_TYPE, EXCEL_MIME)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: error("No se pudo crear archivo en Descargas")
            insertedUri = uri
            resolver.openOutputStream(uri)?.use { out ->
                sourceFile.inputStream().use { input -> input.copyTo(out) }
            } ?: error("No se pudo abrir flujo de salida")
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            true
        }.onFailure {
            insertedUri?.let { orphanUri ->
                runCatching { resolver.delete(orphanUri, null, null) }
            }
        }
    }

    return outcome.fold(
        onSuccess = {
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    "Guardado en Descargas: ${result.fileName}",
                    Toast.LENGTH_LONG
                ).show()
            }
            true
        },
        onFailure = { error ->
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    error.message ?: "No se pudo guardar en Descargas.",
                    Toast.LENGTH_LONG
                ).show()
            }
            false
        }
    )
}

fun shareExcelFile(context: Context, result: ExportResult) {
    val file = File(result.filePath)
    if (!file.exists()) {
        Toast.makeText(context, "No se encontró el archivo Excel.", Toast.LENGTH_SHORT).show()
        return
    }
    runCatching {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = EXCEL_MIME
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, result.fileName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir reporte Excel"))
    }.onFailure {
        Toast.makeText(context, "No hay aplicación disponible para compartir.", Toast.LENGTH_SHORT)
            .show()
    }
}
