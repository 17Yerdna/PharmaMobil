package pe.edu.upeu.pharmamobil.domain.model

/**
 * Credenciales de usuario para la autenticación en PharmaSoft.
 */
data class Credenciales(
    val usuario: String,
    val clave: String
)

/**
 * Representa la sesión del usuario autenticado en la capa de dominio.
 */
data class Sesion(
    val token: String,
    val usuario: String,
    val nombreCompleto: String,
    val permisos: Set<String> = emptySet()
) {
    /**
     * Evalúa si la sesión activa cuenta con un permiso específico para autorización RBAC.
     */
    fun puede(permiso: String): Boolean = permiso in permisos
}
