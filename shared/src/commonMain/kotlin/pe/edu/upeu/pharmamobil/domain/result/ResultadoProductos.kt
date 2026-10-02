package pe.edu.upeu.pharmamobil.domain.result

import pe.edu.upeu.pharmamobil.domain.model.Producto

sealed class ResultadoProductos{
    data object cargando : ResultadoProductos()

    data class Exito(
        val productos : List<Producto>
    ): ResultadoProductos()

    data class Error(
        val mensaje: String
    ): ResultadoProductos()
}
