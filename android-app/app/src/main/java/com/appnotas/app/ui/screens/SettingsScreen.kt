package com.appnotas.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Password
import androidx.compose.material.icons.rounded.PhonelinkLock
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SettingsBrightness
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.appnotas.app.BuildConfig
import com.appnotas.app.ui.ExplorerUiState
import com.appnotas.app.ui.NavigationDirection
import com.appnotas.app.ui.SettingsSection
import com.appnotas.app.ui.components.ExcelGeneratedResultCard
import com.appnotas.app.ui.components.ExcelPreviewCard
import com.appnotas.app.ui.components.ExportOptionChip
import com.appnotas.app.ui.components.InteractiveSwipeBackContainer
import com.appnotas.app.ui.components.MorphLockIcon
import com.appnotas.app.ui.components.extractEmoji
import com.appnotas.app.ui.components.saveExportToDownloads
import com.appnotas.app.ui.components.shareExcelFile
import com.appnotas.app.ui.motion.EmilDrawerEase
import com.appnotas.app.ui.motion.performConfirmHaptic
import com.appnotas.app.ui.motion.performTickHaptic
import com.appnotas.app.ui.motion.pressableScale
import com.appnotas.app.ui.motion.rememberReducedMotion
import com.appnotas.app.ui.theme.AppPalettes
import com.appnotas.core.ExportPeriod
import com.appnotas.core.LockMode
import kotlinx.coroutines.launch

private fun SettingsSection.label(): String = when (this) {
    SettingsSection.HOME -> "Ajustes"
    SettingsSection.APPEARANCE -> "Apariencia"
    SettingsSection.SECURITY -> "Seguridad"
    SettingsSection.EXPORT -> "Exportar Excel"
    SettingsSection.SYNC -> "Información y versión"
}

private fun SettingsSection.subtitle(): String = when (this) {
    SettingsSection.HOME -> "Elige qué apartado configurar"
    SettingsSection.APPEARANCE -> "Paletas, brillo y negro AMOLED"
    SettingsSection.SECURITY -> "Contraseña propia, huella y bloqueo automático"
    SettingsSection.EXPORT -> "Vista previa y respaldo en Excel (.xlsx)"
    SettingsSection.SYNC -> "Almacenamiento en el dispositivo y versión"
}

