use std::num::NonZeroU32;

use libsql::Connection;
use ring::pbkdf2;
use ring::rand::{SecureRandom, SystemRandom};

use crate::db::CoreEngine;
use crate::error::AppError;
use crate::models::{AppearanceSettings, LockMode, SecuritySettings};
use crate::repository::normalize_required;

const KEY_LOCK_MODE: &str = "sec.lock_mode";
const KEY_PASSWORD_SALT: &str = "sec.password_salt_hex";
const KEY_PASSWORD_HASH: &str = "sec.password_hash_hex";
const KEY_BIOMETRIC_ENABLED: &str = "sec.biometric_enabled";
const KEY_AUTO_LOCK_BG: &str = "sec.auto_lock_bg";

const KEY_PALETTE_ID: &str = "ui.palette_id";
const KEY_THEME_MODE: &str = "ui.theme_mode";
const KEY_PURE_BLACK: &str = "ui.pure_black";

const PBKDF2_ITERATIONS: u32 = 100_000;
const SALT_LEN: usize = 16;
const HASH_LEN: usize = 32;

fn encode_hex(bytes: &[u8]) -> String {
    let mut out = String::with_capacity(bytes.len() * 2);
    for byte in bytes {
        use std::fmt::Write;
        let _ = write!(&mut out, "{byte:02x}");
    }
    out
}

fn decode_hex(raw: &str) -> Result<Vec<u8>, AppError> {
    let clean = raw.trim();
    if !clean.len().is_multiple_of(2) {
        return Err(AppError::Internal {
            detail: "Formato hexadecimal inválido en configuración de seguridad".to_string(),
        });
    }
    let mut out = Vec::with_capacity(clean.len() / 2);
    for idx in (0..clean.len()).step_by(2) {
        let byte = u8::from_str_radix(&clean[idx..idx + 2], 16).map_err(|_| AppError::Internal {
            detail: "Byte hexadecimal inválido en configuración de seguridad".to_string(),
        })?;
        out.push(byte);
    }
    Ok(out)
}

async fn get_setting(conn: &Connection, key: &str) -> Result<Option<String>, AppError> {
    let mut rows = conn
        .query("SELECT value FROM app_settings WHERE key = ?1 LIMIT 1", [key])
        .await?;
    if let Some(row) = rows.next().await? {
        let value: String = row.get(0)?;
        return Ok(Some(value));
    }
    Ok(None)
}

async fn upsert_setting(conn: &Connection, key: &str, value: &str) -> Result<(), AppError> {
    conn.execute(
        "INSERT INTO app_settings (key, value) VALUES (?1, ?2)
         ON CONFLICT(key) DO UPDATE SET value = excluded.value",
        (key, value),
    )
    .await?;
    Ok(())
}

async fn delete_setting(conn: &Connection, key: &str) -> Result<(), AppError> {
    conn.execute("DELETE FROM app_settings WHERE key = ?1", [key])
        .await?;
    Ok(())
}

fn lock_mode_to_str(mode: LockMode) -> &'static str {
    match mode {
        LockMode::None => "none",
        LockMode::AppPassword => "app_password",
        LockMode::DeviceBiometric => "device_biometric",
    }
}

fn lock_mode_from_str(raw: Option<&str>) -> LockMode {
    match raw {
        Some("app_password") => LockMode::AppPassword,
        Some("device_biometric") => LockMode::DeviceBiometric,
        _ => LockMode::None,
    }
}

fn parse_bool(raw: Option<&str>, default: bool) -> bool {
    match raw {
        Some("true") => true,
        Some("false") => false,
        _ => default,
    }
}

