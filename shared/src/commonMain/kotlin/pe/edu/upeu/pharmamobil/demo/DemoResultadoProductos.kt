package pe.edu.upeu.pharmamobil.demo

import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.result.ResultadoProductos
import pe.edu.upeu.pharmamobil.domain.usecase.buscarProductoPorId
import pe.edu.upeu.pharmamobil.domain.usecase.nombresDeProductos
import pe.edu.upeu.pharmamobil.domain.usecase.productosDisponibles
import pe.edu.upeu.pharmamobil.domain.usecase.valorTotalInventario

fun mostrarResultado(resultado: ResultadoProductos) {
    when (resultado) {
        ResultadoProductos.cargando -> {
            println(
                "Cargando Producto"
            )
        }

        is ResultadoProductos.Exito -> {
            println(
                "Productos Encontrados: ${resultado.productos.size}"
            )
        }

        is ResultadoProductos.Error -> {
            println(
                "Error: ${resultado.mensaje}"
            )
        }
    }
}