@Composable
fun SettingsScreen(
    uiState: ExplorerUiState,
    biometricAvailable: Boolean,
    deviceSecurityAvailable: Boolean,
    onBack: () -> Unit,
    onBackFromSwipe: () -> Unit,
    onSelectSection: (SettingsSection) -> Unit,
    onSelectSectionFromSwipe: (SettingsSection) -> Unit,
    explorerUnderlay: @Composable () -> Unit,
    onClearSnackbar: () -> Unit,
    onOpenPasswordSetup: () -> Unit,
    onUpdateSecurity: (LockMode, Boolean, Boolean) -> Unit,
    onClearAppPassword: () -> Unit,
    onRequestExportPreview: (folderId: String?, period: ExportPeriod) -> Unit,
    onExportExcel: (folderId: String?, period: ExportPeriod) -> Unit,
    onSelectPalette: (String) -> Unit,
    onSelectThemeMode: (String) -> Unit,
    onTogglePureBlack: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val snackbarHostState = remember { SnackbarHostState() }
    val reducedMotion = rememberReducedMotion()
    val section = uiState.settingsSection
    val density = LocalDensity.current
    val initialWidthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    var containerWidthPx by remember { mutableFloatStateOf(initialWidthPx) }

    var lastSection by remember { mutableStateOf(section) }
    var backwardSection by remember { mutableStateOf<SettingsSection?>(null) }
    var backwardOffsetPx by remember { mutableFloatStateOf(0f) }
    var backwardTrigger by remember { mutableIntStateOf(0) }

    if (section != lastSection) {
        if (
            section == SettingsSection.HOME &&
            uiState.settingsNavDirection == NavigationDirection.BACKWARD &&
            !reducedMotion
        ) {
            backwardSection = lastSection
            backwardOffsetPx = 0f
            backwardTrigger += 1
        } else {
            backwardSection = null
            backwardOffsetPx = 0f
        }
        lastSection = section
    }

    LaunchedEffect(backwardTrigger) {
        val exiting = backwardSection ?: return@LaunchedEffect
        val targetWidth = containerWidthPx.coerceAtLeast(1f)
        animate(
            initialValue = 0f,
            targetValue = targetWidth,
            animationSpec = tween(durationMillis = 220, easing = EmilDrawerEase)
        ) { value, _ ->
            if (backwardSection === exiting) {
                backwardOffsetPx = value
            }
        }
        if (backwardSection === exiting) {
            backwardSection = null
            backwardOffsetPx = 0f
        }
    }

    LaunchedEffect(uiState.snackbarMessage) {
        val message = uiState.snackbarMessage
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message)
            onClearSnackbar()
        }
    }

    val backwardProgress = if (backwardSection != null && containerWidthPx > 0f) {
        (backwardOffsetPx / containerWidthPx).coerceIn(0f, 1f)
    } else {
        1f
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .clipToBounds()
            .onSizeChanged {
                if (it.width > 0) containerWidthPx = it.width.toFloat()
            }
    ) {
        InteractiveSwipeBackContainer(
            enabled = backwardSection == null,
            fullWidth = false,
            contentKey = section,
            direction = uiState.settingsNavDirection,
            enterFromRightOnAppear = uiState.rootNavDirection == NavigationDirection.FORWARD,
            underlayContent = {
                if (section == SettingsSection.HOME) {
                    explorerUnderlay()
                } else {
                    SettingsSectionUnderlay(
                        uiState = uiState,
                        section = SettingsSection.HOME,
                        biometricAvailable = biometricAvailable,
                        deviceSecurityAvailable = deviceSecurityAvailable
                    )
                }
            },
            onSwipeBack = {
                if (section != SettingsSection.HOME) {
                    onSelectSectionFromSwipe(SettingsSection.HOME)
                } else {
                    onBackFromSwipe()
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = if (backwardSection != null && !reducedMotion) {
                        -containerWidthPx * 0.12f * (1f - backwardProgress)
                    } else {
                        0f
                    }
                }
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                topBar = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            FilledTonalIconButton(
                                onClick = {
                                    performTickHaptic(view)
                                    if (section != SettingsSection.HOME) {
                                        onSelectSection(SettingsSection.HOME)
                                    } else {
                                        onBack()
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = "Volver"
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = section.label(),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = section.subtitle(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        SettingsBreadcrumbsRow(
                            currentSection = section,
                            onClickHome = {
                                performTickHaptic(view)
                                onSelectSection(SettingsSection.HOME)
                            }
                        )
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    SettingsSectionContent(
                        section = section,
                        uiState = uiState,
                        biometricAvailable = biometricAvailable,
                        deviceSecurityAvailable = deviceSecurityAvailable,
                        onSelectSection = onSelectSection,
                        onOpenPasswordSetup = onOpenPasswordSetup,
                        onUpdateSecurity = onUpdateSecurity,
                        onClearAppPassword = onClearAppPassword,
                        onRequestExportPreview = onRequestExportPreview,
                        onExportExcel = onExportExcel,
                        onSelectPalette = onSelectPalette,
                        onSelectThemeMode = onSelectThemeMode,
                        onTogglePureBlack = onTogglePureBlack
                    )
                }
            }
        }

        backwardSection?.let { exitingSection ->
            val shadowWidthPx = with(density) { 18.dp.toPx() }
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(18.dp)
                    .graphicsLayer {
                        translationX = backwardOffsetPx - shadowWidthPx
                    }
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.09f * (1f - backwardProgress * 0.5f))
                            )
                        )
                    )
            )
            SettingsSectionUnderlay(
                uiState = uiState,
                section = exitingSection,
                biometricAvailable = biometricAvailable,
                deviceSecurityAvailable = deviceSecurityAvailable,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = backwardOffsetPx
                    }
            )
        }
    }
}