async fn load_security_settings(conn: &Connection) -> Result<SecuritySettings, AppError> {
    let mode_raw = get_setting(conn, KEY_LOCK_MODE).await?;
    let salt_raw = get_setting(conn, KEY_PASSWORD_SALT).await?;
    let hash_raw = get_setting(conn, KEY_PASSWORD_HASH).await?;
    let biometric_raw = get_setting(conn, KEY_BIOMETRIC_ENABLED).await?;
    let auto_lock_raw = get_setting(conn, KEY_AUTO_LOCK_BG).await?;

    let has_password = salt_raw.is_some() && hash_raw.is_some();
    let mut lock_mode = lock_mode_from_str(mode_raw.as_deref());
    if lock_mode == LockMode::AppPassword && !has_password {
        lock_mode = LockMode::None;
    }

    Ok(SecuritySettings {
        lock_mode,
        has_password,
        biometric_enabled: parse_bool(biometric_raw.as_deref(), false),
        auto_lock_on_background: parse_bool(auto_lock_raw.as_deref(), true),
    })
}

fn validate_palette_id(raw: &str) -> Result<String, AppError> {
    let clean = normalize_required(raw, "palette_id")?.to_ascii_lowercase();
    match clean.as_str() {
        "aurora" | "emerald" | "indigo" | "coral" | "amber" | "dynamic" => Ok(clean),
        _ => Err(AppError::Validation {
            detail: format!("Paleta de colores no soportada: '{clean}'."),
        }),
    }
}

fn validate_theme_mode(raw: &str) -> Result<String, AppError> {
    let clean = normalize_required(raw, "theme_mode")?.to_ascii_lowercase();
    match clean.as_str() {
        "system" | "light" | "dark" => Ok(clean),
        _ => Err(AppError::Validation {
            detail: format!("Modo de tema no soportado: '{clean}'."),
        }),
    }
}

impl CoreEngine {
    pub async fn get_security_settings(&self) -> Result<SecuritySettings, AppError> {
        let conn = self.connect().await?;
        load_security_settings(&conn).await
    }

    /// Establece o reemplaza la contraseña dedicada de la aplicación usando PBKDF2-HMAC-SHA256.
    pub async fn set_app_password(
        &self,
        password: String,
        enable_biometric: bool,
    ) -> Result<SecuritySettings, AppError> {
        let trimmed = password.trim();
        if trimmed.chars().count() < 4 {
            return Err(AppError::Validation {
                detail: "La contraseña debe tener al menos 4 caracteres o dígitos.".to_string(),
            });
        }

        let rng = SystemRandom::new();
        let mut salt = [0u8; SALT_LEN];
        rng.fill(&mut salt).map_err(|_| AppError::Internal {
            detail: "No se pudo generar entropía criptográfica para el salt".to_string(),
        })?;

        let mut hash = [0u8; HASH_LEN];
        let iterations = NonZeroU32::new(PBKDF2_ITERATIONS).ok_or_else(|| AppError::Internal {
            detail: "Iteraciones PBKDF2 inválidas".to_string(),
        })?;
        pbkdf2::derive(
            pbkdf2::PBKDF2_HMAC_SHA256,
            iterations,
            &salt,
            trimmed.as_bytes(),
            &mut hash,
        );

        let conn = self.connect().await?;
        upsert_setting(&conn, KEY_PASSWORD_SALT, &encode_hex(&salt)).await?;
        upsert_setting(&conn, KEY_PASSWORD_HASH, &encode_hex(&hash)).await?;
        upsert_setting(&conn, KEY_LOCK_MODE, lock_mode_to_str(LockMode::AppPassword)).await?;
        upsert_setting(
            &conn,
            KEY_BIOMETRIC_ENABLED,
            if enable_biometric { "true" } else { "false" },
        )
        .await?;
        if get_setting(&conn, KEY_AUTO_LOCK_BG).await?.is_none() {
            upsert_setting(&conn, KEY_AUTO_LOCK_BG, "true").await?;
        }

        load_security_settings(&conn).await
    }

