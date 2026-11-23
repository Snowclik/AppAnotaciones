mod db;
mod error;
mod export;
mod models;
mod repository;
mod security;

use std::sync::{Arc, OnceLock, RwLock};

use tokio::runtime::{Builder as RuntimeBuilder, Runtime};

use crate::db::CoreEngine;
pub use crate::error::AppError;
pub use crate::models::{
    AppearanceSettings, BreadcrumbItem, CustomField, ExportPeriod, ExportPreview,
    ExportPreviewRow, ExportResult, FolderItem, FolderViewData, LockMode, RecordItem,
    SearchFolderResult, SearchRecordResult, SearchResults, SecuritySettings, SyncMode, SyncStatus,
    UnitSubtotal,
};

uniffi::setup_scaffolding!();

static RUNTIME: OnceLock<Runtime> = OnceLock::new();
static ENGINE: OnceLock<RwLock<Option<Arc<CoreEngine>>>> = OnceLock::new();

fn runtime() -> Result<&'static Runtime, AppError> {
    if let Some(rt) = RUNTIME.get() {
        return Ok(rt);
    }
    let created = RuntimeBuilder::new_multi_thread()
        .enable_all()
        .thread_name("appnotas-tokio")
        .build()
        .map_err(|e| AppError::Internal {
            detail: format!("No se pudo inicializar el runtime Tokio: {e}"),
        })?;
    let _ = RUNTIME.set(created);
    RUNTIME.get().ok_or_else(|| AppError::Internal {
        detail: "No se pudo obtener el runtime Tokio".to_string(),
    })
}

fn engine_slot() -> &'static RwLock<Option<Arc<CoreEngine>>> {
    ENGINE.get_or_init(|| RwLock::new(None))
}

fn current_engine() -> Result<Arc<CoreEngine>, AppError> {
    let guard = engine_slot()
        .read()
        .map_err(|_| AppError::Internal {
            detail: "Bloqueo de lectura corrupto en CoreEngine".to_string(),
        })?;
    guard.as_ref().cloned().ok_or(AppError::NotInitialized)
}

#[uniffi::export]
pub fn init_db(
    local_path: String,
    turso_url: Option<String>,
    turso_token: Option<String>,
) -> Result<SyncStatus, AppError> {
    let rt = runtime()?;
    let engine = rt.block_on(CoreEngine::open(local_path, turso_url, turso_token))?;
    let status = engine.get_sync_status();

    let mut guard = engine_slot().write().map_err(|_| AppError::Internal {
        detail: "Bloqueo de escritura corrupto en CoreEngine".to_string(),
    })?;
    *guard = Some(Arc::new(engine));

    Ok(status)
}

#[uniffi::export]
pub fn sync_now() -> Result<SyncStatus, AppError> {
    let rt = runtime()?;
    let engine = current_engine()?;
    rt.block_on(engine.sync_now())
}

#[uniffi::export]
pub fn get_sync_status() -> Result<SyncStatus, AppError> {
    let engine = current_engine()?;
    Ok(engine.get_sync_status())
}

#[uniffi::export]
pub fn list_folder_contents(parent_id: Option<String>) -> Result<FolderViewData, AppError> {
    let rt = runtime()?;
    let engine = current_engine()?;
    rt.block_on(engine.list_folder_contents(parent_id))
}

#[uniffi::export]
pub fn create_folder(
    parent_id: Option<String>,
    name: String,
    color: Option<String>,
    icon: Option<String>,
) -> Result<FolderItem, AppError> {
    let rt = runtime()?;
    let engine = current_engine()?;
    rt.block_on(engine.create_folder(parent_id, name, color, icon))
}

#[uniffi::export]
pub fn update_folder(
    id: String,
    name: String,
    color: Option<String>,
    icon: Option<String>,
) -> Result<FolderItem, AppError> {
    let rt = runtime()?;
    let engine = current_engine()?;
    rt.block_on(engine.update_folder(id, name, color, icon))
}

#[uniffi::export]
pub fn delete_folder(id: String) -> Result<(), AppError> {
    let rt = runtime()?;
    let engine = current_engine()?;
    rt.block_on(engine.delete_folder(id))
}