@Composable
private fun SettingsSectionContent(
    section: SettingsSection,
    uiState: ExplorerUiState,
    biometricAvailable: Boolean,
    deviceSecurityAvailable: Boolean,
    onSelectSection: (SettingsSection) -> Unit,
    onOpenPasswordSetup: () -> Unit,
    onUpdateSecurity: (LockMode, Boolean, Boolean) -> Unit,
    onClearAppPassword: () -> Unit,
    onRequestExportPreview: (folderId: String?, period: ExportPeriod) -> Unit,
    onExportExcel: (folderId: String?, period: ExportPeriod) -> Unit,
    onSelectPalette: (String) -> Unit,
    onSelectThemeMode: (String) -> Unit,
    onTogglePureBlack: (Boolean) -> Unit
) {
    when (section) {
        SettingsSection.HOME -> SettingsHomeList(
            uiState = uiState,
            onOpenSection = onSelectSection
        )
        SettingsSection.SECURITY -> SettingsSecurityPage(
            uiState = uiState,
            biometricAvailable = biometricAvailable,
            deviceSecurityAvailable = deviceSecurityAvailable,
            onOpenPasswordSetup = onOpenPasswordSetup,
            onUpdateSecurity = onUpdateSecurity,
            onClearAppPassword = onClearAppPassword
        )
        SettingsSection.EXPORT -> SettingsExportPage(
            uiState = uiState,
            onRequestPreview = onRequestExportPreview,
            onExport = onExportExcel
        )
        SettingsSection.APPEARANCE -> SettingsAppearancePage(
            uiState = uiState,
            onSelectPalette = onSelectPalette,
            onSelectThemeMode = onSelectThemeMode,
            onTogglePureBlack = onTogglePureBlack
        )
        SettingsSection.SYNC -> SettingsSyncPage()
    }
}

@Composable
private fun SettingsBreadcrumbsRow(
    currentSection: SettingsSection,
    onClickHome: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val isHome = currentSection == SettingsSection.HOME
        val homeShape = RoundedCornerShape(14.dp)
        Surface(
            shape = homeShape,
            color = if (isHome) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
            modifier = Modifier
                .clip(homeShape)
                .clickable(onClick = onClickHome)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = null,
                    tint = if (isHome) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = "Ajustes",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isHome) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (isHome) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }

        if (!isHome) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Text(
                    text = currentSection.label(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                )
            }
        }
    }
}

@Composable
private fun SettingsHomeList(
    uiState: ExplorerUiState,
    onOpenSection: (SettingsSection) -> Unit
) {
    val view = LocalView.current
    val currentPaletteName = AppPalettes.firstOrNull { it.id == uiState.appearance.paletteId }?.label ?: "Aurora"
    val themeName = when (uiState.appearance.themeMode) {
        "light" -> "Claro"
        "dark" -> if (uiState.appearance.pureBlack) "Oscuro AMOLED" else "Oscuro"
        else -> "Sistema"
    }
    val securitySummary = when (uiState.security.lockMode) {
        LockMode.NONE -> "Sin bloqueo"
        LockMode.APP_PASSWORD -> "Contraseña propia activa"
        LockMode.DEVICE_BIOMETRIC -> "Seguridad del dispositivo"
    }
    val infoSummary = "Guardado en tu dispositivo · v${BuildConfig.VERSION_NAME}"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "home_appearance") {
            SettingsMenuCard(
                icon = Icons.Rounded.Palette,
                accentColor = MaterialTheme.colorScheme.primary,
                title = "Apariencia y color",
                subtitle = "$currentPaletteName · $themeName",
                onClick = {
                    performTickHaptic(view)
                    onOpenSection(SettingsSection.APPEARANCE)
                }
            )
        }
        item(key = "home_security") {
            SettingsMenuCard(
                icon = Icons.Rounded.Lock,
                accentColor = MaterialTheme.colorScheme.tertiary,
                title = "Seguridad y bloqueo",
                subtitle = securitySummary,
                onClick = {
                    performTickHaptic(view)
                    onOpenSection(SettingsSection.SECURITY)
                }
            )
        }
        item(key = "home_export") {
            SettingsMenuCard(
                icon = Icons.Rounded.TableChart,
                accentColor = Color(0xFF0F766E),
                title = "Reportes y respaldo Excel",
                subtitle = "Vista previa y exportación por negocio o período",
                onClick = {
                    performTickHaptic(view)
                    onOpenSection(SettingsSection.EXPORT)
                }
            )
        }
        item(key = "home_sync") {
            SettingsMenuCard(
                icon = Icons.Rounded.Info,
                accentColor = MaterialTheme.colorScheme.secondary,
                title = "Información y versión",
                subtitle = infoSummary,
                onClick = {
                    performTickHaptic(view)
                    onOpenSection(SettingsSection.SYNC)
                }
            )
        }
    }
}

