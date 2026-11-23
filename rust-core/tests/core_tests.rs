use std::fs::File;
use std::io::Read;
use std::path::Path;
use std::sync::{Mutex, MutexGuard};

use appnotas_core::{
    clear_app_password, create_folder, create_record, delete_folder, delete_record, export_excel,
    get_appearance_settings, get_security_settings, get_sync_status, init_db, list_folder_contents,
    preview_excel_export, save_appearance_settings, search_all, set_app_password,
    suggest_folder_fields, sync_now, update_folder, update_record, update_security_options,
    verify_app_password, AppError, AppearanceSettings, CustomField, ExportPeriod, LockMode,
    SyncMode,
};
use tempfile::TempDir;

static TEST_LOCK: Mutex<()> = Mutex::new(());

fn setup_temp_db() -> (MutexGuard<'static, ()>, TempDir) {
    let guard = TEST_LOCK.lock().unwrap_or_else(|e| e.into_inner());
    let dir = TempDir::new().expect("crear directorio temporal");
    let db_path = dir.path().join("appnotas_test.db");
    let status = init_db(db_path.to_string_lossy().into_owned(), None, None)
        .expect("init_db en modo local debe funcionar");
    assert_eq!(status.mode, SyncMode::LocalOnly);
    assert!(!status.synced);
    (guard, dir)
}

#[test]
fn test_nested_folders_breadcrumbs_and_counts() {
    let _dir = setup_temp_db();

    let fruteria = create_folder(
        None,
        "Frutería".to_string(),
        Some("#2E7D32".to_string()),
        Some("store".to_string()),
    )
    .expect("crear carpeta raíz");
    assert_eq!(fruteria.parent_id, None);

    let ventas = create_folder(Some(fruteria.id.clone()), "Ventas".to_string(), None, None)
        .expect("crear subcarpeta nivel 2");
    let noviembre = create_folder(
        Some(ventas.id.clone()),
        "Noviembre".to_string(),
        None,
        None,
    )
    .expect("crear subcarpeta nivel 3");

    create_record(
        noviembre.id.clone(),
        "Mangos".to_string(),
        Some(450.5),
        None,
        None,
        None,
        Some("$".to_string()),
        Some("Caja grande".to_string()),
        vec![],
    )
    .expect("crear registro en nivel 3");

    let root_view = list_folder_contents(None).expect("listar raíz");
    assert_eq!(root_view.breadcrumbs.len(), 1);
    assert_eq!(root_view.breadcrumbs[0].id, None);
    assert_eq!(root_view.breadcrumbs[0].name, "Inicio");
    assert_eq!(root_view.subfolders.len(), 1);
    assert_eq!(root_view.subfolders[0].subfolder_count, 1);
    assert_eq!(root_view.subfolders[0].record_count, 0);
    assert!(!root_view.has_numeric_summary);

    let level3_view =
        list_folder_contents(Some(noviembre.id.clone())).expect("listar nivel 3");
    assert_eq!(level3_view.breadcrumbs.len(), 4);
    let breadcrumb_names: Vec<&str> = level3_view
        .breadcrumbs
        .iter()
        .map(|b| b.name.as_str())
        .collect();
    assert_eq!(
        breadcrumb_names,
        vec!["Inicio", "Frutería", "Ventas", "Noviembre"]
    );
    assert_eq!(level3_view.record_count, 1);
    assert_eq!(level3_view.total_count, 1);
    assert!(level3_view.has_numeric_summary);
    assert!((level3_view.numeric_sum - 450.5).abs() < 1e-6);
}

