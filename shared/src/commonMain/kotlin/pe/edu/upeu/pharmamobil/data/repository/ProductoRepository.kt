package pe.edu.upeu.pharmamobil.data.repository

import kotlinx.coroutines.delay
import pe.edu.upeu.pharmamobil.domain.model.Producto

interface ProductoRepository {
    suspend fun obtenerProductos(): List<Producto>

    suspend fun actualizarStock(
        productoId: Long,
        nuevoStock: Int
    ): Result<Producto>
}

class ProductoRepositoryFake : ProductoRepository {
    private val productos = listOf(
        Producto(
            id = 1L,
            nombre = "Paracetamol 500 mg",
            precio = 5.50,
            stock = 25
        ),
        Producto(
            id = 2L,
            nombre = "Ibuprofeno 400 mg",
            precio = 8.90,
            stock = 12
        ),
        Producto(
            id = 3L,
            nombre = "Amoxicilina 500 mg",
            precio = 15.00,
            stock = 0
        )
    )

    override suspend fun obtenerProductos(): List<Producto> {
        delay(1_000)
        return productos.toList()
    }

    override suspend fun actualizarStock(
        productoId: Long,
        nuevoStock: Int
    ): Result<Producto> {
        if (nuevoStock < 0) {
            return Result.failure(
                IllegalArgumentException("El stock no puede ser negativo")
            )
        }

        delay(500)

        val producto = productos.find { it.id == productoId }
            ?: return Result.failure(
                NoSuchElementException("Producto no encontrado")
            )

        // ✅ Calcula la diferencia entre stock actual y nuevo stock
        val cantidadADisminuir = producto.stock - nuevoStock

        // ✅ Si el nuevo stock es mayor, es una reposición (aumenta stock)
        // ✅ Si el nuevo stock es menor, es una venta (disminuye stock)
        return if (cantidadADisminuir >= 0) {
            // Disminuir stock (venta)
            Result.success(
                producto.disminuirStock(cantidadADisminuir)
            )
        } else {
            // Aumentar stock (reposición) - usa copy directamente
            Result.success(
                producto.copy(stock = nuevoStock)
            )
        }
    }
}