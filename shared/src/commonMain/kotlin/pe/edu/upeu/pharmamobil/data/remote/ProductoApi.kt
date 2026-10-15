package pe.edu.upeu.pharmamobil.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import pe.edu.upeu.pharmamobil.data.remote.dto.ProductoDto

/**
 * Cliente de servicio remoto para el consumo de la API REST de productos.
 */
class ProductoApi(private val client: HttpClient) {

    suspend fun obtenerProductos(offset: Int? = null, limite: Int? = null): List<ProductoDto> =
        client.get("products") {
            offset?.let { parameter("offset", it) }
            limite?.let { parameter("limit", it) }
        }.body()
}
