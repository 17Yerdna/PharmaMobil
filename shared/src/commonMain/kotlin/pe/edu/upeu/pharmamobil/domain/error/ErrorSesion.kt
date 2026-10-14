package pe.edu.upeu.pharmamobil.domain.error

/**
 * Jerarquía tipificada de errores específicos de sesión y credenciales.
 */
sealed interface ErrorSesion {
    data object CredencialesInvalidas : ErrorSesion
    data object SesionExpirada : ErrorSesion
    data object SinPermiso : ErrorSesion
    data object SinConexion : ErrorSesion
    data class ErrorDesconocido(val mensaje: String) : ErrorSesion
}

/**
 * Excepción de transporte desacoplada para propagar errores tipados de sesión.
 */
class ErrorSesionException(val error: ErrorSesion) : Exception(error.toString())
