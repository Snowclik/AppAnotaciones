/// Representa una carpeta dentro del explorador jerárquico.
#[derive(Debug, Clone, PartialEq, uniffi::Record)]
pub struct FolderItem {
    pub id: String,
    pub parent_id: Option<String>,
    pub name: String,
    pub icon: Option<String>,
    pub color: Option<String>,
    pub created_at: i64,
    pub updated_at: i64,
    pub subfolder_count: u64,
    pub record_count: u64,
}

/// Par clave/valor definido dinámicamente por el usuario para un registro.
#[derive(Debug, Clone, PartialEq, Eq, uniffi::Record)]
pub struct CustomField {
    pub name: String,
    pub value: String,
}

/// Representa un registro numérico y/o textual dentro de una carpeta.
#[derive(Debug, Clone, PartialEq, uniffi::Record)]
pub struct RecordItem {
    pub id: String,
    pub folder_id: String,
    pub title: String,
    pub numeric_value: Option<f64>,
    pub quantity: Option<f64>,
    pub unit_multiplier: Option<f64>,
    pub multiplier_label: Option<String>,
    pub unit: Option<String>,
    pub text_value: Option<String>,
    pub custom_fields: Vec<CustomField>,
    pub created_at: i64,
    pub updated_at: i64,
}

/// Elemento de navegación rápida en la barra de Breadcrumbs.
#[derive(Debug, Clone, PartialEq, uniffi::Record)]
pub struct BreadcrumbItem {
    /// `None` representa la raíz ("Inicio").
    pub id: Option<String>,
    pub name: String,
}

/// Subtotal numérico agrupado por unidad dentro de una carpeta.
#[derive(Debug, Clone, PartialEq, uniffi::Record)]
pub struct UnitSubtotal {
    pub unit: Option<String>,
    pub total: f64,
    pub count: u64,
}

/// Vista completa e inmutable de la carpeta actual lista para renderizar en Compose.
#[derive(Debug, Clone, PartialEq, uniffi::Record)]
pub struct FolderViewData {
    pub current_folder: Option<FolderItem>,
    pub breadcrumbs: Vec<BreadcrumbItem>,
    pub subfolders: Vec<FolderItem>,
    pub records: Vec<RecordItem>,
    pub total_count: u64,
    pub subfolder_count: u64,
    pub record_count: u64,
    pub numeric_record_count: u64,
    pub has_numeric_summary: bool,
    pub numeric_sum: f64,
    pub unit_subtotals: Vec<UnitSubtotal>,
}

/// Coincidencia de carpeta en la búsqueda global.
#[derive(Debug, Clone, PartialEq, uniffi::Record)]
pub struct SearchFolderResult {
    pub folder: FolderItem,
    pub path_display: String,
}

/// Coincidencia de registro en la búsqueda global.
#[derive(Debug, Clone, PartialEq, uniffi::Record)]
pub struct SearchRecordResult {
    pub record: RecordItem,
    pub folder_name: String,
    pub path_display: String,
}

/// Resultado consolidado de la búsqueda global.
#[derive(Debug, Clone, PartialEq, uniffi::Record)]
pub struct SearchResults {
    pub query: String,
    pub folders: Vec<SearchFolderResult>,
    pub records: Vec<SearchRecordResult>,
    pub total_matches: u64,
    pub numeric_sum: f64,
    pub has_numeric_summary: bool,
}

/// Modo operativo de la base de datos local / réplica embebida.
#[derive(Debug, Clone, PartialEq, Eq, uniffi::Enum)]
pub enum SyncMode {
    LocalOnly,
    EmbeddedReplica,
    OfflineFallback,
}

/// Estado actual de sincronización expuesto hacia la UI.
#[derive(Debug, Clone, PartialEq, Eq, uniffi::Record)]
pub struct SyncStatus {
    pub mode: SyncMode,
    pub synced: bool,
    pub frames_synced: u64,
    pub last_sync_epoch_ms: Option<i64>,
    pub message: String,
}

/// Rango temporal soportado para exportar reportes a Excel.
#[derive(Debug, Clone, Copy, PartialEq, Eq, uniffi::Enum)]
pub enum ExportPeriod {
    Week,
    Month,
    All,
}

/// Metadatos resultantes de una exportación `.xlsx` generada por Rust.
#[derive(Debug, Clone, PartialEq, uniffi::Record)]
pub struct ExportResult {
    pub file_path: String,
    pub file_name: String,
    pub record_count: u64,
    pub folder_count: u64,
    pub numeric_sum: f64,
    pub period_label: String,
}

/// Estrategia de bloqueo activa en la aplicación.
#[derive(Debug, Clone, Copy, PartialEq, Eq, uniffi::Enum)]
pub enum LockMode {
    None,
    AppPassword,
    DeviceBiometric,
}

/// Preferencias de seguridad persistidas localmente en `app_settings`.
#[derive(Debug, Clone, PartialEq, Eq, uniffi::Record)]
pub struct SecuritySettings {
    pub lock_mode: LockMode,
    pub has_password: bool,
    pub biometric_enabled: bool,
    pub auto_lock_on_background: bool,
}

/// Preferencias visuales 2026 persistidas localmente en `app_settings`.
#[derive(Debug, Clone, PartialEq, Eq, uniffi::Record)]
pub struct AppearanceSettings {
    pub palette_id: String,
    pub theme_mode: String,
    pub pure_black: bool,
}

/// Fila de muestra formateada para previsualizar el reporte Excel antes de exportarlo.
#[derive(Debug, Clone, PartialEq, Eq, uniffi::Record)]
pub struct ExportPreviewRow {
    pub date_label: String,
    pub folder_path: String,
    pub title: String,
    pub value_display: String,
    pub calculation_display: Option<String>,
    pub extra_summary: String,
}

/// Resumen y vista previa de la hoja Excel generable según alcance y período.
#[derive(Debug, Clone, PartialEq, uniffi::Record)]
pub struct ExportPreview {
    pub scope_label: String,
    pub period_label: String,
    pub record_count: u64,
    pub folder_count: u64,
    pub numeric_sum: f64,
    pub columns: Vec<String>,
    pub sample_rows: Vec<ExportPreviewRow>,
}
