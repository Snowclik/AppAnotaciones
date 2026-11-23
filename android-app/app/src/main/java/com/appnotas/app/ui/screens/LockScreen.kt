package com.appnotas.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.appnotas.app.ui.components.ExpressiveTextField
import com.appnotas.app.ui.components.MorphLockIcon
import com.appnotas.app.ui.components.MorphVisibilityIcon
import com.appnotas.app.ui.motion.performRejectHaptic
import com.appnotas.app.ui.motion.performTickHaptic
import com.appnotas.app.ui.motion.pressableScale
import com.appnotas.app.ui.motion.rememberReducedMotion

@Composable
fun LockScreen(
    usesAppPassword: Boolean,
    biometricAvailable: Boolean,
    biometricEnabled: Boolean,
    isVerifying: Boolean,
    errorMessage: String?,
    onSubmitPassword: (String) -> Unit,
    onRequestBiometric: () -> Unit,
    modifier: Modifier = Modifier
) {
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    val shakeOffset = remember { Animatable(0f) }
    val reducedMotion = rememberReducedMotion()
    val view = LocalView.current

    LaunchedEffect(errorMessage) {
        if (!errorMessage.isNullOrBlank()) {
            performRejectHaptic(view)
            if (!reducedMotion) {
                listOf(-18f, 18f, -12f, 12f, -6f, 0f).forEach { target ->
                    shakeOffset.animateTo(target, animationSpec = tween(durationMillis = 48))
                }
            }
        }
    }

    LaunchedEffect(biometricAvailable, biometricEnabled) {
        if (biometricAvailable && biometricEnabled) {
            onRequestBiometric()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
            .systemBarsPadding()
            .imePadding()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 4.dp,
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .graphicsLayer { translationX = shakeOffset.value }
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(68.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        MorphLockIcon(
                            isLocked = true,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            size = 32.dp
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "AppNotas protegido",
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = if (usesAppPassword) {
                            "Ingresa tu contraseña de acceso para ver tus carpetas y totales."
                        } else {
                            "Confirma tu identidad del dispositivo para abrir tus registros."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                if (usesAppPassword) {
                    ExpressiveTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = "Contraseña de la app",
                        placeholder = "PIN o contraseña",
                        isError = !errorMessage.isNullOrBlank(),
                        visualTransformation = if (showPassword) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (password.isNotBlank() && !isVerifying) {
                                    onSubmitPassword(password)
                                }
                            }
                        ),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                MorphVisibilityIcon(
                                    isVisible = showPassword,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )

                    AnimatedVisibility(visible = !errorMessage.isNullOrBlank()) {
                        Text(
                            text = errorMessage.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }

                    val unlockInteraction = remember { MutableInteractionSource() }
                    Button(
                        onClick = {
                            performTickHaptic(view)
                            onSubmitPassword(password)
                        },
                        enabled = password.isNotBlank() && !isVerifying,
                        interactionSource = unlockInteraction,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .pressableScale(
                                unlockInteraction,
                                pressedScale = 0.97f,
                                enabled = password.isNotBlank() && !isVerifying
                            )
                    ) {
                        if (isVerifying) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(
                                text = "Desbloquear",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }

                if (biometricAvailable && biometricEnabled) {
                    val bioInteraction = remember { MutableInteractionSource() }
                    FilledTonalButton(
                        onClick = {
                            performTickHaptic(view)
                            onRequestBiometric()
                        },
                        interactionSource = bioInteraction,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .pressableScale(bioInteraction, pressedScale = 0.97f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Fingerprint,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = if (usesAppPassword) {
                                    "Usar huella o rostro"
                                } else {
                                    "Desbloquear con dispositivo"
                                },
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                } else if (!usesAppPassword && !biometricAvailable) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Configura una huella o patrón en los ajustes de tu teléfono.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
