package pe.edu.upeu.pharmamobil.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Payload de solicitud para inicio de sesión en POST /api/v1/auth/login.
 */
@Serializable
data class LoginRequestDto(
    val username: String,
    val password: String
)

/**
 * Respuesta del servidor tras autenticación exitosa.
 */
@Serializable
data class LoginResponseDto(
    val token: String,
    val tipo: String = "Bearer",
    val expiraEnSegundos: Long = 3600,
    val username: String = "",
    val nombreCompleto: String = "",
    val roles: List<String> = emptyList(),
    val permisos: List<String> = emptyList()
)

/**
 * Respuesta del endpoint GET /api/v1/auth/me con los datos del usuario actual.
 */
@Serializable
data class UsuarioResponseDto(
    val id: Long? = null,
    val username: String,
    val estado: Boolean = true,
    val empleadoId: Long? = null,
    val empleadoNombre: String? = null,
    val cargo: String? = null,
    val roles: List<String> = emptyList(),
    val permisos: List<String> = emptyList(),
    val fechaCreacion: String? = null
)

