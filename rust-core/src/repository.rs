use std::collections::HashMap;
use std::time::{SystemTime, UNIX_EPOCH};

use libsql::Connection;
use uuid::Uuid;

use crate::db::CoreEngine;
use crate::error::AppError;
use crate::models::{
    BreadcrumbItem, CustomField, FolderItem, FolderViewData, RecordItem, SearchFolderResult,
    SearchRecordResult, SearchResults, UnitSubtotal,
};

const FOLDER_SELECT_SQL: &str = "
    SELECT
        f.id,
        f.parent_id,
        f.name,
        f.icon,
        f.color,
        f.created_at,
        f.updated_at,
        (SELECT COUNT(*) FROM folders c WHERE c.parent_id = f.id) AS subfolder_count,
        (SELECT COUNT(*) FROM records r WHERE r.folder_id = f.id) AS record_count
    FROM folders f
";

pub(crate) fn now_epoch_ms() -> i64 {
    SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .map(|d| d.as_millis() as i64)
        .unwrap_or(0)
}

pub(crate) fn normalize_required(value: &str, field_name: &str) -> Result<String, AppError> {
    let trimmed = value.trim();
    if trimmed.is_empty() {
        return Err(AppError::Validation {
            detail: format!("El campo '{field_name}' no puede estar vacío."),
        });
    }
    Ok(trimmed.to_string())
}

pub(crate) fn normalize_optional(value: Option<String>) -> Option<String> {
    value.and_then(|raw| {
        let trimmed = raw.trim();
        if trimmed.is_empty() {
            None
        } else {
            Some(trimmed.to_string())
        }
    })
}

fn normalize_custom_fields(fields: Vec<CustomField>) -> Result<Vec<CustomField>, AppError> {
    let mut normalized = Vec::with_capacity(fields.len());
    for field in fields {
        let name = field.name.trim();
        let value = field.value.trim();
        if name.is_empty() && value.is_empty() {
            continue;
        }
        if name.is_empty() {
            return Err(AppError::Validation {
                detail: "Cada dato adicional debe tener un nombre de campo.".to_string(),
            });
        }
        if value.is_empty() {
            return Err(AppError::Validation {
                detail: format!("Completa el valor para el campo adicional '{name}'."),
            });
        }
        normalized.push(CustomField {
            name: name.to_string(),
            value: value.to_string(),
        });
    }
    Ok(normalized)
}

fn validate_numeric(value: Option<f64>) -> Result<Option<f64>, AppError> {
    if let Some(num) = value {
        if !num.is_finite() {
            return Err(AppError::Validation {
                detail: "El valor numérico debe ser un número finito válido.".to_string(),
            });
        }
    }
    Ok(value)
}

type ResolvedCalculation = (Option<f64>, Option<f64>, Option<f64>, Option<String>);

fn resolve_numeric_calculation(
    numeric_value: Option<f64>,
    quantity: Option<f64>,
    unit_multiplier: Option<f64>,
    multiplier_label: Option<String>,
) -> Result<ResolvedCalculation, AppError> {
    let valid_numeric = validate_numeric(numeric_value)?;
    let valid_qty = validate_numeric(quantity)?;
    let valid_mult = validate_numeric(unit_multiplier)?;
    let clean_label = normalize_optional(multiplier_label);

    match (valid_qty, valid_mult, valid_numeric) {
        (Some(q), Some(m), _) => {
            let product = q * m;
            if !product.is_finite() {
                return Err(AppError::Validation {
                    detail: "El resultado del cálculo excede el rango numérico válido.".to_string(),
                });
            }
            Ok((Some(product), Some(q), Some(m), clean_label))
        }
        (Some(q), None, Some(total)) => Ok((Some(total), Some(q), None, clean_label)),
        (None, None, num) => Ok((num, None, None, None)),
        _ => Err(AppError::Validation {
            detail: "Completa cantidad y precio o cantidad y multiplicador válidos.".to_string(),
        }),
    }
}

fn escape_like_pattern(raw: &str) -> String {
    let mut escaped = String::with_capacity(raw.len() + 4);
    for ch in raw.chars() {
        match ch {
            '\\' | '%' | '_' => {
                escaped.push('\\');
                escaped.push(ch);
            }
            _ => escaped.push(ch),
        }
    }
    format!("%{escaped}%")
}

pub(crate) fn non_negative_count(value: i64) -> u64 {
    if value <= 0 {
        0
    } else {
        value as u64
    }
}

