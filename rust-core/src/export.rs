use std::collections::{BTreeMap, HashSet};
use std::fs;
use std::path::Path;

use chrono::{Datelike, Duration, Local, TimeZone};
use libsql::Connection;
use rust_xlsxwriter::{Color, Format, FormatAlign, Workbook};

use crate::db::CoreEngine;
use crate::error::AppError;
use crate::models::{
    CustomField, ExportPeriod, ExportPreview, ExportPreviewRow, ExportResult, RecordItem,
};
use crate::repository::{
    ensure_folder_exists, format_folder_path, load_fields_for_records, load_folder_hierarchy_map,
    normalize_optional, normalize_required, row_to_record,
};

struct PeriodWindow {
    start_ms: Option<i64>,
    period_label: &'static str,
    file_slug: &'static str,
}

fn compute_period_window(period: ExportPeriod) -> PeriodWindow {
    let now = Local::now();
    match period {
        ExportPeriod::Week => {
            let days_from_monday = i64::from(now.weekday().num_days_from_monday());
            let start_date = now.date_naive() - Duration::days(days_from_monday);
            let start_ms = start_date
                .and_hms_opt(0, 0, 0)
                .and_then(|naive| Local.from_local_datetime(&naive).earliest())
                .map(|dt| dt.timestamp_millis())
                .unwrap_or_else(|| (now - Duration::days(7)).timestamp_millis());
            PeriodWindow {
                start_ms: Some(start_ms),
                period_label: "Semana actual",
                file_slug: "semana",
            }
        }
        ExportPeriod::Month => {
            let start_date = now.date_naive().with_day(1).unwrap_or(now.date_naive());
            let start_ms = start_date
                .and_hms_opt(0, 0, 0)
                .and_then(|naive| Local.from_local_datetime(&naive).earliest())
                .map(|dt| dt.timestamp_millis())
                .unwrap_or_else(|| (now - Duration::days(30)).timestamp_millis());
            PeriodWindow {
                start_ms: Some(start_ms),
                period_label: "Mes actual",
                file_slug: "mes",
            }
        }
        ExportPeriod::All => PeriodWindow {
            start_ms: None,
            period_label: "Todo el historial",
            file_slug: "todo",
        },
    }
}

fn sanitize_slug(raw: &str) -> String {
    let mut slug = String::with_capacity(raw.len());
    let mut last_underscore = false;
    for ch in raw.chars() {
        if ch.is_ascii_alphanumeric() {
            slug.push(ch.to_ascii_lowercase());
            last_underscore = false;
        } else if !last_underscore && !slug.is_empty() {
            slug.push('_');
            last_underscore = true;
        }
        if slug.len() >= 28 {
            break;
        }
    }
    while slug.ends_with('_') {
        slug.pop();
    }
    if slug.is_empty() {
        "negocio".to_string()
    } else {
        slug
    }
}

fn format_epoch_local(epoch_ms: i64) -> String {
    Local
        .timestamp_millis_opt(epoch_ms)
        .single()
        .map(|dt| dt.format("%Y-%m-%d %H:%M").to_string())
        .unwrap_or_else(|| epoch_ms.to_string())
}

fn format_epoch_short(epoch_ms: i64) -> String {
    Local
        .timestamp_millis_opt(epoch_ms)
        .single()
        .map(|dt| dt.format("%d/%m %H:%M").to_string())
        .unwrap_or_else(|| epoch_ms.to_string())
}

fn format_compact_number(value: f64) -> String {
    if (value - value.round()).abs() < 1e-9 {
        format!("{}", value.round() as i64)
    } else {
        let s = format!("{value:.2}");
        s.trim_end_matches('0').trim_end_matches('.').to_string()
    }
}

