package pe.edu.upeu.pharmamobil.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import pe.edu.upeu.pharmamobil.data.remote.dto.PaginaResponseDto
import pe.edu.upeu.pharmamobil.data.remote.dto.ProductoDto
import pe.edu.upeu.pharmamobil.data.remote.dto.ProductoRequestDto
import pe.edu.upeu.pharmamobil.data.remote.dto.ProductoResponseDto

/**
 * Cliente de servicio remoto para el consumo de la API REST de productos con soporte CRUD completo.
 */
class ProductoApi(private val client: HttpClient) {

    /**
     * GET: Obtiene la lista paginada de productos desde el backend.
     */
    suspend fun listar(pagina: Int = 0, tamanio: Int = 50): PaginaResponseDto<ProductoResponseDto> =
        client.get("productos") {
            parameter("pagina", pagina)
            parameter("tamanio", tamanio)
        }.body()

    /**
     * GET: Obtiene el detalle de un producto por su identificador único.
     */
    suspend fun obtener(id: Long): ProductoResponseDto =
        client.get("productos/$id").body()

    /**
     * POST: Envía la solicitud para registrar un nuevo producto en el servidor.
     */
    suspend fun crear(request: ProductoRequestDto): ProductoResponseDto =
        client.post("productos") {
            setBody(request)
        }.body()

    /**
     * PUT: Actualiza los datos de un producto existente.
     */
    suspend fun actualizar(id: Long, request: ProductoRequestDto): ProductoResponseDto =
        client.put("productos/$id") {
            setBody(request)
        }.body()

    /**
     * DELETE: Elimina un producto por ID.
     * IMPORTANTE: No se invoca .body() para evitar excepciones sobre respuestas HTTP 204 No Content.
     */
    suspend fun eliminar(id: Long) {
        client.delete("productos/$id")
    }

    /**
     * Endpoint complementario para compatibilidad con servicios REST de lista simple.
     */
    suspend fun obtenerProductos(offset: Int? = null, limite: Int? = null): List<ProductoDto> =
        client.get("products") {
            offset?.let { parameter("offset", it) }
            limite?.let { parameter("limit", it) }
        }.body()
}
