package pe.edu.upeu.pharmamobil.data.mapper

import pe.edu.upeu.pharmamobil.data.remote.dto.ProductoDto
import pe.edu.upeu.pharmamobil.domain.model.Producto

/**
 * Función de extensión que convierte un DTO de red al modelo de dominio.
 * Este archivo es la única pieza que conoce ambas capas — mantenerlo en data.mapper.
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
