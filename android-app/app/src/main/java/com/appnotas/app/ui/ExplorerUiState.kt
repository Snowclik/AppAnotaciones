package com.appnotas.app.ui

import com.appnotas.core.AppearanceSettings
import com.appnotas.core.ExportPreview
import com.appnotas.core.ExportResult
import com.appnotas.core.FolderItem
import com.appnotas.core.FolderViewData
import com.appnotas.core.LockMode
import com.appnotas.core.RecordItem
import com.appnotas.core.SearchResults
import com.appnotas.core.SecuritySettings
import com.appnotas.core.SyncStatus

enum class AppDestination {
    EXPLORER,
    SETTINGS
}

enum class SettingsSection {
    HOME,
    APPEARANCE,
    SECURITY,
    EXPORT,
    SYNC
}

enum class NavigationDirection {
    FORWARD,
    BACKWARD,
    NEUTRAL,
    INSTANT
}

sealed interface FolderSheetState {
    data class Create(val parentId: String?) : FolderSheetState
    data class Edit(val folder: FolderItem) : FolderSheetState
}

sealed interface RecordSheetState {
    data class Create(
        val folderId: String,
        val availableFolders: List<FolderItem> = emptyList()
    ) : RecordSheetState

    data class Edit(val record: RecordItem) : RecordSheetState
}

sealed interface DeleteConfirmationState {
    data class Folder(val folder: FolderItem) : DeleteConfirmationState
    data class Record(val record: RecordItem) : DeleteConfirmationState
}

data class ExportDialogState(
    val folderId: String?,
    val folderName: String?
)

data class ExplorerUiState(
    val isReady: Boolean = false,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isSyncing: Boolean = false,
    val destination: AppDestination = AppDestination.EXPLORER,
    val rootNavDirection: NavigationDirection = NavigationDirection.NEUTRAL,
    val settingsSection: SettingsSection = SettingsSection.HOME,
    val settingsNavDirection: NavigationDirection = NavigationDirection.NEUTRAL,
    val appearance: AppearanceSettings = AppearanceSettings(
        paletteId = "aurora",
        themeMode = "system",
        pureBlack = false
    ),
    val security: SecuritySettings = SecuritySettings(
        lockMode = LockMode.NONE,
        hasPassword = false,
        biometricEnabled = false,
        autoLockOnBackground = true
    ),
    val isLocked: Boolean = false,
    val isVerifyingLock: Boolean = false,
    val lockError: String? = null,
    val showPasswordSetupSheet: Boolean = false,
    val currentFolderId: String? = null,
    val navigationDirection: NavigationDirection = NavigationDirection.NEUTRAL,
    val folderView: FolderViewData? = null,
    val folderViewCache: Map<String?, FolderViewData> = emptyMap(),
    val rootFolders: List<FolderItem> = emptyList(),
    val syncStatus: SyncStatus? = null,
    val isSearchExpanded: Boolean = false,
    val searchQuery: String = "",
    val searchResults: SearchResults? = null,
    val folderSheet: FolderSheetState? = null,
    val recordSheet: RecordSheetState? = null,
    val deleteConfirmation: DeleteConfirmationState? = null,
    val exportSheet: ExportDialogState? = null,
    val exportPreview: ExportPreview? = null,
    val isLoadingExportPreview: Boolean = false,
    val isExporting: Boolean = false,
    val lastExportResult: ExportResult? = null,
    val showSyncDialog: Boolean = false,
    val isFabMenuExpanded: Boolean = false,
    val snackbarMessage: String? = null
)