    /// Verifica la contraseña ingresada en tiempo constante contra el hash almacenado.
    pub async fn verify_app_password(&self, password: String) -> Result<bool, AppError> {
        let trimmed = password.trim();
        if trimmed.is_empty() {
            return Ok(false);
        }

        let conn = self.connect().await?;
        let Some(salt_hex) = get_setting(&conn, KEY_PASSWORD_SALT).await? else {
            return Ok(false);
        };
        let Some(hash_hex) = get_setting(&conn, KEY_PASSWORD_HASH).await? else {
            return Ok(false);
        };

        let salt = decode_hex(&salt_hex)?;
        let expected_hash = decode_hex(&hash_hex)?;
        let iterations = NonZeroU32::new(PBKDF2_ITERATIONS).ok_or_else(|| AppError::Internal {
            detail: "Iteraciones PBKDF2 inválidas".to_string(),
        })?;

        Ok(pbkdf2::verify(
            pbkdf2::PBKDF2_HMAC_SHA256,
            iterations,
            &salt,
            trimmed.as_bytes(),
            &expected_hash,
        )
        .is_ok())
    }

    pub async fn update_security_options(
        &self,
        lock_mode: LockMode,
        biometric_enabled: bool,
        auto_lock_on_background: bool,
    ) -> Result<SecuritySettings, AppError> {
        let conn = self.connect().await?;
        let current = load_security_settings(&conn).await?;

        if lock_mode == LockMode::AppPassword && !current.has_password {
            return Err(AppError::Validation {
                detail: "Configura primero una contraseña de la app antes de activar este modo."
                    .to_string(),
            });
        }

        let effective_biometric = match lock_mode {
            LockMode::DeviceBiometric => true,
            LockMode::None => false,
            LockMode::AppPassword => biometric_enabled,
        };

        upsert_setting(&conn, KEY_LOCK_MODE, lock_mode_to_str(lock_mode)).await?;
        upsert_setting(
            &conn,
            KEY_BIOMETRIC_ENABLED,
            if effective_biometric { "true" } else { "false" },
        )
        .await?;
        upsert_setting(
            &conn,
            KEY_AUTO_LOCK_BG,
            if auto_lock_on_background { "true" } else { "false" },
        )
        .await?;

        load_security_settings(&conn).await
    }

    pub async fn clear_app_password(&self) -> Result<SecuritySettings, AppError> {
        let conn = self.connect().await?;
        delete_setting(&conn, KEY_PASSWORD_SALT).await?;
        delete_setting(&conn, KEY_PASSWORD_HASH).await?;
        upsert_setting(&conn, KEY_LOCK_MODE, lock_mode_to_str(LockMode::None)).await?;
        upsert_setting(&conn, KEY_BIOMETRIC_ENABLED, "false").await?;
        load_security_settings(&conn).await
    }

    pub async fn get_appearance_settings(&self) -> Result<AppearanceSettings, AppError> {
        let conn = self.connect().await?;
        let palette_id = get_setting(&conn, KEY_PALETTE_ID)
            .await?
            .unwrap_or_else(|| "aurora".to_string());
        let theme_mode = get_setting(&conn, KEY_THEME_MODE)
            .await?
            .unwrap_or_else(|| "system".to_string());
        let pure_black = parse_bool(get_setting(&conn, KEY_PURE_BLACK).await?.as_deref(), false);

        Ok(AppearanceSettings {
            palette_id,
            theme_mode,
            pure_black,
        })
    }

    pub async fn save_appearance_settings(
        &self,
        settings: AppearanceSettings,
    ) -> Result<AppearanceSettings, AppError> {
        let palette_id = validate_palette_id(&settings.palette_id)?;
        let theme_mode = validate_theme_mode(&settings.theme_mode)?;

        let conn = self.connect().await?;
        upsert_setting(&conn, KEY_PALETTE_ID, &palette_id).await?;
        upsert_setting(&conn, KEY_THEME_MODE, &theme_mode).await?;
        upsert_setting(
            &conn,
            KEY_PURE_BLACK,
            if settings.pure_black { "true" } else { "false" },
        )
        .await?;

        Ok(AppearanceSettings {
            palette_id,
            theme_mode,
            pure_black: settings.pure_black,
        })
    }
}
