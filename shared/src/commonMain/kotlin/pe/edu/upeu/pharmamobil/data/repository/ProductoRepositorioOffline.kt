package pe.edu.upeu.pharmamobil.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pe.edu.upeu.pharmamobil.data.local.ProductoLocal
import pe.edu.upeu.pharmamobil.data.mapper.toDomain
import pe.edu.upeu.pharmamobil.data.mapper.toRequest
import pe.edu.upeu.pharmamobil.data.remote.ProductoApi
import pe.edu.upeu.pharmamobil.data.remote.ejecutarLlamada
import pe.edu.upeu.pharmamobil.domain.error.ErrorApi
import pe.edu.upeu.pharmamobil.domain.error.ErrorApiException
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository
import pe.edu.upeu.pharmamobil.platform.ProveedorHora

/**
 * Repositorio de Productos con arquitectura Offline-First.
 * Prioriza la lectura continua desde la base de datos local SQLite (Single Source of Truth)
 * y sincroniza de forma bidireccional y atómica con el backend REST mediante un [Mutex].
 */
class ProductoRepositorioOffline(
    private val local: ProductoLocal,
    private val api: ProductoApi,
    private val proveedorHora: ProveedorHora,
    private val categoriaPorDefecto: Long = 1L
) : ProductoRepository {

    private val mutexSincronizacion = Mutex()
    private var ultimaHoraSincronizada: String? = null

    /**
     * Expone un flujo reactivo que emite en tiempo real cualquier cambio en el inventario local.
     */
    override fun observar(): Flow<List<Producto>> = local.observarTodos()

    /**
     * Retorna la hora formateada de la última sincronización remota exitosa.
     */
    override fun obtenerUltimaHoraSincronizada(): String? = ultimaHoraSincronizada

    /**
     * Retorna el instante en milisegundos de la última sincronización correcta persistida en SQLite.
     */
    override fun obtenerUltimoSyncTimestamp(): Long? = local.obtenerUltimoSyncTimestamp()

    /**
     * Ejecuta el ciclo de sincronización bidireccional con el servidor protegido por [Mutex]:
     * 1. Sube todos los productos pendientes (IDs negativos) mediante POST.
     * 2. Reemplaza atómicamente cada ID negativo por la respuesta positiva del servidor.
     * 3. Descarga el catálogo completo del servidor y refresca la caché local sin borrar pendientes.
     * 4. Propaga cualquier error de red o timeout tipado como [ErrorApiException] sin silenciarlo.
     */
    override suspend fun sincronizar(): Result<Unit> = mutexSincronizacion.withLock {
        runCatching {
            // 1. Subir pendientes
            val pendientes = local.pendientes()
            for (pendiente in pendientes) {
                val resultadoPost = ejecutarLlamada {
                    api.crear(pendiente.toRequest(categoriaPorDefecto))
                }
                val respuestaDto = resultadoPost.getOrThrow()
                local.reemplazarIdTemporalPorRemoto(pendiente.id, respuestaDto.toDomain())
            }

            // 2. Descargar catálogo remoto más reciente
            val resultadoListar = ejecutarLlamada {
                api.listar(pagina = 0, tamanio = 100).contenido.map { it.toDomain() }
            }
            val remotos = resultadoListar.getOrThrow()
            local.guardarRemotos(remotos)

            // 3. SOLO TRAS ÉXITO TOTAL DE 1 Y 2: Registrar timestamp persistente en SQLite
            val ahora = proveedorHora.ahoraMillis()
            local.guardarTimestampSincronizacion(ahora)
            ultimaHoraSincronizada = proveedorHora.obtenerHoraActualFormateada()
        }
    }

    /**
     * Retorna la lista de productos almacenados localmente en SQLite.
     */
    override suspend fun listar(): List<Producto> = local.listarActual()

    /**
     * Retorna un producto por ID desde el almacenamiento local.
     */
    override suspend fun obtener(id: Long): Producto {
        return local.listarActual().firstOrNull { it.id == id }
            ?: throw NoSuchElementException("Producto con ID $id no encontrado")
    }

    /**
     * Registra un nuevo producto siguiendo la estrategia Offline-First:
     * 1. Guarda de inmediato en SQLite con ID negativo temporal y pendiente = 1.
     * 2. Intenta la sincronización inmediata con el backend REST.
     * 3. Si no hay conexión o expira el tiempo, devuelve el producto guardado localmente (éxito offline).
     * 4. Si el servidor rechaza con 400 (validación) o 403 (permisos), revierte el pendiente local y lanza el error.
     */
    override suspend fun registrar(producto: Producto): Producto = mutexSincronizacion.withLock {
        val guardadoLocal = local.guardarLocalPendiente(producto)

        val resultadoRemoto = ejecutarLlamada {
            api.crear(guardadoLocal.toRequest(categoriaPorDefecto))
        }

        if (resultadoRemoto.isSuccess) {
            val remoto = resultadoRemoto.getOrThrow().toDomain()
            local.reemplazarIdTemporalPorRemoto(guardadoLocal.id, remoto)
            remoto
        } else {
            val excepcion = resultadoRemoto.exceptionOrNull()
            val errorApi = (excepcion as? ErrorApiException)?.error
            when (errorApi) {
                is ErrorApi.SinConexion, is ErrorApi.TiempoAgotado -> {
                    // Éxito local: queda pendiente de sincronización
                    guardadoLocal
                }
                else -> {
                    // Fallo del servidor/negocio: descartar de SQLite local y propagar error tipado
                    local.eliminarPorId(guardadoLocal.id)
                    throw (excepcion ?: Exception("Error al registrar producto"))
                }
            }
        }
    }

    /**
     * Actualiza un producto existente en el backend.
     * Bloquea la edición si el producto aún tiene un identificador temporal negativo.
     */
    override suspend fun actualizar(producto: Producto): Producto {
        if (producto.id < 0L) {
            throw IllegalStateException("No se puede editar un producto pendiente de sincronización")
        }

        val resultado = ejecutarLlamada {
            api.actualizar(producto.id, producto.toRequest(categoriaPorDefecto))
        }
        val actualizado = resultado.getOrThrow().toDomain()
        local.actualizar(actualizado, pendiente = false)
        return actualizado
    }

    /**
     * Elimina un producto por ID del backend y de SQLite.
     * Bloquea la eliminación si el producto tiene un ID temporal negativo.
     */
    override suspend fun eliminar(id: Long) {
        if (id < 0L) {
            throw IllegalStateException("No se puede eliminar un producto pendiente de sincronización")
        }

        val resultado = ejecutarLlamada {
            api.eliminar(id)
        }
        resultado.getOrThrow()
        local.eliminarPorId(id)
    }
}
