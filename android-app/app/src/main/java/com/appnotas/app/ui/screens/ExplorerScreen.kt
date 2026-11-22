package com.appnotas.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.SyncProblem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
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
import com.appnotas.app.ui.components.FolderEditorSheet
import com.appnotas.app.ui.components.FolderRowCard
import com.appnotas.app.ui.components.RecordEditorSheet
import com.appnotas.app.ui.components.RecordRowCard
import com.appnotas.app.ui.components.SummaryCard
import com.appnotas.app.ui.components.SyncStatusDialog
import com.appnotas.app.ui.components.formatNumberDisplay
import com.appnotas.app.ui.components.resolveFolderColor
import com.appnotas.app.ui.components.resolveFolderIcon
import com.appnotas.app.ui.motion.AppExpressiveMotion
import com.appnotas.app.ui.motion.EmilEaseOut
import com.appnotas.app.ui.motion.performConfirmHaptic
import com.appnotas.app.ui.motion.performTickHaptic
import com.appnotas.app.ui.motion.pressableScale
import com.appnotas.app.ui.motion.rememberReducedMotion
import com.appnotas.app.ui.theme.segmentedGroupShape
import com.appnotas.core.FolderItem
import com.appnotas.core.FolderViewData
import com.appnotas.core.SyncMode

private data class FolderContentTransitionState(
    val folderView: FolderViewData,
    val folderId: String?,
    val direction: NavigationDirection
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplorerScreen(
    uiState: ExplorerUiState,
    viewModel: ExplorerViewModel
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val reducedMotion = rememberReducedMotion()
    val view = LocalView.current

    LaunchedEffect(uiState.snackbarMessage) {
        val message = uiState.snackbarMessage
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearSnackbar()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopSearchHeader(
                    uiState = uiState,
                    reducedMotion = reducedMotion,
                    onQueryChange = viewModel::onSearchQueryChange,
                    onExpandedChange = viewModel::setSearchExpanded,
                    onSyncIconClick = viewModel::showSyncStatusDialog,
                    onNavigateBack = { viewModel.navigateUp() },
                    onOpenFolder = viewModel::openFolder,
                    onEditFolder = viewModel::openEditFolderSheet,
                    onDeleteFolder = viewModel::requestDeleteFolder,
                    onEditRecord = viewModel::openEditRecordSheet,
                    onDeleteRecord = viewModel::requestDeleteRecord
                )
            }
        ) { innerPadding ->
            val folderView = uiState.folderView
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = viewModel::pullToRefresh,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (folderView != null) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        BreadcrumbsBar(
                            breadcrumbs = folderView.breadcrumbs,
                            onBreadcrumbClick = viewModel::openFolder
                        )

                        AnimatedContent(
                            targetState = FolderContentTransitionState(
                                folderView = folderView,
                                folderId = uiState.currentFolderId,
                                direction = uiState.navigationDirection
                            ),
                            contentKey = { it.folderId },
                            transitionSpec = {
                                folderNavigationTransition(
                                    direction = targetState.direction,
                                    reducedMotion = reducedMotion
                                )
                            },
                            label = "folderNavigation",
                            modifier = Modifier.weight(1f)
                        ) { contentState ->
                            FolderExplorerPage(
                                folderView = contentState.folderView,
                                isRoot = contentState.folderId == null,
                                reducedMotion = reducedMotion,
                                onOpenFolder = viewModel::openFolder,
                                onEditCurrentFolder = {
                                    contentState.folderView.currentFolder?.let { current ->
                                        viewModel.openEditFolderSheet(current)
                                    }
                                },
                                onEditFolder = viewModel::openEditFolderSheet,
                                onDeleteFolder = viewModel::requestDeleteFolder,
                                onEditRecord = viewModel::openEditRecordSheet,
                                onDeleteRecord = viewModel::requestDeleteRecord,
                                onCreatePrimaryAction = {
                                    if (contentState.folderId == null) {
                                        viewModel.openCreateFolderSheet()
                                    } else {
                                        viewModel.openCreateRecordSheet()
                                    }
                                }
                            )
                        }
                    }
                } else if (uiState.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }

        ExpressiveFabMenuOverlay(
            expanded = uiState.isFabMenuExpanded,
            visible = !uiState.isSearchExpanded,
            onToggle = viewModel::toggleFabMenu,
            onDismiss = viewModel::dismissFabMenu,
            onCreateFolder = viewModel::openCreateFolderSheet,
            onCreateRecord = viewModel::openCreateRecordSheet
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

    if (uiState.showSyncDialog) {
        SyncStatusDialog(
            syncStatus = uiState.syncStatus,
            isSyncing = uiState.isSyncing,
            onSyncNow = viewModel::syncNow,
            onDismiss = viewModel::dismissSyncStatusDialog
        )
    }
}

