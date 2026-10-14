package pe.edu.upeu.pharmamobil.auth

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import pe.edu.upeu.pharmamobil.data.local.AlmacenSeguro
import pe.edu.upeu.pharmamobil.data.remote.crearHttpClient
import pe.edu.upeu.pharmamobil.data.repository.AuthRepositorioRest
import pe.edu.upeu.pharmamobil.domain.error.ErrorSesion
import pe.edu.upeu.pharmamobil.domain.error.ErrorSesionException
import pe.edu.upeu.pharmamobil.domain.model.Credenciales
import pe.edu.upeu.pharmamobil.domain.model.Sesion
import pe.edu.upeu.pharmamobil.domain.session.SesionEstado
import pe.edu.upeu.pharmamobil.domain.session.SessionCoordinator
import pe.edu.upeu.pharmamobil.platform.PlatformConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pruebas unitarias de integración HTTP y autenticación con MockEngine.
 *
 * Estructurado en dos grupos de responsabilidad:
 * - Grupo A: Pruebas de HttpClientFactory (intercepción Bearer, 401 Expirado, 403 Conservado).
 * - Grupo B: Pruebas de AuthRepositorioRest (ciclo de vida login, /auth/me, resiliencia y sesionGuardada).
 */
class ClienteHttpAuthMockTest {

    private class FakeAlmacenSeguro(private var tokenMemoria: String? = null) : AlmacenSeguro {
        override fun guardarToken(token: String) { tokenMemoria = token }
        override fun leerToken(): String? = tokenMemoria
        override fun borrarToken() { tokenMemoria = null }
    }

    private val configTest = PlatformConfig(
        esDepuracion = true,
        urlBase = "http://localhost:8080/api/v1/"
    )

    // =========================================================================
    // GRUPO A: PRUEBAS DE HttpClientFactory
    // =========================================================================

