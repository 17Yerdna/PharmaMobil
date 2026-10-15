package pe.edu.upeu.pharmamobil.platform

/**
 * Configuración de entorno y red provista por cada plataforma nativa.
 * Permite desacoplar la URL base del backend y el nivel de logging según el ambiente.
 */
data class PlatformConfig(
    val urlBase: String,
    val esDepuracion: Boolean
)
