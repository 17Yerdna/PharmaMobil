package pe.edu.upeu.pharmamobil.domain.error

/**
 * Modela los posibles errores devueltos por el servidor o fallos de red,
 * desacoplando la capa de presentación de las excepciones de Ktor.
 */
sealed interface ErrorApi {
    data class Validacion(val porCampo: Map<String, String>) : ErrorApi
    data object NoEncontrado : ErrorApi
    data class Conflicto(val mensaje: String) : ErrorApi
    data object Servidor : ErrorApi
    data object SinConexion : ErrorApi
    data object TiempoAgotado : ErrorApi
    data object NoAutorizado : ErrorApi
    data object SinPermiso : ErrorApi
}

/**
 * Excepción de dominio que encapsula un [ErrorApi].
 */
class ErrorApiException(val error: ErrorApi) : Exception()