#[test]
fn test_delete_folder_cascade_multilevel() {
    let _dir = setup_temp_db();

    let negocio = create_folder(None, "Papelería".to_string(), None, None).unwrap();
    let caja = create_folder(Some(negocio.id.clone()), "Caja".to_string(), None, None).unwrap();
    let gastos = create_folder(Some(caja.id.clone()), "Gastos".to_string(), None, None).unwrap();
    let otro = create_folder(None, "Cafetería".to_string(), None, None).unwrap();

    create_record(
        caja.id.clone(),
        "Venta hojas".to_string(),
        Some(120.0),
        None,
        None,
        None,
        Some("$".to_string()),
        None,
        vec![],
    )
    .unwrap();
    create_record(
        gastos.id.clone(),
        "Tóner".to_string(),
        Some(650.0),
        None,
        None,
        None,
        Some("$".to_string()),
        None,
        vec![],
    )
    .unwrap();
    create_record(
        otro.id.clone(),
        "Café grano".to_string(),
        Some(200.0),
        None,
        None,
        None,
        Some("kg".to_string()),
        None,
        vec![],
    )
    .unwrap();

    delete_folder(negocio.id.clone()).expect("eliminar carpeta padre en cascada");

    // Todas las subcarpetas descendientes deben haber sido eliminadas por ON DELETE CASCADE.
    assert!(matches!(
        list_folder_contents(Some(caja.id)),
        Err(AppError::NotFound { .. })
    ));
    assert!(matches!(
        list_folder_contents(Some(gastos.id)),
        Err(AppError::NotFound { .. })
    ));

    // Los registros descendientes no deben aparecer en búsqueda global.
    let search_deleted = search_all("Tóner".to_string()).unwrap();
    assert_eq!(search_deleted.total_matches, 0);

    // La carpeta hermana y su registro deben permanecer intactos.
    let root_view = list_folder_contents(None).unwrap();
    assert_eq!(root_view.subfolders.len(), 1);
    assert_eq!(root_view.subfolders[0].id, otro.id);
    let otro_view = list_folder_contents(Some(otro.id)).unwrap();
    assert_eq!(otro_view.record_count, 1);
}

#[test]
fn test_numeric_summaries_mixed_records_and_updates() {
    let _dir = setup_temp_db();

    let folder = create_folder(None, "Inventario".to_string(), None, None).unwrap();

    // Carpeta solo textual al principio: no debe mostrar resumen numérico
    let nota = create_record(
        folder.id.clone(),
        "Proveedor lunes".to_string(),
        None,
        None,
        None,
        None,
        None,
        Some("Entregar antes de las 9am".to_string()),
        vec![],
    )
    .unwrap();
    let view_text_only = list_folder_contents(Some(folder.id.clone())).unwrap();
    assert!(!view_text_only.has_numeric_summary);
    assert_eq!(view_text_only.numeric_record_count, 0);
    assert_eq!(view_text_only.record_count, 1);

    // Agregar registros numéricos mixtos y unidades
    let r1 = create_record(
        folder.id.clone(),
        "Manzanas".to_string(),
        Some(12.5),
        None,
        None,
        None,
        Some("kg".to_string()),
        None,
        vec![],
    )
    .unwrap();
    let r2 = create_record(
        folder.id.clone(),
        "Peras".to_string(),
        Some(7.5),
        None,
        None,
        None,
        Some("kg".to_string()),
        None,
        vec![],
    )
    .unwrap();
    let r3 = create_record(
        folder.id.clone(),
        "Caja diaria".to_string(),
        Some(1500.0),
        None,
        None,
        None,
        Some("$".to_string()),
        None,
        vec![],
    )
    .unwrap();
    let r4 = create_record(
        folder.id.clone(),
        "Ajuste merma".to_string(),
        Some(-2.0),
        None,
        None,
        None,
        Some("kg".to_string()),
        None,
        vec![],
    )
    .unwrap();

    let view = list_folder_contents(Some(folder.id.clone())).unwrap();
    assert!(view.has_numeric_summary);
    assert_eq!(view.record_count, 5);
    assert_eq!(view.numeric_record_count, 4);
    assert!((view.numeric_sum - 1518.0).abs() < 1e-6);
    assert_eq!(view.unit_subtotals.len(), 2);

    let kg_sub = view
        .unit_subtotals
        .iter()
        .find(|u| u.unit.as_deref() == Some("kg"))
        .unwrap();
    assert!((kg_sub.total - 18.0).abs() < 1e-6);
    assert_eq!(kg_sub.count, 3);

    let money_sub = view
        .unit_subtotals
        .iter()
        .find(|u| u.unit.as_deref() == Some("$"))
        .unwrap();
    assert!((money_sub.total - 1500.0).abs() < 1e-6);
    assert_eq!(money_sub.count, 1);

    // Actualizar registro y carpeta
    let updated_r1 = update_record(
        r1.id.clone(),
        "Manzanas Rojas".to_string(),
        Some(15.0),
        None,
        None,
        None,
        Some("kg".to_string()),
        Some("Lote B".to_string()),
        vec![],
    )
    .unwrap();
    assert_eq!(updated_r1.title, "Manzanas Rojas");
    assert_eq!(updated_r1.numeric_value, Some(15.0));

    let updated_folder = update_folder(
        folder.id.clone(),
        "Inventario General".to_string(),
        Some("#1565C0".to_string()),
        Some("inventory".to_string()),
    )
    .unwrap();
    assert_eq!(updated_folder.name, "Inventario General");
    assert_eq!(updated_folder.record_count, 5);

    // Eliminar todos los registros numéricos y comprobar que has_numeric_summary vuelve a false
    delete_record(r1.id).unwrap();
    delete_record(r2.id).unwrap();
    delete_record(r3.id).unwrap();
    delete_record(r4.id).unwrap();

    let view_after_delete = list_folder_contents(Some(folder.id)).unwrap();
    assert!(!view_after_delete.has_numeric_summary);
    assert_eq!(view_after_delete.record_count, 1);
    assert_eq!(view_after_delete.records[0].id, nota.id);
}

