package pe.edu.upeu.pharmamobil.domain.usecase

import pe.edu.upeu.pharmamobil.domain.error.ErrorSesion
import pe.edu.upeu.pharmamobil.domain.error.ErrorSesionException
import pe.edu.upeu.pharmamobil.domain.model.Sesion
import pe.edu.upeu.pharmamobil.domain.repository.AuthRepository
import pe.edu.upeu.pharmamobil.domain.session.SessionCoordinator

class RevalidarSesionUseCase(
    private val authRepository: AuthRepository,
    private val sessionCoordinator: SessionCoordinator
) {
    suspend operator fun invoke(): Result<Sesion> = runCatching {
        val sesion = authRepository.revalidarSesion()
        sessionCoordinator.notificarSesionIniciada(sesion)
        sesion
    }.onFailure { excepcion ->
        val errorSesion = (excepcion as? ErrorSesionException)?.error
        if (errorSesion is ErrorSesion.SesionExpirada || errorSesion is ErrorSesion.SinPermiso) {
            authRepository.cerrarSesion()
            sessionCoordinator.notificarSesionExpirada("Su sesión ha expirado. Ingrese sus credenciales nuevamente.")
        }
    }
}
