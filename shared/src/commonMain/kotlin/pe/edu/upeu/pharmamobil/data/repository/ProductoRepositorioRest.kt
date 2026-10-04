package pe.edu.upeu.pharmamobil.data.repository

import pe.edu.upeu.pharmamobil.data.mapper.toDomain
import pe.edu.upeu.pharmamobil.data.mapper.toRequest
import pe.edu.upeu.pharmamobil.data.remote.ProductoApi
import pe.edu.upeu.pharmamobil.data.remote.ejecutarLlamada
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository

/**
 * Implementación REST de [ProductoRepository] conectada con Ktor Client.
 */
class ProductoRepositorioRest(
    private val api: ProductoApi,
    private val categoriaPorDefecto: Long = 1L
) : ProductoRepository {

    private var productosLocales: MutableList<Producto> = mutableListOf()

    override suspend fun listar(): List<Producto> {
        val resultado = ejecutarLlamada {
            api.listar().contenido.map { it.toDomain() }
        }

        return if (resultado.isSuccess && resultado.getOrThrow().isNotEmpty()) {
            val remotos = resultado.getOrThrow()
            productosLocales = remotos.distinctBy { it.id }.toMutableList()
            productosLocales
        } else {
            val alternativo = ejecutarLlamada {
                api.obtenerProductos().map { it.toDomain() }
            }
            if (alternativo.isSuccess && alternativo.getOrThrow().isNotEmpty()) {
                val remotos = alternativo.getOrThrow()
                productosLocales = remotos.distinctBy { it.id }.toMutableList()
                productosLocales
            } else if (productosLocales.isNotEmpty()) {
                productosLocales
            } else {
                throw resultado.exceptionOrNull()
                    ?: alternativo.exceptionOrNull()
                    ?: Exception("Error al cargar la lista de productos")
            }
        }
    }

    override suspend fun obtener(id: Long): Producto {
        return ejecutarLlamada {
            api.obtener(id).toDomain()
        }.getOrThrow()
    }

    override suspend fun registrar(producto: Producto): Producto {
        val resultado = ejecutarLlamada {
            api.crear(producto.toRequest(categoriaPorDefecto)).toDomain()
        }
        return if (resultado.isSuccess) {
            val creado = resultado.getOrThrow()
            productosLocales.add(0, creado)
            creado
        } else {
            val nuevoId = if (producto.id > 0L) producto.id else (1000L..9999L).random()
            val nuevo = producto.copy(id = nuevoId)
            productosLocales.add(0, nuevo)
            nuevo
        }
    }

    override suspend fun actualizar(producto: Producto): Producto {
        val resultado = ejecutarLlamada {
            api.actualizar(producto.id, producto.toRequest(categoriaPorDefecto)).toDomain()
        }
        return if (resultado.isSuccess) {
            val actualizado = resultado.getOrThrow()
            val index = productosLocales.indexOfFirst { it.id == producto.id }
            if (index != -1) {
                productosLocales[index] = actualizado
            }
            actualizado
        } else {
            val index = productosLocales.indexOfFirst { it.id == producto.id }
            if (index != -1) {
                productosLocales[index] = producto
            }
            producto
        }
    }

    override suspend fun eliminar(id: Long) {
        val resultado = ejecutarLlamada {
            api.eliminar(id)
        }
        productosLocales.removeAll { it.id == id }
    }
}
