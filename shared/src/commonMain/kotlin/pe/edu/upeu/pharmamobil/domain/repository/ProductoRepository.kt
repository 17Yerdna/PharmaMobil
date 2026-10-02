package pe.edu.upeu.pharmamobil.domain.repository

import pe.edu.upeu.pharmamobil.domain.model.Producto

/**
 * Puerto de salida para la persistencia y consulta del inventario de productos.
 * Redactada en el lenguaje ubicuo del negocio, sin detalles tecnológicos ni dependencias externas.
 */
interface ProductoRepository {
    suspend fun registrar(producto: Producto): Producto
    suspend fun listar(): List<Producto>
}