fn format_calculation_display(record: &RecordItem) -> Option<String> {
    let q = record.quantity?;
    let q_str = format_compact_number(q);
    let label = record.multiplier_label.as_deref().unwrap_or("");

    match record.unit_multiplier {
        Some(m) => {
            let m_str = format_compact_number(m);
            if record.unit.as_deref() == Some("$") {
                if !label.is_empty() && label != "$/u" {
                    Some(format!("{q_str} {label} × ${m_str} c/u"))
                } else {
                    Some(format!("{q_str} × ${m_str} c/u"))
                }
            } else if !label.is_empty() {
                Some(format!("{q_str} × {m_str} {label}"))
            } else {
                Some(format!("{q_str} × {m_str}"))
            }
        }
        None => {
            if !label.is_empty() {
                Some(format!("por {q_str} {label}"))
            } else {
                Some(format!("por {q_str}"))
            }
        }
    }
}

fn format_value_display(numeric_value: Option<f64>, unit: Option<&str>) -> String {
    let Some(val) = numeric_value else {
        return "—".to_string();
    };
    let num_str = format_compact_number(val);
    match unit {
        Some("$") => format!("${num_str}"),
        Some(u) if !u.is_empty() => format!("{num_str} {u}"),
        _ => num_str,
    }
}

fn build_record_sheet_headers(custom_columns: &[String]) -> Vec<String> {
    let mut headers: Vec<String> = vec![
        "Fecha".to_string(),
        "Carpeta".to_string(),
        "Título".to_string(),
        "Valor numérico".to_string(),
        "Cálculo".to_string(),
        "Unidad".to_string(),
        "Notas".to_string(),
    ];
    headers.extend(custom_columns.iter().cloned());
    headers
}

async fn count_scope_folders(
    conn: &Connection,
    folder_id: Option<&str>,
    total_folders: usize,
) -> Result<u64, AppError> {
    let Some(root_id) = folder_id else {
        return Ok(total_folders as u64);
    };
    let mut rows = conn
        .query(
            "WITH RECURSIVE subtree(id) AS (
                SELECT ?1
                UNION ALL
                SELECT f.id FROM folders f INNER JOIN subtree s ON f.parent_id = s.id
            )
            SELECT COUNT(*) FROM subtree",
            [root_id],
        )
        .await?;
    if let Some(row) = rows.next().await? {
        let count: i64 = row.get(0)?;
        return Ok(if count < 0 { 0 } else { count as u64 });
    }
    Ok(1)
}

async fn fetch_records_for_export(
    conn: &Connection,
    folder_id: Option<&str>,
    start_ms: Option<i64>,
) -> Result<Vec<RecordItem>, AppError> {
    let mut rows = match folder_id {
        Some(root_id) => {
            conn.query(
                "WITH RECURSIVE subtree(id) AS (
                    SELECT ?1
                    UNION ALL
                    SELECT f.id FROM folders f INNER JOIN subtree s ON f.parent_id = s.id
                )
                SELECT r.id, r.folder_id, r.title, r.numeric_value, r.quantity, r.unit_multiplier, r.multiplier_label, r.unit, r.text_value, r.created_at, r.updated_at
                FROM records r
                INNER JOIN subtree s ON r.folder_id = s.id
                WHERE (?2 IS NULL OR r.created_at >= ?2)
                ORDER BY r.created_at DESC, r.updated_at DESC",
                (root_id, start_ms),
            )
            .await?
        }
        None => {
            conn.query(
                "SELECT r.id, r.folder_id, r.title, r.numeric_value, r.quantity, r.unit_multiplier, r.multiplier_label, r.unit, r.text_value, r.created_at, r.updated_at
                 FROM records r
                 WHERE (?1 IS NULL OR r.created_at >= ?1)
                 ORDER BY r.created_at DESC, r.updated_at DESC",
                [start_ms],
            )
            .await?
        }
    };

    let mut records = Vec::new();
    while let Some(row) = rows.next().await? {
        records.push(row_to_record(&row, Vec::new())?);
    }

    if !records.is_empty() {
        let record_ids: Vec<String> = records.iter().map(|r| r.id.clone()).collect();
        let mut fields_map = load_fields_for_records(conn, &record_ids).await?;
        for record in &mut records {
            if let Some(fields) = fields_map.remove(&record.id) {
                record.custom_fields = fields;
            }
        }
    }

    Ok(records)
}

