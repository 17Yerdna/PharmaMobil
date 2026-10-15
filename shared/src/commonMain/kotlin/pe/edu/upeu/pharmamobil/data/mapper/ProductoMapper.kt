package pe.edu.upeu.pharmamobil.data.mapper

import pe.edu.upeu.pharmamobil.data.remote.dto.ProductoDto
import pe.edu.upeu.pharmamobil.data.remote.dto.ProductoRequestDto
import pe.edu.upeu.pharmamobil.data.remote.dto.ProductoResponseDto
import pe.edu.upeu.pharmamobil.domain.model.Producto

/**
 * Mapeo de [ProductoResponseDto] hacia la entidad de dominio [Producto].
 */
fun ProductoResponseDto.toDomain(): Producto {
    val urlLimpia = images.firstOrNull()
        ?.replace("[", "")
        ?.replace("]", "")
        ?.replace("\"", "")
        ?.replace("\\", "")
        ?.trim() ?: ""

    return Producto(
        id = id,
        nombre = nombre,
        precio = precio,
        stock = stock,
        activo = estado,
        imagen = urlLimpia,
        categoria = categoriaNombre ?: "General"
    )
}

/**
 * Mapeo de la entidad [Producto] hacia [ProductoRequestDto] para envíos POST / PUT.
 */
fun Producto.toRequest(categoriaId: Long = 1L): ProductoRequestDto = ProductoRequestDto(
    nombre = nombre,
    precio = precio,
    stock = stock,
    estado = activo,
    categoriaId = categoriaId
)

/**
 * Mapeo de compatibilidad para [ProductoDto].
 */
fun ProductoDto.toDomain(): Producto {
    val urlLimpia = images.firstOrNull()
        ?.replace("[", "")
        ?.replace("]", "")
        ?.replace("\"", "")
        ?.replace("\\", "")
        ?.trim() ?: ""

    return Producto(
        id = id.toLong(),
        nombre = title,
        precio = price,
        stock = when {
            id % 7 == 0 -> 0 // Agotado
            id % 3 == 0 -> (id % 5) + 1 // Bajo stock (1 a 5 u.)
            else -> ((id * 3) % 70) + 10 // En stock (10 a 80 u.)
        },
        activo = id % 8 != 0,
        imagen = urlLimpia,
        categoria = categoria?.name ?: "General"
    )
}
