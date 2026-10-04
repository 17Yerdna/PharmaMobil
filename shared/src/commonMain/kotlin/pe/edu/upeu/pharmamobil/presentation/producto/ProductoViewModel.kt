package pe.edu.upeu.pharmamobil.presentation.producto

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.edu.upeu.pharmamobil.domain.error.ErrorApi
import pe.edu.upeu.pharmamobil.domain.error.ErrorApiException
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.usecase.ActualizarProductoUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.EliminarProductoUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.ListarProductosUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.RegistrarProductoUseCase

/**
 * ViewModel multiplataforma para el módulo de Productos.
 * Gestiona el ciclo de vida de operaciones CRUD y el flujo unidireccional de datos (UDF).
 */
class ProductoViewModel(
    private val listarProductosUseCase: ListarProductosUseCase,
    private val registrarProductoUseCase: RegistrarProductoUseCase,
    private val actualizarProductoUseCase: ActualizarProductoUseCase,
    private val eliminarProductoUseCase: EliminarProductoUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductoUiState())
    val uiState: StateFlow<ProductoUiState> = _uiState.asStateFlow()

    private var inventarioCompleto: List<Producto> = emptyList()

    init {
        cargarProductos()
    }

    /**
     * Carga el catálogo completo de productos desde el backend REST.
     */
    fun cargarProductos() {
        viewModelScope.launch {
            _uiState.update { it.copy(fase = FaseInventario.Cargando) }
            val resultado = listarProductosUseCase()
            resultado.onSuccess { productos ->
                inventarioCompleto = productos
                actualizarFaseYContadores()
            }.onFailure { fallo ->
                val errorApi = (fallo as? ErrorApiException)?.error
                val mensaje = mensajeDe(errorApi, fallo)
                _uiState.update { it.copy(fase = FaseInventario.Error(mensaje)) }
            }
        }
    }

    fun seleccionarTab(tabIndex: Int) {
        _uiState.update { it.copy(tabSeleccionada = tabIndex, mostrarFormulario = false) }
        actualizarFaseYContadores()
    }

    fun onBusquedaChanged(query: String) {
        _uiState.update { it.copy(busqueda = query) }
        actualizarFaseYContadores()
    }

    fun toggleMostrarFormulario() {
        if (_uiState.value.mostrarFormulario) {
            cerrarFormulario()
        } else {
            iniciarCreacion()
        }
    }

    fun iniciarCreacion() {
        _uiState.update {
            it.copy(
                formulario = FormularioProductoState(),
                mostrarFormulario = true
            )
        }
    }

    fun iniciarEdicion(producto: Producto) {
        _uiState.update {
            it.copy(
                formulario = FormularioProductoState(
                    id = producto.id,
                    nombre = producto.nombre,
                    precio = producto.precio.toString(),
                    stock = producto.stock.toString(),
                    activo = producto.activo,
                    imagen = producto.imagen,
                    categoriaId = 1L
                ),
                mostrarFormulario = true
            )
        }
    }

    fun cerrarFormulario() {
        _uiState.update {
            it.copy(
                mostrarFormulario = false,
                formulario = FormularioProductoState()
            )
        }
    }

    fun onNombreChanged(nuevoNombre: String) {
        _uiState.update {
            it.copy(
                formulario = it.formulario.copy(
                    nombre = nuevoNombre,
                    nombreError = null,
                    mensajeFeedback = null
                )
            )
        }
    }

    fun onPrecioChanged(nuevoPrecio: String) {
        _uiState.update {
            it.copy(
                formulario = it.formulario.copy(
                    precio = nuevoPrecio,
                    precioError = null,
                    mensajeFeedback = null
                )
            )
        }
    }

    fun onStockChanged(nuevoStock: String) {
        _uiState.update {
            it.copy(
                formulario = it.formulario.copy(
                    stock = nuevoStock,
                    stockError = null,
                    mensajeFeedback = null
                )
            )
        }
    }

    fun onActivoChanged(nuevoActivo: Boolean) {
        _uiState.update {
            it.copy(formulario = it.formulario.copy(activo = nuevoActivo))
        }
    }

    /**
     * Guarda el producto (Crear con POST o Actualizar con PUT).
     */
    fun guardarProducto() {
        val form = _uiState.value.formulario

        // Validación local previa
        var errorNombre: String? = null
        var errorPrecio: String? = null
        var errorStock: String? = null

        if (form.nombre.isBlank()) {
            errorNombre = "El nombre es obligatorio."
        }
        val precioNum = form.precio.toDoubleOrNull()
        if (precioNum == null) {
            errorPrecio = "Ingrese un precio numérico."
        } else if (precioNum <= 0.0) {
            errorPrecio = "El precio debe ser mayor que cero."
        }

        val stockNum = form.stock.toIntOrNull()
        if (stockNum == null) {
            errorStock = "Ingrese un stock entero."
        } else if (stockNum < 0) {
            errorStock = "El stock no puede ser negativo."
        }

        if (errorNombre != null || errorPrecio != null || errorStock != null) {
            _uiState.update {
                it.copy(
                    formulario = it.formulario.copy(
                        nombreError = errorNombre,
                        precioError = errorPrecio,
                        stockError = errorStock
                    )
                )
            }
            return
        }

        viewModelScope.launch {
            val tipoOp = if (form.esEdicion) Operacion.Tipo.Actualizar else Operacion.Tipo.Crear
            _uiState.update {
                it.copy(
                    operacion = Operacion.EnCurso(tipoOp),
                    formulario = it.formulario.copy(estaGuardando = true, mensajeFeedback = null)
                )
            }

            val resultado = if (form.esEdicion) {
                actualizarProductoUseCase(
                    id = form.id ?: 0L,
                    nombre = form.nombre,
                    precio = form.precio,
                    stock = form.stock,
                    activo = form.activo,
                    imagen = form.imagen
                )
            } else {
                registrarProductoUseCase(
                    nombre = form.nombre,
                    precio = form.precio,
                    stock = form.stock,
                    activo = form.activo
                )
            }

            resultado.onSuccess { productoGuardado ->
                // Recargar catálogo para sincronizar con el backend
                cargarProductos()
                _uiState.update {
                    it.copy(
                        operacion = Operacion.Inactiva,
                        formulario = FormularioProductoState(),
                        mostrarFormulario = false,
                        mensajeExito = if (form.esEdicion) {
                            "Producto '${productoGuardado.nombre}' actualizado con éxito."
                        } else {
                            "Producto '${productoGuardado.nombre}' registrado con éxito."
                        }
                    )
                }
            }.onFailure { fallo ->
                manejarFallo(fallo)
            }
        }
    }

    fun solicitarEliminacion(producto: Producto) {
        _uiState.update { it.copy(productoAEliminar = producto) }
    }

    fun cancelarEliminacion() {
        _uiState.update { it.copy(productoAEliminar = null) }
    }

    fun confirmarEliminacion() {
        val producto = _uiState.value.productoAEliminar ?: return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    operacion = Operacion.EnCurso(Operacion.Tipo.Eliminar),
                    productoAEliminar = null
                )
            }

            eliminarProductoUseCase(producto.id)
                .onSuccess {
                    cargarProductos()
                    _uiState.update {
                        it.copy(
                            operacion = Operacion.Inactiva,
                            mensajeExito = "Producto '${producto.nombre}' eliminado correctamente."
                        )
                    }
                }
                .onFailure { fallo ->
                    manejarFallo(fallo)
                }
        }
    }

    fun descartarMensajes() {
        _uiState.update {
            it.copy(
                mensajeExito = null,
                operacion = if (it.operacion is Operacion.Fallida) Operacion.Inactiva else it.operacion
            )
        }
    }

    private fun manejarFallo(fallo: Throwable) {
        val error = (fallo as? ErrorApiException)?.error
        when (error) {
            is ErrorApi.Validacion -> {
                _uiState.update {
                    it.copy(
                        operacion = Operacion.Inactiva,
                        formulario = it.formulario.copy(
                            estaGuardando = false,
                            nombreError = error.porCampo["nombre"],
                            precioError = error.porCampo["precio"],
                            stockError = error.porCampo["stock"]
                        )
                    )
                }
            }
            else -> {
                val mensaje = mensajeDe(error, fallo)
                _uiState.update {
                    it.copy(
                        operacion = Operacion.Fallida(mensaje),
                        formulario = it.formulario.copy(estaGuardando = false)
                    )
                }
            }
        }
    }

    private fun mensajeDe(errorApi: ErrorApi?, fallo: Throwable): String = when (errorApi) {
        is ErrorApi.NoEncontrado -> "El recurso solicitado no fue encontrado (404)."
        is ErrorApi.Conflicto -> errorApi.mensaje
        is ErrorApi.Servidor -> "Error interno del servidor (500). Intente más tarde."
        is ErrorApi.SinConexion -> "Sin conexión a internet. Verifique su red."
        is ErrorApi.TiempoAgotado -> "Tiempo de espera agotado al conectar con el servidor."
        is ErrorApi.Validacion -> "Revise los campos con errores en el formulario."
        null -> fallo.message ?: "Ocurrió un error inesperado al procesar la solicitud."
    }

    private fun actualizarFaseYContadores() {
        val activos = inventarioCompleto.filter { it.activo }
        val inactivos = inventarioCompleto.filter { !it.activo }
        val bajoStock = inventarioCompleto.filter { it.requiereReposicion() }

        val productosPorTab = when (_uiState.value.tabSeleccionada) {
            0 -> activos
            1 -> inactivos
            2 -> bajoStock
            else -> inventarioCompleto
        }

        val query = _uiState.value.busqueda.trim()
        val productosFiltrados = if (query.isEmpty()) {
            productosPorTab
        } else {
            productosPorTab.filter { it.nombre.contains(query, ignoreCase = true) }
        }

        val fase = if (productosFiltrados.isEmpty()) {
            FaseInventario.SinProductos
        } else {
            FaseInventario.ConProductos(productosFiltrados)
        }

        _uiState.update {
            it.copy(
                fase = fase,
                totalActivos = activos.size,
                totalInactivos = inactivos.size,
                totalBajoStock = bajoStock.size
            )
        }
    }
}