#[test]
fn test_search_all_escape_paths_and_validation() {
    let _dir = setup_temp_db();

    let tienda = create_folder(None, "Tienda Centro".to_string(), None, None).unwrap();
    let promociones = create_folder(
        Some(tienda.id.clone()),
        "Descuentos 50%_VIP".to_string(),
        None,
        None,
    )
    .unwrap();

    create_record(
        promociones.id.clone(),
        "Promo 50% café".to_string(),
        Some(99.0),
        None,
        None,
        None,
        Some("$".to_string()),
        Some("Notas con código_especial".to_string()),
        vec![],
    )
    .unwrap();
    create_record(
        tienda.id.clone(),
        "Café americano".to_string(),
        Some(45.0),
        None,
        None,
        None,
        Some("$".to_string()),
        Some("Mostrador".to_string()),
        vec![],
    )
    .unwrap();

    // Búsqueda vacía
    let empty = search_all("   ".to_string()).unwrap();
    assert_eq!(empty.total_matches, 0);
    assert!(!empty.has_numeric_summary);

    // Escape de caracteres LIKE (% y _)
    let percent_match = search_all("50%".to_string()).unwrap();
    assert_eq!(percent_match.folders.len(), 1);
    assert_eq!(percent_match.records.len(), 1);
    assert_eq!(percent_match.total_matches, 2);
    assert_eq!(percent_match.folders[0].path_display, "Inicio > Tienda Centro");
    assert_eq!(
        percent_match.records[0].path_display,
        "Inicio > Tienda Centro > Descuentos 50%_VIP"
    );
    assert!((percent_match.numeric_sum - 99.0).abs() < 1e-6);

    let underscore_match = search_all("código_especial".to_string()).unwrap();
    assert_eq!(underscore_match.records.len(), 1);

    // Búsqueda case-insensitive
    let ci_match = search_all("café".to_string()).unwrap();
    assert_eq!(ci_match.records.len(), 2);
    assert!((ci_match.numeric_sum - 144.0).abs() < 1e-6);

    // Validaciones de entrada
    assert!(matches!(
        create_folder(None, "   ".to_string(), None, None),
        Err(AppError::Validation { .. })
    ));
    assert!(matches!(
        create_record(
            tienda.id.clone(),
            "Mal".to_string(),
            Some(f64::NAN),
            None,
            None,
            None,
            None,
            None,
            vec![]
        ),
        Err(AppError::Validation { .. })
    ));
    assert!(matches!(
        create_record(
            "id-inexistente".to_string(),
            "Test".to_string(),
            None,
            None,
            None,
            None,
            None,
            None,
            vec![]
        ),
        Err(AppError::NotFound { .. })
    ));

    // Sync en modo local no falla
    let sync = sync_now().unwrap();
    assert_eq!(sync.mode, SyncMode::LocalOnly);
    assert!(!sync.synced);
    assert_eq!(get_sync_status().unwrap().mode, SyncMode::LocalOnly);
}

