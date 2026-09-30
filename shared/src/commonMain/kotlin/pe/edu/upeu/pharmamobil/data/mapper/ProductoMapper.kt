package pe.edu.upeu.pharmamobil.data.mapper

import pe.edu.upeu.pharmamobil.data.remote.dto.ProductoDto
import pe.edu.upeu.pharmamobil.domain.model.Producto

/**
 * Función de extensión que traduce un ProductoDto de red a un Producto de dominio.
 * El dominio permanece desacoplado de la capa de red y las anotaciones de serialización.
 */
fun ProductoDto.toDomain(): Producto = Producto(
    id = id,
    nombre = title,
    precio = if (price > 0) price else 1.0,
    stock = 15
)
