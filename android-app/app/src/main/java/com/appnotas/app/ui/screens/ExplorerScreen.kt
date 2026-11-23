package com.appnotas.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.appnotas.app.ui.DeleteConfirmationState
import com.appnotas.app.ui.ExplorerUiState
import com.appnotas.app.ui.ExplorerViewModel
import com.appnotas.app.ui.NavigationDirection
import com.appnotas.app.ui.components.BreadcrumbsBar
import com.appnotas.app.ui.components.ExpressiveFabMenuOverlay
import com.appnotas.app.ui.components.FolderAvatarBadge
import com.appnotas.app.ui.components.FolderEditorSheet
import com.appnotas.app.ui.components.FolderRowCard
import com.appnotas.app.ui.components.InteractiveSwipeBackContainer
import com.appnotas.app.ui.components.MorphSearchBackIcon
import com.appnotas.app.ui.components.RecordEditorSheet
import com.appnotas.app.ui.components.RecordRowCard
import com.appnotas.app.ui.components.SummaryCard
import com.appnotas.app.ui.components.formatNumberDisplay
import com.appnotas.app.ui.components.resolveFolderColor
import com.appnotas.app.ui.motion.AppExpressiveMotion
import com.appnotas.app.ui.motion.EmilDrawerEase
import com.appnotas.app.ui.motion.EmilEaseOut
import com.appnotas.app.ui.motion.performConfirmHaptic
import com.appnotas.app.ui.motion.performTickHaptic
import com.appnotas.app.ui.motion.pressableScale
import com.appnotas.app.ui.motion.rememberReducedMotion
import com.appnotas.app.ui.theme.segmentedGroupShape
import com.appnotas.core.FolderItem
import com.appnotas.core.FolderViewData
import com.appnotas.core.RecordItem
import com.appnotas.core.SearchResults

