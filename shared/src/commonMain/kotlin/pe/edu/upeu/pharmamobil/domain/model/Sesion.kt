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
    val permisos: Set<String> = emptySet(),
    val esOffline: Boolean = false
) {
    /**
     * Evalúa si la sesión activa cuenta con un permiso específico para autorización RBAC.
     * En modo offline de solo lectura, no se permite ninguna mutación privilegiada.
     */
    fun puede(permiso: String): Boolean = !esOffline && permiso in permisos
}
