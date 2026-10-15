package pe.edu.upeu.pharmamobil.domain.usecase

import pe.edu.upeu.pharmamobil.domain.model.Producto

fun productosDisponibles(
    productos: List<Producto>
): List<Producto>{
    return productos.filter { producto -> producto.stock>0 }
}

fun nombresDeProductos(
    productos: List<Producto>
): List<String> {
    return productos.map { producto ->
        producto.nombre
    }
}

fun buscarProductoPorId(
    productos: List<Producto>,
    id: Long
): Producto? {
    return productos.find { producto ->
        producto.id == id
    }
}

fun valorTotalInventario(
    productos: List<Producto>
): Double {
    return productos.sumOf { producto ->
        producto.precio * producto.stock
    }
}

fun productosConStockBajo(
    productos: List<Producto>,
    limite: Int = 5
): List<Producto> {
    return productos.filter { producto ->
        producto.stock in 1..limite
    }
}

