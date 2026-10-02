package pe.edu.upeu.pharmamobil.data.repository

import kotlinx.coroutines.delay
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository

/**
 * Implementación en memoria del repositorio de productos.
 * Simula latencia mediante delay() para exponer los estados de carga en la UI.
 * Asigna de forma determinista el identificador de cada producto nuevo.
 */
class ProductoRepositorioEnMemoria : ProductoRepository {

    // Lista mutable interna en memoria con datos simulados iniciales
    private val inventario = mutableListOf(
        Producto(id = 1L, nombre = "Paracetamol", precio = 15.50, stock = 100, activo = true),
        Producto(id = 2L, nombre = "Ibuprofeno", precio = 18.90, stock = 50, activo = true),
        Producto(id = 3L, nombre = "Amoxicilina", precio = 25.00, stock = 5, activo = true),
        Producto(id = 4L, nombre = "Loratadina", precio = 12.50, stock = 0, activo = false),
        Producto(id = 5L, nombre = "Diclofenaco", precio = 20.00, stock = 3, activo = true)
    )

    private var nextId = 6L

    override suspend fun registrar(producto: Producto): Producto {
        // Retardo simulado para visibilidad de estados de carga (entre 300 y 800 ms)
        delay(400)
        val productoConId = producto.copy(id = nextId++)
        inventario.add(0, productoConId)
        return productoConId
    }

    override suspend fun listar(): List<Producto> {
        delay(500)
        return inventario.toList()
    }
}
