package com.appnotas.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.appnotas.app.BuildConfig
import com.appnotas.app.data.RustCoreRepository
import com.appnotas.app.data.toReadableMessage
import com.appnotas.core.AppearanceSettings
import com.appnotas.core.CustomField
import com.appnotas.core.ExportPeriod
import com.appnotas.core.FolderItem
import com.appnotas.core.FolderViewData
import com.appnotas.core.LockMode
import com.appnotas.core.RecordItem
import com.appnotas.core.SyncMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    private var previewJob: Job? = null
    private var isSavingFolder = false
    private var isSavingRecord = false
    private var isDeleting = false
    private var isSavingPassword = false

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
                val security = repository.securitySettings()
                val appearance = repository.appearanceSettings()
                val rootView = repository.listFolder(null)
                Triple(status, security, appearance to rootView)
            }.onSuccess { (status, security, uiAndView) ->
                val (appearance, rootView) = uiAndView
                _uiState.update {
                    it.copy(
                        isReady = true,
                        isLoading = false,
                        currentFolderId = null,
                        navigationDirection = NavigationDirection.NEUTRAL,
                        folderView = rootView,
                        folderViewCache = mapOf(null to rootView),
                        rootFolders = rootView.subfolders,
                        syncStatus = status,
                        security = security,
                        appearance = appearance,
                        isLocked = security.lockMode != LockMode.NONE
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

    fun onAppBackgrounded() {
        val sec = _uiState.value.security
        if (sec.lockMode != LockMode.NONE && sec.autoLockOnBackground) {
            _uiState.update { it.copy(isLocked = true, lockError = null) }
        }
    }

    fun unlockWithPassword(password: String) {
        if (_uiState.value.isVerifyingLock) return
        viewModelScope.launch {
            _uiState.update { it.copy(isVerifyingLock = true, lockError = null) }
            runCatching {
                repository.checkAppPassword(password)
            }.onSuccess { matched ->
                if (matched) {
                    _uiState.update {
                        it.copy(
                            isVerifyingLock = false,
                            isLocked = false,
                            lockError = null
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isVerifyingLock = false,
                            lockError = "Contraseña incorrecta. Intenta de nuevo."
                        )
                    }
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isVerifyingLock = false,
                        lockError = error.toReadableMessage()
                    )
                }
            }
        }
    }

    fun onBiometricUnlockSucceeded() {
        _uiState.update { it.copy(isLocked = false, lockError = null) }
    }

    fun openSettings() {
        _uiState.update {
            it.copy(
                destination = AppDestination.SETTINGS,
                rootNavDirection = NavigationDirection.FORWARD,
                settingsSection = SettingsSection.HOME,
                settingsNavDirection = NavigationDirection.NEUTRAL,
                isFabMenuExpanded = false,
                isSearchExpanded = false
            )
        }
    }

    fun openSettingsSection(section: SettingsSection, instant: Boolean = false) {
        val current = _uiState.value.settingsSection
        val direction = when {
            instant -> NavigationDirection.INSTANT
            section == current -> NavigationDirection.NEUTRAL
            section == SettingsSection.HOME -> NavigationDirection.BACKWARD
            current == SettingsSection.HOME -> NavigationDirection.FORWARD
            else -> NavigationDirection.FORWARD
        }
        _uiState.update {
            it.copy(
                settingsSection = section,
                settingsNavDirection = direction,
                lastExportResult = if (section == SettingsSection.EXPORT) null else it.lastExportResult
            )
        }
        if (section == SettingsSection.EXPORT) {
            loadExportPreview(folderId = null, period = ExportPeriod.WEEK)
        }
    }

    fun closeSettings() {
        _uiState.update {
            it.copy(
                destination = AppDestination.EXPLORER,
                rootNavDirection = NavigationDirection.BACKWARD,
                settingsSection = SettingsSection.HOME,
                settingsNavDirection = NavigationDirection.NEUTRAL
            )
        }
    }

    fun closeSettingsFromSwipe() {
        _uiState.update {
            it.copy(
                destination = AppDestination.EXPLORER,
                rootNavDirection = NavigationDirection.INSTANT,
                settingsSection = SettingsSection.HOME,
                settingsNavDirection = NavigationDirection.NEUTRAL
            )
        }
    }

    fun openExportSheet(folderId: String?, folderName: String?) {
        _uiState.update {
            it.copy(
                isFabMenuExpanded = false,
                lastExportResult = null,
                exportPreview = null,
                exportSheet = ExportDialogState(folderId = folderId, folderName = folderName)
            )
        }
        loadExportPreview(folderId = folderId, period = ExportPeriod.WEEK)
    }

    fun dismissExportSheet() {
        previewJob?.cancel()
        _uiState.update {
            it.copy(
                exportSheet = null,
                isExporting = false,
                isLoadingExportPreview = false
            )
        }
    }

    fun loadExportPreview(folderId: String?, period: ExportPeriod) {
        previewJob?.cancel()
        previewJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingExportPreview = true) }
            runCatching {
                repository.previewExcel(folderId, period)
            }.onSuccess { preview ->
                _uiState.update {
                    it.copy(
                        isLoadingExportPreview = false,
                        exportPreview = preview
                    )
                }
            }.onFailure {
                _uiState.update { it.copy(isLoadingExportPreview = false) }
            }
        }
    }

    fun exportExcel(folderId: String?, period: ExportPeriod) {
        if (_uiState.value.isExporting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true) }
            val exportDir = File(getApplication<Application>().cacheDir, "exports").absolutePath
            runCatching {
                repository.exportToExcel(folderId, period, exportDir)
            }.onSuccess { result ->
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        lastExportResult = result
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        snackbarMessage = error.toReadableMessage()
                    )
                }
            }
        }
    }

    fun openPasswordSetupSheet() {
        _uiState.update { it.copy(showPasswordSetupSheet = true) }
    }

    fun dismissPasswordSetupSheet() {
        _uiState.update { it.copy(showPasswordSetupSheet = false) }
    }

    fun saveAppPassword(password: String, enableBiometric: Boolean) {
        if (isSavingPassword) return
        isSavingPassword = true
        viewModelScope.launch {
            try {
                runCatching {
                    repository.configureAppPassword(password, enableBiometric)
                }.onSuccess { updated ->
                    _uiState.update {
                        it.copy(
                            security = updated,
                            showPasswordSetupSheet = false,
                            snackbarMessage = "Contraseña de acceso activada."
                        )
                    }
                }.onFailure { error ->
                    _uiState.update { it.copy(snackbarMessage = error.toReadableMessage()) }
                }
            } finally {
                isSavingPassword = false
            }
        }
    }

    fun updateSecurityOptions(
        lockMode: LockMode,
        biometricEnabled: Boolean,
        autoLockOnBackground: Boolean
    ) {
        viewModelScope.launch {
            runCatching {
                repository.saveSecurityOptions(lockMode, biometricEnabled, autoLockOnBackground)
            }.onSuccess { updated ->
                _uiState.update { it.copy(security = updated) }
            }.onFailure { error ->
                _uiState.update { it.copy(snackbarMessage = error.toReadableMessage()) }
            }
        }
    }

    fun clearAppPassword() {
        if (isSavingPassword) return
        isSavingPassword = true
        viewModelScope.launch {
            try {
                runCatching {
                    repository.removeAppPassword()
                }.onSuccess { updated ->
                    _uiState.update {
                        it.copy(
                            security = updated,
                            snackbarMessage = "Bloqueo por contraseña desactivado."
                        )
                    }
                }.onFailure { error ->
                    _uiState.update { it.copy(snackbarMessage = error.toReadableMessage()) }
                }
            } finally {
                isSavingPassword = false
            }
        }
    }

    fun updateAppearance(
        paletteId: String = _uiState.value.appearance.paletteId,
        themeMode: String = _uiState.value.appearance.themeMode,
        pureBlack: Boolean = _uiState.value.appearance.pureBlack
    ) {
        val next = AppearanceSettings(
            paletteId = paletteId,
            themeMode = themeMode,
            pureBlack = pureBlack
        )
        _uiState.update { it.copy(appearance = next) }
        viewModelScope.launch {
            runCatching {
                repository.updateAppearanceSettings(next)
            }.onSuccess { persisted ->
                _uiState.update { it.copy(appearance = persisted) }
            }.onFailure { error ->
                _uiState.update { it.copy(snackbarMessage = error.toReadableMessage()) }
            }
        }
    }

    fun openFolder(folderId: String?) {
        searchJob?.cancel()
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

        val cachedTarget = snapshot.folderViewCache[folderId]
        if (cachedTarget != null) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isFabMenuExpanded = false,
                    isSearchExpanded = false,
                    searchQuery = "",
                    searchResults = null,
                    currentFolderId = folderId,
                    folderView = cachedTarget,
                    navigationDirection = direction
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                isFabMenuExpanded = false,
                isSearchExpanded = false,
                searchQuery = "",
                searchResults = null
            )
        }
        viewModelScope.launch {
            loadFolderInternal(folderId, targetDirection = direction)
        }
    }

    fun navigateUpFromSwipe(): Boolean {
        val state = _uiState.value
        if (state.isFabMenuExpanded || state.isSearchExpanded) return false
        val breadcrumbs = state.folderView?.breadcrumbs.orEmpty()
        if (breadcrumbs.size <= 1) return false

        val parentId = breadcrumbs[breadcrumbs.size - 2].id
        val cachedParent = state.folderViewCache[parentId]
        _uiState.update {
            it.copy(
                currentFolderId = parentId,
                folderView = cachedParent ?: it.folderView,
                navigationDirection = NavigationDirection.INSTANT,
                isFabMenuExpanded = false,
                isSearchExpanded = false
            )
        }
        if (cachedParent == null) {
            viewModelScope.launch {
                loadFolderInternal(parentId)
            }
        }
        return true
    }

    fun navigateUp(): Boolean {
        val state = _uiState.value
        if (state.destination == AppDestination.SETTINGS) {
            if (state.settingsSection != SettingsSection.HOME) {
                openSettingsSection(SettingsSection.HOME)
            } else {
                closeSettings()
            }
            return true
        }
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

    private suspend fun loadFolderInternal(
        folderId: String?,
        targetDirection: NavigationDirection? = null
    ) {
        runCatching {
            val viewData = repository.listFolder(folderId)
            val status = repository.syncStatus()
            val rootView = if (folderId == null) viewData else repository.listFolder(null)
            val parentId = if (viewData.breadcrumbs.size >= 2) {
                viewData.breadcrumbs[viewData.breadcrumbs.size - 2].id
            } else {
                null
            }
            val parentEntry = if (parentId != null && parentId != folderId) {
                parentId to repository.listFolder(parentId)
            } else {
                null
            }
            Triple(viewData, status, rootView to parentEntry)
        }.onSuccess { (viewData, status, rootAndParent) ->
            val (rootView, parentEntry) = rootAndParent
            _uiState.update { state ->
                val updatedCache = trimFolderCache(
                    cache = buildMap {
                        putAll(state.folderViewCache)
                        put(null, rootView)
                        put(folderId, viewData)
                        if (parentEntry != null) {
                            put(parentEntry.first, parentEntry.second)
                        }
                    },
                    keepFolderId = folderId
                )
                if (targetDirection == null && state.currentFolderId != folderId) {
                    if (updatedCache == state.folderViewCache &&
                        rootView.subfolders == state.rootFolders &&
                        status == state.syncStatus
                    ) {
                        state
                    } else {
                        state.copy(
                            folderViewCache = updatedCache,
                            rootFolders = rootView.subfolders,
                            syncStatus = status
                        )
                    }
                } else {
                    if (targetDirection == null &&
                        viewData == state.folderView &&
                        status == state.syncStatus &&
                        updatedCache == state.folderViewCache &&
                        !state.isLoading
                    ) {
                        state
                    } else {
                        state.copy(
                            isLoading = false,
                            currentFolderId = folderId,
                            folderView = viewData,
                            folderViewCache = updatedCache,
                            rootFolders = rootView.subfolders,
                            syncStatus = status,
                            navigationDirection = targetDirection ?: state.navigationDirection
                        )
                    }
                }
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
        if (!expanded) {
            searchJob?.cancel()
        }
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
            delay(150)
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
        if (isSavingFolder) return
        val sheet = _uiState.value.folderSheet ?: return
        isSavingFolder = true
        viewModelScope.launch {
            try {
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
            } finally {
                isSavingFolder = false
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
        quantityRaw: String,
        measureUnit: String?,
        priceRaw: String,
        priceIsPerUnit: Boolean,
        textValue: String?,
        customFields: List<CustomField>
    ) {
        if (isSavingRecord) return
        val sheet = _uiState.value.recordSheet ?: return
        val cleanQtyRaw = quantityRaw.trim().replace(',', '.')
        val cleanPriceRaw = priceRaw.trim().replace(',', '.')

        val qty: Double? = if (cleanQtyRaw.isEmpty()) {
            null
        } else {
            val parsed = cleanQtyRaw.toDoubleOrNull()
            if (parsed == null) {
                _uiState.update { it.copy(snackbarMessage = "Ingresa una cantidad numérica válida.") }
                return
            }
            parsed
        }

        val price: Double? = if (cleanPriceRaw.isEmpty()) {
            null
        } else {
            val parsed = cleanPriceRaw.toDoubleOrNull()
            if (parsed == null) {
                _uiState.update { it.copy(snackbarMessage = "Ingresa un precio válido.") }
                return
            }
            parsed
        }

        val cleanMeasure = measureUnit?.trim()?.takeIf { it.isNotEmpty() }

        val numericValue: Double?
        val persistedQuantity: Double?
        val unitMultiplier: Double?
        val multiplierLabel: String?
        val unit: String?

        when {
            qty != null && price != null && priceIsPerUnit -> {
                numericValue = null
                persistedQuantity = qty
                unitMultiplier = price
                multiplierLabel = cleanMeasure
                unit = "$"
            }
            qty != null && price != null -> {
                numericValue = price
                persistedQuantity = qty
                unitMultiplier = null
                multiplierLabel = cleanMeasure
                unit = "$"
            }
            qty == null && price != null -> {
                numericValue = price
                persistedQuantity = null
                unitMultiplier = null
                multiplierLabel = null
                unit = "$"
            }
            qty != null -> {
                numericValue = qty
                persistedQuantity = null
                unitMultiplier = null
                multiplierLabel = null
                unit = cleanMeasure ?: "u"
            }
            else -> {
                numericValue = null
                persistedQuantity = null
                unitMultiplier = null
                multiplierLabel = null
                unit = null
            }
        }

        isSavingRecord = true
        viewModelScope.launch {
            try {
                runCatching {
                    when (sheet) {
                        is RecordSheetState.Create -> repository.createRecordItem(
                            folderId = targetFolderId,
                            title = title,
                            numericValue = numericValue,
                            quantity = persistedQuantity,
                            unitMultiplier = unitMultiplier,
                            multiplierLabel = multiplierLabel,
                            unit = unit,
                            textValue = textValue,
                            customFields = customFields
                        )
                        is RecordSheetState.Edit -> repository.updateRecordItem(
                            id = sheet.record.id,
                            title = title,
                            numericValue = numericValue,
                            quantity = persistedQuantity,
                            unitMultiplier = unitMultiplier,
                            multiplierLabel = multiplierLabel,
                            unit = unit,
                            textValue = textValue,
                            customFields = customFields
                        )
                    }
                }.onSuccess {
                    _uiState.update { it.copy(recordSheet = null) }
                    refreshActiveViews()
                }.onFailure { error ->
                    _uiState.update { it.copy(snackbarMessage = error.toReadableMessage()) }
                }
            } finally {
                isSavingRecord = false
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
        if (isDeleting) return
        val target = _uiState.value.deleteConfirmation ?: return
        isDeleting = true
        viewModelScope.launch {
            try {
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
            } finally {
                isDeleting = false
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
        if (_uiState.value.isSyncing) return
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
        _uiState.update { it.copy(folderViewCache = emptyMap()) }
        loadFolderInternal(currentId)
        val currentQuery = _uiState.value.searchQuery
        if (_uiState.value.isSearchExpanded && currentQuery.isNotBlank()) {
            runCatching { repository.search(currentQuery) }
                .onSuccess { results -> _uiState.update { it.copy(searchResults = results) } }
        }
    }

    private fun trimFolderCache(
        cache: Map<String?, FolderViewData>,
        keepFolderId: String?,
        maxEntries: Int = 12
    ): Map<String?, FolderViewData> {
        if (cache.size <= maxEntries) return cache
        val result = LinkedHashMap<String?, FolderViewData>(maxEntries)
        cache[null]?.let { result[null] = it }
        if (keepFolderId != null) {
            cache[keepFolderId]?.let { result[keepFolderId] = it }
        }
        val extraLimit = (maxEntries - result.size).coerceAtLeast(0)
        val recentExtras = cache.entries
            .asSequence()
            .filter { it.key != null && it.key != keepFolderId }
            .toList()
            .takeLast(extraLimit)
        for (entry in recentExtras) {
            result[entry.key] = entry.value
        }
        return result
    }
}
