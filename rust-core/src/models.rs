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

/// Representa un registro numérico y/o textual dentro de una carpeta.
#[derive(Debug, Clone, PartialEq, uniffi::Record)]
pub struct RecordItem {
    pub id: String,
    pub folder_id: String,
    pub title: String,
    pub numeric_value: Option<f64>,
    pub unit: Option<String>,
    pub text_value: Option<String>,
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
