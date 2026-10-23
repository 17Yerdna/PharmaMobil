package pe.edu.upeu.pharmamobil.presentation.sesion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import kotlinx.coroutines.Job
import pe.edu.upeu.pharmamobil.data.local.AlmacenSeguro
import pe.edu.upeu.pharmamobil.domain.error.ErrorSesion
import pe.edu.upeu.pharmamobil.domain.error.ErrorSesionException
import pe.edu.upeu.pharmamobil.domain.model.Credenciales
import pe.edu.upeu.pharmamobil.domain.session.SesionEstado
import pe.edu.upeu.pharmamobil.domain.session.SessionCoordinator
import pe.edu.upeu.pharmamobil.domain.usecase.CerrarSesionUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.IniciarSesionUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.SesionGuardadaUseCase
import pe.edu.upeu.pharmamobil.platform.PlatformConfig

/**
 * ViewModel que gestiona el ciclo de vida del formulario de Login y la restauración de sesión.
 */
class SesionViewModel(
    private val iniciarSesionUseCase: IniciarSesionUseCase,
    private val cerrarSesionUseCase: CerrarSesionUseCase,
    private val sesionGuardadaUseCase: SesionGuardadaUseCase,
    private val sessionCoordinator: SessionCoordinator,
    private val almacenSeguro: AlmacenSeguro? = null,
    private val httpClient: HttpClient? = null,
    private val platformConfig: PlatformConfig? = null
) : ViewModel() {

    val esDepuracion: Boolean
        get() = platformConfig?.esDepuracion ?: false

    private val _uiState = MutableStateFlow(SesionUiState())
    val uiState: StateFlow<SesionUiState> = _uiState.asStateFlow()

    init {
        restaurarSesion()
    }

    fun onUsuarioChanged(nuevoUsuario: String) {
        _uiState.update { it.copy(usuario = nuevoUsuario, mensajeError = null) }
    }

    fun onContraseniaChanged(nuevaContrasenia: String) {
        _uiState.update { it.copy(contrasenia = nuevaContrasenia, mensajeError = null) }
    }

    fun toggleContraseniaVisible() {
        _uiState.update { it.copy(contraseniaVisible = !it.contraseniaVisible) }
    }

    fun limpiarError() {
        _uiState.update { it.copy(mensajeError = null) }
    }

    fun seleccionarUsuarioDebug(usuario: String) {
        _uiState.update {
            it.copy(
                usuario = usuario,
                contrasenia = "",
                mensajeError = null
            )
        }
    }

    /**
     * Restaura la sesión persistida en el almacén seguro llamando a GET /auth/me.
     * Si el interceptor 401 puso el coordinador en Expirado, preserva ese estado.
     */
    fun restaurarSesion() {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true) }
            val sesion = sesionGuardadaUseCase()
            if (sesion != null) {
                if (sesion.esOffline) {
                    sessionCoordinator.notificarSesionOffline(sesion)
                } else {
                    sessionCoordinator.notificarSesionIniciada(sesion)
                }
            } else {
                if (sessionCoordinator.estadoSesion.value !is SesionEstado.Expirado) {
                    sessionCoordinator.notificarSesionCerrada()
                }
            }
            _uiState.update { it.copy(cargando = false) }
        }
    }

    /**
     * Inicia sesión contra POST /api/v1/auth/login.
     */
    fun iniciarSesion() {
        val usuario = _uiState.value.usuario.trim()
        val contrasenia = _uiState.value.contrasenia

        if (usuario.isBlank() || contrasenia.isBlank()) {
            _uiState.update { it.copy(mensajeError = "Ingrese su usuario y contraseña.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, mensajeError = null) }
            try {
                val sesion = iniciarSesionUseCase(Credenciales(usuario, contrasenia))
                sessionCoordinator.notificarSesionIniciada(sesion)
                _uiState.update {
                    it.copy(
                        cargando = false,
                        contrasenia = "",
                        mensajeError = null
                    )
                }
            } catch (e: ErrorSesionException) {
                val mensaje = when (e.error) {
                    is ErrorSesion.CredencialesInvalidas -> "Usuario o contraseña incorrectos."
                    is ErrorSesion.SesionExpirada -> "Tu sesión ha expirado. Ingresa tus credenciales nuevamente."
                    is ErrorSesion.SinPermiso -> "No cuenta con permisos para acceder al sistema."
                    is ErrorSesion.SinConexion -> "Sin conexión al servidor. Verifique su red."
                    is ErrorSesion.ErrorDesconocido -> e.error.mensaje
                }
                _uiState.update { it.copy(cargando = false, mensajeError = mensaje) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        cargando = false,
                        mensajeError = e.message ?: "Error inesperado al iniciar sesión."
                    )
                }
            }
        }
    }

    /**
     * Cierra la sesión activa eliminando tokens y notificando al coordinador.
     * Limpia completamente los campos de usuario y contraseña para cumplir con el Efecto 3 de logout.
     */
    fun cerrarSesion(): Job = viewModelScope.launch {
        _uiState.update { it.copy(cargando = true) }
        cerrarSesionUseCase()
        sessionCoordinator.notificarSesionCerrada()
        _uiState.update {
            it.copy(
                cargando = false,
                usuario = "",
                contrasenia = "",
                mensajeError = null
            )
        }
    }

    /**
     * Utilidad exclusiva de depuración (Escenario E3):
     * Muta de forma controlada la firma criptográfica del JWT almacenado y dispara una petición
     * autenticada para validar que el backend rechaza con 401 y la app redirige a Login.
     * Solo opera cuando [esDepuracion] es true; en release retorna de inmediato sin efecto alguno.
     */
    fun corromperTokenParaPruebaDebug() {
        if (!esDepuracion) return
        val tokenActual = almacenSeguro?.leerToken() ?: return
        val tokenCorrupto = if (tokenActual.endsWith("x")) tokenActual.dropLast(1) + "y" else tokenActual.dropLast(1) + "x"
        almacenSeguro.guardarToken(tokenCorrupto)
        viewModelScope.launch {
            try {
                httpClient?.get("/api/v1/auth/me")
            } catch (_: Exception) {
                // El 401 es capturado por HttpResponseValidator en HttpClientFactory
            }
        }
    }

    /**
     * Utilidad exclusiva de depuración (Efecto 2 de Logout):
     * Aguarda explícitamente a que culmine cerrarSesion() antes de disparar una petición de prueba,
     * garantizando que viaje sin Authorization y reciba 401 sin condiciones de carrera.
     * Solo opera cuando [esDepuracion] es true; en release retorna de inmediato sin efecto alguno.
     */
    fun probarPeticionSinTokenPostLogout(onResultado: (Int) -> Unit = {}) {
        if (!esDepuracion) return
        viewModelScope.launch {
            val job = cerrarSesion()
            job.join()
            try {
                val respuesta = httpClient?.get("/api/v1/productos")
                if (respuesta != null) {
                    onResultado(respuesta.status.value)
                }
            } catch (_: Exception) {
                onResultado(401)
            }
        }
    }
}