fn collect_custom_field_columns(records: &[RecordItem]) -> Vec<String> {
    let mut seen = HashSet::new();
    let mut columns = Vec::new();
    for record in records {
        for CustomField { name, .. } in &record.custom_fields {
            if seen.insert(name.clone()) {
                columns.push(name.clone());
            }
        }
    }
    columns
}

struct PreparedExportData {
    window: PeriodWindow,
    hierarchy_map: std::collections::HashMap<String, (Option<String>, String)>,
    folder_count: u64,
    records: Vec<RecordItem>,
    scope_display: String,
    scope_slug: String,
    numeric_sum: f64,
    unit_totals: BTreeMap<String, (f64, u64)>,
    custom_columns: Vec<String>,
}

async fn prepare_export_data(
    conn: &Connection,
    folder_id: Option<String>,
    period: ExportPeriod,
) -> Result<PreparedExportData, AppError> {
    let clean_folder_id = normalize_optional(folder_id);
    if let Some(fid) = clean_folder_id.as_deref() {
        ensure_folder_exists(conn, fid).await?;
    }

    let window = compute_period_window(period);
    let hierarchy_map = load_folder_hierarchy_map(conn).await?;
    let folder_count =
        count_scope_folders(conn, clean_folder_id.as_deref(), hierarchy_map.len()).await?;
    let records =
        fetch_records_for_export(conn, clean_folder_id.as_deref(), window.start_ms).await?;

    let scope_display = clean_folder_id
        .as_deref()
        .map(|fid| format_folder_path(&hierarchy_map, fid))
        .unwrap_or_else(|| "Todos los negocios".to_string());
    let scope_slug = clean_folder_id
        .as_deref()
        .and_then(|fid| hierarchy_map.get(fid).map(|(_, name)| sanitize_slug(name)))
        .unwrap_or_else(|| "todos".to_string());

    let mut numeric_sum = 0.0f64;
    let mut unit_totals: BTreeMap<String, (f64, u64)> = BTreeMap::new();
    for record in &records {
        if let Some(val) = record.numeric_value {
            numeric_sum += val;
            let unit_key = record
                .unit
                .clone()
                .unwrap_or_else(|| "(sin unidad)".to_string());
            let entry = unit_totals.entry(unit_key).or_insert((0.0, 0));
            entry.0 += val;
            entry.1 += 1;
        }
    }

    let custom_columns = collect_custom_field_columns(&records);

    Ok(PreparedExportData {
        window,
        hierarchy_map,
        folder_count,
        records,
        scope_display,
        scope_slug,
        numeric_sum,
        unit_totals,
        custom_columns,
    })
}

impl CoreEngine {
    /// Previsualiza qué incluirá el reporte Excel antes de escribir el `.xlsx`.
    pub async fn preview_excel_export(
        &self,
        folder_id: Option<String>,
        period: ExportPeriod,
    ) -> Result<ExportPreview, AppError> {
        let conn = self.connect().await?;
        let data = prepare_export_data(&conn, folder_id, period).await?;
        let columns = build_record_sheet_headers(&data.custom_columns);

        let sample_rows = data
            .records
            .iter()
            .take(5)
            .map(|record| {
                let extra_summary = if !record.custom_fields.is_empty() {
                    record
                        .custom_fields
                        .iter()
                        .take(3)
                        .map(|f| format!("{}: {}", f.name, f.value))
                        .collect::<Vec<_>>()
                        .join(" · ")
                } else {
                    record
                        .text_value
                        .as_deref()
                        .filter(|s| !s.is_empty())
                        .unwrap_or("—")
                        .to_string()
                };

                ExportPreviewRow {
                    date_label: format_epoch_short(record.created_at),
                    folder_path: format_folder_path(&data.hierarchy_map, &record.folder_id),
                    title: record.title.clone(),
                    value_display: format_value_display(
                        record.numeric_value,
                        record.unit.as_deref(),
                    ),
                    calculation_display: format_calculation_display(record),
                    extra_summary,
                }
            })
            .collect();

        Ok(ExportPreview {
            scope_label: data.scope_display,
            period_label: data.window.period_label.to_string(),
            record_count: data.records.len() as u64,
            folder_count: data.folder_count,
            numeric_sum: data.numeric_sum,
            columns,
            sample_rows,
        })
    }

