package com.appnotas.app.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Password
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.appnotas.app.ui.motion.EmilEaseOut
import com.appnotas.app.ui.motion.performConfirmHaptic
import com.appnotas.app.ui.motion.performTickHaptic
import com.appnotas.app.ui.motion.pressableScale
import com.appnotas.app.ui.motion.rememberReducedMotion

private enum class PasswordInputMode {
    PIN,
    ALPHANUMERIC
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordSetupSheet(
    hasExistingPassword: Boolean,
    canUseBiometric: Boolean,
    initialBiometricEnabled: Boolean,
    onDismiss: () -> Unit,
    onSave: (password: String, enableBiometric: Boolean) -> Unit
) {
    var mode by remember { mutableStateOf(PasswordInputMode.PIN) }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var biometricEnabled by remember { mutableStateOf(initialBiometricEnabled && canUseBiometric) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val reducedMotion = rememberReducedMotion()
    val view = LocalView.current

    val cleanPassword = password.trim()
    val cleanConfirm = confirmPassword.trim()
    val isLengthValid = cleanPassword.length >= 4
    val matches = cleanPassword == cleanConfirm && cleanConfirm.isNotEmpty()
    val canSubmit = isLengthValid && matches

    val submit = {
        if (canSubmit) {
            performConfirmHaptic(view)
            onSave(cleanPassword, biometricEnabled)
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(46.dp)
                ) {
                    androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                        MorphLockIcon(
                            isLocked = true,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            size = 24.dp
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (hasExistingPassword) {
                            "Cambiar contraseña de la app"
                        } else {
                            "Crear contraseña de acceso"
                        },
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Clave independiente de la del celular, cifrada con PBKDF2-SHA256 en Rust.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Selector de formato: PIN numérico rápido o contraseña mixta
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ModeTogglePill(
                        selected = mode == PasswordInputMode.PIN,
                        icon = Icons.Rounded.Dialpad,
                        label = "PIN numérico",
                        onClick = {
                            performTickHaptic(view)
                            mode = PasswordInputMode.PIN
                            password = password.filter { it.isDigit() }
                            confirmPassword = confirmPassword.filter { it.isDigit() }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    ModeTogglePill(
                        selected = mode == PasswordInputMode.ALPHANUMERIC,
                        icon = Icons.Rounded.Password,
                        label = "Texto / mixta",
                        onClick = {
                            performTickHaptic(view)
                            mode = PasswordInputMode.ALPHANUMERIC
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            ExpressiveTextField(
                value = password,
                onValueChange = { raw ->
                    password = if (mode == PasswordInputMode.PIN) {
                        raw.filter { it.isDigit() }.take(12)
                    } else {
                        raw.take(48)
                    }
                },
                label = if (mode == PasswordInputMode.PIN) "PIN (mínimo 4 dígitos)" else "Contraseña (mínimo 4 caracteres)",
                placeholder = if (mode == PasswordInputMode.PIN) "Ej. 2580" else "Ej. Fruteria2026",
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (mode == PasswordInputMode.PIN) KeyboardType.NumberPassword else KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        MorphVisibilityIcon(
                            isVisible = showPassword,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                supportingText = if (password.isNotEmpty() && !isLengthValid) {
                    { Text("Mínimo 4 caracteres o dígitos.") }
                } else null
            )

            ExpressiveTextField(
                value = confirmPassword,
                onValueChange = { raw ->
                    confirmPassword = if (mode == PasswordInputMode.PIN) {
                        raw.filter { it.isDigit() }.take(12)
                    } else {
                        raw.take(48)
                    }
                },
                label = "Confirmar contraseña",
                placeholder = "Vuelve a escribirla",
                isError = confirmPassword.isNotEmpty() && !matches,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (mode == PasswordInputMode.PIN) KeyboardType.NumberPassword else KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                supportingText = if (confirmPassword.isNotEmpty() && !matches) {
                    { Text("Las contraseñas no coinciden.") }
                } else null
            )

            if (canUseBiometric) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = LocalIndication.current
                            ) {
                                performTickHaptic(view)
                                biometricEnabled = !biometricEnabled
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Fingerprint,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Permitir también huella / rostro",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Úsala como acceso rápido además de tu contraseña.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = biometricEnabled,
                            onCheckedChange = {
                                performTickHaptic(view)
                                biometricEnabled = it
                            }
                        )
                    }
                }
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
                    text = if (hasExistingPassword) "Actualizar contraseña" else "Activar bloqueo",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
private fun ModeTogglePill(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    Surface(
        shape = shape,
        color = if (selected) MaterialTheme.colorScheme.surfaceContainerLowest else Color.Transparent,
        tonalElevation = if (selected) 2.dp else 0.dp,
        modifier = modifier
            .clip(shape)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(vertical = 9.dp, horizontal = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
