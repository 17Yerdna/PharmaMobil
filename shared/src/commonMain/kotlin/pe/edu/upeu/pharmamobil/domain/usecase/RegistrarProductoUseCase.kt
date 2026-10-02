package pe.edu.upeu.pharmamobil.domain.usecase

import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository

/**
 * Caso de uso que concentra las reglas de negocio para el registro de productos farmacéuticos.
 * Reemplaza y centraliza la lógica que previamente residía en la capa de presentación.
 *
 * Ofrece:
 * 1. validar(nombre, precio, stock, activo): Validación pura sincrónica de reglas de negocio.
 * 2. invoke(nombre, precio, stock, activo): Operación completa que valida y persiste a través del repositorio.
 */
class RegistrarProductoUseCase(
    private val productoRepository: ProductoRepository
) {
    /**
     * Valida de manera pura y sincrónica las reglas de negocio para el registro de un producto.
     */
    fun validar(
        nombre: String,
        precio: String,
        stock: String,
        activo: Boolean = true
    ): Result<Producto> {
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
                id = 0L,
                nombre = nombreTrimmed,
                precio = precioDouble,
                stock = stockInt,
                activo = activo
            )
            Result.success(productoValido)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Valida y persiste el producto invocando al repositorio de forma suspendida.
     */
    suspend operator fun invoke(
        nombre: String,
        precio: String,
        stock: String,
        activo: Boolean = true
    ): Result<Producto> {
        val validacion = validar(nombre, precio, stock, activo)
        if (validacion.isFailure) {
            return validacion
        }
        val productoParaGuardar = validacion.getOrThrow()
        return try {
            val productoGuardado = productoRepository.registrar(productoParaGuardar)
            Result.success(productoGuardado)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