fn row_to_folder(row: &libsql::Row) -> Result<FolderItem, AppError> {
    Ok(FolderItem {
        id: row.get(0)?,
        parent_id: row.get(1)?,
        name: row.get(2)?,
        icon: row.get(3)?,
        color: row.get(4)?,
        created_at: row.get(5)?,
        updated_at: row.get(6)?,
        subfolder_count: non_negative_count(row.get(7)?),
        record_count: non_negative_count(row.get(8)?),
    })
}

pub(crate) fn row_to_record(
    row: &libsql::Row,
    custom_fields: Vec<CustomField>,
) -> Result<RecordItem, AppError> {
    Ok(RecordItem {
        id: row.get(0)?,
        folder_id: row.get(1)?,
        title: row.get(2)?,
        numeric_value: row.get(3)?,
        quantity: row.get(4)?,
        unit_multiplier: row.get(5)?,
        multiplier_label: row.get(6)?,
        unit: row.get(7)?,
        text_value: row.get(8)?,
        custom_fields,
        created_at: row.get(9)?,
        updated_at: row.get(10)?,
    })
}

pub(crate) async fn load_fields_for_records(
    conn: &Connection,
    record_ids: &[String],
) -> Result<HashMap<String, Vec<CustomField>>, AppError> {
    let mut map: HashMap<String, Vec<CustomField>> = HashMap::new();
    if record_ids.is_empty() {
        return Ok(map);
    }

    for chunk in record_ids.chunks(200) {
        let placeholders = (1..=chunk.len())
            .map(|idx| format!("?{idx}"))
            .collect::<Vec<_>>()
            .join(", ");
        let sql = format!(
            "SELECT record_id, field_name, field_value
             FROM record_fields
             WHERE record_id IN ({placeholders})
             ORDER BY sort_order ASC"
        );
        let mut rows = conn
            .query(&sql, libsql::params_from_iter(chunk.iter().map(String::as_str)))
            .await?;
        while let Some(row) = rows.next().await? {
            let record_id: String = row.get(0)?;
            let name: String = row.get(1)?;
            let value: String = row.get(2)?;
            map.entry(record_id)
                .or_default()
                .push(CustomField { name, value });
        }
    }

    Ok(map)
}

async fn replace_record_fields(
    conn: &Connection,
    record_id: &str,
    fields: &[CustomField],
) -> Result<(), AppError> {
    conn.execute("DELETE FROM record_fields WHERE record_id = ?1", [record_id])
        .await?;

    for (idx, field) in fields.iter().enumerate() {
        let field_id = Uuid::new_v4().to_string();
        conn.execute(
            "INSERT INTO record_fields (id, record_id, field_name, field_value, sort_order)
             VALUES (?1, ?2, ?3, ?4, ?5)",
            (
                field_id.as_str(),
                record_id,
                field.name.as_str(),
                field.value.as_str(),
                idx as i64,
            ),
        )
        .await?;
    }

    Ok(())
}

pub(crate) async fn ensure_folder_exists(conn: &Connection, id: &str) -> Result<(), AppError> {
    let mut rows = conn
        .query("SELECT 1 FROM folders WHERE id = ?1 LIMIT 1", [id])
        .await?;
    if rows.next().await?.is_none() {
        return Err(AppError::NotFound {
            entity: "carpeta".to_string(),
            id: id.to_string(),
        });
    }
    Ok(())
}

async fn fetch_folder_by_id(conn: &Connection, id: &str) -> Result<FolderItem, AppError> {
    let sql = format!("{FOLDER_SELECT_SQL} WHERE f.id = ?1 LIMIT 1");
    let mut rows = conn.query(&sql, [id]).await?;
    let row = rows.next().await?.ok_or_else(|| AppError::NotFound {
        entity: "carpeta".to_string(),
        id: id.to_string(),
    })?;
    row_to_folder(&row)
}

async fn fetch_record_by_id(conn: &Connection, id: &str) -> Result<RecordItem, AppError> {
    let mut rows = conn
        .query(
            "SELECT id, folder_id, title, numeric_value, quantity, unit_multiplier, multiplier_label, unit, text_value, created_at, updated_at
             FROM records WHERE id = ?1 LIMIT 1",
            [id],
        )
        .await?;
    let row = rows.next().await?.ok_or_else(|| AppError::NotFound {
        entity: "registro".to_string(),
        id: id.to_string(),
    })?;
    let id_str = id.to_string();
    let mut fields_map = load_fields_for_records(conn, std::slice::from_ref(&id_str)).await?;
    let fields = fields_map.remove(&id_str).unwrap_or_default();
    row_to_record(&row, fields)
}

