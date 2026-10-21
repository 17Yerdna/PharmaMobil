package pe.edu.upeu.pharmamobil.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val productosObservables = MutableStateFlow<List<Producto>>(emptyList())

    override fun observar() = productosObservables.asStateFlow()
    override suspend fun sincronizar(): Result<Unit> = runCatching { listar() }.map { }
    override fun obtenerUltimaHoraSincronizada(): String? = null

    override suspend fun listar(): List<Producto> {
        val resultado = ejecutarLlamada {
            api.listar().contenido.map { it.toDomain() }
        }

        return if (resultado.isSuccess && resultado.getOrThrow().isNotEmpty()) {
            val remotos = resultado.getOrThrow()
            productosLocales = remotos.distinctBy { it.id }.toMutableList()
            productosObservables.value = productosLocales.toList()
            productosLocales
        } else {
            val alternativo = ejecutarLlamada {
                api.obtenerProductos().map { it.toDomain() }
            }
            if (alternativo.isSuccess && alternativo.getOrThrow().isNotEmpty()) {
                val remotos = alternativo.getOrThrow()
                productosLocales = remotos.distinctBy { it.id }.toMutableList()
                productosObservables.value = productosLocales.toList()
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
        val creado = resultado.getOrThrow()
        productosLocales.add(0, creado)
        productosObservables.value = productosLocales.toList()
        return creado
    }

    override suspend fun actualizar(producto: Producto): Producto {
        val resultado = ejecutarLlamada {
            api.actualizar(producto.id, producto.toRequest(categoriaPorDefecto)).toDomain()
        }
        val actualizado = resultado.getOrThrow()
        val index = productosLocales.indexOfFirst { it.id == producto.id }
        if (index != -1) {
            productosLocales[index] = actualizado
            productosObservables.value = productosLocales.toList()
        }
        return actualizado
    }

    override suspend fun eliminar(id: Long) {
        val resultado = ejecutarLlamada {
            api.eliminar(id)
        }
        resultado.getOrThrow()
        productosLocales.removeAll { it.id == id }
        productosObservables.value = productosLocales.toList()
    }
}
