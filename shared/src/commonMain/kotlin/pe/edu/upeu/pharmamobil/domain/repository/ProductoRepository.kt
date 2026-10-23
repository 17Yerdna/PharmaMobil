package pe.edu.upeu.pharmamobil.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.edu.upeu.pharmamobil.domain.model.Producto

/**
 * Puerto de salida del dominio para las operaciones CRUD del inventario de productos.
 * Redactado en el lenguaje ubicuo del negocio, sin detalles tecnológicos ni dependencias externas.
 */
interface ProductoRepository {
    fun observar(): Flow<List<Producto>>
    suspend fun sincronizar(): Result<Unit>
    fun obtenerUltimaHoraSincronizada(): String?
    fun obtenerUltimoSyncTimestamp(): Long?
    suspend fun listar(): List<Producto>
    suspend fun obtener(id: Long): Producto
    suspend fun registrar(producto: Producto): Producto
    suspend fun actualizar(producto: Producto): Producto
    suspend fun eliminar(id: Long)
}
