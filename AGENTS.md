# AGENTS.md — Guía para Agentes de IA y Desarrolladores

> **Estado actual (`v1.0.0`):** Aplicación Android **Local-First** compilando de extremo a extremo (`rust-core` + `cargo-ndk` + `uniffi` + `android-app` Material 3 Expressive).

---

## 1. Arquitectura y Regla de Oro

**AppAnotaciones** separa estrictamente el motor de datos/negocio en **Rust** y la interfaz reactiva en **Kotlin / Jetpack Compose**:

1. **`rust-core/` es la única fuente de verdad:**
   - Esquema SQLite/`libsql` (`folders` y `records`), migraciones idempotentes, validaciones de entrada, cálculo de sumas y subtotales por unidad (`UnitSubtotal`), construcción de *breadcrumbs* recursivos y búsqueda global con escape `LIKE`.
   - Concurrencia gestionada internamente con `tokio` (`OnceLock<Runtime>`) exponiendo funciones síncronas seguras mediante `#[uniffi::export]`, llamadas desde Kotlin en `Dispatchers.IO`.
2. **`android-app/` NO contiene lógica de negocio:**
   - Kotlin **no** debe parsear JSON, ejecutar SQL ni calcular sumas, conteos o subtotales.
   - `RustCoreRepository` invoca 1-a-1 las funciones generadas en `com.appnotas.core` y Compose únicamente renderiza structs planos e inmutables (`FolderViewData`, `SearchResults`, `SyncStatus`, etc.).

---

## 2. Protecciones que NO se rompen

- **Integridad referencial SQLite:** Cada conexión abierta en `CoreEngine::connect()` ejecuta `PRAGMA foreign_keys = ON;` antes de devolverse, garantizando que `ON DELETE CASCADE` funcione siempre al eliminar carpetas anidadas.
- **Modo Local-First sin credenciales:** Si `TURSO_DATABASE_URL` o `TURSO_AUTH_TOKEN` en `android-app/local.properties` están vacíos (o no hay red al iniciar una réplica), la app debe operar al 100% en modo local sin fallar ni bloquear al usuario.
- **No editar código autogenerado:** `android-app/app/src/main/java/com/appnotas/core/appnotas_core.kt` es generado por `uniffi-bindgen`. Cualquier cambio en la API FFI se hace en `rust-core/` y se regenera con `scripts/build-rust-android.ps1`.
- **Cero secretos en Git:** Nunca commitear `android-app/local.properties`, tokens de Turso, keystores (`*.keystore`, `*.jks`), binarios `.so` / `.apk`, bases de datos `*.db*` ni rutas absolutas del equipo local. Usa `android-app/local.properties.example` como plantilla.
- **Gestor de paquetes JS (si se usa tooling externo):** Utilizar `pnpm` / `pnpm dlx` (no `npm` ni `npx`).

---

## 3. Trampas Conocidas y Reglas Técnicas

1. **Colisión `message` en `uniffi::Error` (`rust-core/src/error.rs`):**
   - Nunca llames `message` a un campo dentro de variantes `#[derive(uniffi::Error)]`. UniFFI genera clases que heredan de `kotlin.Exception` y colisionan con `Throwable.message`. Usa `detail: String`.
2. **`cargo-ndk v4.1.2` y `uniffi-bindgen v0.32.2` (`scripts/build-rust-android.ps1`):**
   - En `cargo-ndk`, el nivel de API es `--platform 26` (o `-P 26`); `-p 26` se interpreta como `--package 26` y provoca panic.
   - Tanto `cargo-ndk` como `uniffi-bindgen` ejecutan `cargo metadata` en el directorio de trabajo actual: deben ejecutarse dentro de `rust-core/` (el script lo maneja con `Push-Location`). No pasar `--config rust-core/uniffi.toml` a `uniffi-bindgen 0.32.2`, ya que descubre automáticamente el `uniffi.toml` de cada crate.
