package pe.edu.upeu.pharmamobil.presentation.sesion

/**
 * Estado reactivo del formulario de autenticación e inicio de sesión.
 */
data class SesionUiState(
    val usuario: String = "",
    val contrasenia: String = "",
    val contraseniaVisible: Boolean = false,
    val cargando: Boolean = false,
    val mensajeError: String? = null
)
