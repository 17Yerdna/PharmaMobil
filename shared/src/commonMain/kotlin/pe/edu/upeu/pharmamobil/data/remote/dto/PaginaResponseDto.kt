package pe.edu.upeu.pharmamobil.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * DTO genérico para respuestas paginadas del backend REST.
 */
@Serializable
data class PaginaResponseDto<T>(
    val contenido: List<T> = emptyList(),
    val pagina: Int = 0,
    val tamanio: Int = 20,
    val totalElementos: Long = 0,
    val totalPaginas: Int = 1,
    val ultima: Boolean = true
)
