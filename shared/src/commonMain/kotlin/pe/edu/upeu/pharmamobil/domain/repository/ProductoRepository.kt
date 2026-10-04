package pe.edu.upeu.pharmamobil.domain.repository

import pe.edu.upeu.pharmamobil.domain.model.Producto

/**
 * Puerto de salida del dominio para las operaciones CRUD del inventario de productos.
 * Redactado en el lenguaje ubicuo del negocio, sin detalles tecnológicos ni dependencias externas.
 */
interface ProductoRepository {
    suspend fun listar(): List<Producto>
    suspend fun obtener(id: Long): Producto
    suspend fun registrar(producto: Producto): Producto
    suspend fun actualizar(producto: Producto): Producto
    suspend fun eliminar(id: Long)
}
