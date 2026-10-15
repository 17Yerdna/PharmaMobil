package pe.edu.upeu.pharmamobil.presentation.producto

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository
import pe.edu.upeu.pharmamobil.domain.usecase.RegistrarProductoUseCase

/**
 * ViewModel multiplataforma para el módulo de Productos.
 * Gestiona el flujo unidireccional de datos (UDF) hacia la UI.
 */
class ProductoViewModel(
    private val registrarProductoUseCase: RegistrarProductoUseCase,
    private val productoRepository: ProductoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductoUiState())
    val uiState: StateFlow<ProductoUiState> = _uiState.asStateFlow()

    private var inventarioCompleto: List<Producto> = emptyList()

    init {
        cargarProductos()
    }

    fun cargarProductos() {
        viewModelScope.launch {
            _uiState.update { it.copy(fase = FaseInventario.Cargando) }
            try {
                inventarioCompleto = productoRepository.listar()
                actualizarFaseYContadores()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(fase = FaseInventario.Error(e.message ?: "Error desconocido al cargar el inventario"))
                }
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
        _uiState.update { it.copy(mostrarFormulario = !it.mostrarFormulario) }
    }

    fun onNombreChanged(nuevoNombre: String) {
        _uiState.update {
            it.copy(
                formulario = it.formulario.copy(
                    nombre = nuevoNombre,
                    errorNombre = null,
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
                    errorPrecio = null,
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
                    errorStock = null,
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

    fun registrarProducto() {
        val form = _uiState.value.formulario

        // Validación previa de campos para retroalimentación inmediata en UI
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
                        errorNombre = errorNombre,
                        errorPrecio = errorPrecio,
                        errorStock = errorStock
                    )
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(formulario = it.formulario.copy(estaGuardando = true, mensajeFeedback = null))
            }

            val resultado = registrarProductoUseCase(
                nombre = form.nombre,
                precio = form.precio,
                stock = form.stock,
                activo = form.activo
            )

            resultado.onSuccess { nuevoProducto ->
                inventarioCompleto = listOf(nuevoProducto) + inventarioCompleto
                _uiState.update {
                    it.copy(
                        formulario = FormularioProductoState(
                            mensajeFeedback = "¡Registro exitoso! Producto: ${nuevoProducto.nombre} creado correctamente."
                        ),
                        mostrarFormulario = false
                    )
                }
                actualizarFaseYContadores()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        formulario = it.formulario.copy(
                            estaGuardando = false,
                            mensajeFeedback = error.message ?: "Error al registrar el producto"
                        )
                    )
                }
            }
        }
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
