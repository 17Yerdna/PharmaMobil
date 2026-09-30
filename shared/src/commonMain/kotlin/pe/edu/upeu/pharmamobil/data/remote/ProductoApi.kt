package pe.edu.upeu.pharmamobil.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import pe.edu.upeu.pharmamobil.data.remote.dto.ProductoDto

/**
 * Cliente de servicio remoto para el consumo de la API de productos.
 */
class ProductoApi(private val client: HttpClient) {

    suspend fun obtenerProductos(limite: Int = 10): List<ProductoDto> =
        client.get("products") {
            parameter("limit", limite)
        }.body()
}
