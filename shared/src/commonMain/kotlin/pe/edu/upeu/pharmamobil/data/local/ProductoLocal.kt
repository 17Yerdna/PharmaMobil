package pe.edu.upeu.pharmamobil.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pe.edu.upeu.pharmamobil.db.PharmaDb
import pe.edu.upeu.pharmamobil.domain.model.Producto

/**
 * Fuente de datos local para la entidad [Producto], respaldada por SQLDelight / SQLite.
 * Gestiona el ciclo de vida de persistencia, generación de identificadores temporales negativos
 * y transacciones atómicas para sincronización offline-first.
 */
class ProductoLocal(private val db: PharmaDb) {

    private val queries = db.productoQueries

    /**
     * Emite un [Flow] reactivo con todos los productos almacenados en SQLite, ordenados alfabéticamente.
     * Notifica automáticamente a los observadores ante cualquier inserción, actualización o borrado.
     */
    fun observarTodos(): Flow<List<Producto>> =
        queries.listarTodos()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { filas -> filas.map { it.toDomain() } }

    /**
     * Retorna una instantánea sincrónica de todos los productos presentes actualmente en SQLite.
     */
    fun listarActual(): List<Producto> =
        queries.listarTodos().executeAsList().map { it.toDomain() }

    /**
     * Retorna los productos registrados localmente con bandera pendiente = 1 (IDs negativos).
     */
    fun pendientes(): List<Producto> =
        queries.listarPendientes().executeAsList().map { it.toDomain() }

    /**
     * Calcula el siguiente identificador temporal negativo para evitar colisiones con IDs remotos positivos.
     * Si no existen registros locales, retorna -1L; de lo contrario, `min(id) - 1L`.
     */
    fun siguienteIdTemporal(): Long {
        val minId = queries.siguienteIdLocal().executeAsOne()
        return if (minId <= 0L) minId - 1L else -1L
    }

    /**
     * Guarda un producto nuevo de forma local con estado pendiente de sincronización (ID negativo y pendiente = 1).
     */
    fun guardarLocalPendiente(producto: Producto): Producto {
        val idNegativo = siguienteIdTemporal()
        val pendienteGuardado = producto.copy(id = idNegativo)
        queries.insertar(
            id = idNegativo,
            nombre = pendienteGuardado.nombre,
            precio = pendienteGuardado.precio,
            stock = pendienteGuardado.stock.toLong(),
            activo = if (pendienteGuardado.activo) 1L else 0L,
            imagen = pendienteGuardado.imagen,
            categoria = pendienteGuardado.categoria,
            pendiente = 1L
        )
        return pendienteGuardado
    }

    /**
     * Reemplaza atómicamente un registro temporal negativo por el registro devuelto por el servidor (ID positivo).
     */
    fun reemplazarIdTemporalPorRemoto(idTemporal: Long, productoRemoto: Producto) {
        db.transaction {
            queries.borrarPorId(idTemporal)
            queries.insertar(
                id = productoRemoto.id,
                nombre = productoRemoto.nombre,
                precio = productoRemoto.precio,
                stock = productoRemoto.stock.toLong(),
                activo = if (productoRemoto.activo) 1L else 0L,
                imagen = productoRemoto.imagen,
                categoria = productoRemoto.categoria,
                pendiente = 0L
            )
        }
    }

    /**
     * Actualiza el catálogo local tras una sincronización remota:
     * borra los registros previamente sincronizados (pendiente = 0) para reflejar bajas del servidor,
     * e inserta los productos remotos más recientes preservando los pendientes locales intactos.
     */
    fun guardarRemotos(remotos: List<Producto>) {
        db.transaction {
            queries.borrarSincronizados()
            remotos.forEach { producto ->
                queries.insertar(
                    id = producto.id,
                    nombre = producto.nombre,
                    precio = producto.precio,
                    stock = producto.stock.toLong(),
                    activo = if (producto.activo) 1L else 0L,
                    imagen = producto.imagen,
                    categoria = producto.categoria,
                    pendiente = 0L
                )
            }
        }
    }

    /**
     * Actualiza un producto existente en la base de datos local.
     */
    fun actualizar(producto: Producto, pendiente: Boolean = false) {
        queries.insertar(
            id = producto.id,
            nombre = producto.nombre,
            precio = producto.precio,
            stock = producto.stock.toLong(),
            activo = if (producto.activo) 1L else 0L,
            imagen = producto.imagen,
            categoria = producto.categoria,
            pendiente = if (pendiente) 1L else 0L
        )
    }

    /**
     * Elimina un producto por ID de la base de datos local.
     */
    fun eliminarPorId(id: Long) {
        queries.borrarPorId(id)
    }

    /**
     * Mapeo de la entidad SQLDelight a la entidad de dominio [Producto].
     */
    private fun pe.edu.upeu.pharmamobil.db.Producto.toDomain(): Producto = Producto(
        id = id,
        nombre = nombre,
        precio = precio,
        stock = stock.toInt(),
        activo = activo == 1L,
        imagen = imagen,
        categoria = categoria
    )
}
