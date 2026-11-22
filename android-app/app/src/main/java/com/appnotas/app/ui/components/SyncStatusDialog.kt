package com.appnotas.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.SyncProblem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.appnotas.core.SyncMode
import com.appnotas.core.SyncStatus
import java.text.DateFormat
import java.util.Date

@Composable
fun SyncStatusDialog(
    syncStatus: SyncStatus?,
    isSyncing: Boolean,
    onSyncNow: () -> Unit,
    onDismiss: () -> Unit
) {
    val modeLabel = when (syncStatus?.mode) {
        SyncMode.EMBEDDED_REPLICA -> "Réplica embebida (Turso)"
        SyncMode.OFFLINE_FALLBACK -> "Offline temporal (fallback local)"
        SyncMode.LOCAL_ONLY, null -> "Solo local (100% dispositivo)"
    }

    val icon = when (syncStatus?.mode) {
        SyncMode.EMBEDDED_REPLICA ->
            if (syncStatus.synced) Icons.Rounded.CloudDone else Icons.Rounded.SyncProblem
        SyncMode.OFFLINE_FALLBACK -> Icons.Rounded.SyncProblem
        SyncMode.LOCAL_ONLY, null -> Icons.Rounded.CloudOff
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = { Text("Estado de sincronización") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Modo: $modeLabel",
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = syncStatus?.message ?: "Base de datos local lista.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (syncStatus?.mode == SyncMode.EMBEDDED_REPLICA) {
                    Text(
                        text = "Frames sincronizados: ${syncStatus.framesSynced}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    syncStatus.lastSyncEpochMs?.let { epoch ->
                        val formatted = DateFormat.getDateTimeInstance().format(Date(epoch))
                        Text(
                            text = "Última sincronización: $formatted",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (syncStatus?.mode == SyncMode.EMBEDDED_REPLICA) {
                Button(
                    onClick = onSyncNow,
                    enabled = !isSyncing
                ) {
                    if (isSyncing) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Text("Sincronizando...")
                        }
                    } else {
                        Text("Sincronizar ahora")
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}