#[allow(clippy::too_many_arguments)]
#[uniffi::export]
pub fn create_record(
    folder_id: String,
    title: String,
    numeric_value: Option<f64>,
    quantity: Option<f64>,
    unit_multiplier: Option<f64>,
    multiplier_label: Option<String>,
    unit: Option<String>,
    text_value: Option<String>,
    custom_fields: Vec<CustomField>,
) -> Result<RecordItem, AppError> {
    let rt = runtime()?;
    let engine = current_engine()?;
    rt.block_on(engine.create_record(
        folder_id,
        title,
        numeric_value,
        quantity,
        unit_multiplier,
        multiplier_label,
        unit,
        text_value,
        custom_fields,
    ))
}

#[allow(clippy::too_many_arguments)]
#[uniffi::export]
pub fn update_record(
    id: String,
    title: String,
    numeric_value: Option<f64>,
    quantity: Option<f64>,
    unit_multiplier: Option<f64>,
    multiplier_label: Option<String>,
    unit: Option<String>,
    text_value: Option<String>,
    custom_fields: Vec<CustomField>,
) -> Result<RecordItem, AppError> {
    let rt = runtime()?;
    let engine = current_engine()?;
    rt.block_on(engine.update_record(
        id,
        title,
        numeric_value,
        quantity,
        unit_multiplier,
        multiplier_label,
        unit,
        text_value,
        custom_fields,
    ))
}

#[uniffi::export]
pub fn suggest_folder_fields(folder_id: Option<String>) -> Result<Vec<String>, AppError> {
    let rt = runtime()?;
    let engine = current_engine()?;
    rt.block_on(engine.suggest_folder_fields(folder_id))
}

#[uniffi::export]
pub fn delete_record(id: String) -> Result<(), AppError> {
    let rt = runtime()?;
    let engine = current_engine()?;
    rt.block_on(engine.delete_record(id))
}

#[uniffi::export]
pub fn search_all(query: String) -> Result<SearchResults, AppError> {
    let rt = runtime()?;
    let engine = current_engine()?;
    rt.block_on(engine.search_all(query))
}

#[uniffi::export]
pub fn preview_excel_export(
    folder_id: Option<String>,
    period: ExportPeriod,
) -> Result<ExportPreview, AppError> {
    let rt = runtime()?;
    let engine = current_engine()?;
    rt.block_on(engine.preview_excel_export(folder_id, period))
}

#[uniffi::export]
pub fn export_excel(
    folder_id: Option<String>,
    period: ExportPeriod,
    output_dir: String,
) -> Result<ExportResult, AppError> {
    let rt = runtime()?;
    let engine = current_engine()?;
    rt.block_on(engine.export_excel(folder_id, period, output_dir))
}

#[uniffi::export]
pub fn get_security_settings() -> Result<SecuritySettings, AppError> {
    let rt = runtime()?;
    let engine = current_engine()?;
    rt.block_on(engine.get_security_settings())
}

#[uniffi::export]
pub fn set_app_password(
    password: String,
    enable_biometric: bool,
) -> Result<SecuritySettings, AppError> {
    let rt = runtime()?;
    let engine = current_engine()?;
    rt.block_on(engine.set_app_password(password, enable_biometric))
}

#[uniffi::export]
pub fn verify_app_password(password: String) -> Result<bool, AppError> {
    let rt = runtime()?;
    let engine = current_engine()?;
    rt.block_on(engine.verify_app_password(password))
}

#[uniffi::export]
pub fn update_security_options(
    lock_mode: LockMode,
    biometric_enabled: bool,
    auto_lock_on_background: bool,
) -> Result<SecuritySettings, AppError> {
    let rt = runtime()?;
    let engine = current_engine()?;
    rt.block_on(engine.update_security_options(
        lock_mode,
        biometric_enabled,
        auto_lock_on_background,
    ))
}

#[uniffi::export]
pub fn clear_app_password() -> Result<SecuritySettings, AppError> {
    let rt = runtime()?;
    let engine = current_engine()?;
    rt.block_on(engine.clear_app_password())
}

#[uniffi::export]
pub fn get_appearance_settings() -> Result<AppearanceSettings, AppError> {
    let rt = runtime()?;
    let engine = current_engine()?;
    rt.block_on(engine.get_appearance_settings())
}

#[uniffi::export]
pub fn save_appearance_settings(
    settings: AppearanceSettings,
) -> Result<AppearanceSettings, AppError> {
    let rt = runtime()?;
    let engine = current_engine()?;
    rt.block_on(engine.save_appearance_settings(settings))
}
