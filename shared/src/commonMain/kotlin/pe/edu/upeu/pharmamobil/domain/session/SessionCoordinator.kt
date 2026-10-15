package pe.edu.upeu.pharmamobil.domain.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import pe.edu.upeu.pharmamobil.domain.model.Sesion

/**
 * Estados observables de la sesión de usuario en la aplicación.
 */
sealed interface SesionEstado {
    data object NoAutenticado : SesionEstado
    data class Autenticado(val sesion: Sesion) : SesionEstado
    data class Expirado(val mensaje: String) : SesionEstado
}

/**
 * Coordinador de estado reactivo puro en la capa de dominio.
 * No posee dependencias hacia la capa de datos (Clean Architecture).
 */
class SessionCoordinator {
    private val _estadoSesion = MutableStateFlow<SesionEstado>(SesionEstado.NoAutenticado)
    val estadoSesion: StateFlow<SesionEstado> = _estadoSesion.asStateFlow()

    fun notificarSesionIniciada(sesion: Sesion) {
        _estadoSesion.value = SesionEstado.Autenticado(sesion)
    }

    fun notificarSesionExpirada(mensaje: String = "Su sesión ha expirado. Ingrese sus credenciales nuevamente.") {
        _estadoSesion.value = SesionEstado.Expirado(mensaje)
    }

    fun notificarSesionCerrada() {
        _estadoSesion.value = SesionEstado.NoAutenticado
    }
}