#[test]
fn test_custom_fields_crud_search_and_cascade() {
    let _dir = setup_temp_db();

    let fruteria = create_folder(
        None,
        "Frutería El Sol".to_string(),
        Some("#10B981".to_string()),
        Some("emoji:🍎".to_string()),
    )
    .unwrap();
    assert_eq!(fruteria.icon.as_deref(), Some("emoji:🍎"));

    let created = create_record(
        fruteria.id.clone(),
        "Compra matutina".to_string(),
        Some(840.0),
        None,
        None,
        None,
        Some("$".to_string()),
        Some("Entrega puntual".to_string()),
        vec![
            CustomField {
                name: "Fruta".to_string(),
                value: "Mango Ataulfo".to_string(),
            },
            CustomField {
                name: "Cantidad".to_string(),
                value: "24 kg".to_string(),
            },
            CustomField {
                name: "   ".to_string(),
                value: "   ".to_string(),
            },
        ],
    )
    .unwrap();

    assert_eq!(created.custom_fields.len(), 2);
    assert_eq!(created.custom_fields[0].name, "Fruta");
    assert_eq!(created.custom_fields[0].value, "Mango Ataulfo");

    // Búsqueda global debe encontrar registros también por valores de custom_fields
    let search_hit = search_all("Ataulfo".to_string()).unwrap();
    assert_eq!(search_hit.records.len(), 1);
    assert_eq!(search_hit.records[0].record.custom_fields.len(), 2);

    // Edición reemplaza ordenadamente los campos adicionales
    let updated = update_record(
        created.id.clone(),
        "Compra matutina".to_string(),
        Some(900.0),
        None,
        None,
        None,
        Some("$".to_string()),
        None,
        vec![CustomField {
            name: "Proveedor".to_string(),
            value: "Central Abastos".to_string(),
        }],
    )
    .unwrap();
    assert_eq!(updated.custom_fields.len(), 1);
    assert_eq!(updated.custom_fields[0].name, "Proveedor");

    // Validación: campo con nombre pero sin valor debe fallar
    assert!(matches!(
        create_record(
            fruteria.id.clone(),
            "Inválido".to_string(),
            None,
            None,
            None,
            None,
            None,
            None,
            vec![CustomField {
                name: "Fruta".to_string(),
                value: "   ".to_string(),
            }],
        ),
        Err(AppError::Validation { .. })
    ));

    // Cascade al eliminar la carpeta
    delete_folder(fruteria.id).unwrap();
    assert_eq!(search_all("Central Abastos".to_string()).unwrap().records.len(), 0);
}

