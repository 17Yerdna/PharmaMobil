package pe.edu.upeu.pharmamobil.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO que envía la aplicación para crear o actualizar un producto.
 */
@Serializable
data class ProductoRequestDto(
    val nombre: String,
    val precio: Double,
    val stock: Int,
    val estado: Boolean = true,
    val categoriaId: Long = 1L
)

/**
 * DTO que devuelve el servidor al consultar, crear o actualizar un producto.
 */
@Serializable
data class ProductoResponseDto(
    val id: Long,
    val nombre: String,
    val precio: Double,
    val stock: Int,
    val estado: Boolean = true,
    val categoriaId: Long? = null,
    val categoriaNombre: String? = null,
    val images: List<String> = emptyList()
)

/**
 * DTO que representa un error devuelto por el backend con código 4xx / 5xx.
 */
@Serializable
data class ErrorResponseDto(
    val timestamp: String? = null,
    val status: Int? = null,
    val error: String? = null,
    val message: String? = null,
    val validationErrors: Map<String, String> = emptyMap()
)

/**
 * DTO de compatibilidad para APIs alternativas (como Fake Store API).
 */
@Serializable
data class ProductoDto(
    val id: Int,
    val title: String,
    val price: Double,
    val description: String = "",
    val images: List<String> = emptyList(),
    @SerialName("category") val categoria: CategoriaDto? = null
)

@Serializable
data class CategoriaDto(val id: Int, val name: String)
