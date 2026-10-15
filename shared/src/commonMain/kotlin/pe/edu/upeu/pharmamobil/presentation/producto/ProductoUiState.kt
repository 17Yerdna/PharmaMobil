package pe.edu.upeu.pharmamobil.presentation.producto

import pe.edu.upeu.pharmamobil.domain.model.Producto

/**
 * Modela las cuatro fases excluyentes del inventario de productos.
 */
sealed interface FaseInventario {
    data object Cargando : FaseInventario
    data object SinProductos : FaseInventario
    data class ConProductos(val productos: List<ProductoUi>) : FaseInventario
    data class Error(val mensaje: String) : FaseInventario
}

/**
 * Modela el estado de la operación de mutación CRUD en curso,
 * desacoplándola de la fase global de la pantalla para evitar parpadeos (Punto de Control 5).
 */
sealed interface Operacion {
    data object Inactiva : Operacion
    data class EnCurso(val tipo: Tipo) : Operacion
    data class Fallida(val mensaje: String) : Operacion

    enum class Tipo { Crear, Actualizar, Eliminar }
}

/**
 * Modela el estado reactivo del formulario de creación y edición de productos,
 * mapeando los errores campo por campo devueltos por el servidor o validados localmente.
 */
data class FormularioProductoState(
    val id: Long? = null,
    val nombre: String = "",
    val precio: String = "",
    val stock: String = "",
    val activo: Boolean = true,
    val categoriaId: Long = 1L,
    val imagen: String = "",
    val nombreError: String? = null,
    val precioError: String? = null,
    val stockError: String? = null,
    val estaGuardando: Boolean = false,
    val mensajeFeedback: String? = null
) {
    val esEdicion: Boolean
        get() = id != null && id > 0L

    val tieneErrores: Boolean
        get() = nombreError != null || precioError != null || stockError != null
}

/**
 * Estado general unificado para la pantalla de Productos.
 */
data class ProductoUiState(
    val fase: FaseInventario = FaseInventario.Cargando,
    val operacion: Operacion = Operacion.Inactiva,
    val tabSeleccionada: Int = 0, // 0: Activos, 1: Inactivos, 2: Bajo Stock
    val busqueda: String = "",
    val formulario: FormularioProductoState = FormularioProductoState(),
    val mostrarFormulario: Boolean = false,
    val productoAEliminar: Producto? = null,
    val productoEnDetalle: ProductoUi? = null,
    val mensajeExito: String? = null,
    val totalActivos: Int = 0,
    val totalInactivos: Int = 0,
    val totalBajoStock: Int = 0,
    val esDepuracion: Boolean = false
)

