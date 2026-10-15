package pe.edu.upeu.pharmamobil.domain.usecase

import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository

/**
 * Caso de uso para la eliminación de un producto del inventario.
 */
class EliminarProductoUseCase(
    private val repository: ProductoRepository
) {
    suspend operator fun invoke(id: Long): Result<Unit> {
        if (id <= 0L) {
            return Result.failure(IllegalArgumentException("El identificador del producto es inválido."))
        }
        return runCatching { repository.eliminar(id) }
    }
}
