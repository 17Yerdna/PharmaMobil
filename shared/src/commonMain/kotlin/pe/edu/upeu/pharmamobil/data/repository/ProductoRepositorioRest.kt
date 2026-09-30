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

    override suspend fun listar(): List<Producto> {
        return api.obtenerProductos().map { it.toDomain() }
    }

    override suspend fun registrar(producto: Producto): Producto {
        // En Sesión 7 la API remota pública se consume mediante GET (listar).
        // Se preserva la compatibilidad con el caso de uso de registro.
        return producto.copy(id = (1000L..9999L).random())
    }
}
