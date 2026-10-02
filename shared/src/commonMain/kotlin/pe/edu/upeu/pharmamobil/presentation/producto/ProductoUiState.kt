package pe.edu.upeu.pharmamobil.presentation.producto

import pe.edu.upeu.pharmamobil.domain.model.Producto

/**
 * Modela las cuatro fases excluyentes del inventario de productos.
 */
sealed interface FaseInventario {
    data object Cargando : FaseInventario
    data object SinProductos : FaseInventario
    data class ConProductos(val productos: List<Producto>) : FaseInventario
    data class Error(val mensaje: String) : FaseInventario
}

/**
 * Modela el estado reactivo del formulario de creación de productos,
 * incluyendo los tres mensajes de error específicos.
 */
data class FormularioProductoState(
    val nombre: String = "",
    val precio: String = "",
    val stock: String = "",
    val activo: Boolean = true,
    val errorNombre: String? = null,
    val errorPrecio: String? = null,
    val errorStock: String? = null,
    val estaGuardando: Boolean = false,
    val mensajeFeedback: String? = null
) {
    val tieneErrores: Boolean
        get() = errorNombre != null || errorPrecio != null || errorStock != null
}

/**
 * Estado general unificado para la pantalla de Productos.
 */
data class ProductoUiState(
    val fase: FaseInventario = FaseInventario.Cargando,
    val tabSeleccionada: Int = 0, // 0: Activos, 1: Inactivos, 2: Bajo Stock
    val busqueda: String = "",
    val formulario: FormularioProductoState = FormularioProductoState(),
    val mostrarFormulario: Boolean = false,
    val totalActivos: Int = 0,
    val totalInactivos: Int = 0,
    val totalBajoStock: Int = 0
)
