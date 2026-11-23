use std::collections::HashSet;
use std::path::Path;
use std::sync::RwLock;
use std::time::{Duration, SystemTime, UNIX_EPOCH};

use libsql::{Builder, Connection, Database};

use crate::error::AppError;
use crate::models::{SyncMode, SyncStatus};

const MIGRATIONS_SQL: &str = "
    PRAGMA foreign_keys = ON;

    CREATE TABLE IF NOT EXISTS folders (
        id TEXT PRIMARY KEY,
        parent_id TEXT NULL REFERENCES folders(id) ON DELETE CASCADE,
        name TEXT NOT NULL,
        icon TEXT NULL,
        color TEXT NULL,
        created_at INTEGER NOT NULL,
        updated_at INTEGER NOT NULL
    );

    CREATE TABLE IF NOT EXISTS records (
        id TEXT PRIMARY KEY,
        folder_id TEXT NOT NULL REFERENCES folders(id) ON DELETE CASCADE,
        title TEXT NOT NULL,
        numeric_value REAL NULL,
        unit TEXT NULL,
        text_value TEXT NULL,
        created_at INTEGER NOT NULL,
        updated_at INTEGER NOT NULL
    );

    CREATE TABLE IF NOT EXISTS record_fields (
        id TEXT PRIMARY KEY NOT NULL,
        record_id TEXT NOT NULL,
        field_name TEXT NOT NULL,
        field_value TEXT NOT NULL,
        sort_order INTEGER NOT NULL DEFAULT 0,
        FOREIGN KEY (record_id) REFERENCES records(id) ON DELETE CASCADE
    );

    CREATE TABLE IF NOT EXISTS app_settings (
        key TEXT PRIMARY KEY NOT NULL,
        value TEXT NOT NULL
    );

    CREATE INDEX IF NOT EXISTS idx_folders_parent_id ON folders(parent_id);
    CREATE INDEX IF NOT EXISTS idx_records_folder_id ON records(folder_id);
    CREATE INDEX IF NOT EXISTS idx_folders_name ON folders(name);
    CREATE INDEX IF NOT EXISTS idx_records_title ON records(title);
    CREATE INDEX IF NOT EXISTS idx_record_fields_record_id ON record_fields(record_id);
";

fn now_epoch_ms() -> i64 {
    SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .map(|d| d.as_millis() as i64)
        .unwrap_or(0)
}

fn clean_opt(value: Option<String>) -> Option<String> {
    value.and_then(|s| {
        let trimmed = s.trim();
        if trimmed.is_empty() {
            None
        } else {
            Some(trimmed.to_string())
        }
    })
}

async fn ensure_record_columns(conn: &Connection) -> Result<(), AppError> {
    let mut rows = conn.query("PRAGMA table_info(records)", ()).await?;
    let mut existing = HashSet::new();
    while let Some(row) = rows.next().await? {
        let name: String = row.get(1)?;
        existing.insert(name);
    }

    for (column, sql_type) in [
        ("quantity", "REAL"),
        ("unit_multiplier", "REAL"),
        ("multiplier_label", "TEXT"),
    ] {
        if !existing.contains(column) {
            let ddl = format!("ALTER TABLE records ADD COLUMN {column} {sql_type};");
            conn.execute(&ddl, ()).await?;
        }
    }

    Ok(())
}

pub struct CoreEngine {
    db: Database,
    sync_status: RwLock<SyncStatus>,
}