    /// Exporta registros y subtotales de una carpeta (recursivamente) o de toda la app a `.xlsx`.
    pub async fn export_excel(
        &self,
        folder_id: Option<String>,
        period: ExportPeriod,
        output_dir: String,
    ) -> Result<ExportResult, AppError> {
        let clean_dir = normalize_required(&output_dir, "output_dir")?;
        let conn = self.connect().await?;
        let data = prepare_export_data(&conn, folder_id, period).await?;
        let PreparedExportData {
            window,
            hierarchy_map,
            folder_count,
            records,
            scope_display,
            scope_slug,
            numeric_sum,
            unit_totals,
            custom_columns,
        } = data;

        fs::create_dir_all(&clean_dir).map_err(|e| AppError::Internal {
            detail: format!("No se pudo preparar la carpeta de exportación '{clean_dir}': {e}"),
        })?;

        let timestamp_slug = Local::now().format("%Y%m%d_%H%M%S").to_string();
        let file_name = format!(
            "AppNotas_{scope_slug}_{}_{timestamp_slug}.xlsx",
            window.file_slug
        );
        let full_path = Path::new(&clean_dir).join(&file_name);

        let mut workbook = Workbook::new();

        let title_fmt = Format::new()
            .set_bold()
            .set_font_size(15)
            .set_font_color(Color::RGB(0x0F172A));
        let label_fmt = Format::new()
            .set_bold()
            .set_font_color(Color::RGB(0x334155))
            .set_background_color(Color::RGB(0xF1F5F9));
        let value_fmt = Format::new().set_font_color(Color::RGB(0x0F172A));
        let header_fmt = Format::new()
            .set_bold()
            .set_font_color(Color::White)
            .set_background_color(Color::RGB(0x0F766E))
            .set_align(FormatAlign::Left);
        let num_fmt = Format::new().set_num_format("#,##0.##");

        // Hoja 1: Resumen ejecutivo y subtotales por unidad
        {
            let summary = workbook.add_worksheet();
            summary.set_name("Resumen").map_err(|e| AppError::Internal {
                detail: format!("Error creando hoja Resumen: {e}"),
            })?;

            summary
                .write_string_with_format(0, 0, "AppNotas — Reporte de Datos", &title_fmt)
                .map_err(|e| AppError::Internal {
                    detail: format!("Error escribiendo encabezado Excel: {e}"),
                })?;

            let meta_rows = [
                ("Alcance", scope_display.as_str()),
                ("Período", window.period_label),
                (
                    "Generado el",
                    &Local::now().format("%Y-%m-%d %H:%M").to_string(),
                ),
            ];
            for (idx, (k, v)) in meta_rows.iter().enumerate() {
                let row_idx = (idx + 2) as u32;
                let _ = summary.write_string_with_format(row_idx, 0, *k, &label_fmt);
                let _ = summary.write_string_with_format(row_idx, 1, *v, &value_fmt);
            }

            let _ = summary.write_string_with_format(5, 0, "Total de registros", &label_fmt);
            let _ = summary.write_number(5, 1, records.len() as f64);

            let _ = summary.write_string_with_format(6, 0, "Carpetas en alcance", &label_fmt);
            let _ = summary.write_number(6, 1, folder_count as f64);

            let _ = summary.write_string_with_format(7, 0, "Suma numérica global", &label_fmt);
            let _ = summary.write_number_with_format(7, 1, numeric_sum, &num_fmt);

            let _ = summary.write_string_with_format(9, 0, "Unidad", &header_fmt);
            let _ = summary.write_string_with_format(9, 1, "Registros", &header_fmt);
            let _ = summary.write_string_with_format(9, 2, "Subtotal", &header_fmt);

            if unit_totals.is_empty() {
                let _ = summary.write_string(10, 0, "Sin valores numéricos en el período");
            } else {
                for (idx, (unit, (total, count))) in unit_totals.iter().enumerate() {
                    let row_idx = (10 + idx) as u32;
                    let _ = summary.write_string(row_idx, 0, unit);
                    let _ = summary.write_number(row_idx, 1, *count as f64);
                    let _ = summary.write_number_with_format(row_idx, 2, *total, &num_fmt);
                }
            }

            let _ = summary.set_column_width(0, 26);
            let _ = summary.set_column_width(1, 38);
            let _ = summary.set_column_width(2, 18);
        }

        // Hoja 2: Registros detallados + columnas dinámicas por campo adicional
        {
            let sheet = workbook.add_worksheet();
            sheet.set_name("Registros").map_err(|e| AppError::Internal {
                detail: format!("Error creando hoja Registros: {e}"),
            })?;

            let headers = build_record_sheet_headers(&custom_columns);

            let mut max_widths: Vec<usize> = headers
                .iter()
                .map(|h| h.chars().count().clamp(12, 42))
                .collect();

            for (col_idx, header) in headers.iter().enumerate() {
                let _ = sheet.write_string_with_format(0, col_idx as u16, header, &header_fmt);
            }
            let _ = sheet.set_freeze_panes(1, 0);

            for (row_offset, record) in records.iter().enumerate() {
                let row_idx = (row_offset + 1) as u32;
                let date_str = format_epoch_local(record.created_at);
                let folder_path = format_folder_path(&hierarchy_map, &record.folder_id);
                let calc_str = format_calculation_display(record).unwrap_or_default();
                let unit_str = record.unit.as_deref().unwrap_or("");
                let notes_str = record.text_value.as_deref().unwrap_or("");

                let _ = sheet.write_string(row_idx, 0, &date_str);
                let _ = sheet.write_string(row_idx, 1, &folder_path);
                let _ = sheet.write_string(row_idx, 2, &record.title);
                if let Some(num) = record.numeric_value {
                    let _ = sheet.write_number_with_format(row_idx, 3, num, &num_fmt);
                }
                let _ = sheet.write_string(row_idx, 4, &calc_str);
                let _ = sheet.write_string(row_idx, 5, unit_str);
                let _ = sheet.write_string(row_idx, 6, notes_str);

                max_widths[0] = max_widths[0].max(date_str.chars().count().min(22));
                max_widths[1] = max_widths[1].max(folder_path.chars().count().min(42));
                max_widths[2] = max_widths[2].max(record.title.chars().count().min(36));
                max_widths[4] = max_widths[4].max(calc_str.chars().count().min(24));
                max_widths[5] = max_widths[5].max(unit_str.chars().count().min(16));
                max_widths[6] = max_widths[6].max(notes_str.chars().count().min(42));

                for (field_offset, col_name) in custom_columns.iter().enumerate() {
                    let col_idx = 7 + field_offset;
                    if let Some(field) = record.custom_fields.iter().find(|f| &f.name == col_name) {
                        let _ = sheet.write_string(row_idx, col_idx as u16, &field.value);
                        max_widths[col_idx] =
                            max_widths[col_idx].max(field.value.chars().count().min(32));
                    }
                }
            }

            if !records.is_empty() && !headers.is_empty() {
                let _ = sheet.autofilter(0, 0, records.len() as u32, (headers.len() - 1) as u16);
            }

            for (col_idx, width) in max_widths.into_iter().enumerate() {
                let _ = sheet.set_column_width(col_idx as u16, (width + 3) as f64);
            }
        }

        workbook.save(&full_path).map_err(|e| AppError::Internal {
            detail: format!("No se pudo guardar el archivo Excel: {e}"),
        })?;

        Ok(ExportResult {
            file_path: full_path.to_string_lossy().into_owned(),
            file_name,
            record_count: records.len() as u64,
            folder_count,
            numeric_sum,
            period_label: window.period_label.to_string(),
        })
    }
}
