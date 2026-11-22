use std::sync::{Mutex, MutexGuard};

use appnotas_core::{
    create_folder, create_record, delete_folder, delete_record, get_sync_status, init_db,
    list_folder_contents, search_all, sync_now, update_folder, update_record, AppError, SyncMode,
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
        Some("$".to_string()),
        Some("Caja grande".to_string()),
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
        Some("$".to_string()),
        None,
    )
    .unwrap();
    create_record(
        gastos.id.clone(),
        "Tóner".to_string(),
        Some(650.0),
        Some("$".to_string()),
        None,
    )
    .unwrap();
    create_record(
        otro.id.clone(),
        "Café grano".to_string(),
        Some(200.0),
        Some("kg".to_string()),
        None,
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
        Some("Entregar antes de las 9am".to_string()),
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
        Some("kg".to_string()),
        None,
    )
    .unwrap();
    let r2 = create_record(
        folder.id.clone(),
        "Peras".to_string(),
        Some(7.5),
        Some("kg".to_string()),
        None,
    )
    .unwrap();
    let r3 = create_record(
        folder.id.clone(),
        "Caja diaria".to_string(),
        Some(1500.0),
        Some("$".to_string()),
        None,
    )
    .unwrap();
    let r4 = create_record(
        folder.id.clone(),
        "Ajuste merma".to_string(),
        Some(-2.0),
        Some("kg".to_string()),
        None,
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
        Some("kg".to_string()),
        Some("Lote B".to_string()),
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
        Some("$".to_string()),
        Some("Notas con código_especial".to_string()),
    )
    .unwrap();
    create_record(
        tienda.id.clone(),
        "Café americano".to_string(),
        Some(45.0),
        Some("$".to_string()),
        Some("Mostrador".to_string()),
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
        create_record(tienda.id.clone(), "Mal".to_string(), Some(f64::NAN), None, None),
        Err(AppError::Validation { .. })
    ));
    assert!(matches!(
        create_record(
            "id-inexistente".to_string(),
            "Test".to_string(),
            None,
            None,
            None
        ),
        Err(AppError::NotFound { .. })
    ));

    // Sync en modo local no falla
    let sync = sync_now().unwrap();
    assert_eq!(sync.mode, SyncMode::LocalOnly);
    assert!(!sync.synced);
    assert_eq!(get_sync_status().unwrap().mode, SyncMode::LocalOnly);
}
