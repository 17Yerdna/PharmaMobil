package pe.edu.upeu.pharmamobil.demo

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import pe.edu.upeu.pharmamobil.domain.model.Producto

fun observarCambiosDeProductos(
    productosIniciales: List<Producto>
): Flow<List<Producto>> = flow {
    var productosActuales = productosIniciales.toList()

    emit(productosActuales)

    delay(2_000)

    val producto = productosActuales.firstOrNull()

    if (producto != null) {
        productosActuales = productosActuales.map { actual ->
            if (actual.id == producto.id) {
                actual.copy(stock = actual.stock - 1)
            } else {
                actual
            }
        }

        emit(productosActuales)
    }
}