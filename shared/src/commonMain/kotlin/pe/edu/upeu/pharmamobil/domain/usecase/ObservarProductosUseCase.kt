package pe.edu.upeu.pharmamobil.domain.usecase

import kotlinx.coroutines.flow.Flow
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository

/**
 * Caso de uso reactivo para observar el inventario de productos en tiempo real
 * desde la fuente única de verdad local (SQLite / SQLDelight).
 */
class ObservarProductosUseCase(
    private val repositorio: ProductoRepository
) {
    operator fun invoke(): Flow<List<Producto>> = repositorio.observar()
}
