package pe.edu.upeu.pharmamobil.data.repository

import pe.edu.upeu.pharmamobil.data.mapper.toDomain
import pe.edu.upeu.pharmamobil.data.remote.ProductoApi
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository

/**
 * Implementación REST de [ProductoRepository] conectada con Ktor Client.
 */
class ProductoRepositorioRest(
    private val api: ProductoApi
) : ProductoRepository {

    private var productosLocales: MutableList<Producto> = mutableListOf()

    override suspend fun listar(): List<Producto> {
        return try {
            val remotos = api.obtenerProductos().map { it.toDomain() }
            productosLocales = (productosLocales + remotos).distinctBy { it.id }.toMutableList()
            productosLocales
        } catch (e: Exception) {
            if (productosLocales.isNotEmpty()) {
                productosLocales
            } else {
                throw e
            }
        }
    }

    override suspend fun registrar(producto: Producto): Producto {
        val nuevoId = (1000L..9999L).random()
        val nuevoProducto = producto.copy(id = nuevoId)
        productosLocales.add(0, nuevoProducto)
        return nuevoProducto
    }
}
