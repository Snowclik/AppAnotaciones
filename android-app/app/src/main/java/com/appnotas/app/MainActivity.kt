package com.appnotas.app

import android.app.KeyguardManager
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appnotas.app.ui.AppDestination
import com.appnotas.app.ui.ExplorerViewModel
import com.appnotas.app.ui.NavigationDirection
import com.appnotas.app.ui.components.ExportExcelSheet
import com.appnotas.app.ui.components.PasswordSetupSheet
import com.appnotas.app.ui.motion.EmilDrawerEase
import com.appnotas.app.ui.motion.rememberReducedMotion
import com.appnotas.app.ui.screens.ExplorerScreen
import com.appnotas.app.ui.screens.ExplorerUnderlayBackdrop
import com.appnotas.app.ui.screens.LockScreen
import com.appnotas.app.ui.screens.SettingsScreen
import com.appnotas.app.ui.theme.AppNotasTheme
import com.appnotas.core.LockMode

class MainActivity : FragmentActivity() {

    private val viewModel: ExplorerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        splashScreen.setKeepOnScreenCondition { !viewModel.uiState.value.isReady }
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT)
        )

        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                if (!isChangingConfigurations) {
                    viewModel.onAppBackgrounded()
                }
            }
        })

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val canUseBiometric = remember(uiState.isLocked, uiState.destination) {
                hasBiometricEnrolled()
            }
            val canUseDeviceSecurity = remember(uiState.isLocked, uiState.destination) {
                hasDeviceSecurity()
            }

            BackHandler(
                enabled = !uiState.isLocked && (
                    uiState.destination == AppDestination.SETTINGS ||
                        uiState.isFabMenuExpanded ||
                        uiState.isSearchExpanded ||
                        (uiState.folderView?.breadcrumbs?.size ?: 0) > 1
                    )
            ) {
                viewModel.navigateUp()
            }

            AppNotasTheme(
                paletteId = uiState.appearance.paletteId,
                themeMode = uiState.appearance.themeMode,
                pureBlack = uiState.appearance.pureBlack
            ) {
                if (uiState.isLocked) {
                    val usesAppPassword = uiState.security.lockMode == LockMode.APP_PASSWORD
                    LockScreen(
                        usesAppPassword = usesAppPassword,
                        biometricAvailable = if (usesAppPassword) canUseBiometric else canUseDeviceSecurity,
                        biometricEnabled = uiState.security.biometricEnabled ||
                            uiState.security.lockMode == LockMode.DEVICE_BIOMETRIC,
                        isVerifying = uiState.isVerifyingLock,
                        errorMessage = uiState.lockError,
                        onSubmitPassword = viewModel::unlockWithPassword,
                        onRequestBiometric = {
                            launchBiometricPrompt(
                                allowDeviceCredential = uiState.security.lockMode == LockMode.DEVICE_BIOMETRIC,
                                onSuccess = viewModel::onBiometricUnlockSucceeded
                            )
                        }
                    )
                } else {
                    val reducedMotion = rememberReducedMotion()
                    val density = LocalDensity.current
                    val initialWidthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
                    var rootWidthPx by remember { mutableFloatStateOf(initialWidthPx) }
                    var lastDestination by remember { mutableStateOf(uiState.destination) }
                    var settingsExiting by remember { mutableStateOf(false) }
                    var settingsExitOffsetPx by remember { mutableFloatStateOf(0f) }
                    var settingsExitTrigger by remember { mutableIntStateOf(0) }

                    if (uiState.destination != lastDestination) {
                        if (
                            lastDestination == AppDestination.SETTINGS &&
                            uiState.destination == AppDestination.EXPLORER &&
                            uiState.rootNavDirection == NavigationDirection.BACKWARD &&
                            !reducedMotion
                        ) {
                            settingsExiting = true
                            settingsExitOffsetPx = 0f
                            settingsExitTrigger += 1
                        } else {
                            settingsExiting = false
                            settingsExitOffsetPx = 0f
                        }
                        lastDestination = uiState.destination
                    }

                    LaunchedEffect(settingsExitTrigger) {
                        if (settingsExitTrigger == 0 || !settingsExiting) return@LaunchedEffect
                        val currentTrigger = settingsExitTrigger
                        val targetWidth = rootWidthPx.coerceAtLeast(1f)
                        animate(
                            initialValue = 0f,
                            targetValue = targetWidth,
                            animationSpec = tween(durationMillis = 220, easing = EmilDrawerEase)
                        ) { value, _ ->
                            if (settingsExiting && settingsExitTrigger == currentTrigger) {
                                settingsExitOffsetPx = value
                            }
                        }
                        if (settingsExiting && settingsExitTrigger == currentTrigger) {
                            settingsExiting = false
                            settingsExitOffsetPx = 0f
                        }
                    }

                    val settingsExitProgress = if (settingsExiting && rootWidthPx > 0f) {
                        (settingsExitOffsetPx / rootWidthPx).coerceIn(0f, 1f)
                    } else {
                        1f
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clipToBounds()
                            .onSizeChanged {
                                if (it.width > 0) rootWidthPx = it.width.toFloat()
                            }
                    ) {
                        if (uiState.destination == AppDestination.EXPLORER || settingsExiting) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        translationX = if (settingsExiting && !reducedMotion) {
                                            -rootWidthPx * 0.12f * (1f - settingsExitProgress)
                                        } else {
                                            0f
                                        }
                                    }
                            ) {
                                ExplorerScreen(
                                    uiState = uiState,
                                    viewModel = viewModel
                                )
                            }
                        }

                        if (uiState.destination == AppDestination.SETTINGS) {
                            SettingsScreen(
                                uiState = uiState,
                                biometricAvailable = canUseBiometric,
                                deviceSecurityAvailable = canUseDeviceSecurity,
                                onBack = viewModel::closeSettings,
                                onBackFromSwipe = viewModel::closeSettingsFromSwipe,
                                onSelectSection = { section ->
                                    viewModel.openSettingsSection(section)
                                },
                                onSelectSectionFromSwipe = { section ->
                                    viewModel.openSettingsSection(section, instant = true)
                                },
                                explorerUnderlay = {
                                    ExplorerUnderlayBackdrop(uiState = uiState)
                                },
                                onClearSnackbar = viewModel::clearSnackbar,
                                onOpenPasswordSetup = viewModel::openPasswordSetupSheet,
                                onUpdateSecurity = viewModel::updateSecurityOptions,
                                onClearAppPassword = viewModel::clearAppPassword,
                                onRequestExportPreview = viewModel::loadExportPreview,
                                onExportExcel = viewModel::exportExcel,
                                onSelectPalette = { paletteId ->
                                    viewModel.updateAppearance(paletteId = paletteId)
                                },
                                onSelectThemeMode = { themeMode ->
                                    viewModel.updateAppearance(themeMode = themeMode)
                                },
                                onTogglePureBlack = { pureBlack ->
                                    viewModel.updateAppearance(pureBlack = pureBlack)
                                }
                            )
                        } else if (settingsExiting) {
                            val shadowWidthPx = with(density) { 18.dp.toPx() }
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(18.dp)
                                    .graphicsLayer {
                                        translationX = settingsExitOffsetPx - shadowWidthPx
                                    }
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                ComposeColor.Transparent,
                                                ComposeColor.Black.copy(alpha = 0.09f * (1f - settingsExitProgress * 0.5f))
                                            )
                                        )
                                    )
                            )
                            SettingsScreen(
                                uiState = uiState.copy(snackbarMessage = null),
                                biometricAvailable = canUseBiometric,
                                deviceSecurityAvailable = canUseDeviceSecurity,
                                onBack = {},
                                onBackFromSwipe = {},
                                onSelectSection = {},
                                onSelectSectionFromSwipe = {},
                                explorerUnderlay = {},
                                onClearSnackbar = {},
                                onOpenPasswordSetup = {},
                                onUpdateSecurity = { _, _, _ -> },
                                onClearAppPassword = {},
                                onRequestExportPreview = { _, _ -> },
                                onExportExcel = { _, _ -> },
                                onSelectPalette = {},
                                onSelectThemeMode = {},
                                onTogglePureBlack = {},
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        translationX = settingsExitOffsetPx
                                    }
                            )
                        }
                    }

                    uiState.exportSheet?.let { sheet ->
                        ExportExcelSheet(
                            state = sheet,
                            preview = uiState.exportPreview,
                            isLoadingPreview = uiState.isLoadingExportPreview,
                            isExporting = uiState.isExporting,
                            lastResult = uiState.lastExportResult,
                            onRequestPreview = viewModel::loadExportPreview,
                            onDismiss = viewModel::dismissExportSheet,
                            onExport = viewModel::exportExcel
                        )
                    }

                    if (uiState.showPasswordSetupSheet) {
                        PasswordSetupSheet(
                            hasExistingPassword = uiState.security.hasPassword,
                            canUseBiometric = canUseBiometric,
                            initialBiometricEnabled = uiState.security.biometricEnabled,
                            onDismiss = viewModel::dismissPasswordSetupSheet,
                            onSave = viewModel::saveAppPassword
                        )
                    }
                }
            }
        }
    }

    private fun hasBiometricEnrolled(): Boolean = runCatching {
        val manager = BiometricManager.from(this)
        manager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }.getOrDefault(false)

    private fun hasDeviceSecurity(): Boolean = runCatching {
        val manager = BiometricManager.from(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return@runCatching manager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            ) == BiometricManager.BIOMETRIC_SUCCESS
        }
        val keyguard = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        hasBiometricEnrolled() || (keyguard?.isDeviceSecure == true)
    }.getOrDefault(false)

    private fun launchBiometricPrompt(
        allowDeviceCredential: Boolean,
        onSuccess: () -> Unit
    ) {
        if (isFinishing || isDestroyed) return
        val executor = ContextCompat.getMainExecutor(this)
        runCatching {
            val prompt = BiometricPrompt(
                this,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        onSuccess()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        val isUserDismissed = errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                            errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                            errorCode == BiometricPrompt.ERROR_CANCELED
                        if (!isUserDismissed && errString.isNotBlank()) {
                            Toast.makeText(
                                this@MainActivity,
                                errString.toString(),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            )

            val builder = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Desbloquear AppNotas")
                .setSubtitle("Confirma tu identidad para continuar")

            if (allowDeviceCredential) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    builder.setAllowedAuthenticators(
                        BiometricManager.Authenticators.BIOMETRIC_STRONG or
                            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                            BiometricManager.Authenticators.DEVICE_CREDENTIAL
                    )
                } else {
                    @Suppress("DEPRECATION")
                    builder.setDeviceCredentialAllowed(true)
                }
            } else {
                builder.setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.BIOMETRIC_WEAK
                )
                builder.setNegativeButtonText("Usar contraseña")
            }

            prompt.authenticate(builder.build())
        }.onFailure {
            Toast.makeText(
                this,
                "No se pudo abrir la biometría en este dispositivo.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
