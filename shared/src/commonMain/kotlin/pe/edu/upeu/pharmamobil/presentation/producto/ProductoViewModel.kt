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
import pe.edu.upeu.pharmamobil.domain.platform.Compartidor
import pe.edu.upeu.pharmamobil.domain.session.SesionEstado
import pe.edu.upeu.pharmamobil.domain.session.SessionCoordinator
import pe.edu.upeu.pharmamobil.domain.usecase.ActualizarProductoUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.EliminarProductoUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.ObservarProductosUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.RegistrarProductoUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.RevalidarSesionUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.SincronizarProductosUseCase
import pe.edu.upeu.pharmamobil.platform.PlatformConfig
import pe.edu.upeu.pharmamobil.platform.ProveedorHora

/**
 * ViewModel multiplataforma para el módulo de Productos con persistencia Offline-First.
 * Se alimenta de forma reactiva exclusivamente desde SQLite a través de [ObservarProductosUseCase]
 * y sincroniza atómicamente con el backend mediante [SincronizarProductosUseCase].
 */
class ProductoViewModel(
    private val observarProductosUseCase: ObservarProductosUseCase,
    private val sincronizarProductosUseCase: SincronizarProductosUseCase,
    private val registrarProductoUseCase: RegistrarProductoUseCase,
    private val actualizarProductoUseCase: ActualizarProductoUseCase,
    private val eliminarProductoUseCase: EliminarProductoUseCase,
    private val compartidor: Compartidor,
    private val platformConfig: PlatformConfig,
    private val sessionCoordinator: SessionCoordinator = SessionCoordinator(),
    private val revalidarSesionUseCase: RevalidarSesionUseCase? = null,
    private val proveedorHora: ProveedorHora? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductoUiState(esDepuracion = platformConfig.esDepuracion))
    val uiState: StateFlow<ProductoUiState> = _uiState.asStateFlow()

    private var inventarioCompleto: List<Producto> = emptyList()

    init {
        observarCatalogoLocal()
        actualizarTextoAntiguedad()
        if (sessionCoordinator.estadoSesion.value !is SesionEstado.OfflineSoloLectura) {
            sincronizar()
        }
    }

    /**
     * Observa continuamente la base de datos local SQLite (Single Source of Truth).
     * Cada inserción, actualización o sincronización remota emite aquí de forma automática.
     */
    private fun observarCatalogoLocal() {
        viewModelScope.launch {
            observarProductosUseCase().collect { productos ->
                inventarioCompleto = productos
                val pendientes = productos.count { it.id < 0L }
                _uiState.update { it.copy(totalPendientes = pendientes) }
                actualizarFaseYContadores()
            }
        }
    }

    /**
     * Dispara la sincronización con el servidor:
     * sube los pendientes acumulados, descarga el catálogo remoto y actualiza la hora de sincronización.
     * No silencia los errores de conectividad para poder activar el indicador [avisoRed].
     */
    fun sincronizar() {
        viewModelScope.launch {
            if (sessionCoordinator.estadoSesion.value is SesionEstado.OfflineSoloLectura) {
                _uiState.update { it.copy(estaSincronizando = true) }
                val revalidador = revalidarSesionUseCase
                if (revalidador != null) {
                    val resultado = revalidador()
                    if (resultado.isFailure) {
                        _uiState.update { it.copy(estaSincronizando = false) }
                        return@launch
                    }
                }
            }

            _uiState.update { it.copy(estaSincronizando = true) }
            val resultado = sincronizarProductosUseCase()
            resultado.onSuccess {
                actualizarTextoAntiguedad()
                _uiState.update {
                    it.copy(
                        estaSincronizando = false,
                        avisoRed = false
                    )
                }
            }.onFailure { fallo ->
                val errorApi = (fallo as? ErrorApiException)?.error
                val esFalloRed = errorApi is ErrorApi.SinConexion || errorApi is ErrorApi.TiempoAgotado
                _uiState.update {
                    it.copy(
                        estaSincronizando = false,
                        avisoRed = esFalloRed
                    )
                }
            }
        }
    }

    fun calcularAntiguedad(timestampMillis: Long?, ahoraMillis: Long): String {
        if (timestampMillis == null || timestampMillis <= 0L) {
            return "Nunca se ha sincronizado"
        }
        val diferencia = (ahoraMillis - timestampMillis).coerceAtLeast(0L)
        val minutos = diferencia / 60_000L
        val horas = minutos / 60L
        val dias = horas / 24L

        val relativo = when {
            dias >= 1L -> "hace más de un día"
            horas >= 1L -> if (horas == 1L) "hace 1 hora" else "hace $horas horas"
            minutos >= 1L -> if (minutos == 1L) "hace 1 minuto" else "hace $minutos minutos"
            else -> "hace unos momentos"
        }
        return "Actualizado $relativo"
    }

    fun actualizarTextoAntiguedad() {
        val timestamp = sincronizarProductosUseCase.obtenerUltimoSyncTimestamp()
        val ahora = proveedorHora?.ahoraMillis() ?: 0L
        val texto = calcularAntiguedad(timestamp, ahora)
        _uiState.update { it.copy(ultimaSincronizacion = texto) }
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
        if (producto.id < 0L) {
            _uiState.update {
                it.copy(operacion = Operacion.Fallida("No se puede editar un producto pendiente de sincronización."))
            }
            return
        }
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

    fun mostrarDetalle(producto: ProductoUi) {
        _uiState.update { it.copy(productoEnDetalle = producto) }
    }

    fun cerrarDetalle() {
        _uiState.update { it.copy(productoEnDetalle = null) }
    }

    fun compartirProducto(producto: Producto) {
        compartidor.compartir(producto.comoTextoParaCompartir())
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
     * Guarda el producto. Si no hay conexión a red, el alta se guarda localmente en SQLite
     * con ID negativo temporal y queda pendiente de sincronización.
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
                // No se invoca cargarProductos(); el Flow de SQLite actualizará la UI reactivamente
                _uiState.update {
                    it.copy(
                        operacion = Operacion.Inactiva,
                        formulario = FormularioProductoState(),
                        mostrarFormulario = false,
                        mensajeExito = if (productoGuardado.id < 0L) {
                            "Producto '${productoGuardado.nombre}' guardado localmente; pendiente de sincronización."
                        } else if (form.esEdicion) {
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
        if (producto.id < 0L) {
            _uiState.update {
                it.copy(operacion = Operacion.Fallida("No se puede eliminar un producto pendiente de sincronización."))
            }
            return
        }
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

    /**
     * Intenta registrar un producto para verificar el control de acceso (RBAC) en el backend.
     * Si el usuario autenticado carece del permiso CATALOGO_ESCRIBIR (e.g. vendedor), el backend responde 403 Forbidden.
     * Solo disponible en modo depuración (BuildConfig.DEBUG).
     */
    fun simularOperacion403() {
        if (!platformConfig.esDepuracion) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    operacion = Operacion.EnCurso(Operacion.Tipo.Crear),
                    formulario = it.formulario.copy(estaGuardando = true)
                )
            }
            val resultado = registrarProductoUseCase(
                nombre = "Prueba Permisos 403",
                precio = "99.90",
                stock = "5",
                activo = true
            )
            resultado.onSuccess { productoCreado ->
                _uiState.update {
                    it.copy(
                        operacion = Operacion.Inactiva,
                        formulario = it.formulario.copy(estaGuardando = false),
                        mensajeExito = "Producto '${productoCreado.nombre}' registrado con éxito."
                    )
                }
            }.onFailure { fallo ->
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
        is ErrorApi.NoAutorizado -> "Sesión no válida o expirada (401)."
        is ErrorApi.SinPermiso -> "No cuenta con permisos para realizar esta acción (403)."
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
            FaseInventario.ConProductos(productosFiltrados.map(::ProductoUi))
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
