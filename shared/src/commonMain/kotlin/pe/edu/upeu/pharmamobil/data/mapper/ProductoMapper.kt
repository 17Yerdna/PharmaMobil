package pe.edu.upeu.pharmamobil.data.mapper

import pe.edu.upeu.pharmamobil.data.remote.dto.ProductoDto
import pe.edu.upeu.pharmamobil.domain.model.Producto

/**
 * Función de extensión que traduce un ProductoDto de red a un Producto de dominio.
 * El dominio permanece desacoplado de la capa de red y las anotaciones de serialización.
 */
fun ProductoDto.toDomain(): Producto {
    // Calculo de stock determinista para evidenciar badges de stock y filtrado de tabs
    val stockCalculado = when {
        id % 5 == 0L -> 3   // Bajo Stock (<= 5)
        id % 9 == 0L -> 0   // Agotado
        id % 4 == 0L -> 4   // Bajo Stock (<= 5)
        else -> ((id % 20) + 6).toInt() // Disponible
    }

    return Producto(
        id = id,
        nombre = title,
        precio = if (price > 0) price else 1.0,
        stock = stockCalculado,
        descripcion = description,
        imagen = limpiarUrl(images.firstOrNull()),
        categoria = categoria?.name ?: "General"
    )
}

private fun limpiarUrl(url: String?): String {
    if (url.isNullOrBlank()) return ""
    return url.replace("[", "")
        .replace("]", "")
        .replace("\"", "")
        .trim()
}
