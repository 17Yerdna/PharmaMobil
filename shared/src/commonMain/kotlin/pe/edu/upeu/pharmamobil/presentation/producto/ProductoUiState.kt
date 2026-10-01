package pe.edu.upeu.pharmamobil.presentation.producto

enum class FiltroInventario(val titulo: String) {
    TODOS("Todos"),
    DISPONIBLES("Disponibles"),
    BAJO_STOCK("Bajo Stock (≤5)"),
    AGOTADOS("Agotados")
}

data class ProductoUiState(
    val fase: Fase = Fase.Cargando,
    val formulario: FormularioProducto = FormularioProducto(),
    val registrando: Boolean = false,
    val formularioVisible: Boolean = false,
    val filtroSeleccionado: FiltroInventario = FiltroInventario.TODOS,
    val mensajeExito: String? = null,
    val paginaActual: Int = 1,
    val limitePorPagina: Int = 10,
    val hayMasProductos: Boolean = true
) {

    /** Fases excluyentes del inventario: solo una puede estar activa. */
    sealed interface Fase {

        data object Cargando : Fase

        data object SinProductos : Fase

        data class ConProductos(val productos: List<ProductoUi>) : Fase

        data class Error(val mensaje: String) : Fase
    }
}

data class FormularioProducto(
    val nombre: String = "",
    val precio: String = "",
    val stock: String = "",
    val nombreError: String? = null,
    val precioError: String? = null,
    val stockError: String? = null
)
