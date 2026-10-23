package pe.edu.upeu.pharmamobil.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
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
        } catch (e: ConnectTimeoutException) {
            throw ErrorSesionException(ErrorSesion.SinConexion)
        } catch (e: IOException) {
            throw ErrorSesionException(ErrorSesion.SinConexion)
        } catch (e: Exception) {
            throw ErrorSesionException(ErrorSesion.ErrorDesconocido(e.message ?: "Error al autenticar"))
        }

        // 1. Guardar token en el almacén seguro
        almacen.guardarToken(loginResponse.token)

        // 2. Limpiar caché del proveedor Bearer para que tome el token guardado
        cliente.limpiarTokenAuth()

        // 3. Consultar /auth/me para reconstruir el perfil con nombre y permisos
        val meResponse: UsuarioResponseDto = try {
            cliente.get("auth/me").body()
        } catch (c: CancellationException) {
            throw c
        } catch (e: ClientRequestException) {
            almacen.borrarToken()
            almacen.borrarIdentidad()
            cliente.limpiarTokenAuth()
            if (e.response.status == HttpStatusCode.Unauthorized) {
                throw ErrorSesionException(ErrorSesion.SesionExpirada)
            } else if (e.response.status == HttpStatusCode.Forbidden) {
                throw ErrorSesionException(ErrorSesion.SinPermiso)
            } else {
                throw ErrorSesionException(ErrorSesion.ErrorDesconocido("Error HTTP ${e.response.status.value} al consultar perfil"))
            }
        } catch (e: ServerResponseException) {
            almacen.borrarToken()
            almacen.borrarIdentidad()
            cliente.limpiarTokenAuth()
            throw ErrorSesionException(ErrorSesion.ErrorDesconocido("Error interno del servidor al consultar perfil"))
        } catch (e: HttpRequestTimeoutException) {
            almacen.borrarToken()
            almacen.borrarIdentidad()
            cliente.limpiarTokenAuth()
            throw ErrorSesionException(ErrorSesion.SinConexion)
        } catch (e: ConnectTimeoutException) {
            almacen.borrarToken()
            almacen.borrarIdentidad()
            cliente.limpiarTokenAuth()
            throw ErrorSesionException(ErrorSesion.SinConexion)
        } catch (e: IOException) {
            almacen.borrarToken()
            almacen.borrarIdentidad()
            cliente.limpiarTokenAuth()
            throw ErrorSesionException(ErrorSesion.SinConexion)
        } catch (e: SerializationException) {
            almacen.borrarToken()
            almacen.borrarIdentidad()
            cliente.limpiarTokenAuth()
            throw ErrorSesionException(ErrorSesion.ErrorDesconocido("Error de serialización al procesar perfil"))
        } catch (e: Exception) {
            almacen.borrarToken()
            almacen.borrarIdentidad()
            cliente.limpiarTokenAuth()
            throw ErrorSesionException(ErrorSesion.ErrorDesconocido(e.message ?: "Error desconocido al procesar perfil"))
        }

        val nombreFinal = meResponse.empleadoNombre ?: loginResponse.nombreCompleto.ifEmpty { meResponse.username }
        val permisosFinales = if (meResponse.permisos.isNotEmpty()) {
            meResponse.permisos.toSet()
        } else {
            loginResponse.permisos.toSet()
        }

        // Cachear identidad para restauración offline
        almacen.guardarIdentidad(meResponse.username, nombreFinal, permisosFinales)

        return Sesion(
            token = loginResponse.token,
            usuario = meResponse.username,
            nombreCompleto = nombreFinal,
            permisos = permisosFinales,
            esOffline = false
        )
    }

    override suspend fun sesionGuardada(): Sesion? {
        val token = almacen.leerToken() ?: return null
        return try {
            val me: UsuarioResponseDto = cliente.get("auth/me").body()
            val nombreFinal = me.empleadoNombre ?: me.username
            val permisosFinales = me.permisos.toSet()
            almacen.guardarIdentidad(me.username, nombreFinal, permisosFinales)
            Sesion(
                token = token,
                usuario = me.username,
                nombreCompleto = nombreFinal,
                permisos = permisosFinales,
                esOffline = false
            )
        } catch (c: CancellationException) {
            throw c
        } catch (e: ClientRequestException) {
            if (e.response.status == HttpStatusCode.Unauthorized || e.response.status == HttpStatusCode.Forbidden) {
                almacen.borrarToken()
                almacen.borrarIdentidad()
                cliente.limpiarTokenAuth()
            }
            null
        } catch (e: HttpRequestTimeoutException) {
            resolverSesionOffline(token)
        } catch (e: ConnectTimeoutException) {
            resolverSesionOffline(token)
        } catch (e: IOException) {
            resolverSesionOffline(token)
        } catch (e: ServerResponseException) {
            resolverSesionOffline(token)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun revalidarSesion(): Sesion {
        val token = almacen.leerToken() ?: throw ErrorSesionException(ErrorSesion.SesionExpirada)
        val me: UsuarioResponseDto = try {
            cliente.get("auth/me").body()
        } catch (c: CancellationException) {
            throw c
        } catch (e: ClientRequestException) {
            if (e.response.status == HttpStatusCode.Unauthorized) {
                almacen.borrarToken()
                almacen.borrarIdentidad()
                cliente.limpiarTokenAuth()
                throw ErrorSesionException(ErrorSesion.SesionExpirada)
            } else if (e.response.status == HttpStatusCode.Forbidden) {
                almacen.borrarToken()
                almacen.borrarIdentidad()
                cliente.limpiarTokenAuth()
                throw ErrorSesionException(ErrorSesion.SinPermiso)
            } else {
                throw ErrorSesionException(ErrorSesion.ErrorDesconocido("Error HTTP ${e.response.status.value}"))
            }
        } catch (e: ServerResponseException) {
            throw ErrorSesionException(ErrorSesion.ErrorDesconocido("Error interno del servidor"))
        } catch (e: HttpRequestTimeoutException) {
            throw ErrorSesionException(ErrorSesion.SinConexion)
        } catch (e: ConnectTimeoutException) {
            throw ErrorSesionException(ErrorSesion.SinConexion)
        } catch (e: IOException) {
            throw ErrorSesionException(ErrorSesion.SinConexion)
        } catch (e: Exception) {
            throw ErrorSesionException(ErrorSesion.ErrorDesconocido(e.message ?: "Error al revalidar sesión"))
        }

        val nombreFinal = me.empleadoNombre ?: me.username
        val permisosFinales = me.permisos.toSet()
        almacen.guardarIdentidad(me.username, nombreFinal, permisosFinales)

        return Sesion(
            token = token,
            usuario = me.username,
            nombreCompleto = nombreFinal,
            permisos = permisosFinales,
            esOffline = false
        )
    }

    override suspend fun cerrarSesion() {
        almacen.borrarToken()
        almacen.borrarIdentidad()
        cliente.limpiarTokenAuth()
    }

    private fun resolverSesionOffline(token: String): Sesion? {
        val identidad = almacen.leerIdentidad() ?: return null
        return Sesion(
            token = token,
            usuario = identidad.first,
            nombreCompleto = identidad.second,
            permisos = emptySet(),
            esOffline = true
        )
    }
}