3. **`strip` en `[profile.release]` (`rust-core/Cargo.toml`):**
   - Mantener `strip = "debuginfo"`. Usar `strip = true` o `"symbols"` elimina metadatos que `uniffi-bindgen` necesita leer del `.so`.
4. **Material 3 `1.4.0` (`compose-bom:2025.10.01`) y `compileSdk = 35`:**
   - En `material3:1.4.0`, `MaterialExpressiveTheme` y `MotionScheme` están marcados como `internal` en metadata Kotlin (y `material3:1.5.0` exige `minCompileSdk = 37` + `AGP 9.1.0`). Los resortes expresivos oficiales (`ExpressiveMotionTokens`) viven centralizados en `AppExpressiveMotion` dentro de `ui/motion/AppMotion.kt`.
   - `LocalIndication` debe importarse desde `androidx.compose.foundation.LocalIndication`.
5. **JNA + R8/ProGuard en Android (`android-app/app/build.gradle.kts`):**
   - Mantener `implementation("net.java.dev.jna:jna:5.17.0@aar")` (con `@aar` para incluir `libjnidispatch.so`) y conservar las reglas de `proguard-rules.pro` para `com.sun.jna.**` y `com.appnotas.core.**`.
6. **Concurrencia en tests de integración (`rust-core/tests/core_tests.rs`):**
   - El motor `ENGINE` en `lib.rs` es global (`OnceLock<RwLock<...>>`). Los tests que llamen a `init_db` deben adquirir `TEST_LOCK` para evitar condiciones de carrera al ejecutarse en paralelo.
7. **Estándares de Motion, Gestos y Accesibilidad:**
   - Respetar `rememberReducedMotion()` (`Settings.Global.ANIMATOR_DURATION_SCALE`).
   - Usar `EmilEaseOut` en entradas y *press feedback* (`pressableScale` sobre `graphicsLayer`), nunca `ease-in` ni `scale(0f)`.
   - Usar cifras tabulares (`tnum`) en contadores/totales y conservar el menú `MoreVert` accesible en cada fila además del gesto `SwipeToDismissBox` 1:1.

---

## 4. Mapa del Repositorio

```text
├── README.md                              # Presentación, descarga directa del APK y guía de build
├── AGENTS.md                              # Esta guía para agentes y colaboradores
├── assets/                                # Recursos gráficos SVG del README
├── rust-core/                             # Core en Rust (libSQL, Tokio, UniFFI, tests)
│   ├── Cargo.toml / uniffi.toml
│   ├── src/{lib.rs,db.rs,repository.rs,models.rs,error.rs}
│   └── tests/core_tests.rs
├── scripts/
│   └── build-rust-android.ps1             # Compila .so (arm64-v8a, x86_64) y bindings Kotlin
└── android-app/                           # Proyecto Android (Gradle 8.14.3, AGP 8.10.1, Kotlin 2.1.21)
    ├── gradle/libs.versions.toml
    ├── local.properties.example
    └── app/src/main/java/com/appnotas/
        ├── core/appnotas_core.kt          # Autogenerado por UniFFI (no editar a mano)
        └── app/                           # MainActivity, data/, ui/{motion,theme,components,screens}
```

---

## 5. Flujo de Validación Obligatorio tras Cambios

1. **Si modificaste `rust-core/`:**
   ```powershell
   cargo test --manifest-path rust-core/Cargo.toml
   cargo clippy --manifest-path rust-core/Cargo.toml --all-targets -- -D warnings
   powershell -ExecutionPolicy Bypass -File scripts/build-rust-android.ps1
   ```
2. **Si modificaste `android-app/` (o después de regenerar bindings/SOs):**
   ```powershell
   cd android-app
   .\gradlew.bat assembleDebug --console=plain
   ```
3. **Antes de publicar/commitear:** verificar `git status` y confirmar que ni `local.properties`, ni `jniLibs/*.so`, ni `*.apk` estén rastreados en el commit.
