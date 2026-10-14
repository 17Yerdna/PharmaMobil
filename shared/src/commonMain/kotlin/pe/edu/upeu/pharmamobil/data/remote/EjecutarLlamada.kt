package pe.edu.upeu.pharmamobil.data.remote

import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ServerResponseException
import kotlinx.coroutines.CancellationException
import pe.edu.upeu.pharmamobil.data.remote.dto.ErrorResponseDto
import pe.edu.upeu.pharmamobil.domain.error.ErrorApi
import pe.edu.upeu.pharmamobil.domain.error.ErrorApiException

/**
 * Encapsula la ejecución de una llamada de red Ktor, capturando las excepciones
 * y traduciéndolas a tipos [ErrorApi] del dominio.
 */
suspend fun <T> ejecutarLlamada(bloque: suspend () -> T): Result<T> =
    try {
        Result.success(bloque())
    } catch (cancelacion: CancellationException) {
        throw cancelacion
    } catch (e: ClientRequestException) {
        Result.failure(ErrorApiException(traducirCliente(e)))
    } catch (e: ServerResponseException) {
        Result.failure(ErrorApiException(ErrorApi.Servidor))
    } catch (e: HttpRequestTimeoutException) {
        Result.failure(ErrorApiException(ErrorApi.TiempoAgotado))
    } catch (e: ErrorApiException) {
        Result.failure(e)
    } catch (e: Exception) {
        val mensaje = e.message.orEmpty().lowercase()
        if (mensaje.contains("timeout")) {
            Result.failure(ErrorApiException(ErrorApi.TiempoAgotado))
        } else if (mensaje.contains("unresolved") || mensaje.contains("socket") || mensaje.contains("connect") || mensaje.contains("network")) {
            Result.failure(ErrorApiException(ErrorApi.SinConexion))
        } else {
            Result.failure(ErrorApiException(ErrorApi.SinConexion))
        }
    }

private suspend fun traducirCliente(e: ClientRequestException): ErrorApi {
    val cuerpo = runCatching {
        e.response.body<ErrorResponseDto>()
    }.getOrNull()

    return when (e.response.status.value) {
        400 -> ErrorApi.Validacion(cuerpo?.validationErrors.orEmpty())
        401 -> ErrorApi.NoAutorizado
        403 -> ErrorApi.SinPermiso
        404 -> ErrorApi.NoEncontrado
        409 -> ErrorApi.Conflicto(cuerpo?.message ?: "Operación no permitida")
        else -> ErrorApi.Servidor
    }
}