private fun folderNavigationTransition(
    direction: NavigationDirection,
    reducedMotion: Boolean
): ContentTransform {
    if (reducedMotion || direction == NavigationDirection.NEUTRAL) {
        return fadeIn(tween(durationMillis = 110, easing = EmilEaseOut)) togetherWith
            fadeOut(tween(durationMillis = 90, easing = EmilEaseOut))
    }

    return when (direction) {
        NavigationDirection.FORWARD -> {
            (slideInHorizontally(tween(220, easing = EmilEaseOut)) { width -> (width * 0.16f).toInt() } +
                fadeIn(tween(200, easing = EmilEaseOut))) togetherWith
                (slideOutHorizontally(tween(150, easing = EmilEaseOut)) { width -> -(width * 0.08f).toInt() } +
                    fadeOut(tween(130, easing = EmilEaseOut)))
        }
        NavigationDirection.BACKWARD -> {
            (slideInHorizontally(tween(220, easing = EmilEaseOut)) { width -> -(width * 0.16f).toInt() } +
                fadeIn(tween(200, easing = EmilEaseOut))) togetherWith
                (slideOutHorizontally(tween(150, easing = EmilEaseOut)) { width -> (width * 0.10f).toInt() } +
                    fadeOut(tween(130, easing = EmilEaseOut)))
        }
        NavigationDirection.NEUTRAL -> {
            fadeIn(tween(110, easing = EmilEaseOut)) togetherWith
                fadeOut(tween(90, easing = EmilEaseOut))
        }
    }
}