impl CoreEngine {
    pub async fn open(
        local_path: String,
        turso_url: Option<String>,
        turso_token: Option<String>,
    ) -> Result<Self, AppError> {
        let clean_path = local_path.trim().to_string();
        if clean_path.is_empty() {
            return Err(AppError::Validation {
                detail: "La ruta local de la base de datos no puede estar vacía.".to_string(),
            });
        }

        if clean_path != ":memory:" {
            if let Some(parent) = Path::new(&clean_path).parent() {
                if !parent.as_os_str().is_empty() {
                    std::fs::create_dir_all(parent).map_err(|e| AppError::Internal {
                        detail: format!(
                            "No se pudo preparar el directorio de la base de datos: {e}"
                        ),
                    })?;
                }
            }
        }

        let url_opt = clean_opt(turso_url);
        let token_opt = clean_opt(turso_token);

        let (db, initial_status) = match (url_opt, token_opt) {
            (Some(url), Some(token)) => {
                match Builder::new_remote_replica(&clean_path, url, token)
                    .sync_interval(Duration::from_secs(60))
                    .read_your_writes(true)
                    .build()
                    .await
                {
                    Ok(replica_db) => {
                        let status = match replica_db.sync().await {
                            Ok(replicated) => SyncStatus {
                                mode: SyncMode::EmbeddedReplica,
                                synced: true,
                                frames_synced: replicated.frames_synced() as u64,
                                last_sync_epoch_ms: Some(now_epoch_ms()),
                                message: "Réplica embebida sincronizada con Turso".to_string(),
                            },
                            Err(sync_err) => SyncStatus {
                                mode: SyncMode::EmbeddedReplica,
                                synced: false,
                                frames_synced: 0,
                                last_sync_epoch_ms: None,
                                message: format!(
                                    "Modo offline con réplica preparada (pendiente de sincronizar): {sync_err}"
                                ),
                            },
                        };
                        (replica_db, status)
                    }
                    Err(replica_err) => {
                        let local_db = Builder::new_local(&clean_path).build().await?;
                        let status = SyncStatus {
                            mode: SyncMode::OfflineFallback,
                            synced: false,
                            frames_synced: 0,
                            last_sync_epoch_ms: None,
                            message: format!(
                                "Operando en modo local sin conexión a Turso: {replica_err}"
                            ),
                        };
                        (local_db, status)
                    }
                }
            }
            _ => {
                let local_db = Builder::new_local(&clean_path).build().await?;
                let status = SyncStatus {
                    mode: SyncMode::LocalOnly,
                    synced: false,
                    frames_synced: 0,
                    last_sync_epoch_ms: None,
                    message: "Modo local activo (sin credenciales de Turso)".to_string(),
                };
                (local_db, status)
            }
        };

        let engine = Self {
            db,
            sync_status: RwLock::new(initial_status),
        };
        engine.run_migrations().await?;
        Ok(engine)
    }

    pub async fn connect(&self) -> Result<Connection, AppError> {
        let conn = self.db.connect()?;
        conn.execute("PRAGMA foreign_keys = ON;", ()).await?;
        Ok(conn)
    }

    async fn run_migrations(&self) -> Result<(), AppError> {
        let conn = self.connect().await?;
        conn.execute_batch(MIGRATIONS_SQL).await?;
        ensure_record_columns(&conn).await?;
        Ok(())
    }

    pub fn get_sync_status(&self) -> SyncStatus {
        self.sync_status
            .read()
            .map(|guard| guard.clone())
            .unwrap_or_else(|poisoned| poisoned.into_inner().clone())
    }

    pub async fn sync_now(&self) -> Result<SyncStatus, AppError> {
        let current_mode = self.get_sync_status().mode;
        match current_mode {
            SyncMode::LocalOnly => Ok(self.get_sync_status()),
            SyncMode::OfflineFallback => Err(AppError::Sync {
                detail: "La app inició sin conexión a Turso en modo local offline. Reintenta con conexión al reiniciar.".to_string(),
            }),
            SyncMode::EmbeddedReplica => match self.db.sync().await {
                Ok(replicated) => {
                    let next_status = SyncStatus {
                        mode: SyncMode::EmbeddedReplica,
                        synced: true,
                        frames_synced: replicated.frames_synced() as u64,
                        last_sync_epoch_ms: Some(now_epoch_ms()),
                        message: "Sincronizado con Turso".to_string(),
                    };
                    if let Ok(mut guard) = self.sync_status.write() {
                        *guard = next_status.clone();
                    }
                    Ok(next_status)
                }
                Err(err) => {
                    let msg = format!("No se pudo sincronizar con Turso: {err}");
                    if let Ok(mut guard) = self.sync_status.write() {
                        guard.synced = false;
                        guard.message = msg.clone();
                    }
                    Err(AppError::Sync { detail: msg })
                }
            },
        }
    }
}