@Composable
fun ExplorerScreen(
    uiState: ExplorerUiState,
    viewModel: ExplorerViewModel
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val reducedMotion = rememberReducedMotion()
    val view = LocalView.current
    val density = LocalDensity.current
    val initialWidthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    var explorerWidthPx by remember { mutableFloatStateOf(initialWidthPx) }

    LaunchedEffect(uiState.snackbarMessage) {
        val message = uiState.snackbarMessage
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearSnackbar()
        }
    }

    val folderView = uiState.folderView
    val currentFolderKey = folderView?.currentFolder?.id
    var lastFolderKey by remember { mutableStateOf(currentFolderKey) }
    var lastFolderView by remember { mutableStateOf(folderView) }
    var backwardFolderView by remember { mutableStateOf<FolderViewData?>(null) }
    var backwardOffsetPx by remember { mutableFloatStateOf(0f) }
    var backwardTrigger by remember { mutableIntStateOf(0) }

    if (currentFolderKey != lastFolderKey) {
        val previous = lastFolderView
        if (
            previous != null &&
            folderView != null &&
            uiState.navigationDirection == NavigationDirection.BACKWARD &&
            !reducedMotion
        ) {
            backwardFolderView = previous
            backwardOffsetPx = 0f
            backwardTrigger += 1
        } else {
            backwardFolderView = null
            backwardOffsetPx = 0f
        }
        lastFolderKey = currentFolderKey
        lastFolderView = folderView
    } else if (folderView != null) {
        lastFolderView = folderView
    }

    LaunchedEffect(backwardTrigger) {
        val exiting = backwardFolderView ?: return@LaunchedEffect
        val targetWidth = explorerWidthPx.coerceAtLeast(1f)
        animate(
            initialValue = 0f,
            targetValue = targetWidth,
            animationSpec = tween(durationMillis = 220, easing = EmilDrawerEase)
        ) { value, _ ->
            if (backwardFolderView === exiting) {
                backwardOffsetPx = value
            }
        }
        if (backwardFolderView === exiting) {
            backwardFolderView = null
            backwardOffsetPx = 0f
        }
    }

    val breadcrumbs = folderView?.breadcrumbs.orEmpty()
    val parentFolderId = if (breadcrumbs.size >= 2) {
        breadcrumbs[breadcrumbs.size - 2].id
    } else {
        null
    }
    val parentFolderView = uiState.folderViewCache[parentFolderId]
    val canSwipeBack = !uiState.isSearchExpanded &&
        !uiState.isFabMenuExpanded &&
        breadcrumbs.size > 1 &&
        parentFolderView != null &&
        backwardFolderView == null

    val backwardProgress = if (backwardFolderView != null && explorerWidthPx > 0f) {
        (backwardOffsetPx / explorerWidthPx).coerceIn(0f, 1f)
    } else {
        1f
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .clipToBounds()
            .onSizeChanged {
                if (it.width > 0) explorerWidthPx = it.width.toFloat()
            }
    ) {
        if (folderView != null) {
            InteractiveSwipeBackContainer(
                enabled = canSwipeBack,
                fullWidth = true,
                contentKey = folderView.currentFolder?.id,
                direction = uiState.navigationDirection,
                underlayContent = {
                    if (parentFolderView != null) {
                        FolderScreenLayer(
                            folderView = parentFolderView,
                            isRefreshing = false,
                            reducedMotion = reducedMotion,
                            isUnderlay = true,
                            onRefresh = {},
                            onOpenSearch = {},
                            onNavigateBack = {},
                            onSettingsClick = {},
                            onOpenFolder = {},
                            onEditCurrentFolder = {},
                            onExportCurrentScope = {},
                            onCreateFolderHere = {},
                            onOpenSettings = {},
                            onEditFolder = {},
                            onExportFolder = {},
                            onDeleteFolder = {},
                            onEditRecord = {},
                            onDeleteRecord = {},
                            onCreatePrimaryAction = {}
                        )
                    }
                },
                onSwipeBack = { viewModel.navigateUpFromSwipe() },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = if (backwardFolderView != null && !reducedMotion) {
                            -explorerWidthPx * 0.12f * (1f - backwardProgress)
                        } else {
                            0f
                        }
                    }
            ) {
                FolderScreenLayer(
                    folderView = folderView,
                    isRefreshing = uiState.isRefreshing,
                    reducedMotion = reducedMotion,
                    isUnderlay = false,
                    onRefresh = viewModel::pullToRefresh,
                    onOpenSearch = { viewModel.setSearchExpanded(true) },
                    onNavigateBack = { viewModel.navigateUp() },
                    onSettingsClick = viewModel::openSettings,
                    onOpenFolder = viewModel::openFolder,
                    onEditCurrentFolder = {
                        folderView.currentFolder?.let { current ->
                            viewModel.openEditFolderSheet(current)
                        }
                    },
                    onExportCurrentScope = {
                        val current = folderView.currentFolder
                        viewModel.openExportSheet(
                            folderId = current?.id,
                            folderName = current?.name
                        )
                    },
                    onCreateFolderHere = viewModel::openCreateFolderSheet,
                    onOpenSettings = viewModel::openSettings,
                    onEditFolder = viewModel::openEditFolderSheet,
                    onExportFolder = { folder ->
                        viewModel.openExportSheet(folder.id, folder.name)
                    },
                    onDeleteFolder = viewModel::requestDeleteFolder,
                    onEditRecord = viewModel::openEditRecordSheet,
                    onDeleteRecord = viewModel::requestDeleteRecord,
                    onCreatePrimaryAction = {
                        if (uiState.currentFolderId == null) {
                            viewModel.openCreateFolderSheet()
                        } else {
                            viewModel.openCreateRecordSheet()
                        }
                    }
                )
            }

            backwardFolderView?.let { exitingFolderView ->
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
                FolderScreenLayer(
                    folderView = exitingFolderView,
                    isRefreshing = false,
                    reducedMotion = true,
                    isUnderlay = true,
                    onRefresh = {},
                    onOpenSearch = {},
                    onNavigateBack = {},
                    onSettingsClick = {},
                    onOpenFolder = {},
                    onEditCurrentFolder = {},
                    onExportCurrentScope = {},
                    onCreateFolderHere = {},
                    onOpenSettings = {},
                    onEditFolder = {},
                    onExportFolder = {},
                    onDeleteFolder = {},
                    onEditRecord = {},
                    onDeleteRecord = {},
                    onCreatePrimaryAction = {},
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationX = backwardOffsetPx
                        }
                )
            }

            ExpressiveFabMenuOverlay(
                expanded = uiState.isFabMenuExpanded,
                visible = !uiState.isSearchExpanded,
                isInsideFolder = uiState.currentFolderId != null,
                onToggle = viewModel::toggleFabMenu,
                onDismiss = viewModel::dismissFabMenu,
                onCreateFolder = viewModel::openCreateFolderSheet,
                onCreateRecord = viewModel::openCreateRecordSheet
            )
        } else if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        AnimatedVisibility(
            visible = uiState.isSearchExpanded,
            enter = if (reducedMotion) {
                fadeIn(tween(90))
            } else {
                fadeIn(tween(200, easing = EmilDrawerEase)) +
                    slideInVertically(tween(240, easing = EmilDrawerEase)) { it / 16 }
            },
            exit = if (reducedMotion) {
                fadeOut(tween(80))
            } else {
                fadeOut(tween(170, easing = EmilEaseOut)) +
                    slideOutVertically(tween(200, easing = EmilEaseOut)) { it / 16 }
            },
            modifier = Modifier.fillMaxSize()
        ) {
            SearchOverlayScreen(
                query = uiState.searchQuery,
                results = uiState.searchResults,
                placeholder = folderView?.currentFolder?.name?.let { "Buscar en ($it)" }
                    ?: "Buscar negocios y registros...",
                onQueryChange = viewModel::onSearchQueryChange,
                onClose = { viewModel.setSearchExpanded(false) },
                onOpenFolder = viewModel::openFolder,
                onEditFolder = viewModel::openEditFolderSheet,
                onExportFolder = { folder -> viewModel.openExportSheet(folder.id, folder.name) },
                onDeleteFolder = viewModel::requestDeleteFolder,
                onEditRecord = viewModel::openEditRecordSheet,
                onDeleteRecord = viewModel::requestDeleteRecord
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 84.dp)
        )
    }

    uiState.folderSheet?.let { sheet ->
        FolderEditorSheet(
            state = sheet,
            onDismiss = viewModel::dismissFolderSheet,
            onSave = viewModel::saveFolder
        )
    }

    uiState.recordSheet?.let { sheet ->
        RecordEditorSheet(
            state = sheet,
            onDismiss = viewModel::dismissRecordSheet,
            onSave = viewModel::saveRecord
        )
    }

    uiState.deleteConfirmation?.let { target ->
        val (title, body) = when (target) {
            is DeleteConfirmationState.Folder -> {
                "Eliminar carpeta" to
                    "¿Eliminar '${target.folder.name}'? También se eliminarán todas sus subcarpetas y registros."
            }
            is DeleteConfirmationState.Record -> {
                "Eliminar registro" to
                    "¿Eliminar el registro '${target.record.title}'?"
            }
        }

        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteConfirmation,
            title = { Text(title) },
            text = { Text(body) },
            confirmButton = {
                TextButton(
                    onClick = {
                        performConfirmHaptic(view)
                        viewModel.confirmDelete()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDeleteConfirmation) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FolderScreenLayer(
    folderView: FolderViewData,
    isRefreshing: Boolean,
    reducedMotion: Boolean,
    isUnderlay: Boolean,
    onRefresh: () -> Unit,
    onOpenSearch: () -> Unit,
    onNavigateBack: () -> Unit,
    onSettingsClick: () -> Unit,
    onOpenFolder: (String?) -> Unit,
    onEditCurrentFolder: () -> Unit,
    onExportCurrentScope: () -> Unit,
    onCreateFolderHere: () -> Unit,
    onOpenSettings: () -> Unit,
    onEditFolder: (FolderItem) -> Unit,
    onExportFolder: (FolderItem) -> Unit,
    onDeleteFolder: (FolderItem) -> Unit,
    onEditRecord: (RecordItem) -> Unit,
    onDeleteRecord: (RecordItem) -> Unit,
    onCreatePrimaryAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isRoot = folderView.currentFolder == null
    val accentColor by animateColorAsState(
        targetValue = resolveFolderColor(
            hex = folderView.currentFolder?.color,
            fallback = MaterialTheme.colorScheme.primary
        ),
        animationSpec = tween(durationMillis = if (reducedMotion) 0 else 240, easing = EmilEaseOut),
        label = "layerAmbianceAccent"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Degradado continuo único desde la barra de estado hacia todo el encabezado
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.14f),
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.06f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            TopSearchPill(
                currentFolderName = folderView.currentFolder?.name,
                canGoUp = !isRoot,
                onOpenSearch = onOpenSearch,
                onNavigateBack = onNavigateBack,
                onSettingsClick = onSettingsClick
            )

            BreadcrumbsBar(
                breadcrumbs = folderView.breadcrumbs,
                onBreadcrumbClick = onOpenFolder
            )

            if (isUnderlay) {
                Box(modifier = Modifier.weight(1f)) {
                    FolderExplorerBody(
                        folderView = folderView,
                        isRoot = isRoot,
                        accentColor = accentColor,
                        reducedMotion = reducedMotion,
                        onOpenFolder = onOpenFolder,
                        onEditCurrentFolder = onEditCurrentFolder,
                        onExportCurrentScope = onExportCurrentScope,
                        onCreateFolderHere = onCreateFolderHere,
                        onOpenSettings = onOpenSettings,
                        onEditFolder = onEditFolder,
                        onExportFolder = onExportFolder,
                        onDeleteFolder = onDeleteFolder,
                        onEditRecord = onEditRecord,
                        onDeleteRecord = onDeleteRecord,
                        onCreatePrimaryAction = onCreatePrimaryAction
                    )
                }
            } else {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.weight(1f)
                ) {
                    FolderExplorerBody(
                        folderView = folderView,
                        isRoot = isRoot,
                        accentColor = accentColor,
                        reducedMotion = reducedMotion,
                        onOpenFolder = onOpenFolder,
                        onEditCurrentFolder = onEditCurrentFolder,
                        onExportCurrentScope = onExportCurrentScope,
                        onCreateFolderHere = onCreateFolderHere,
                        onOpenSettings = onOpenSettings,
                        onEditFolder = onEditFolder,
                        onExportFolder = onExportFolder,
                        onDeleteFolder = onDeleteFolder,
                        onEditRecord = onEditRecord,
                        onDeleteRecord = onDeleteRecord,
                        onCreatePrimaryAction = onCreatePrimaryAction
                    )
                }
            }
        }
    }
}

