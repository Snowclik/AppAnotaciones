package com.appnotas.app.ui

import com.appnotas.core.FolderItem
import com.appnotas.core.FolderViewData
import com.appnotas.core.RecordItem
import com.appnotas.core.SearchResults
import com.appnotas.core.SyncStatus

enum class NavigationDirection {
    FORWARD,
    BACKWARD,
    NEUTRAL
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

data class ExplorerUiState(
    val isReady: Boolean = false,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isSyncing: Boolean = false,
    val currentFolderId: String? = null,
    val navigationDirection: NavigationDirection = NavigationDirection.NEUTRAL,
    val folderView: FolderViewData? = null,
    val syncStatus: SyncStatus? = null,
    val isSearchExpanded: Boolean = false,
    val searchQuery: String = "",
    val searchResults: SearchResults? = null,
    val folderSheet: FolderSheetState? = null,
    val recordSheet: RecordSheetState? = null,
    val deleteConfirmation: DeleteConfirmationState? = null,
    val showSyncDialog: Boolean = false,
    val isFabMenuExpanded: Boolean = false,
    val snackbarMessage: String? = null
)
