package pe.edu.upeu.pharmamobil.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import pe.edu.upeu.pharmamobil.data.repository.ProductoRepository
import pe.edu.upeu.pharmamobil.domain.result.ResultadoProductos

class ObservarProductosUseCase (
    private val repository: ProductoRepository
) {
    operator fun invoke(): Flow<ResultadoProductos> = flow {
        emit(ResultadoProductos.cargando)

        try {
            val productos = repository.obtenerProductos()

            emit(
                ResultadoProductos.Exito(
                    productos = productos
                )
            )
} catch (exception: Exception) {
            emit(
                ResultadoProductos.Error(
                    mensaje = exception.message ?: "Error al cargar productos"
                )
            )
        }
    }
    }