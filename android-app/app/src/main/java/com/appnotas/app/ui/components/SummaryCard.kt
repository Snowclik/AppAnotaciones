package com.appnotas.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.appnotas.app.ui.motion.AppExpressiveMotion
import com.appnotas.app.ui.motion.rememberReducedMotion
import com.appnotas.core.FolderViewData

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SummaryCard(
    folderView: FolderViewData,
    modifier: Modifier = Modifier
) {
    val reducedMotion = rememberReducedMotion()

    AnimatedVisibility(
        visible = folderView.hasNumericSummary,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        val animatedSum by animateFloatAsState(
            targetValue = folderView.numericSum.toFloat(),
            animationSpec = if (reducedMotion) snap() else AppExpressiveMotion.defaultSpatialSpec(),
            label = "summaryTotal"
        )
        val displaySum = if (reducedMotion) folderView.numericSum else animatedSum.toDouble()
        val singleUnit = folderView.unitSubtotals.singleOrNull()?.unit

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .animateContentSize(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Calculate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier
                                    .padding(6.dp)
                                    .size(18.dp)
                            )
                        }
                        Text(
                            text = "Resumen rápido",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.10f)
                    ) {
                        Text(
                            text = "${folderView.numericRecordCount} con valor",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Text(
                    text = if (folderView.unitSubtotals.size == 1 && !singleUnit.isNullOrBlank()) {
                        formatValueWithUnit(displaySum, singleUnit)
                    } else {
                        formatNumberDisplay(displaySum)
                    },
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                if (folderView.unitSubtotals.size > 1) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        folderView.unitSubtotals.forEach { subtotal ->
                            val label = subtotal.unit ?: "Sin unidad"
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)
                            ) {
                                Text(
                                    text = "$label: ${formatNumberDisplay(subtotal.total)} · ${subtotal.count}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