async fn build_breadcrumbs(
    conn: &Connection,
    folder_id: Option<&str>,
) -> Result<Vec<BreadcrumbItem>, AppError> {
    let mut breadcrumbs = vec![BreadcrumbItem {
        id: None,
        name: "Inicio".to_string(),
    }];

    let Some(target_id) = folder_id else {
        return Ok(breadcrumbs);
    };

    let mut rows = conn
        .query(
            "WITH RECURSIVE ancestors(id, parent_id, name, depth) AS (
                SELECT id, parent_id, name, 0 FROM folders WHERE id = ?1
                UNION ALL
                SELECT f.id, f.parent_id, f.name, a.depth + 1
                FROM folders f
                INNER JOIN ancestors a ON f.id = a.parent_id
                WHERE a.depth < 128
            )
            SELECT id, name FROM ancestors ORDER BY depth DESC",
            [target_id],
        )
        .await?;

    while let Some(row) = rows.next().await? {
        let id: String = row.get(0)?;
        let name: String = row.get(1)?;
        breadcrumbs.push(BreadcrumbItem { id: Some(id), name });
    }

    Ok(breadcrumbs)
}

pub(crate) async fn load_folder_hierarchy_map(
    conn: &Connection,
) -> Result<HashMap<String, (Option<String>, String)>, AppError> {
    let mut rows = conn
        .query("SELECT id, parent_id, name FROM folders", ())
        .await?;
    let mut map = HashMap::new();
    while let Some(row) = rows.next().await? {
        let id: String = row.get(0)?;
        let parent_id: Option<String> = row.get(1)?;
        let name: String = row.get(2)?;
        map.insert(id, (parent_id, name));
    }
    Ok(map)
}

pub(crate) fn format_folder_path(
    map: &HashMap<String, (Option<String>, String)>,
    folder_id: &str,
) -> String {
    let mut segments = Vec::new();
    let mut cursor = Some(folder_id.to_string());
    let mut guard = 0usize;

    while let Some(id) = cursor {
        if guard >= 128 {
            break;
        }
        guard += 1;
        if let Some((parent_id, name)) = map.get(&id) {
            segments.push(name.as_str());
            cursor = parent_id.clone();
        } else {
            break;
        }
    }

    segments.push("Inicio");
    segments.reverse();
    segments.join(" > ")
}

impl CoreEngine {
    pub async fn list_folder_contents(
        &self,
        parent_id: Option<String>,
    ) -> Result<FolderViewData, AppError> {
        let conn = self.connect().await?;
        let target_parent = normalize_optional(parent_id);

        let current_folder = match target_parent.as_deref() {
            Some(pid) => Some(fetch_folder_by_id(&conn, pid).await?),
            None => None,
        };

        let breadcrumbs = build_breadcrumbs(&conn, target_parent.as_deref()).await?;

        let mut subfolders = Vec::new();
        let mut folder_rows = match target_parent.as_deref() {
            Some(pid) => {
                let sql = format!(
                    "{FOLDER_SELECT_SQL} WHERE f.parent_id = ?1 ORDER BY f.name COLLATE NOCASE ASC"
                );
                conn.query(&sql, [pid]).await?
            }
            None => {
                let sql = format!(
                    "{FOLDER_SELECT_SQL} WHERE f.parent_id IS NULL ORDER BY f.name COLLATE NOCASE ASC"
                );
                conn.query(&sql, ()).await?
            }
        };
        while let Some(row) = folder_rows.next().await? {
            subfolders.push(row_to_folder(&row)?);
        }

        let mut records = Vec::new();
        let mut numeric_record_count = 0u64;
        let mut numeric_sum = 0.0f64;
        let mut unit_subtotals = Vec::new();

        if let Some(pid) = target_parent.as_deref() {
            let mut record_rows = conn
                .query(
                    "SELECT id, folder_id, title, numeric_value, quantity, unit_multiplier, multiplier_label, unit, text_value, created_at, updated_at
                     FROM records
                     WHERE folder_id = ?1
                     ORDER BY updated_at DESC, created_at DESC",
                    [pid],
                )
                .await?;
            while let Some(row) = record_rows.next().await? {
                records.push(row_to_record(&row, Vec::new())?);
            }
            if !records.is_empty() {
                let ids: Vec<String> = records.iter().map(|r| r.id.clone()).collect();
                let mut fields_map = load_fields_for_records(&conn, &ids).await?;
                for record in &mut records {
                    if let Some(fields) = fields_map.remove(&record.id) {
                        record.custom_fields = fields;
                    }
                }
            }

            let mut summary_rows = conn
                .query(
                    "SELECT COUNT(numeric_value), COALESCE(SUM(numeric_value), 0.0)
                     FROM records
                     WHERE folder_id = ?1",
                    [pid],
                )
                .await?;
            if let Some(row) = summary_rows.next().await? {
                numeric_record_count = non_negative_count(row.get(0)?);
                numeric_sum = row.get(1)?;
            }

            let mut subtotal_rows = conn
                .query(
                    "SELECT unit, COALESCE(SUM(numeric_value), 0.0), COUNT(*)
                     FROM records
                     WHERE folder_id = ?1 AND numeric_value IS NOT NULL
                     GROUP BY unit
                     ORDER BY COALESCE(unit, '') COLLATE NOCASE ASC",
                    [pid],
                )
                .await?;
            while let Some(row) = subtotal_rows.next().await? {
                unit_subtotals.push(UnitSubtotal {
                    unit: row.get(0)?,
                    total: row.get(1)?,
                    count: non_negative_count(row.get(2)?),
                });
            }
        }

        let subfolder_count = subfolders.len() as u64;
        let record_count = records.len() as u64;

        Ok(FolderViewData {
            current_folder,
            breadcrumbs,
            subfolders,
            records,
            total_count: subfolder_count + record_count,
            subfolder_count,
            record_count,
            numeric_record_count,
            has_numeric_summary: numeric_record_count > 0,
            numeric_sum,
            unit_subtotals,
        })
    }

