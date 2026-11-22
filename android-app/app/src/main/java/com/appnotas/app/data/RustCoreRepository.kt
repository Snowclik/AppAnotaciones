package com.appnotas.app.data

import com.appnotas.core.AppException
import com.appnotas.core.FolderItem
import com.appnotas.core.FolderViewData
import com.appnotas.core.RecordItem
import com.appnotas.core.SearchResults
import com.appnotas.core.SyncStatus
import com.appnotas.core.createFolder
import com.appnotas.core.createRecord
import com.appnotas.core.deleteFolder
import com.appnotas.core.deleteRecord
import com.appnotas.core.getSyncStatus
import com.appnotas.core.initDb
import com.appnotas.core.listFolderContents
import com.appnotas.core.searchAll
import com.appnotas.core.syncNow
import com.appnotas.core.updateFolder
import com.appnotas.core.updateRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Capa delgada de acceso al core en Rust vía UniFFI.
 * No ejecuta lógica de negocio ni cálculos; delega todas las operaciones en Dispatchers.IO.
 */
class RustCoreRepository {

    suspend fun initDatabase(
        localPath: String,
        tursoUrl: String?,
        tursoToken: String?
    ): SyncStatus = withContext(Dispatchers.IO) {
        initDb(localPath, tursoUrl, tursoToken)
    }

    suspend fun sync(): SyncStatus = withContext(Dispatchers.IO) {
        syncNow()
    }

    suspend fun syncStatus(): SyncStatus = withContext(Dispatchers.IO) {
        getSyncStatus()
    }

    suspend fun listFolder(parentId: String?): FolderViewData = withContext(Dispatchers.IO) {
        listFolderContents(parentId)
    }

    suspend fun createFolderItem(
        parentId: String?,
        name: String,
        color: String?,
        icon: String?
    ): FolderItem = withContext(Dispatchers.IO) {
        createFolder(parentId, name, color, icon)
    }

    suspend fun updateFolderItem(
        id: String,
        name: String,
        color: String?,
        icon: String?
    ): FolderItem = withContext(Dispatchers.IO) {
        updateFolder(id, name, color, icon)
    }

    suspend fun deleteFolderItem(id: String) = withContext(Dispatchers.IO) {
        deleteFolder(id)
    }

    suspend fun createRecordItem(
        folderId: String,
        title: String,
        numericValue: Double?,
        unit: String?,
        textValue: String?
    ): RecordItem = withContext(Dispatchers.IO) {
        createRecord(folderId, title, numericValue, unit, textValue)
    }

    suspend fun updateRecordItem(
        id: String,
        title: String,
        numericValue: Double?,
        unit: String?,
        textValue: String?
    ): RecordItem = withContext(Dispatchers.IO) {
        updateRecord(id, title, numericValue, unit, textValue)
    }

    suspend fun deleteRecordItem(id: String) = withContext(Dispatchers.IO) {
        deleteRecord(id)
    }

    suspend fun search(query: String): SearchResults = withContext(Dispatchers.IO) {
        searchAll(query)
    }
}

fun Throwable.toReadableMessage(): String = when (this) {
    is AppException.NotInitialized -> "La base de datos no está inicializada."
    is AppException.Validation -> detail
    is AppException.Database -> detail
    is AppException.NotFound -> "No se encontró $entity."
    is AppException.Sync -> detail
    is AppException.Internal -> detail
    else -> message ?: "Ocurrió un error inesperado."
}
