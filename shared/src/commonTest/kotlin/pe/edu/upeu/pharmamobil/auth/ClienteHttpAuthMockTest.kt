package pe.edu.upeu.pharmamobil.auth

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import pe.edu.upeu.pharmamobil.data.local.AlmacenSeguro
import pe.edu.upeu.pharmamobil.data.remote.crearHttpClient
import pe.edu.upeu.pharmamobil.domain.session.SesionEstado
import pe.edu.upeu.pharmamobil.domain.session.SessionCoordinator
import pe.edu.upeu.pharmamobil.platform.PlatformConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pruebas de integración HTTP con MockEngine para verificar la inyección de Bearer tokens
 * y el manejo global de expiración de sesión ante respuestas 401 Unauthorized.
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
}