    pub async fn create_folder(
        &self,
        parent_id: Option<String>,
        name: String,
        color: Option<String>,
        icon: Option<String>,
    ) -> Result<FolderItem, AppError> {
        let conn = self.connect().await?;
        let clean_name = normalize_required(&name, "nombre")?;
        let clean_parent = normalize_optional(parent_id);
        let clean_color = normalize_optional(color);
        let clean_icon = normalize_optional(icon);

        if let Some(pid) = clean_parent.as_deref() {
            ensure_folder_exists(&conn, pid).await?;
        }

        let id = Uuid::new_v4().to_string();
        let now = now_epoch_ms();

        conn.execute(
            "INSERT INTO folders (id, parent_id, name, icon, color, created_at, updated_at)
             VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7)",
            (
                id.as_str(),
                clean_parent.as_deref(),
                clean_name.as_str(),
                clean_icon.as_deref(),
                clean_color.as_deref(),
                now,
                now,
            ),
        )
        .await?;

        fetch_folder_by_id(&conn, &id).await
    }

    pub async fn update_folder(
        &self,
        id: String,
        name: String,
        color: Option<String>,
        icon: Option<String>,
    ) -> Result<FolderItem, AppError> {
        let conn = self.connect().await?;
        let clean_id = normalize_required(&id, "id")?;
        let clean_name = normalize_required(&name, "nombre")?;
        let clean_color = normalize_optional(color);
        let clean_icon = normalize_optional(icon);
        let now = now_epoch_ms();

        let affected = conn
            .execute(
                "UPDATE folders
                 SET name = ?1, color = ?2, icon = ?3, updated_at = ?4
                 WHERE id = ?5",
                (
                    clean_name.as_str(),
                    clean_color.as_deref(),
                    clean_icon.as_deref(),
                    now,
                    clean_id.as_str(),
                ),
            )
            .await?;

        if affected == 0 {
            return Err(AppError::NotFound {
                entity: "carpeta".to_string(),
                id: clean_id,
            });
        }

        fetch_folder_by_id(&conn, &clean_id).await
    }

    pub async fn delete_folder(&self, id: String) -> Result<(), AppError> {
        let conn = self.connect().await?;
        let clean_id = normalize_required(&id, "id")?;
        let affected = conn
            .execute("DELETE FROM folders WHERE id = ?1", [clean_id.as_str()])
            .await?;
        if affected == 0 {
            return Err(AppError::NotFound {
                entity: "carpeta".to_string(),
                id: clean_id,
            });
        }
        Ok(())
    }

    #[allow(clippy::too_many_arguments)]
    pub async fn create_record(
        &self,
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
        let conn = self.connect().await?;
        let clean_folder_id = normalize_required(&folder_id, "folder_id")?;
        let clean_title = normalize_required(&title, "título")?;
        let (effective_numeric, clean_qty, clean_mult, clean_mult_label) =
            resolve_numeric_calculation(numeric_value, quantity, unit_multiplier, multiplier_label)?;
        let clean_unit = normalize_optional(unit);
        let clean_text = normalize_optional(text_value);
        let clean_fields = normalize_custom_fields(custom_fields)?;

        ensure_folder_exists(&conn, &clean_folder_id).await?;

        let id = Uuid::new_v4().to_string();
        let now = now_epoch_ms();

        conn.execute(
            "INSERT INTO records (id, folder_id, title, numeric_value, quantity, unit_multiplier, multiplier_label, unit, text_value, created_at, updated_at)
             VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?11)",
            libsql::params![
                id.as_str(),
                clean_folder_id.as_str(),
                clean_title.as_str(),
                effective_numeric,
                clean_qty,
                clean_mult,
                clean_mult_label.as_deref(),
                clean_unit.as_deref(),
                clean_text.as_deref(),
                now,
                now
            ],
        )
        .await?;

        replace_record_fields(&conn, &id, &clean_fields).await?;

        fetch_record_by_id(&conn, &id).await
    }

    #[allow(clippy::too_many_arguments)]
    pub async fn update_record(
        &self,
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
        let conn = self.connect().await?;
        let clean_id = normalize_required(&id, "id")?;
        let clean_title = normalize_required(&title, "título")?;
        let (effective_numeric, clean_qty, clean_mult, clean_mult_label) =
            resolve_numeric_calculation(numeric_value, quantity, unit_multiplier, multiplier_label)?;
        let clean_unit = normalize_optional(unit);
        let clean_text = normalize_optional(text_value);
        let clean_fields = normalize_custom_fields(custom_fields)?;
        let now = now_epoch_ms();

        let affected = conn
            .execute(
                "UPDATE records
                 SET title = ?1, numeric_value = ?2, quantity = ?3, unit_multiplier = ?4, multiplier_label = ?5, unit = ?6, text_value = ?7, updated_at = ?8
                 WHERE id = ?9",
                libsql::params![
                    clean_title.as_str(),
                    effective_numeric,
                    clean_qty,
                    clean_mult,
                    clean_mult_label.as_deref(),
                    clean_unit.as_deref(),
                    clean_text.as_deref(),
                    now,
                    clean_id.as_str()
                ],
            )
            .await?;

        if affected == 0 {
            return Err(AppError::NotFound {
                entity: "registro".to_string(),
                id: clean_id,
            });
        }

        replace_record_fields(&conn, &clean_id, &clean_fields).await?;

        fetch_record_by_id(&conn, &clean_id).await
    }

    pub async fn suggest_folder_fields(
        &self,
        folder_id: Option<String>,
    ) -> Result<Vec<String>, AppError> {
        let conn = self.connect().await?;
        let clean_folder = normalize_optional(folder_id);

        let mut rows = if let Some(fid) = clean_folder {
            let hierarchy_map = load_folder_hierarchy_map(&conn).await?;
            let mut scope_root = fid;
            for _ in 0..128 {
                if let Some((Some(parent), _)) = hierarchy_map.get(&scope_root) {
                    scope_root = parent.clone();
                } else {
                    break;
                }
            }
            conn.query(
                "WITH RECURSIVE subtree(id) AS (
                    SELECT ?1
                    UNION ALL
                    SELECT f.id FROM folders f INNER JOIN subtree s ON f.parent_id = s.id
                )
                SELECT MIN(TRIM(rf.field_name)) AS display_name
                FROM record_fields rf
                INNER JOIN records r ON r.id = rf.record_id
                INNER JOIN subtree s ON r.folder_id = s.id
                GROUP BY LOWER(TRIM(rf.field_name))
                ORDER BY COUNT(*) DESC, MAX(r.updated_at) DESC, MIN(rf.sort_order) ASC
                LIMIT 8",
                [scope_root.as_str()],
            )
            .await?
        } else {
            conn.query(
                "SELECT MIN(TRIM(rf.field_name)) AS display_name
                 FROM record_fields rf
                 INNER JOIN records r ON r.id = rf.record_id
                 GROUP BY LOWER(TRIM(rf.field_name))
                 ORDER BY COUNT(*) DESC, MAX(r.updated_at) DESC, MIN(rf.sort_order) ASC
                 LIMIT 8",
                (),
            )
            .await?
        };

        let mut suggestions = Vec::new();
        while let Some(row) = rows.next().await? {
            let name: String = row.get(0)?;
            if !name.is_empty() {
                suggestions.push(name);
            }
        }
        Ok(suggestions)
    }

    pub async fn delete_record(&self, id: String) -> Result<(), AppError> {
        let conn = self.connect().await?;
        let clean_id = normalize_required(&id, "id")?;
        let affected = conn
            .execute("DELETE FROM records WHERE id = ?1", [clean_id.as_str()])
            .await?;
        if affected == 0 {
            return Err(AppError::NotFound {
                entity: "registro".to_string(),
                id: clean_id,
            });
        }
        Ok(())
    }

    pub async fn search_all(&self, query: String) -> Result<SearchResults, AppError> {
        let trimmed = query.trim().to_string();
        if trimmed.is_empty() {
            return Ok(SearchResults {
                query: String::new(),
                folders: Vec::new(),
                records: Vec::new(),
                total_matches: 0,
                numeric_sum: 0.0,
                has_numeric_summary: false,
            });
        }

        let conn = self.connect().await?;
        let hierarchy_map = load_folder_hierarchy_map(&conn).await?;
        let pattern = escape_like_pattern(&trimmed);

        let folder_sql = format!(
            "{FOLDER_SELECT_SQL}
             WHERE f.name LIKE ?1 ESCAPE '\\'
             ORDER BY f.name COLLATE NOCASE ASC
             LIMIT 100"
        );
        let mut folder_rows = conn.query(&folder_sql, [pattern.as_str()]).await?;
        let mut folders = Vec::new();
        while let Some(row) = folder_rows.next().await? {
            let folder = row_to_folder(&row)?;
            let path_display = match folder.parent_id.as_deref() {
                Some(pid) => format_folder_path(&hierarchy_map, pid),
                None => "Inicio".to_string(),
            };
            folders.push(SearchFolderResult {
                folder,
                path_display,
            });
        }

        let mut record_rows = conn
            .query(
                "SELECT id, folder_id, title, numeric_value, quantity, unit_multiplier, multiplier_label, unit, text_value, created_at, updated_at
                 FROM records
                 WHERE title LIKE ?1 ESCAPE '\\'
                    OR COALESCE(text_value, '') LIKE ?1 ESCAPE '\\'
                    OR COALESCE(unit, '') LIKE ?1 ESCAPE '\\'
                    OR COALESCE(multiplier_label, '') LIKE ?1 ESCAPE '\\'
                    OR EXISTS (
                        SELECT 1 FROM record_fields rf
                        WHERE rf.record_id = records.id
                          AND (rf.field_name LIKE ?1 ESCAPE '\\' OR rf.field_value LIKE ?1 ESCAPE '\\')
                    )
                 ORDER BY updated_at DESC, created_at DESC
                 LIMIT 200",
                [pattern.as_str()],
            )
            .await?;

        let mut matched_records = Vec::new();
        let mut numeric_sum = 0.0f64;
        let mut numeric_matches = 0u64;

        while let Some(row) = record_rows.next().await? {
            let record = row_to_record(&row, Vec::new())?;
            if let Some(val) = record.numeric_value {
                numeric_sum += val;
                numeric_matches += 1;
            }
            matched_records.push(record);
        }

        if !matched_records.is_empty() {
            let ids: Vec<String> = matched_records.iter().map(|r| r.id.clone()).collect();
            let mut fields_map = load_fields_for_records(&conn, &ids).await?;
            for record in &mut matched_records {
                if let Some(fields) = fields_map.remove(&record.id) {
                    record.custom_fields = fields;
                }
            }
        }

        let mut records = Vec::with_capacity(matched_records.len());
        for record in matched_records {
            let folder_name = hierarchy_map
                .get(&record.folder_id)
                .map(|(_, name)| name.clone())
                .unwrap_or_else(|| "Carpeta".to_string());
            let path_display = format_folder_path(&hierarchy_map, &record.folder_id);
            records.push(SearchRecordResult {
                record,
                folder_name,
                path_display,
            });
        }

        let total_matches = (folders.len() + records.len()) as u64;

        Ok(SearchResults {
            query: trimmed,
            folders,
            records,
            total_matches,
            numeric_sum,
            has_numeric_summary: numeric_matches > 0,
        })
    }
}
