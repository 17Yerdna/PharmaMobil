package pe.edu.upeu.pharmamobil.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import pe.edu.upeu.pharmamobil.data.local.AlmacenSeguro
import pe.edu.upeu.pharmamobil.domain.error.ErrorSesion
import pe.edu.upeu.pharmamobil.domain.error.ErrorSesionException
import pe.edu.upeu.pharmamobil.domain.model.Credenciales
import pe.edu.upeu.pharmamobil.domain.model.Sesion
import pe.edu.upeu.pharmamobil.domain.repository.AuthRepository
import pe.edu.upeu.pharmamobil.domain.session.SesionEstado
import pe.edu.upeu.pharmamobil.domain.session.SessionCoordinator
import pe.edu.upeu.pharmamobil.domain.usecase.CerrarSesionUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.IniciarSesionUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.SesionGuardadaUseCase
import pe.edu.upeu.pharmamobil.platform.PlatformConfig
import pe.edu.upeu.pharmamobil.presentation.sesion.SesionViewModel
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SesionViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeAuthRepository(
        var sesionGuardadaRetorno: Sesion? = null,
        var sesionLoginRetorno: Sesion? = null,
        var excepcionLogin: ErrorSesionException? = null
    ) : AuthRepository {
        var logoutEjecutado = false

        override suspend fun iniciarSesion(credenciales: Credenciales): Sesion {
            excepcionLogin?.let { throw it }
            return sesionLoginRetorno ?: throw ErrorSesionException(ErrorSesion.CredencialesInvalidas)
        }

        override suspend fun cerrarSesion() {
            logoutEjecutado = true
            sesionGuardadaRetorno = null
        }

        override suspend fun sesionGuardada(): Sesion? = sesionGuardadaRetorno
    }

    private class FakeAlmacenSeguro(var tokenAlmacenado: String? = null) : AlmacenSeguro {
        override fun guardarToken(token: String) { tokenAlmacenado = token }
        override fun leerToken(): String? = tokenAlmacenado
        override fun borrarToken() { tokenAlmacenado = null }
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun iniciarSesion_conCredencialesValidas_notificaSesionAutenticada() = runTest {
        val repo = FakeAuthRepository(
            sesionLoginRetorno = Sesion(
                token = "jwt.fake.admin",
                usuario = "admin",
                nombreCompleto = "Administrador",
                permisos = setOf("CATALOGO_ESCRIBIR")
            )
        )
        val coordinator = SessionCoordinator()
        val viewModel = SesionViewModel(
            iniciarSesionUseCase = IniciarSesionUseCase(repo),
            cerrarSesionUseCase = CerrarSesionUseCase(repo),
            sesionGuardadaUseCase = SesionGuardadaUseCase(repo),
            sessionCoordinator = coordinator
        )

        advanceUntilIdle()

        viewModel.onUsuarioChanged("admin")
        viewModel.onContraseniaChanged("Admin123")
        viewModel.iniciarSesion()

        advanceUntilIdle()

        val estado = coordinator.estadoSesion.value
        assertTrue(estado is SesionEstado.Autenticado)
        assertEquals("admin", estado.sesion.usuario)
        assertEquals(false, viewModel.uiState.value.cargando)
        assertEquals(null, viewModel.uiState.value.mensajeError)
    }

    @Test
    fun iniciarSesion_conCredencialesInvalidas_muestraMensajeErrorYPermaneceNoAutenticado() = runTest {
        val repo = FakeAuthRepository(
            excepcionLogin = ErrorSesionException(ErrorSesion.CredencialesInvalidas)
        )
        val coordinator = SessionCoordinator()
        val viewModel = SesionViewModel(
            iniciarSesionUseCase = IniciarSesionUseCase(repo),
            cerrarSesionUseCase = CerrarSesionUseCase(repo),
            sesionGuardadaUseCase = SesionGuardadaUseCase(repo),
            sessionCoordinator = coordinator
        )

        advanceUntilIdle()

        viewModel.onUsuarioChanged("admin")
        viewModel.onContraseniaChanged("claveIncorrecta")
        viewModel.iniciarSesion()

        advanceUntilIdle()

        assertTrue(coordinator.estadoSesion.value is SesionEstado.NoAutenticado)
        assertEquals("Usuario o contraseña incorrectos.", viewModel.uiState.value.mensajeError)
        assertFalse(viewModel.uiState.value.cargando)
    }

    @Test
    fun cerrarSesion_limpiaAlmacenYNotificaCoordinador() = runTest {
        val sesionInicial = Sesion(
            token = "jwt.activo",
            usuario = "vendedor",
            nombreCompleto = "Vendedor Test",
            permisos = setOf("CATALOGO_LEER")
        )
        val repo = FakeAuthRepository(sesionGuardadaRetorno = sesionInicial)
        val coordinator = SessionCoordinator()
        val viewModel = SesionViewModel(
            iniciarSesionUseCase = IniciarSesionUseCase(repo),
            cerrarSesionUseCase = CerrarSesionUseCase(repo),
            sesionGuardadaUseCase = SesionGuardadaUseCase(repo),
            sessionCoordinator = coordinator
        )

        advanceUntilIdle()
        assertTrue(coordinator.estadoSesion.value is SesionEstado.Autenticado)

        viewModel.onUsuarioChanged("vendedor")
        viewModel.onContraseniaChanged("Vendedor123")

        viewModel.cerrarSesion()
        advanceUntilIdle()

        assertTrue(coordinator.estadoSesion.value is SesionEstado.NoAutenticado)
        assertTrue(repo.logoutEjecutado)
        assertTrue(viewModel.uiState.value.usuario.isEmpty(), "El campo usuario debe quedar vacío tras logout")
        assertTrue(viewModel.uiState.value.contrasenia.isEmpty(), "El campo contraseña debe quedar vacío tras logout")
    }

    @Test
    fun restaurarSesion_siCoordinadorYaEstaEnExpirado_noSobreescribeConNoAutenticado() = runTest {
        val repo = FakeAuthRepository(sesionGuardadaRetorno = null)
        val coordinator = SessionCoordinator()

        // El interceptor 401 ya marcó el estado como Expirado
        coordinator.notificarSesionExpirada("Sesión expirada")

        val viewModel = SesionViewModel(
            iniciarSesionUseCase = IniciarSesionUseCase(repo),
            cerrarSesionUseCase = CerrarSesionUseCase(repo),
            sesionGuardadaUseCase = SesionGuardadaUseCase(repo),
            sessionCoordinator = coordinator
        )

        advanceUntilIdle()

        val estadoFinal = coordinator.estadoSesion.value
        assertTrue(estadoFinal is SesionEstado.Expirado, "No debe reemplazar Expirado por NoAutenticado")
    }

    @Test
    fun sesionViewModel_iniciarSesionFallaEnAuthMe_mantieneSesionNoValidaYReflejaError() = runTest {
        val repo = FakeAuthRepository(
            excepcionLogin = ErrorSesionException(ErrorSesion.SesionExpirada)
        )
        val coordinator = SessionCoordinator()
        val viewModel = SesionViewModel(
            iniciarSesionUseCase = IniciarSesionUseCase(repo),
            cerrarSesionUseCase = CerrarSesionUseCase(repo),
            sesionGuardadaUseCase = SesionGuardadaUseCase(repo),
            sessionCoordinator = coordinator
        )

        advanceUntilIdle()

        viewModel.onUsuarioChanged("admin")
        viewModel.onContraseniaChanged("Admin123")
        viewModel.iniciarSesion()

        advanceUntilIdle()

        assertFalse(coordinator.estadoSesion.value is SesionEstado.Autenticado, "El estado no debe ser Autenticado")
        assertFalse(viewModel.uiState.value.cargando, "El estado cargando debe ser falso")
        assertNotNull(viewModel.uiState.value.mensajeError, "Debe existir un mensaje de error visible")
    }

    @Test
    fun corromperTokenParaPruebaDebug_enModoRelease_noModificaToken() = runTest {
        val repo = FakeAuthRepository()
        val coordinator = SessionCoordinator()
        val almacen = FakeAlmacenSeguro(tokenAlmacenado = "jwt.token.valido.sig123x")
        val configRelease = PlatformConfig(urlBase = "http://localhost:8080/api/v1/", esDepuracion = false)

        val viewModel = SesionViewModel(
            iniciarSesionUseCase = IniciarSesionUseCase(repo),
            cerrarSesionUseCase = CerrarSesionUseCase(repo),
            sesionGuardadaUseCase = SesionGuardadaUseCase(repo),
            sessionCoordinator = coordinator,
            almacenSeguro = almacen,
            platformConfig = configRelease
        )

        assertFalse(viewModel.esDepuracion, "esDepuracion debe ser false en release")
        viewModel.corromperTokenParaPruebaDebug()
        advanceUntilIdle()

        assertEquals("jwt.token.valido.sig123x", almacen.leerToken(), "El token no debe mutar en release")
    }

    @Test
    fun corromperTokenParaPruebaDebug_enModoDebug_mutaFirmaDeToken() = runTest {
        val repo = FakeAuthRepository()
        val coordinator = SessionCoordinator()
        val almacen = FakeAlmacenSeguro(tokenAlmacenado = "jwt.token.valido.sig123x")
        val configDebug = PlatformConfig(urlBase = "http://localhost:8080/api/v1/", esDepuracion = true)

        val viewModel = SesionViewModel(
            iniciarSesionUseCase = IniciarSesionUseCase(repo),
            cerrarSesionUseCase = CerrarSesionUseCase(repo),
            sesionGuardadaUseCase = SesionGuardadaUseCase(repo),
            sessionCoordinator = coordinator,
            almacenSeguro = almacen,
            platformConfig = configDebug
        )

        assertTrue(viewModel.esDepuracion, "esDepuracion debe ser true en debug")
        viewModel.corromperTokenParaPruebaDebug()
        advanceUntilIdle()

        assertEquals("jwt.token.valido.sig123y", almacen.leerToken(), "El token debe haber sido mutado de forma controlada en debug")
    }
}


