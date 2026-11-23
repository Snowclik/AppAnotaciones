package com.appnotas.app.data

import com.appnotas.core.AppException
import com.appnotas.core.AppearanceSettings
import com.appnotas.core.CustomField
import com.appnotas.core.ExportPeriod
import com.appnotas.core.ExportPreview
import com.appnotas.core.ExportResult
import com.appnotas.core.FolderItem
import com.appnotas.core.FolderViewData
import com.appnotas.core.LockMode
import com.appnotas.core.RecordItem
import com.appnotas.core.SearchResults
import com.appnotas.core.SecuritySettings
import com.appnotas.core.SyncStatus
import com.appnotas.core.clearAppPassword
import com.appnotas.core.createFolder
import com.appnotas.core.createRecord
import com.appnotas.core.deleteFolder
import com.appnotas.core.deleteRecord
import com.appnotas.core.exportExcel
import com.appnotas.core.getAppearanceSettings
import com.appnotas.core.getSecuritySettings
import com.appnotas.core.getSyncStatus
import com.appnotas.core.initDb
import com.appnotas.core.listFolderContents
import com.appnotas.core.previewExcelExport
import com.appnotas.core.saveAppearanceSettings
import com.appnotas.core.searchAll
import com.appnotas.core.setAppPassword
import com.appnotas.core.syncNow
import com.appnotas.core.updateFolder
import com.appnotas.core.updateRecord
import com.appnotas.core.updateSecurityOptions
import com.appnotas.core.verifyAppPassword
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
        quantity: Double?,
        unitMultiplier: Double?,
        multiplierLabel: String?,
        unit: String?,
        textValue: String?,
        customFields: List<CustomField>
    ): RecordItem = withContext(Dispatchers.IO) {
        createRecord(
            folderId,
            title,
            numericValue,
            quantity,
            unitMultiplier,
            multiplierLabel,
            unit,
            textValue,
            customFields
        )
    }

    suspend fun updateRecordItem(
        id: String,
        title: String,
        numericValue: Double?,
        quantity: Double?,
        unitMultiplier: Double?,
        multiplierLabel: String?,
        unit: String?,
        textValue: String?,
        customFields: List<CustomField>
    ): RecordItem = withContext(Dispatchers.IO) {
        updateRecord(
            id,
            title,
            numericValue,
            quantity,
            unitMultiplier,
            multiplierLabel,
            unit,
            textValue,
            customFields
        )
    }

    suspend fun deleteRecordItem(id: String) = withContext(Dispatchers.IO) {
        deleteRecord(id)
    }

    suspend fun search(query: String): SearchResults = withContext(Dispatchers.IO) {
        searchAll(query)
    }

    suspend fun previewExcel(
        folderId: String?,
        period: ExportPeriod
    ): ExportPreview = withContext(Dispatchers.IO) {
        previewExcelExport(folderId, period)
    }

    suspend fun exportToExcel(
        folderId: String?,
        period: ExportPeriod,
        outputDir: String
    ): ExportResult = withContext(Dispatchers.IO) {
        exportExcel(folderId, period, outputDir)
    }

    suspend fun securitySettings(): SecuritySettings = withContext(Dispatchers.IO) {
        getSecuritySettings()
    }

    suspend fun configureAppPassword(
        password: String,
        enableBiometric: Boolean
    ): SecuritySettings = withContext(Dispatchers.IO) {
        setAppPassword(password, enableBiometric)
    }

    suspend fun checkAppPassword(password: String): Boolean = withContext(Dispatchers.IO) {
        verifyAppPassword(password)
    }

    suspend fun saveSecurityOptions(
        lockMode: LockMode,
        biometricEnabled: Boolean,
        autoLockOnBackground: Boolean
    ): SecuritySettings = withContext(Dispatchers.IO) {
        updateSecurityOptions(lockMode, biometricEnabled, autoLockOnBackground)
    }

    suspend fun removeAppPassword(): SecuritySettings = withContext(Dispatchers.IO) {
        clearAppPassword()
    }

    suspend fun appearanceSettings(): AppearanceSettings = withContext(Dispatchers.IO) {
        getAppearanceSettings()
    }

    suspend fun updateAppearanceSettings(settings: AppearanceSettings): AppearanceSettings =
        withContext(Dispatchers.IO) {
            saveAppearanceSettings(settings)
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