@Composable
private fun TopSearchPill(
    currentFolderName: String?,
    canGoUp: Boolean,
    onOpenSearch: () -> Unit,
    onNavigateBack: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val view = LocalView.current
    val pillInteraction = remember { MutableInteractionSource() }

    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .height(54.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    performTickHaptic(view)
                    if (canGoUp) {
                        onNavigateBack()
                    } else {
                        onOpenSearch()
                    }
                }
            ) {
                MorphSearchBackIcon(
                    isBack = canGoUp,
                    tint = if (canGoUp) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(
                        interactionSource = pillInteraction,
                        indication = LocalIndication.current,
                        onClick = {
                            performTickHaptic(view)
                            onOpenSearch()
                        }
                    ),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = currentFolderName?.let { "Buscar en ($it)" }
                        ?: "Buscar negocios y registros...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            IconButton(
                onClick = {
                    performTickHaptic(view)
                    onSettingsClick()
                }
            ) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = "Ajustes",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SearchOverlayScreen(
    query: String,
    results: SearchResults?,
    placeholder: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    onOpenFolder: (String?) -> Unit,
    onEditFolder: (FolderItem) -> Unit,
    onExportFolder: (FolderItem) -> Unit,
    onDeleteFolder: (FolderItem) -> Unit,
    onEditRecord: (RecordItem) -> Unit,
    onDeleteRecord: (RecordItem) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val view = LocalView.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )

                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (query.isEmpty()) {
                                Text(
                                    text = placeholder,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            BasicTextField(
                                value = query,
                                onValueChange = onQueryChange,
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester)
                            )
                        }

                        if (query.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    performTickHaptic(view)
                                    onQueryChange("")
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Limpiar texto",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                FilledTonalIconButton(
                    onClick = {
                        performTickHaptic(view)
                        keyboardController?.hide()
                        onClose()
                    },
                    modifier = Modifier.size(50.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Cerrar búsqueda"
                    )
                }
            }

            when {
                query.isBlank() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                                modifier = Modifier.size(44.dp)
                            )
                            Text(
                                text = "Busca al instante por carpetas, conceptos, datos adicionales, unidades o notas.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                results == null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp))
                    }
                }
                results.totalMatches == 0UL -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Sin resultados para \"${results.query}\".",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        item(key = "search_summary") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${results.totalMatches} coincidencias",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (results.hasNumericSummary) {
                                    Text(
                                        text = "Suma: ${formatNumberDisplay(results.numericSum)}",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        itemsIndexed(
                            items = results.folders,
                            key = { _, match -> "s_folder_${match.folder.id}" }
                        ) { index, match ->
                            FolderRowCard(
                                folder = match.folder,
                                shape = segmentedGroupShape(index, results.folders.size),
                                subtitleOverride = match.pathDisplay,
                                onClick = {
                                    keyboardController?.hide()
                                    onOpenFolder(match.folder.id)
                                },
                                onEdit = { onEditFolder(match.folder) },
                                onExport = { onExportFolder(match.folder) },
                                onDelete = { onDeleteFolder(match.folder) }
                            )
                        }
                        if (results.folders.isNotEmpty() && results.records.isNotEmpty()) {
                            item(key = "search_spacer") {
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                        }
                        itemsIndexed(
                            items = results.records,
                            key = { _, match -> "s_record_${match.record.id}" }
                        ) { index, match ->
                            RecordRowCard(
                                record = match.record,
                                shape = segmentedGroupShape(index, results.records.size),
                                pathSubtitle = match.pathDisplay,
                                onClick = {
                                    keyboardController?.hide()
                                    onEditRecord(match.record)
                                },
                                onDelete = { onDeleteRecord(match.record) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderExplorerBody(
    folderView: FolderViewData,
    isRoot: Boolean,
    accentColor: Color,
    reducedMotion: Boolean,
    onOpenFolder: (String?) -> Unit,
    onEditCurrentFolder: () -> Unit,
    onExportCurrentScope: () -> Unit,
    onCreateFolderHere: () -> Unit,
    onOpenSettings: () -> Unit,
    onEditFolder: (FolderItem) -> Unit,
    onExportFolder: (FolderItem) -> Unit,
    onDeleteFolder: (FolderItem) -> Unit,
    onEditRecord: (RecordItem) -> Unit,
    onDeleteRecord: (RecordItem) -> Unit,
    onCreatePrimaryAction: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        FolderHeroHeader(
            currentFolder = folderView.currentFolder,
            totalCount = folderView.totalCount,
            subfolderCount = folderView.subfolderCount,
            recordCount = folderView.recordCount,
            onEditCurrentFolder = onEditCurrentFolder,
            onExportCurrentScope = onExportCurrentScope,
            onCreateFolderHere = onCreateFolderHere,
            onOpenSettings = onOpenSettings
        )

        SummaryCard(
            folderView = folderView,
            accentColor = accentColor
        )

        if (folderView.subfolders.isEmpty() && folderView.records.isEmpty()) {
            EmptyFolderState(
                isRoot = isRoot,
                onCreatePrimaryAction = onCreatePrimaryAction,
                modifier = Modifier.weight(1f)
            )
        } else {
            val spatialSpec = AppExpressiveMotion.defaultSpatialSpec<androidx.compose.ui.unit.IntOffset>()
            val effectsSpec = AppExpressiveMotion.fastEffectsSpec<Float>()

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 104.dp
                ),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (folderView.subfolders.isNotEmpty()) {
                    item(key = "header_folders") {
                        SectionHeader(
                            title = if (isRoot) {
                                "Negocios y carpetas (${folderView.subfolderCount})"
                            } else {
                                "Subcarpetas (${folderView.subfolderCount})"
                            },
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    itemsIndexed(
                        items = folderView.subfolders,
                        key = { _, folder -> "folder_${folder.id}" }
                    ) { index, folder ->
                        FolderRowCard(
                            folder = folder,
                            shape = segmentedGroupShape(index, folderView.subfolders.size),
                            onClick = { onOpenFolder(folder.id) },
                            onEdit = { onEditFolder(folder) },
                            onExport = { onExportFolder(folder) },
                            onDelete = { onDeleteFolder(folder) },
                            modifier = if (reducedMotion) {
                                Modifier
                            } else {
                                Modifier.animateItem(
                                    fadeInSpec = effectsSpec,
                                    placementSpec = spatialSpec,
                                    fadeOutSpec = effectsSpec
                                )
                            }
                        )
                    }
                }

                if (folderView.records.isNotEmpty()) {
                    item(key = "header_records") {
                        SectionHeader(
                            title = "Registros (${folderView.recordCount})",
                            modifier = Modifier.padding(
                                top = if (folderView.subfolders.isNotEmpty()) 14.dp else 0.dp,
                                bottom = 4.dp
                            )
                        )
                    }
                    itemsIndexed(
                        items = folderView.records,
                        key = { _, record -> "record_${record.id}" }
                    ) { index, record ->
                        RecordRowCard(
                            record = record,
                            shape = segmentedGroupShape(index, folderView.records.size),
                            onClick = { onEditRecord(record) },
                            onDelete = { onDeleteRecord(record) },
                            modifier = if (reducedMotion) {
                                Modifier
                            } else {
                                Modifier.animateItem(
                                    fadeInSpec = effectsSpec,
                                    placementSpec = spatialSpec,
                                    fadeOutSpec = effectsSpec
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
private fun FolderHeroHeader(
    currentFolder: FolderItem?,
    totalCount: ULong,
    subfolderCount: ULong,
    recordCount: ULong,
    onEditCurrentFolder: () -> Unit,
    onExportCurrentScope: () -> Unit,
    onCreateFolderHere: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val view = LocalView.current
    var functionsMenuOpen by remember { mutableStateOf(false) }

    val subtitle = buildString {
        if (currentFolder == null) {
            append("$totalCount ")
            append(if (totalCount == 1UL) "carpeta principal" else "carpetas principales")
        } else {
            append("$subfolderCount ")
            append(if (subfolderCount == 1UL) "subcarpeta" else "subcarpetas")
            append(" · ")
            append("$recordCount ")
            append(if (recordCount == 1UL) "registro" else "registros")
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (currentFolder == null) {
            Surface(
                shape = RoundedCornerShape(17.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .size(52.dp)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                        shape = RoundedCornerShape(17.dp)
                    )
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Storefront,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        } else {
            FolderAvatarBadge(
                iconValue = currentFolder.icon,
                colorHex = currentFolder.color,
                fallbackColor = MaterialTheme.colorScheme.primary,
                size = 52.dp,
                cornerRadius = 17.dp
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = currentFolder?.name ?: "Mis Negocios",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Menú unificado de funciones de la carpeta/vista (extensible y sin clutter visual)
        Box {
            FilledTonalIconButton(
                onClick = {
                    performTickHaptic(view)
                    functionsMenuOpen = true
                }
            ) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "Funciones de carpeta"
                )
            }
            DropdownMenu(
                expanded = functionsMenuOpen,
                onDismissRequest = { functionsMenuOpen = false }
            ) {
                if (currentFolder != null) {
                    DropdownMenuItem(
                        text = { Text("Editar \"${currentFolder.name}\"") },
                        leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                        onClick = {
                            functionsMenuOpen = false
                            onEditCurrentFolder()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Exportar \"${currentFolder.name}\" a Excel") },
                        leadingIcon = { Icon(Icons.Rounded.TableChart, contentDescription = null) },
                        onClick = {
                            functionsMenuOpen = false
                            onExportCurrentScope()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Nueva subcarpeta") },
                        leadingIcon = { Icon(Icons.Rounded.CreateNewFolder, contentDescription = null) },
                        onClick = {
                            functionsMenuOpen = false
                            onCreateFolderHere()
                        }
                    )
                } else {
                    DropdownMenuItem(
                        text = { Text("Exportar todos los negocios") },
                        leadingIcon = { Icon(Icons.Rounded.TableChart, contentDescription = null) },
                        onClick = {
                            functionsMenuOpen = false
                            onExportCurrentScope()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Nuevo negocio / carpeta") },
                        leadingIcon = { Icon(Icons.Rounded.CreateNewFolder, contentDescription = null) },
                        onClick = {
                            functionsMenuOpen = false
                            onCreateFolderHere()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Ajustes y seguridad") },
                        leadingIcon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
                        onClick = {
                            functionsMenuOpen = false
                            onOpenSettings()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(horizontal = 4.dp)
    )
}

@Composable
private fun EmptyFolderState(
    isRoot: Boolean,
    onCreatePrimaryAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ctaInteraction = remember { MutableInteractionSource() }
    val view = LocalView.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(86.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.FolderOpen,
                contentDescription = null,
                modifier = Modifier.size(42.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isRoot) "Aún no tienes negocios" else "Esta carpeta está vacía",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (isRoot) {
                "Crea carpetas con tu emoji y color favorito para organizar ventas, compras o inventario."
            } else {
                "Desliza un elemento a la izquierda para borrar, o añade un nuevo registro simple."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(18.dp))
        FilledTonalButton(
            onClick = {
                performConfirmHaptic(view)
                onCreatePrimaryAction()
            },
            interactionSource = ctaInteraction,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.pressableScale(ctaInteraction, pressedScale = 0.96f)
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(if (isRoot) "Crear primer negocio" else "Agregar registro")
        }
    }
}

@Composable
fun ExplorerUnderlayBackdrop(
    uiState: ExplorerUiState,
    modifier: Modifier = Modifier
) {
    val folderView = uiState.folderView
    if (folderView != null) {
        FolderScreenLayer(
            folderView = folderView,
            isRefreshing = false,
            reducedMotion = true,
            isUnderlay = true,
            onRefresh = {},
            onOpenSearch = {},
            onNavigateBack = {},
            onSettingsClick = {},
            onOpenFolder = {},
            onEditCurrentFolder = {},
            onExportCurrentScope = {},
            onCreateFolderHere = {},
            onOpenSettings = {},
            onEditFolder = {},
            onExportFolder = {},
            onDeleteFolder = {},
            onEditRecord = {},
            onDeleteRecord = {},
            onCreatePrimaryAction = {},
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        )
    }
}