#[test]
fn test_export_excel_week_month_and_subtree() {
    let (_guard, dir) = setup_temp_db();

    let fruteria = create_folder(
        None,
        "Frutería Central".to_string(),
        None,
        Some("emoji:🥭".to_string()),
    )
    .unwrap();
    let ventas = create_folder(
        Some(fruteria.id.clone()),
        "Ventas".to_string(),
        None,
        None,
    )
    .unwrap();
    let otro = create_folder(None, "Cafetería".to_string(), None, None).unwrap();

    create_record(
        ventas.id.clone(),
        "Venta Mangos".to_string(),
        Some(350.0),
        None,
        None,
        None,
        Some("$".to_string()),
        Some("Caja A".to_string()),
        vec![
            CustomField {
                name: "Fruta".to_string(),
                value: "Mango".to_string(),
            },
            CustomField {
                name: "Calidad".to_string(),
                value: "Primera".to_string(),
            },
        ],
    )
    .unwrap();
    create_record(
        otro.id.clone(),
        "Espresso".to_string(),
        Some(80.0),
        None,
        None,
        None,
        Some("$".to_string()),
        None,
        vec![],
    )
    .unwrap();

    let export_dir = dir.path().join("exports");

    // Exportar solo el subárbol de Frutería (Semana actual)
    let week_res = export_excel(
        Some(fruteria.id.clone()),
        ExportPeriod::Week,
        export_dir.to_string_lossy().into_owned(),
    )
    .unwrap();
    assert_eq!(week_res.record_count, 1);
    assert_eq!(week_res.folder_count, 2);
    assert!((week_res.numeric_sum - 350.0).abs() < 1e-6);
    assert_eq!(week_res.period_label, "Semana actual");
    assert!(week_res.file_name.ends_with(".xlsx"));

    let exported_path = Path::new(&week_res.file_path);
    assert!(exported_path.exists());
    let mut magic = [0u8; 2];
    File::open(exported_path).unwrap().read_exact(&mut magic).unwrap();
    assert_eq!(&magic, b"PK", "Un archivo .xlsx válido debe iniciar con firma ZIP PK");

    // Exportar todos los negocios (Mes y Todo el historial)
    let all_res = export_excel(
        None,
        ExportPeriod::All,
        export_dir.to_string_lossy().into_owned(),
    )
    .unwrap();
    assert_eq!(all_res.record_count, 2);
    assert_eq!(all_res.folder_count, 3);
    assert!((all_res.numeric_sum - 430.0).abs() < 1e-6);

    let month_res = export_excel(
        None,
        ExportPeriod::Month,
        export_dir.to_string_lossy().into_owned(),
    )
    .unwrap();
    assert_eq!(month_res.record_count, 2);
    assert_eq!(month_res.period_label, "Mes actual");
}

#[test]
fn test_security_password_hashing_and_appearance_settings() {
    let _dir = setup_temp_db();

    let initial = get_security_settings().unwrap();
    assert_eq!(initial.lock_mode, LockMode::None);
    assert!(!initial.has_password);
    assert!(!initial.biometric_enabled);
    assert!(initial.auto_lock_on_background);

    // No se puede activar AppPassword sin contraseña configurada
    assert!(matches!(
        update_security_options(LockMode::AppPassword, false, true),
        Err(AppError::Validation { .. })
    ));

    // Contraseña demasiado corta
    assert!(matches!(
        set_app_password("123".to_string(), false),
        Err(AppError::Validation { .. })
    ));

    // Establecer contraseña válida + biometría opcional
    let saved = set_app_password("2580".to_string(), true).unwrap();
    assert_eq!(saved.lock_mode, LockMode::AppPassword);
    assert!(saved.has_password);
    assert!(saved.biometric_enabled);

    assert!(verify_app_password("2580".to_string()).unwrap());
    assert!(!verify_app_password("0000".to_string()).unwrap());
    assert!(!verify_app_password("".to_string()).unwrap());

    let updated = update_security_options(LockMode::DeviceBiometric, true, false).unwrap();
    assert_eq!(updated.lock_mode, LockMode::DeviceBiometric);
    assert!(!updated.auto_lock_on_background);

    let cleared = clear_app_password().unwrap();
    assert_eq!(cleared.lock_mode, LockMode::None);
    assert!(!cleared.has_password);
    assert!(!cleared.biometric_enabled);
    assert!(!verify_app_password("2580".to_string()).unwrap());

    // Apariencia por defecto y guardado
    let default_ui = get_appearance_settings().unwrap();
    assert_eq!(default_ui.palette_id, "aurora");
    assert_eq!(default_ui.theme_mode, "system");
    assert!(!default_ui.pure_black);

    let custom_ui = save_appearance_settings(AppearanceSettings {
        palette_id: "Emerald".to_string(),
        theme_mode: "DARK".to_string(),
        pure_black: true,
    })
    .unwrap();
    assert_eq!(custom_ui.palette_id, "emerald");
    assert_eq!(custom_ui.theme_mode, "dark");
    assert!(custom_ui.pure_black);

    let reloaded_ui = get_appearance_settings().unwrap();
    assert_eq!(reloaded_ui, custom_ui);
}

