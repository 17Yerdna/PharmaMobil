package pe.edu.upeu.pharmamobil.presentation.producto

import kotlin.math.roundToLong
import pe.edu.upeu.pharmamobil.domain.model.Producto

data class ProductoUi(
    val id: Long,
    val nombre: String,
    val precio: String,
    val stock: String,
    val stockNumero: Int = 0,
    val requiereReposicion: Boolean = false,
    val estaAgotado: Boolean = false,
    val descripcion: String = "",
    val imagenUrl: String = "",
    val categoria: String = ""
)

fun Producto.aUi(): ProductoUi = ProductoUi(
    id = id,
    nombre = nombre,
    precio = precio.enSoles(),
    stock = "$stock u.",
    stockNumero = stock,
    requiereReposicion = stock in 1..5 || (stock < Producto.STOCK_MINIMO && stock > 0),
    estaAgotado = stock == 0,
    descripcion = descripcion,
    imagenUrl = imagen,
    categoria = categoria
)

/** Formateo en Soles peruanos con dos decimales exactos. */
private fun Double.enSoles(): String {
    val centavos = (this * 100).roundToLong()
    val enteros = centavos / 100
    val decimales = (centavos % 100).toString().padStart(2, '0')
    return "S/ $enteros.$decimales"
}
