package pe.edu.upeu.pharmamobil.data.repository

import kotlinx.coroutines.delay
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository

/**
 * Implementación en memoria del repositorio de productos.
 * Simula latencia mediante delay() para exponer los estados de carga en la UI y tests.
 */
class ProductoRepositorioEnMemoria : ProductoRepository {

    private val inventario = mutableListOf(
        Producto(id = 1L, nombre = "Paracetamol", precio = 15.50, stock = 100, activo = true, categoria = "Analgésicos"),
        Producto(id = 2L, nombre = "Ibuprofeno", precio = 18.90, stock = 50, activo = true, categoria = "Antiinflamatorios"),
        Producto(id = 3L, nombre = "Amoxicilina", precio = 25.00, stock = 5, activo = true, categoria = "Antibióticos"),
        Producto(id = 4L, nombre = "Loratadina", precio = 12.50, stock = 0, activo = false, categoria = "Antialérgicos"),
        Producto(id = 5L, nombre = "Diclofenaco", precio = 20.00, stock = 3, activo = true, categoria = "Antiinflamatorios")
    )

    private var nextId = 6L

    override suspend fun listar(): List<Producto> {
        delay(100)
        return inventario.toList()
    }

    override suspend fun obtener(id: Long): Producto {
        delay(50)
        return inventario.find { it.id == id }
            ?: throw NoSuchElementException("Producto con ID $id no encontrado")
    }

    override suspend fun registrar(producto: Producto): Producto {
        delay(100)
        val productoConId = producto.copy(id = if (producto.id > 0L) producto.id else nextId++)
        inventario.add(0, productoConId)
        return productoConId
    }

    override suspend fun actualizar(producto: Producto): Producto {
        delay(100)
        val index = inventario.indexOfFirst { it.id == producto.id }
        if (index == -1) {
            throw NoSuchElementException("No se puede actualizar: Producto con ID ${producto.id} no existe")
        }
        inventario[index] = producto
        return producto
    }

    override suspend fun eliminar(id: Long) {
        delay(50)
        val removido = inventario.removeAll { it.id == id }
        if (!removido) {
            throw NoSuchElementException("No se puede eliminar: Producto con ID $id no existe")
        }
    }
}