#[test]
fn test_record_calculation_multiplier_and_updates() {
    let _dir = setup_temp_db();

    let pizzeria = create_folder(
        None,
        "Pizzería".to_string(),
        None,
        Some("emoji:🍕".to_string()),
    )
    .unwrap();

    // 12 docenas × 12 unidades = 144 sodas
    let sodas = create_record(
        pizzeria.id.clone(),
        "Soda Coca Cola".to_string(),
        None,
        Some(12.0),
        Some(12.0),
        Some("doc".to_string()),
        Some("u".to_string()),
        Some("Refresco en lata".to_string()),
        vec![],
    )
    .unwrap();
    assert!((sodas.numeric_value.unwrap() - 144.0).abs() < 1e-6);
    assert_eq!(sodas.quantity, Some(12.0));
    assert_eq!(sodas.unit_multiplier, Some(12.0));
    assert_eq!(sodas.multiplier_label.as_deref(), Some("doc"));

    // Validación: cantidad sin multiplicador debe fallar
    assert!(matches!(
        create_record(
            pizzeria.id.clone(),
            "Incompleto".to_string(),
            None,
            Some(5.0),
            None,
            None,
            Some("u".to_string()),
            None,
            vec![],
        ),
        Err(AppError::Validation { .. })
    ));

    let view = list_folder_contents(Some(pizzeria.id.clone())).unwrap();
    assert!((view.numeric_sum - 144.0).abs() < 1e-6);

    // Edición a modo directo limpia los multiplicadores
    let edited = update_record(
        sodas.id.clone(),
        "Soda Coca Cola".to_string(),
        Some(96.0),
        None,
        None,
        None,
        Some("u".to_string()),
        None,
        vec![],
    )
    .unwrap();
    assert!((edited.numeric_value.unwrap() - 96.0).abs() < 1e-6);
    assert_eq!(edited.quantity, None);
    assert_eq!(edited.unit_multiplier, None);
    assert_eq!(edited.multiplier_label, None);
}

#[test]
fn test_suggest_folder_fields_scoped_by_business() {
    let _dir = setup_temp_db();

    let pizzeria = create_folder(None, "Pizzería".to_string(), None, None).unwrap();
    let bebidas = create_folder(Some(pizzeria.id.clone()), "Bebidas".to_string(), None, None).unwrap();
    let farmacia = create_folder(None, "Farmacia".to_string(), None, None).unwrap();

    create_record(
        pizzeria.id.clone(),
        "Masa madre".to_string(),
        Some(20.0),
        None,
        None,
        None,
        Some("kg".to_string()),
        None,
        vec![
            CustomField {
                name: "Masa".to_string(),
                value: "Napolitana".to_string(),
            },
            CustomField {
                name: "Tamaño".to_string(),
                value: "Familiar".to_string(),
            },
        ],
    )
    .unwrap();

    create_record(
        farmacia.id.clone(),
        "Aspirina".to_string(),
        Some(10.0),
        None,
        None,
        None,
        Some("$".to_string()),
        None,
        vec![
            CustomField {
                name: "Dosis".to_string(),
                value: "500 mg".to_string(),
            },
            CustomField {
                name: "Caducidad".to_string(),
                value: "2027-08".to_string(),
            },
        ],
    )
    .unwrap();

    // Una subcarpeta de Pizzería hereda las sugerencias aprendidas del negocio Pizzería
    let pizza_suggestions = suggest_folder_fields(Some(bebidas.id)).unwrap();
    assert!(pizza_suggestions.contains(&"Masa".to_string()));
    assert!(pizza_suggestions.contains(&"Tamaño".to_string()));
    assert!(!pizza_suggestions.contains(&"Dosis".to_string()));

    let pharmacy_suggestions = suggest_folder_fields(Some(farmacia.id)).unwrap();
    assert!(pharmacy_suggestions.contains(&"Dosis".to_string()));
    assert!(!pharmacy_suggestions.contains(&"Masa".to_string()));
}

