use thiserror::Error;

/// Errores tipados del core expuestos de forma segura hacia Kotlin vía UniFFI.
/// Nota: los campos evitan llamarse `message` porque en Kotlin colisionan con `Throwable.message`.
#[derive(Debug, Error, uniffi::Error)]
pub enum AppError {
    #[error("La base de datos no ha sido inicializada. Llama a init_db primero.")]
    NotInitialized,

    #[error("Error de base de datos: {detail}")]
    Database { detail: String },

    #[error("Error de validación: {detail}")]
    Validation { detail: String },

    #[error("No se encontró {entity} con identificador '{id}'")]
    NotFound { entity: String, id: String },

    #[error("Error de sincronización: {detail}")]
    Sync { detail: String },

    #[error("Error interno: {detail}")]
    Internal { detail: String },
}

impl From<libsql::Error> for AppError {
    fn from(err: libsql::Error) -> Self {
        Self::Database {
            detail: err.to_string(),
        }
    }
}
