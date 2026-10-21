package pe.edu.upeu.pharmamobil.domain.usecase

import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository

/**
 * Caso de uso para disparar la sincronización bidireccional entre SQLite y el backend REST.
 * No silencia errores de red o conectividad para que la capa de presentación pueda informar al usuario.
 */
class SincronizarProductosUseCase(
    private val repositorio: ProductoRepository
) {
    suspend operator fun invoke(): Result<Unit> = repositorio.sincronizar()

    fun obtenerUltimaHoraSincronizada(): String? = repositorio.obtenerUltimaHoraSincronizada()
}