@Composable
private fun FolderExplorerPage(
    folderView: FolderViewData,
    isRoot: Boolean,
    reducedMotion: Boolean,
    onOpenFolder: (String?) -> Unit,
    onEditCurrentFolder: () -> Unit,
    onEditFolder: (FolderItem) -> Unit,
    onDeleteFolder: (FolderItem) -> Unit,
    onEditRecord: (com.appnotas.core.RecordItem) -> Unit,
    onDeleteRecord: (com.appnotas.core.RecordItem) -> Unit,
    onCreatePrimaryAction: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        FolderHeroHeader(
            currentFolder = folderView.currentFolder,
            totalCount = folderView.totalCount,
            subfolderCount = folderView.subfolderCount,
            recordCount = folderView.recordCount,
            onEditCurrentFolder = onEditCurrentFolder
        )

        SummaryCard(folderView = folderView)

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
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                if (folderView.subfolders.isNotEmpty()) {
                    item(key = "header_folders") {
                        SectionHeader(
                            title = "Carpetas (${folderView.subfolderCount})",
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
    onEditCurrentFolder: () -> Unit
) {
    val accent = resolveFolderColor(currentFolder?.color, MaterialTheme.colorScheme.primary)
    val heroIcon = if (currentFolder == null) {
        Icons.Rounded.Storefront
    } else {
        resolveFolderIcon(currentFolder.icon)
    }
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
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = heroIcon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(26.dp)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = currentFolder?.name ?: "Mis Negocios",
                style = MaterialTheme.typography.headlineMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (currentFolder != null) {
            val view = LocalView.current
            FilledTonalIconButton(
                onClick = {
                    performTickHaptic(view)
                    onEditCurrentFolder()
                }
            ) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "Editar carpeta actual"
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopSearchHeader(
    uiState: ExplorerUiState,
    reducedMotion: Boolean,
    onQueryChange: (String) -> Unit,
    onExpandedChange: (Boolean) -> Unit,
    onSyncIconClick: () -> Unit,
    onNavigateBack: () -> Unit,
    onOpenFolder: (String?) -> Unit,
    onEditFolder: (FolderItem) -> Unit,
    onDeleteFolder: (FolderItem) -> Unit,
    onEditRecord: (com.appnotas.core.RecordItem) -> Unit,
    onDeleteRecord: (com.appnotas.core.RecordItem) -> Unit
) {
    val canGoUp = (uiState.folderView?.breadcrumbs?.size ?: 0) > 1
    val syncIcon = when {
        uiState.isSyncing -> Icons.Rounded.Sync
        uiState.syncStatus?.mode == SyncMode.EMBEDDED_REPLICA && uiState.syncStatus.synced ->
            Icons.Rounded.CloudDone
        uiState.syncStatus?.mode == SyncMode.LOCAL_ONLY -> Icons.Rounded.CloudOff
        uiState.syncStatus == null -> Icons.Rounded.CloudOff
        else -> Icons.Rounded.SyncProblem
    }

    val infiniteTransition = rememberInfiniteTransition(label = "syncSpin")
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 750, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "syncRotation"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = if (uiState.isSearchExpanded) 0.dp else 16.dp)
    ) {
        SearchBar(
            inputField = {
                SearchBarDefaults.InputField(
                    query = uiState.searchQuery,
                    onQueryChange = onQueryChange,
                    onSearch = { },
                    expanded = uiState.isSearchExpanded,
                    onExpandedChange = onExpandedChange,
                    placeholder = {
                        Text(
                            text = uiState.folderView?.currentFolder?.name?.let {
                                "Buscar en todo ($it)"
                            } ?: "Buscar carpetas y registros..."
                        )
                    },
                    leadingIcon = {
                        if (uiState.isSearchExpanded || canGoUp) {
                            IconButton(
                                onClick = {
                                    if (uiState.isSearchExpanded) {
                                        onExpandedChange(false)
                                    } else {
                                        onNavigateBack()
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = "Atrás"
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = "Buscar"
                            )
                        }
                    },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (uiState.isSearchExpanded && uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onQueryChange("") }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Limpiar búsqueda"
                                    )
                                }
                            } else {
                                IconButton(onClick = onSyncIconClick) {
                                    Icon(
                                        imageVector = syncIcon,
                                        contentDescription = "Estado de sincronización Turso",
                                        tint = if (uiState.syncStatus?.synced == true) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.graphicsLayer {
                                            if (uiState.isSyncing && !reducedMotion) {
                                                rotationZ = spinAngle
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                )
            },
            expanded = uiState.isSearchExpanded,
            onExpandedChange = onExpandedChange,
            modifier = Modifier.fillMaxWidth()
        ) {
            val results = uiState.searchResults
            when {
                uiState.searchQuery.isBlank() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Escribe para buscar al instante en carpetas, títulos, unidades o notas.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                results == null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp))
                    }
                }
                results.totalMatches == 0UL -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Sin resultados para \"${results.query}\".",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
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
                                onClick = { onOpenFolder(match.folder.id) },
                                onEdit = { onEditFolder(match.folder) },
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
                                onClick = { onEditRecord(match.record) },
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
                .size(84.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)),
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
            text = if (isRoot) "Aún no tienes carpetas" else "Esta carpeta está vacía",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (isRoot) {
                "Organiza tus negocios en carpetas y subcarpetas ilimitadas con totales automáticos."
            } else {
                "Desliza un elemento para editar o borrar, o crea tu primer registro aquí."
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
            Text(if (isRoot) "Crear primera carpeta" else "Agregar registro")
        }
    }
}
