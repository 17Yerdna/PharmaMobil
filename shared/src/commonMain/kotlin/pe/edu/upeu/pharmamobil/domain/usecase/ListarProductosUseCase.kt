package pe.edu.upeu.pharmamobil.domain.usecase

import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository

/**
 * Caso de uso para obtener la lista de productos del inventario.
 */
class ListarProductosUseCase(
    private val repository: ProductoRepository
) {
    suspend operator fun invoke(): Result<List<Producto>> =
        runCatching { repository.listar() }
}