    @Test
    fun clienteHttp_incluyeBearerTokenEnRutasProtegidas() = runTest {
        val almacen = FakeAlmacenSeguro(tokenMemoria = "jwt.token.valido")
        val coordinator = SessionCoordinator()

        var authorizationHeaderEnviado: String? = null

        val mockEngine = MockEngine { request ->
            authorizationHeaderEnviado = request.headers[HttpHeaders.Authorization]
            respond(
                content = """[{"id":1,"nombre":"Amoxicilina","precio":15.0,"stock":20,"activo":true,"categoriaId":1}]""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val cliente = crearHttpClient(mockEngine, almacen, coordinator, configTest)

        val respuesta = cliente.get("http://localhost:8080/api/v1/productos")
        assertEquals(HttpStatusCode.OK, respuesta.status)
        assertEquals("Bearer jwt.token.valido", authorizationHeaderEnviado)
    }

    @Test
    fun clienteHttp_recibe401EnRutaProtegida_borraTokenYNotificaExpiracion() = runTest {
        val almacen = FakeAlmacenSeguro(tokenMemoria = "jwt.token.expirado")
        val coordinator = SessionCoordinator()

        val mockEngine = MockEngine { _ ->
            respond(
                content = """{"error":"Unauthorized","message":"Token expired"}""",
                status = HttpStatusCode.Unauthorized,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val cliente = crearHttpClient(mockEngine, almacen, coordinator, configTest)

        runCatching {
            cliente.get("http://localhost:8080/api/v1/productos")
        }

        assertNull(almacen.leerToken(), "El token debe haberse eliminado del almacén seguro tras un 401")
        assertTrue(coordinator.estadoSesion.value is SesionEstado.Expirado, "El SessionCoordinator debe encontrarse en estado Expirado")
    }

    @Test
    fun clienteHttp_recibe403EnRutaProtegida_conservaTokenYContinuaAutenticado() = runTest {
        val almacen = FakeAlmacenSeguro(tokenMemoria = "jwt.vendedor.activo")
        val coordinator = SessionCoordinator()
        val sesion = Sesion(
            token = "jwt.vendedor.activo",
            usuario = "vendedor",
            nombreCompleto = "Vendedor Farmacia",
            permisos = setOf("VENTAS_LEER", "VENTAS_REGISTRAR")
        )
        // Inicializar el coordinador en estado Autenticado
        coordinator.notificarSesionIniciada(sesion)

        val mockEngine = MockEngine { _ ->
            respond(
                content = """{"error":"Forbidden","message":"Acceso denegado a modificacion de catalogo"}""",
                status = HttpStatusCode.Forbidden,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val cliente = crearHttpClient(mockEngine, almacen, coordinator, configTest)

        runCatching {
            cliente.get("http://localhost:8080/api/v1/productos")
        }

        assertEquals("jwt.vendedor.activo", almacen.leerToken(), "El token debe conservarse intacto tras un 403")
        assertEquals(SesionEstado.Autenticado(sesion), coordinator.estadoSesion.value, "El SessionCoordinator debe conservar exactamente la sesión autenticada")
    }

    // =========================================================================
    // GRUPO B: PRUEBAS DE AuthRepositorioRest
    // =========================================================================

    @Test
    fun login_noEnviaHeaderAuthorization_aunSiExisteTokenEnAlmacen() = runTest {
        val almacen = FakeAlmacenSeguro(tokenMemoria = "token.anterior.residual")
        val coordinator = SessionCoordinator()
        var headerAuthEnviado: String? = "NO_EVALUADO"

        val mockEngine = MockEngine { request ->
            if (request.url.encodedPath.contains("auth/login")) {
                headerAuthEnviado = request.headers[HttpHeaders.Authorization]
                respond(
                    content = """{"token":"jwt.nuevo.token","tipo":"Bearer","expiraEnSegundos":3600,"username":"admin"}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            } else {
                respond(
                    content = """{"id":1,"username":"admin","estado":true,"cargo":"ADMINISTRADOR","roles":["ADMIN"],"permisos":["CATALOGO_ESCRIBIR"],"fechaCreacion":"2026-10-14T00:00:00"}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }
        }

        val cliente = crearHttpClient(mockEngine, almacen, coordinator, configTest)
        val repo = AuthRepositorioRest(cliente, almacen)

        repo.iniciarSesion(Credenciales("admin", "Admin123"))

        assertNull(headerAuthEnviado, "POST /auth/login no debe incluir el header Authorization")
    }

    @Test
    fun iniciarSesion_cuandoLoginFallaCon401_lanzaCredencialesInvalidas() = runTest {
        val almacen = FakeAlmacenSeguro(tokenMemoria = null)
        val coordinator = SessionCoordinator()
        var meConsultado = false

        val mockEngine = MockEngine { request ->
            if (request.url.encodedPath.contains("auth/login")) {
                respond(
                    content = """{"error":"Unauthorized","message":"Credenciales incorrectas"}""",
                    status = HttpStatusCode.Unauthorized,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            } else {
                meConsultado = true
                respond(content = "{}", status = HttpStatusCode.OK, headers = headersOf(HttpHeaders.ContentType, "application/json"))
            }
        }

        val cliente = crearHttpClient(mockEngine, almacen, coordinator, configTest)
        val repo = AuthRepositorioRest(cliente, almacen)

        val excepcion = assertFailsWith<ErrorSesionException> {
            repo.iniciarSesion(Credenciales("vendedor", "claveErronea"))
        }

        assertEquals(ErrorSesion.CredencialesInvalidas, excepcion.error)
        assertNull(almacen.leerToken(), "No debe almacenarse ningún token ante credenciales inválidas")
        assertEquals(false, meConsultado, "No debe invocarse /auth/me si login fue rechazado con 401")
    }

    @Test
    fun iniciarSesion_conCredencialesValidasYAuthMe200_retornaSesionYGuardaToken() = runTest {
        val almacen = FakeAlmacenSeguro(tokenMemoria = null)
        val coordinator = SessionCoordinator()

        val mockEngine = MockEngine { request ->
            if (request.url.encodedPath.contains("auth/login")) {
                respond(
                    content = """{"token":"jwt.firmado.vendedor","tipo":"Bearer","expiraEnSegundos":3600,"username":"vendedor","nombreCompleto":"Juan Perez","roles":["VENDEDOR"],"permisos":["VENTAS_LEER"]}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            } else {
                respond(
                    content = """{"id":2,"username":"vendedor","estado":true,"empleadoId":10,"empleadoNombre":"Juan Perez","cargo":"VENDEDOR_MOSTRADOR","roles":["VENDEDOR"],"permisos":["CATALOGO_LEER","VENTAS_REGISTRAR"],"fechaCreacion":"2026-10-14T08:00:00"}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }
        }

        val cliente = crearHttpClient(mockEngine, almacen, coordinator, configTest)
        val repo = AuthRepositorioRest(cliente, almacen)

        val sesion = repo.iniciarSesion(Credenciales("vendedor", "Vendedor123"))

        assertEquals("jwt.firmado.vendedor", sesion.token)
        assertEquals("vendedor", sesion.usuario)
        assertEquals("Juan Perez", sesion.nombreCompleto)
        assertEquals(setOf("CATALOGO_LEER", "VENTAS_REGISTRAR"), sesion.permisos)
        assertEquals("jwt.firmado.vendedor", almacen.leerToken(), "El token debe guardarse en AlmacenSeguro")
    }

    @Test
    fun iniciarSesion_cuandoAuthMeEsCancelado_conservaTokenYRelanzaCancellationException() = runTest {
        val almacen = FakeAlmacenSeguro(tokenMemoria = null)
        val coordinator = SessionCoordinator()

        val mockEngine = MockEngine { request ->
            if (request.url.encodedPath.contains("auth/login")) {
                respond(
                    content = """{"token":"jwt.token.conservable","tipo":"Bearer","expiraEnSegundos":3600,"username":"admin"}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            } else {
                throw CancellationException("Cancelación deliberada de coroutine en /auth/me")
            }
        }

        val cliente = crearHttpClient(mockEngine, almacen, coordinator, configTest)
        val repo = AuthRepositorioRest(cliente, almacen)

        assertFailsWith<CancellationException> {
            repo.iniciarSesion(Credenciales("admin", "Admin123"))
        }

        assertEquals("jwt.token.conservable", almacen.leerToken(), "CancellationException debe conservar el token en AlmacenSeguro")
    }

    @Test
    fun iniciarSesion_cuandoAuthMeArrojaTimeoutOErrorRed_purgaTokenYLanzaSinConexion() = runTest {
        val almacen = FakeAlmacenSeguro(tokenMemoria = null)
        val coordinator = SessionCoordinator()

        val mockEngine = MockEngine { request ->
            if (request.url.encodedPath.contains("auth/login")) {
                respond(
                    content = """{"token":"jwt.token.temporal","tipo":"Bearer","expiraEnSegundos":3600,"username":"admin"}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            } else {
                throw IOException("Fallo de socket / red inaccesible")
            }
        }

        val cliente = crearHttpClient(mockEngine, almacen, coordinator, configTest)
        val repo = AuthRepositorioRest(cliente, almacen)

        val excepcion = assertFailsWith<ErrorSesionException> {
            repo.iniciarSesion(Credenciales("admin", "Admin123"))
        }

        assertEquals(ErrorSesion.SinConexion, excepcion.error)
        assertNull(almacen.leerToken(), "El token debe purgarse si /auth/me falla por red")
    }

    @Test
    fun iniciarSesion_cuandoAuthMeRecibeJsonMalformado_purgaTokenYLanzaErrorDesconocido() = runTest {
        val almacen = FakeAlmacenSeguro(tokenMemoria = null)
        val coordinator = SessionCoordinator()

        val mockEngine = MockEngine { request ->
            if (request.url.encodedPath.contains("auth/login")) {
                respond(
                    content = """{"token":"jwt.token.temporal","tipo":"Bearer","expiraEnSegundos":3600,"username":"admin"}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            } else {
                respond(
                    content = """{ id: malformed, invalid json """,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }
        }

        val cliente = crearHttpClient(mockEngine, almacen, coordinator, configTest)
        val repo = AuthRepositorioRest(cliente, almacen)

        val excepcion = assertFailsWith<ErrorSesionException> {
            repo.iniciarSesion(Credenciales("admin", "Admin123"))
        }

        assertTrue(excepcion.error is ErrorSesion.ErrorDesconocido)
        assertNull(almacen.leerToken(), "El token debe purgarse si el payload de /auth/me es malformado")
    }

    @Test
    fun iniciarSesion_cuandoAuthMeFallaCon401_purgaTokenYLanzaSesionExpirada() = runTest {
        val almacen = FakeAlmacenSeguro(tokenMemoria = null)
        val coordinator = SessionCoordinator()

        val mockEngine = MockEngine { request ->
            if (request.url.encodedPath.contains("auth/login")) {
                respond(
                    content = """{"token":"jwt.token.invalido","tipo":"Bearer","expiraEnSegundos":3600,"username":"admin"}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            } else {
                respond(
                    content = """{"error":"Unauthorized","message":"Token expired or signature invalid"}""",
                    status = HttpStatusCode.Unauthorized,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }
        }

        val cliente = crearHttpClient(mockEngine, almacen, coordinator, configTest)
        val repo = AuthRepositorioRest(cliente, almacen)

        val excepcion = assertFailsWith<ErrorSesionException> {
            repo.iniciarSesion(Credenciales("admin", "Admin123"))
        }

        assertEquals(ErrorSesion.SesionExpirada, excepcion.error)
        assertNull(almacen.leerToken(), "El token debe purgarse si /auth/me responde 401")
    }

    @Test
    fun iniciarSesion_cuandoAuthMeFallaCon403_purgaTokenYLanzaSinPermiso() = runTest {
        val almacen = FakeAlmacenSeguro(tokenMemoria = null)
        val coordinator = SessionCoordinator()

        val mockEngine = MockEngine { request ->
            if (request.url.encodedPath.contains("auth/login")) {
                respond(
                    content = """{"token":"jwt.token.sinpermiso","tipo":"Bearer","expiraEnSegundos":3600,"username":"inactivo"}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            } else {
                respond(
                    content = """{"error":"Forbidden","message":"Acceso denegado a perfil"}""",
                    status = HttpStatusCode.Forbidden,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }
        }

        val cliente = crearHttpClient(mockEngine, almacen, coordinator, configTest)
        val repo = AuthRepositorioRest(cliente, almacen)

        val excepcion = assertFailsWith<ErrorSesionException> {
            repo.iniciarSesion(Credenciales("inactivo", "Clave123"))
        }

        assertEquals(ErrorSesion.SinPermiso, excepcion.error)
        assertNull(almacen.leerToken(), "El token debe purgarse si /auth/me responde 403")
    }

    @Test
    fun iniciarSesion_cuandoAuthMeFallaCon500_purgaTokenYLanzaErrorServidor() = runTest {
        val almacen = FakeAlmacenSeguro(tokenMemoria = null)
        val coordinator = SessionCoordinator()

        val mockEngine = MockEngine { request ->
            if (request.url.encodedPath.contains("auth/login")) {
                respond(
                    content = """{"token":"jwt.token.temp","tipo":"Bearer","expiraEnSegundos":3600,"username":"admin"}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            } else {
                respond(
                    content = """{"error":"Internal Server Error"}""",
                    status = HttpStatusCode.InternalServerError,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }
        }

        val cliente = crearHttpClient(mockEngine, almacen, coordinator, configTest)
        val repo = AuthRepositorioRest(cliente, almacen)

        val excepcion = assertFailsWith<ErrorSesionException> {
            repo.iniciarSesion(Credenciales("admin", "Admin123"))
        }

        assertTrue(excepcion.error is ErrorSesion.ErrorDesconocido)
        assertNull(almacen.leerToken(), "El token debe purgarse si /auth/me falla con 500")
    }

    @Test
    fun sesionGuardada_cuandoAuthMeFallaCon401o403_purgaTokenYRetornaNull() = runTest {
        val almacen = FakeAlmacenSeguro(tokenMemoria = "jwt.guardado.expirado")
        val coordinator = SessionCoordinator()

        var statusCodeMe = HttpStatusCode.Unauthorized

        val mockEngine = MockEngine { _ ->
            respond(
                content = """{"error":"Unauthorized"}""",
                status = statusCodeMe,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val cliente = crearHttpClient(mockEngine, almacen, coordinator, configTest)
        val repo = AuthRepositorioRest(cliente, almacen)

        // Caso 1: 401 Unauthorized
        val sesion401 = repo.sesionGuardada()
        assertNull(sesion401)
        assertNull(almacen.leerToken(), "Token debe purgarse tras 401 en sesionGuardada")

        // Caso 2: 403 Forbidden
        almacen.guardarToken("jwt.guardado.prohibido")
        statusCodeMe = HttpStatusCode.Forbidden
        val sesion403 = repo.sesionGuardada()
        assertNull(sesion403)
        assertNull(almacen.leerToken(), "Token debe purgarse tras 403 en sesionGuardada")
    }

    @Test
    fun sesionGuardada_cuandoAuthMeArrojaErrorRedO500_conservaTokenYRetornaNull() = runTest {
        val almacen = FakeAlmacenSeguro(tokenMemoria = "jwt.guardado.persistente")
        val coordinator = SessionCoordinator()
        var lanzarExcepcionRed = true

        val mockEngine = MockEngine { _ ->
            if (lanzarExcepcionRed) {
                throw IOException("Sin conectividad a internet")
            } else {
                respond(
                    content = """{"error":"Internal Server Error"}""",
                    status = HttpStatusCode.InternalServerError,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }
        }

        val cliente = crearHttpClient(mockEngine, almacen, coordinator, configTest)
        val repo = AuthRepositorioRest(cliente, almacen)

        // Caso Red: IOException
        val sesionRed = repo.sesionGuardada()
        assertNull(sesionRed, "Debe retornar null ante error de red")
        assertEquals("jwt.guardado.persistente", almacen.leerToken(), "El token debe conservarse ante error de red")

        // Caso Servidor: 500
        lanzarExcepcionRed = false
        val sesion500 = repo.sesionGuardada()
        assertNull(sesion500, "Debe retornar null ante error 500")
        assertEquals("jwt.guardado.persistente", almacen.leerToken(), "El token debe conservarse ante error 500")
    }

    @Test
    fun sesionGuardada_cuandoAuthMeEsCancelado_conservaTokenYRelanzaCancellationException() = runTest {
        val almacen = FakeAlmacenSeguro(tokenMemoria = "jwt.guardado.a.conservar")
        val coordinator = SessionCoordinator()

        val mockEngine = MockEngine { _ ->
            throw CancellationException("Cancelación durante sesionGuardada")
        }

        val cliente = crearHttpClient(mockEngine, almacen, coordinator, configTest)
        val repo = AuthRepositorioRest(cliente, almacen)

        assertFailsWith<CancellationException> {
            repo.sesionGuardada()
        }

        assertEquals("jwt.guardado.a.conservar", almacen.leerToken(), "El token debe conservarse en AlmacenSeguro ante CancellationException")
    }
}

