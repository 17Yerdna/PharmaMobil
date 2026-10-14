package pe.edu.upeu.pharmamobil.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import pe.edu.upeu.pharmamobil.data.local.AlmacenSeguro
import pe.edu.upeu.pharmamobil.data.remote.dto.LoginRequestDto
import pe.edu.upeu.pharmamobil.data.remote.dto.LoginResponseDto
import pe.edu.upeu.pharmamobil.data.remote.dto.UsuarioResponseDto
import pe.edu.upeu.pharmamobil.data.remote.limpiarTokenAuth
import pe.edu.upeu.pharmamobil.domain.error.ErrorSesion
import pe.edu.upeu.pharmamobil.domain.error.ErrorSesionException
import pe.edu.upeu.pharmamobil.domain.model.Credenciales
import pe.edu.upeu.pharmamobil.domain.model.Sesion
import pe.edu.upeu.pharmamobil.domain.repository.AuthRepository

/**
 * Implementación del repositorio de autenticación contra la API REST de PharmaSoft.
 */
class AuthRepositorioRest(
    private val cliente: HttpClient,
    private val almacen: AlmacenSeguro
) : AuthRepository {

    override suspend fun iniciarSesion(credenciales: Credenciales): Sesion {
        val loginResponse: LoginResponseDto = try {
            cliente.post("auth/login") {
                setBody(LoginRequestDto(credenciales.usuario.trim(), credenciales.clave))
            }.body()
        } catch (c: CancellationException) {
            throw c
        } catch (e: ClientRequestException) {
            if (e.response.status == HttpStatusCode.Unauthorized) {
                throw ErrorSesionException(ErrorSesion.CredencialesInvalidas)
            } else if (e.response.status == HttpStatusCode.Forbidden) {
                throw ErrorSesionException(ErrorSesion.SinPermiso)
            } else {
                throw ErrorSesionException(ErrorSesion.ErrorDesconocido("Error HTTP ${e.response.status.value}"))
            }
        } catch (e: ServerResponseException) {
            throw ErrorSesionException(ErrorSesion.ErrorDesconocido("Error interno del servidor"))
        } catch (e: HttpRequestTimeoutException) {
            throw ErrorSesionException(ErrorSesion.SinConexion)
        } catch (e: Exception) {
            throw ErrorSesionException(ErrorSesion.SinConexion)
        }

        // 1. Guardar token en el almacén seguro cifrado
        almacen.guardarToken(loginResponse.token)

        // 2. Limpiar caché del proveedor Bearer para que tome el token guardado
        cliente.limpiarTokenAuth()

        // 3. Consultar /auth/me para reconstruir el perfil con nombre y permisos
        val meResponse: UsuarioResponseDto = try {
            cliente.get("auth/me").body()
        } catch (e: Exception) {
            UsuarioResponseDto(
                username = loginResponse.username.ifEmpty { credenciales.usuario.trim() },
                empleadoNombre = loginResponse.nombreCompleto.ifEmpty { loginResponse.username },
                roles = loginResponse.roles,
                permisos = loginResponse.permisos
            )
        }

        val nombreFinal = meResponse.empleadoNombre ?: loginResponse.nombreCompleto.ifEmpty { meResponse.username }
        val permisosFinales = if (meResponse.permisos.isNotEmpty()) {
            meResponse.permisos.toSet()
        } else {
            loginResponse.permisos.toSet()
        }

        return Sesion(
            token = loginResponse.token,
            usuario = meResponse.username,
            nombreCompleto = nombreFinal,
            permisos = permisosFinales
        )
    }

    override suspend fun sesionGuardada(): Sesion? {
        val token = almacen.leerToken() ?: return null
        return try {
            val me: UsuarioResponseDto = cliente.get("auth/me").body()
            val nombreFinal = me.empleadoNombre ?: me.username
            Sesion(
                token = token,
                usuario = me.username,
                nombreCompleto = nombreFinal,
                permisos = me.permisos.toSet()
            )
        } catch (e: ClientRequestException) {
            if (e.response.status == HttpStatusCode.Unauthorized) {
                almacen.borrarToken()
                cliente.limpiarTokenAuth()
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun cerrarSesion() {
        almacen.borrarToken()
        cliente.limpiarTokenAuth()
    }
}