@Composable
private fun SettingsMenuCard(
    icon: ImageVector,
    accentColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    val interaction = remember { MutableInteractionSource() }
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .pressableScale(interaction, pressedScale = 0.98f)
            .clip(shape)
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                onClick = onClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(15.dp),
                color = accentColor.copy(alpha = 0.14f),
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(23.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
            )
        }
    }
}

@Composable
private fun SettingsSecurityPage(
    uiState: ExplorerUiState,
    biometricAvailable: Boolean,
    deviceSecurityAvailable: Boolean,
    onOpenPasswordSetup: () -> Unit,
    onUpdateSecurity: (LockMode, Boolean, Boolean) -> Unit,
    onClearAppPassword: () -> Unit
) {
    val view = LocalView.current
    val security = uiState.security

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "sec_hero") {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = if (security.lockMode == LockMode.NONE) {
                    MaterialTheme.colorScheme.surfaceContainerLow
                } else {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    MorphLockIcon(
                        isLocked = security.lockMode != LockMode.NONE,
                        tint = if (security.lockMode == LockMode.NONE) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        size = 32.dp
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when (security.lockMode) {
                                LockMode.NONE -> "Acceso libre (sin bloqueo)"
                                LockMode.APP_PASSWORD -> "Protegida con contraseña propia"
                                LockMode.DEVICE_BIOMETRIC -> "Protegida con seguridad del celular"
                            },
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = when (security.lockMode) {
                                LockMode.NONE -> "Configura una clave exclusiva para proteger tus negocios y cuentas."
                                LockMode.APP_PASSWORD ->
                                    if (security.biometricEnabled && biometricAvailable) {
                                        "Contraseña independiente de la app + huella opcional activa."
                                    } else {
                                        "Requiere tu contraseña exclusiva de AppNotas."
                                    }
                                LockMode.DEVICE_BIOMETRIC -> "Usa huella, rostro o PIN del sistema Android."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item(key = "sec_modes") {
            SettingsSectionCard(
                title = "Método de desbloqueo",
                subtitle = "Elige cómo se abre la aplicación"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecurityOptionRow(
                        icon = Icons.Rounded.Password,
                        title = "Contraseña propia de la app",
                        subtitle = if (security.hasPassword) {
                            "Clave exclusiva configurada · Lista para usar"
                        } else {
                            "Recomendado: PIN o clave distinta a la del celular"
                        },
                        selected = security.lockMode == LockMode.APP_PASSWORD,
                        onClick = {
                            performTickHaptic(view)
                            if (security.hasPassword) {
                                onUpdateSecurity(
                                    LockMode.APP_PASSWORD,
                                    security.biometricEnabled,
                                    security.autoLockOnBackground
                                )
                            } else {
                                onOpenPasswordSetup()
                            }
                        }
                    )

                    SecurityOptionRow(
                        icon = Icons.Rounded.PhonelinkLock,
                        title = "Seguridad del dispositivo",
                        subtitle = if (deviceSecurityAvailable) {
                            "Huella, rostro o PIN del sistema Android"
                        } else {
                            "Activa primero un bloqueo en Ajustes de Android"
                        },
                        selected = security.lockMode == LockMode.DEVICE_BIOMETRIC,
                        enabled = deviceSecurityAvailable,
                        onClick = {
                            performTickHaptic(view)
                            onUpdateSecurity(
                                LockMode.DEVICE_BIOMETRIC,
                                true,
                                security.autoLockOnBackground
                            )
                        }
                    )

                    SecurityOptionRow(
                        icon = Icons.Rounded.LockOpen,
                        title = "Sin bloqueo",
                        subtitle = "Entrar directamente al explorador",
                        selected = security.lockMode == LockMode.NONE,
                        onClick = {
                            performTickHaptic(view)
                            onUpdateSecurity(
                                LockMode.NONE,
                                false,
                                security.autoLockOnBackground
                            )
                        }
                    )
                }

                if (security.hasPassword) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = {
                                performTickHaptic(view)
                                onOpenPasswordSetup()
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cambiar clave")
                        }
                        TextButton(
                            onClick = {
                                performTickHaptic(view)
                                onClearAppPassword()
                            },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Quitar")
                        }
                    }
                }
            }
        }

        if (security.lockMode != LockMode.NONE) {
            item(key = "sec_extra_switches") {
                SettingsSectionCard(
                    title = "Opciones adicionales",
                    subtitle = "Comportamiento al usar o minimizar la app"
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AnimatedVisibility(
                            visible = security.lockMode == LockMode.APP_PASSWORD && biometricAvailable
                        ) {
                            SettingsSwitchRow(
                                icon = Icons.Rounded.Fingerprint,
                                title = "Permitir también huella / rostro",
                                subtitle = "Mantén tu clave propia como respaldo",
                                checked = security.biometricEnabled,
                                onCheckedChange = { checked ->
                                    performTickHaptic(view)
                                    onUpdateSecurity(
                                        LockMode.APP_PASSWORD,
                                        checked,
                                        security.autoLockOnBackground
                                    )
                                }
                            )
                        }

                        SettingsSwitchRow(
                            icon = Icons.Rounded.PhonelinkLock,
                            title = "Bloquear al salir de la app",
                            subtitle = "Pedir verificación al volver de segundo plano",
                            checked = security.autoLockOnBackground,
                            onCheckedChange = { checked ->
                                performTickHaptic(view)
                                onUpdateSecurity(
                                    security.lockMode,
                                    security.biometricEnabled,
                                    checked
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsExportPage(
    uiState: ExplorerUiState,
    onRequestPreview: (folderId: String?, period: ExportPeriod) -> Unit,
    onExport: (folderId: String?, period: ExportPeriod) -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current

    var selectedFolderId by remember { mutableStateOf<String?>(null) }
    var selectedPeriod by remember { mutableStateOf(ExportPeriod.WEEK) }
    var downloadedToDevice by remember(uiState.lastExportResult) { mutableStateOf(false) }
    var isSavingDownload by remember(uiState.lastExportResult) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(selectedFolderId, selectedPeriod) {
        onRequestPreview(selectedFolderId, selectedPeriod)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "exp_scope") {
            SettingsSectionCard(
                title = "Qué deseas exportar",
                subtitle = "Elige si exportar todos tus negocios o una carpeta principal específica"
            ) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    item(key = "scope_all") {
                        ExportOptionChip(
                            label = "Todos los negocios",
                            selected = selectedFolderId == null,
                            onClick = {
                                performTickHaptic(view)
                                selectedFolderId = null
                            }
                        )
                    }
                    items(uiState.rootFolders, key = { it.id }) { folder ->
                        val emoji = extractEmoji(folder.icon)
                        ExportOptionChip(
                            label = if (emoji != null) "$emoji ${folder.name}" else folder.name,
                            selected = selectedFolderId == folder.id,
                            onClick = {
                                performTickHaptic(view)
                                selectedFolderId = folder.id
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "¿De qué fechas quieres exportar?",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Filtra solo tus anotaciones de esta semana, de este mes o el historial completo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
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
        }

        item(key = "exp_preview") {
            ExcelPreviewCard(
                preview = uiState.exportPreview,
                isLoading = uiState.isLoadingExportPreview
            )
        }

        item(key = "exp_action") {
            val exportInteraction = remember { MutableInteractionSource() }
            Button(
                onClick = {
                    performConfirmHaptic(view)
                    onExport(selectedFolderId, selectedPeriod)
                },
                enabled = !uiState.isExporting,
                interactionSource = exportInteraction,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .pressableScale(exportInteraction, pressedScale = 0.97f, enabled = !uiState.isExporting)
            ) {
                if (uiState.isExporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generando Excel...")
                } else {
                    Icon(
                        imageVector = Icons.Rounded.TableChart,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (uiState.lastExportResult == null) {
                            "Generar Excel (.xlsx)"
                        } else {
                            "Generar otro reporte"
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }

        if (uiState.lastExportResult != null) {
            item(key = "exp_result") {
                ExcelGeneratedResultCard(
                    result = uiState.lastExportResult,
                    downloadedToDevice = downloadedToDevice,
                    onDownload = {
                        if (!isSavingDownload) {
                            performTickHaptic(view)
                            isSavingDownload = true
                            scope.launch {
                                try {
                                    if (saveExportToDownloads(context, uiState.lastExportResult)) {
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
                        shareExcelFile(context, uiState.lastExportResult)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsAppearancePage(
    uiState: ExplorerUiState,
    onSelectPalette: (String) -> Unit,
    onSelectThemeMode: (String) -> Unit,
    onTogglePureBlack: (Boolean) -> Unit
) {
    val view = LocalView.current
    val appearance = uiState.appearance

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "app_palettes") {
            SettingsSectionCard(
                title = "Paleta de colores",
                subtitle = "Personaliza el estilo visual con tipografía Allo"
            ) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = 2
                ) {
                    AppPalettes.forEach { preset ->
                        val selected = appearance.paletteId == preset.id
                        val shape = RoundedCornerShape(18.dp)
                        val cardInteraction = remember { MutableInteractionSource() }
                        Surface(
                            shape = shape,
                            color = if (selected) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                            modifier = Modifier
                                .weight(1f)
                                .pressableScale(cardInteraction, pressedScale = 0.96f)
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
                                .clickable(
                                    interactionSource = cardInteraction,
                                    indication = LocalIndication.current
                                ) {
                                    performTickHaptic(view)
                                    onSelectPalette(preset.id)
                                }
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                        preset.previewColors.forEach { swatch ->
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clip(CircleShape)
                                                    .background(swatch)
                                            )
                                        }
                                    }
                                    if (selected) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = "Activa",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = preset.label,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = preset.tagline,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        item(key = "app_theme_mode") {
            SettingsSectionCard(
                title = "Brillo y contraste",
                subtitle = "Elige entre modo claro, oscuro o negro absoluto"
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeModeChip(
                        icon = Icons.Rounded.SettingsBrightness,
                        label = "Sistema",
                        selected = appearance.themeMode == "system",
                        onClick = {
                            performTickHaptic(view)
                            onSelectThemeMode("system")
                        },
                        modifier = Modifier.weight(1f)
                    )
                    ThemeModeChip(
                        icon = Icons.Rounded.LightMode,
                        label = "Claro",
                        selected = appearance.themeMode == "light",
                        onClick = {
                            performTickHaptic(view)
                            onSelectThemeMode("light")
                        },
                        modifier = Modifier.weight(1f)
                    )
                    ThemeModeChip(
                        icon = Icons.Rounded.DarkMode,
                        label = "Oscuro",
                        selected = appearance.themeMode == "dark",
                        onClick = {
                            performTickHaptic(view)
                            onSelectThemeMode("dark")
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                SettingsSwitchRow(
                    icon = Icons.Rounded.Contrast,
                    title = "Negro puro AMOLED",
                    subtitle = "Fondo negro absoluto al usar tema oscuro",
                    checked = appearance.pureBlack,
                    onCheckedChange = { checked ->
                        performTickHaptic(view)
                        onTogglePureBlack(checked)
                    }
                )
            }
        }
    }
}

@Composable
private fun SettingsSyncPage() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "sync_details") {
            SettingsSectionCard(
                title = "Almacenamiento en el dispositivo",
                subtitle = "Tus negocios, carpetas y registros viven en la memoria interna de tu móvil"
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Smartphone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "100% en tu dispositivo",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Base de datos rápida en tu teléfono con respaldo exportable a Excel (.xlsx) en cualquier momento.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "AppNotas v${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun SecurityOptionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val shape = RoundedCornerShape(18.dp)
    Surface(
        shape = shape,
        color = when {
            !enabled -> MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f)
            selected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
            else -> MaterialTheme.colorScheme.surfaceContainer
        },
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .then(
                if (selected) {
                    Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, shape)
                } else {
                    Modifier
                }
            )
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    }
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Seleccionado",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable { onCheckedChange(!checked) }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun ThemeModeChip(
    icon: ImageVector,
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
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
private fun SettingsSectionUnderlay(
    uiState: ExplorerUiState,
    section: SettingsSection,
    biometricAvailable: Boolean,
    deviceSecurityAvailable: Boolean,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilledTonalIconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = null
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = section.label(),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = section.subtitle(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                SettingsBreadcrumbsRow(
                    currentSection = section,
                    onClickHome = {}
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            SettingsSectionContent(
                section = section,
                uiState = uiState,
                biometricAvailable = biometricAvailable,
                deviceSecurityAvailable = deviceSecurityAvailable,
                onSelectSection = {},
                onOpenPasswordSetup = {},
                onUpdateSecurity = { _, _, _ -> },
                onClearAppPassword = {},
                onRequestExportPreview = { _, _ -> },
                onExportExcel = { _, _ -> },
                onSelectPalette = {},
                onSelectThemeMode = {},
                onTogglePureBlack = {}
            )
        }
    }
}
