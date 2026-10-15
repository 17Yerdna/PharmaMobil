package pe.edu.upeu.pharmamobil.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.authProvider
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.encodedPath
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import pe.edu.upeu.pharmamobil.data.local.AlmacenSeguro
import pe.edu.upeu.pharmamobil.domain.session.SessionCoordinator
import pe.edu.upeu.pharmamobil.platform.PlatformConfig

/**
 * Fábrica centralizada para crear y configurar la instancia de HttpClient con Ktor,
 * incorporando autenticación Bearer, saneamiento de credenciales y validación global de 401.
 */
fun crearHttpClient(
    engine: HttpClientEngine,
    almacen: AlmacenSeguro,
    sessionCoordinator: SessionCoordinator,
    config: PlatformConfig
): HttpClient =
    HttpClient(engine) {
        expectSuccess = true

        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = true
            })
        }

        install(Auth) {
            bearer {
                loadTokens {
                    almacen.leerToken()?.let { BearerTokens(it, "") }
                }
                sendWithoutRequest { request ->
                    !request.url.encodedPath.endsWith("/auth/login")
                }
            }
        }

        HttpResponseValidator {
            handleResponseExceptionWithRequest { cause, request ->
                val responseException = cause as? ClientRequestException ?: return@handleResponseExceptionWithRequest
                val response = responseException.response
                if (response.status == HttpStatusCode.Unauthorized && !request.url.encodedPath.endsWith("/auth/login")) {
                    almacen.borrarToken()
                    sessionCoordinator.notificarSesionExpirada()
                }
            }
        }

        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    println("[KtorHttp] $message")
                }
            }
            level = if (config.esDepuracion) LogLevel.HEADERS else LogLevel.NONE
            sanitizeHeader { it.equals(HttpHeaders.Authorization, ignoreCase = true) }
        }

        install(HttpTimeout) {
            requestTimeoutMillis = 15000
            connectTimeoutMillis = 10000
        }

        defaultRequest {
            url(config.urlBase)
            contentType(ContentType.Application.Json)
        }
    }

/**
 * Invalida la caché interna de tokens del plugin Bearer de Ktor.
 */
fun HttpClient.limpiarTokenAuth() {
    authProvider<BearerAuthProvider>()?.clearToken()
}
