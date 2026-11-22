package com.appnotas.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.appnotas.app.BuildConfig
import com.appnotas.app.data.RustCoreRepository
import com.appnotas.app.data.toReadableMessage
import com.appnotas.core.FolderItem
import com.appnotas.core.RecordItem
import com.appnotas.core.SyncMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class ExplorerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RustCoreRepository()

    private val _uiState = MutableStateFlow(ExplorerUiState())
    val uiState: StateFlow<ExplorerUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        initializeAndLoad()
    }

    private fun initializeAndLoad() {
        viewModelScope.launch {
            val app = getApplication<Application>()
            val dbPath = File(app.filesDir, "appnotas.db").absolutePath
            val tursoUrl = BuildConfig.TURSO_DATABASE_URL.takeIf { it.isNotBlank() }
            val tursoToken = BuildConfig.TURSO_AUTH_TOKEN.takeIf { it.isNotBlank() }

            runCatching {
                val status = repository.initDatabase(dbPath, tursoUrl, tursoToken)
                val rootView = repository.listFolder(null)
                status to rootView
            }.onSuccess { (status, rootView) ->
                _uiState.update {
                    it.copy(
                        isReady = true,
                        isLoading = false,
                        currentFolderId = null,
                        navigationDirection = NavigationDirection.NEUTRAL,
                        folderView = rootView,
                        syncStatus = status
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isReady = true,
                        isLoading = false,
                        snackbarMessage = error.toReadableMessage()
                    )
                }
            }
        }
    }

    fun openFolder(folderId: String?) {
        val snapshot = _uiState.value
        val direction = when {
            folderId == snapshot.currentFolderId && !snapshot.isSearchExpanded ->
                NavigationDirection.NEUTRAL
            snapshot.folderView?.breadcrumbs
                ?.dropLast(1)
                ?.any { it.id == folderId } == true ->
                NavigationDirection.BACKWARD
            else -> NavigationDirection.FORWARD
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = it.folderView == null,
                    isFabMenuExpanded = false,
                    isSearchExpanded = false,
                    searchQuery = "",
                    searchResults = null,
                    navigationDirection = direction
                )
            }
            loadFolderInternal(folderId)
        }
    }

    fun navigateUp(): Boolean {
        val state = _uiState.value
        if (state.isFabMenuExpanded) {
            dismissFabMenu()
            return true
        }
        if (state.isSearchExpanded) {
            setSearchExpanded(false)
            return true
        }
        val breadcrumbs = state.folderView?.breadcrumbs.orEmpty()
        if (breadcrumbs.size <= 1) {
            return false
        }
        val parentBreadcrumb = breadcrumbs[breadcrumbs.size - 2]
        openFolder(parentBreadcrumb.id)
        return true
    }

    private suspend fun loadFolderInternal(folderId: String?) {
        runCatching {
            val viewData = repository.listFolder(folderId)
            val status = repository.syncStatus()
            viewData to status
        }.onSuccess { (viewData, status) ->
            _uiState.update {
                it.copy(
                    isLoading = false,
                    currentFolderId = folderId,
                    folderView = viewData,
                    syncStatus = status
                )
            }
        }.onFailure { error ->
            _uiState.update {
                it.copy(
                    isLoading = false,
                    snackbarMessage = error.toReadableMessage()
                )
            }
        }
    }

    fun pullToRefresh() {
        val currentState = _uiState.value
        if (currentState.isRefreshing) return

        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            val mode = currentState.syncStatus?.mode
            if (mode == SyncMode.EMBEDDED_REPLICA) {
                runCatching { repository.sync() }
                    .onSuccess { status ->
                        _uiState.update { it.copy(syncStatus = status) }
                    }
                    .onFailure { error ->
                        _uiState.update { it.copy(snackbarMessage = error.toReadableMessage()) }
                    }
            }
            loadFolderInternal(_uiState.value.currentFolderId)
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun setSearchExpanded(expanded: Boolean) {
        _uiState.update {
            if (expanded) {
                it.copy(isSearchExpanded = true, isFabMenuExpanded = false)
            } else {
                it.copy(isSearchExpanded = false, searchQuery = "", searchResults = null)
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = null) }
            return
        }
        searchJob = viewModelScope.launch {
            runCatching {
                repository.search(query)
            }.onSuccess { results ->
                _uiState.update { it.copy(searchResults = results) }
            }.onFailure { error ->
                _uiState.update { it.copy(snackbarMessage = error.toReadableMessage()) }
            }
        }
    }

    fun toggleFabMenu() {
        _uiState.update { it.copy(isFabMenuExpanded = !it.isFabMenuExpanded) }
    }

    fun dismissFabMenu() {
        _uiState.update { it.copy(isFabMenuExpanded = false) }
    }

    fun openCreateFolderSheet() {
        val currentId = _uiState.value.currentFolderId
        _uiState.update {
            it.copy(
                isFabMenuExpanded = false,
                folderSheet = FolderSheetState.Create(parentId = currentId)
            )
        }
    }

    fun openEditFolderSheet(folder: FolderItem) {
        _uiState.update {
            it.copy(
                isFabMenuExpanded = false,
                folderSheet = FolderSheetState.Edit(folder = folder)
            )
        }
    }

    fun dismissFolderSheet() {
        _uiState.update { it.copy(folderSheet = null) }
    }

    fun saveFolder(name: String, color: String?, icon: String?) {
        val sheet = _uiState.value.folderSheet ?: return
        viewModelScope.launch {
            runCatching {
                when (sheet) {
                    is FolderSheetState.Create ->
                        repository.createFolderItem(sheet.parentId, name, color, icon)
                    is FolderSheetState.Edit ->
                        repository.updateFolderItem(sheet.folder.id, name, color, icon)
                }
            }.onSuccess {
                _uiState.update { it.copy(folderSheet = null) }
                refreshActiveViews()
            }.onFailure { error ->
                _uiState.update { it.copy(snackbarMessage = error.toReadableMessage()) }
            }
        }
    }

    fun openCreateRecordSheet() {
        val state = _uiState.value
        val currentId = state.currentFolderId
        if (currentId != null) {
            _uiState.update {
                it.copy(
                    isFabMenuExpanded = false,
                    recordSheet = RecordSheetState.Create(folderId = currentId)
                )
            }
            return
        }

        val rootFolders = state.folderView?.subfolders.orEmpty()
        if (rootFolders.isEmpty()) {
            _uiState.update {
                it.copy(
                    isFabMenuExpanded = false,
                    folderSheet = FolderSheetState.Create(parentId = null),
                    snackbarMessage = "Crea primero una carpeta para guardar registros."
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                isFabMenuExpanded = false,
                recordSheet = RecordSheetState.Create(
                    folderId = rootFolders.first().id,
                    availableFolders = rootFolders
                )
            )
        }
    }

    fun openEditRecordSheet(record: RecordItem) {
        _uiState.update {
            it.copy(
                isFabMenuExpanded = false,
                recordSheet = RecordSheetState.Edit(record = record)
            )
        }
    }

    fun dismissRecordSheet() {
        _uiState.update { it.copy(recordSheet = null) }
    }

    fun saveRecord(
        targetFolderId: String,
        title: String,
        numericRaw: String,
        unit: String?,
        textValue: String?
    ) {
        val sheet = _uiState.value.recordSheet ?: return
        val trimmedNum = numericRaw.trim().replace(',', '.')
        val parsedNumber: Double? = if (trimmedNum.isEmpty()) {
            null
        } else {
            val number = trimmedNum.toDoubleOrNull()
            if (number == null) {
                _uiState.update {
                    it.copy(snackbarMessage = "Ingresa un número válido o deja el campo vacío.")
                }
                return
            }
            number
        }

        viewModelScope.launch {
            runCatching {
                when (sheet) {
                    is RecordSheetState.Create -> repository.createRecordItem(
                        folderId = targetFolderId,
                        title = title,
                        numericValue = parsedNumber,
                        unit = unit,
                        textValue = textValue
                    )
                    is RecordSheetState.Edit -> repository.updateRecordItem(
                        id = sheet.record.id,
                        title = title,
                        numericValue = parsedNumber,
                        unit = unit,
                        textValue = textValue
                    )
                }
            }.onSuccess {
                _uiState.update { it.copy(recordSheet = null) }
                refreshActiveViews()
            }.onFailure { error ->
                _uiState.update { it.copy(snackbarMessage = error.toReadableMessage()) }
            }
        }
    }

    fun requestDeleteFolder(folder: FolderItem) {
        _uiState.update { it.copy(deleteConfirmation = DeleteConfirmationState.Folder(folder)) }
    }

    fun requestDeleteRecord(record: RecordItem) {
        _uiState.update { it.copy(deleteConfirmation = DeleteConfirmationState.Record(record)) }
    }

    fun dismissDeleteConfirmation() {
        _uiState.update { it.copy(deleteConfirmation = null) }
    }

    fun confirmDelete() {
        val target = _uiState.value.deleteConfirmation ?: return
        viewModelScope.launch {
            runCatching {
                when (target) {
                    is DeleteConfirmationState.Folder -> repository.deleteFolderItem(target.folder.id)
                    is DeleteConfirmationState.Record -> repository.deleteRecordItem(target.record.id)
                }
            }.onSuccess {
                _uiState.update { it.copy(deleteConfirmation = null) }
                refreshActiveViews()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        deleteConfirmation = null,
                        snackbarMessage = error.toReadableMessage()
                    )
                }
            }
        }
    }

    fun showSyncStatusDialog() {
        _uiState.update { it.copy(showSyncDialog = true) }
    }

    fun dismissSyncStatusDialog() {
        _uiState.update { it.copy(showSyncDialog = false) }
    }

    fun syncNow() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            runCatching {
                repository.sync()
            }.onSuccess { status ->
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        syncStatus = status,
                        snackbarMessage = status.message
                    )
                }
                refreshActiveViews()
            }.onFailure { error ->
                val latestStatus = runCatching { repository.syncStatus() }.getOrNull()
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        syncStatus = latestStatus ?: it.syncStatus,
                        snackbarMessage = error.toReadableMessage()
                    )
                }
            }
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    private suspend fun refreshActiveViews() {
        val currentId = _uiState.value.currentFolderId
        loadFolderInternal(currentId)
        val currentQuery = _uiState.value.searchQuery
        if (_uiState.value.isSearchExpanded && currentQuery.isNotBlank()) {
            runCatching { repository.search(currentQuery) }
                .onSuccess { results -> _uiState.update { it.copy(searchResults = results) } }
        }
    }
}
