package pe.edu.upeu.pharmamobil.domain.usecase

import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository

/**
 * Caso de uso para la actualización de datos de un producto existente.
 */
class ActualizarProductoUseCase(
    private val repository: ProductoRepository
) {
    /**
     * Valida de manera sincrónica las reglas de negocio para la actualización.
     */
    fun validar(
        id: Long,
        nombre: String,
        precio: String,
        stock: String,
        activo: Boolean = true,
        imagen: String = "",
        categoria: String = ""
    ): Result<Producto> {
        if (id <= 0L) {
            return Result.failure(IllegalArgumentException("El identificador del producto es inválido."))
        }

        val nombreTrimmed = nombre.trim()
        if (nombreTrimmed.isBlank()) {
            return Result.failure(IllegalArgumentException("El nombre es obligatorio."))
        }

        val precioDouble = precio.toDoubleOrNull()
            ?: return Result.failure(IllegalArgumentException("Ingrese un precio numérico."))

        if (precioDouble <= 0.0) {
            return Result.failure(IllegalArgumentException("El precio debe ser mayor que cero."))
        }

        val stockInt = stock.toIntOrNull()
            ?: return Result.failure(IllegalArgumentException("Ingrese un stock entero."))

        if (stockInt < 0) {
            return Result.failure(IllegalArgumentException("El stock no puede ser negativo."))
        }

        return try {
            val productoValido = Producto(
                id = id,
                nombre = nombreTrimmed,
                precio = precioDouble,
                stock = stockInt,
                activo = activo,
                imagen = imagen,
                categoria = categoria
            )
            Result.success(productoValido)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Valida y actualiza el producto a través del repositorio.
     */
    suspend operator fun invoke(
        id: Long,
        nombre: String,
        precio: String,
        stock: String,
        activo: Boolean = true,
        imagen: String = "",
        categoria: String = ""
    ): Result<Producto> {
        val validacion = validar(id, nombre, precio, stock, activo, imagen, categoria)
        if (validacion.isFailure) {
            return validacion
        }
        return invoke(validacion.getOrThrow())
    }

    /**
     * Actualiza directamente el producto en el repositorio.
     */
    suspend operator fun invoke(producto: Producto): Result<Producto> =
        runCatching { repository.actualizar(producto) }
}