#[test]
fn test_preview_excel_export_matches_scope_and_sample_rows() {
    let _dir = setup_temp_db();

    let agro = create_folder(None, "Agricultura".to_string(), None, None).unwrap();
    let farmacia = create_folder(None, "Farmacia".to_string(), None, None).unwrap();

    create_record(
        agro.id.clone(),
        "Tierra preparada".to_string(),
        None,
        Some(12.0),
        Some(50.0),
        Some("lb/costal".to_string()),
        Some("lb".to_string()),
        None,
        vec![CustomField {
            name: "Proveedor".to_string(),
            value: "Vivero Sur".to_string(),
        }],
    )
    .unwrap();

    create_record(
        farmacia.id.clone(),
        "Agua oxigenada".to_string(),
        Some(600.0),
        None,
        None,
        None,
        Some("mg".to_string()),
        None,
        vec![],
    )
    .unwrap();

    let agro_preview = preview_excel_export(Some(agro.id.clone()), ExportPeriod::Week).unwrap();
    assert_eq!(agro_preview.scope_label, "Inicio > Agricultura");
    assert_eq!(agro_preview.period_label, "Semana actual");
    assert_eq!(agro_preview.record_count, 1);
    assert!((agro_preview.numeric_sum - 600.0).abs() < 1e-6);
    assert!(agro_preview.columns.contains(&"Cálculo".to_string()));
    assert!(agro_preview.columns.contains(&"Proveedor".to_string()));
    assert_eq!(agro_preview.sample_rows.len(), 1);
    assert_eq!(agro_preview.sample_rows[0].value_display, "600 lb");
    assert_eq!(
        agro_preview.sample_rows[0].calculation_display.as_deref(),
        Some("12 × 50 lb/costal")
    );
    assert_eq!(
        agro_preview.sample_rows[0].extra_summary,
        "Proveedor: Vivero Sur"
    );

    let global_preview = preview_excel_export(None, ExportPeriod::All).unwrap();
    assert_eq!(global_preview.scope_label, "Todos los negocios");
    assert_eq!(global_preview.record_count, 2);
    assert_eq!(global_preview.sample_rows.len(), 2);
}

#[test]
fn test_notebook_quantity_price_per_unit_and_total_modes() {
    let _dir = setup_temp_db();

    let tienda = create_folder(None, "Tienda".to_string(), None, None).unwrap();

    // 2 huevos × $3 c/u = $6
    let por_unidad = create_record(
        tienda.id.clone(),
        "Huevos (cada uno)".to_string(),
        None,
        Some(2.0),
        Some(3.0),
        Some("u".to_string()),
        Some("$".to_string()),
        None,
        vec![],
    )
    .unwrap();
    assert!((por_unidad.numeric_value.unwrap() - 6.0).abs() < 1e-6);
    assert_eq!(por_unidad.quantity, Some(2.0));
    assert_eq!(por_unidad.unit_multiplier, Some(3.0));
    assert_eq!(por_unidad.multiplier_label.as_deref(), Some("u"));
    assert_eq!(por_unidad.unit.as_deref(), Some("$"));

    // 2 huevos por $3 en total -> guarda $3 y conserva cantidad 2 u
    let por_total = create_record(
        tienda.id.clone(),
        "Huevos (en total)".to_string(),
        Some(3.0),
        Some(2.0),
        None,
        Some("u".to_string()),
        Some("$".to_string()),
        None,
        vec![],
    )
    .unwrap();
    assert!((por_total.numeric_value.unwrap() - 3.0).abs() < 1e-6);
    assert_eq!(por_total.quantity, Some(2.0));
    assert_eq!(por_total.unit_multiplier, None);
    assert_eq!(por_total.multiplier_label.as_deref(), Some("u"));
    assert_eq!(por_total.unit.as_deref(), Some("$"));

    let view = list_folder_contents(Some(tienda.id.clone())).unwrap();
    assert!((view.numeric_sum - 9.0).abs() < 1e-6);

    let preview = preview_excel_export(Some(tienda.id), ExportPeriod::Week).unwrap();
    assert_eq!(preview.record_count, 2);
    let calcs: Vec<&str> = preview
        .sample_rows
        .iter()
        .filter_map(|r| r.calculation_display.as_deref())
        .collect();
    assert!(calcs.contains(&"2 u × $3 c/u"));
    assert!(calcs.contains(&"por 2 u"));
}
