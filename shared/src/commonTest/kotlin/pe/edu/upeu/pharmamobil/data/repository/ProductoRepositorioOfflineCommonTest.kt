package pe.edu.upeu.pharmamobil.data.repository

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import pe.edu.upeu.pharmamobil.data.local.AlmacenSeguro
import pe.edu.upeu.pharmamobil.data.local.ProductoLocal
import pe.edu.upeu.pharmamobil.data.remote.ProductoApi
import pe.edu.upeu.pharmamobil.data.remote.crearHttpClient
import pe.edu.upeu.pharmamobil.db.PharmaDb
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.session.SessionCoordinator
import pe.edu.upeu.pharmamobil.platform.PlatformConfig
import pe.edu.upeu.pharmamobil.platform.ProveedorHora
import pe.edu.upeu.pharmamobil.test.crearDriverEnMemoria
import kotlin.test.*

/**
 * Suite de 6 pruebas unitarias canónicas en commonTest sobre [ProductoRepositorioOffline],
 * ejecutada sobre driver SQLite en memoria multiplataforma y Ktor MockEngine.
 *
 * Cumple estrictamente con las especificaciones del Producto 1 (Pág. 3 de la guía oficial):
 * - Cada prueba crea su propia base en memoria aislada.
 * - Verifica sin red, con red, reconexión, convivencia de identificadores e idempotencia.
 * - Incluye verificación de resiliencia del timestamp ante fallos de sincronización.
 */
class ProductoRepositorioOfflineCommonTest {

    private class TestAlmacenSeguro : AlmacenSeguro {
        private var token: String? = "jwt.test.token"
        private var identidad: Triple<String, String, Set<String>>? = null
        override fun guardarToken(token: String) { this.token = token }
        override fun leerToken(): String? = token
        override fun borrarToken() { token = null }
        override fun guardarIdentidad(usuario: String, nombreCompleto: String, permisos: Set<String>) {
            identidad = Triple(usuario, nombreCompleto, permisos)
        }
        override fun leerIdentidad(): Triple<String, String, Set<String>>? = identidad
        override fun borrarIdentidad() { identidad = null }
    }

    private class TestProveedorHora(
        var ahora: Long = 1729600000000L,
        var formateada: String = "10:30:00"
    ) : ProveedorHora {
        override fun ahoraMillis(): Long = ahora
        override fun obtenerHoraActualFormateada(): String = formateada
    }

    private val configTest = PlatformConfig(
        esDepuracion = true,
        urlBase = "http://localhost:8080/api/v1/"
    )

    private fun crearBaseAislada(): PharmaDb {
        val driver = crearDriverEnMemoria()
        return PharmaDb(driver)
    }

    private fun crearApiConEngine(engine: MockEngine): ProductoApi {
        val client = crearHttpClient(engine, TestAlmacenSeguro(), SessionCoordinator(), configTest)
        return ProductoApi(client)
    }

    @Test
    fun prueba1_sincronizacion_con_red_guarda_productos_y_flow_emite() = runTest {
        val db = crearBaseAislada()
        val local = ProductoLocal(db)
        val mockEngine = MockEngine { request ->
            val ruta = request.url.encodedPath
            if (ruta.endsWith("/productos") && request.method.value == "GET") {
                respond(
                    content = """{"contenido":[{"id":101,"nombre":"Paracetamol 500mg","precio":5.5,"stock":40,"estado":true,"categoriaId":1,"categoriaNombre":"Analgésicos","images":[]}],"pagina":0,"tamanio":100,"totalElementos":1,"totalPaginas":1,"ultima":true}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            } else {
                respond("", HttpStatusCode.NotFound)
            }
        }
        val repo = ProductoRepositorioOffline(local, crearApiConEngine(mockEngine), TestProveedorHora())

        val resultado = repo.sincronizar()

        assertTrue(resultado.isSuccess, "La sincronización con red disponible debe ser exitosa")
        val listaLocal = local.observarTodos().first()
        assertEquals(1, listaLocal.size)
        assertEquals(101L, listaLocal[0].id)
        assertEquals("Paracetamol 500mg", listaLocal[0].nombre)
    }

    @Test
    fun prueba2_registro_sin_red_guarda_pendiente_y_flow_emite() = runTest {
        val db = crearBaseAislada()
        val local = ProductoLocal(db)
        val mockEngine = MockEngine { _ ->
            throw IOException("Sin conectividad simulada")
        }
        val repo = ProductoRepositorioOffline(local, crearApiConEngine(mockEngine), TestProveedorHora())

        val producto = Producto(
            id = 0L,
            nombre = "Ibuprofeno 800 mg Forte",
            precio = 12.50,
            stock = 25,
            activo = true
        )

        val registrado = repo.registrar(producto)

        assertTrue(registrado.id < 0L, "El registro sin red debe asignar un ID negativo temporal")
        val pendientes = local.pendientes()
        assertEquals(1, pendientes.size, "Debe quedar exactamente un producto pendiente en SQLite")
        assertEquals(registrado.id, pendientes[0].id)

        val emitidos = local.observarTodos().first()
        assertEquals(1, emitidos.size)
        assertEquals("Ibuprofeno 800 mg Forte", emitidos[0].nombre)
    }

    @Test
    fun prueba3_envio_al_reconectar_sube_pendiente_y_desmarca() = runTest {
        val db = crearBaseAislada()
        val local = ProductoLocal(db)
        var redActiva = false
        val mockEngine = MockEngine { request ->
            if (!redActiva) throw IOException("Sin red")
            val ruta = request.url.encodedPath
            when {
                ruta.endsWith("/productos") && request.method.value == "POST" -> {
                    respond(
                        content = """{"id":202,"nombre":"Amoxicilina 500mg","precio":18.0,"stock":30,"estado":true,"categoriaId":1,"categoriaNombre":"Antibióticos","images":[]}""",
                        status = HttpStatusCode.Created,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                ruta.endsWith("/productos") && request.method.value == "GET" -> {
                    respond(
                        content = """{"contenido":[{"id":202,"nombre":"Amoxicilina 500mg","precio":18.0,"stock":30,"estado":true,"categoriaId":1,"categoriaNombre":"Antibióticos","images":[]}],"pagina":0,"tamanio":100,"totalElementos":1,"totalPaginas":1,"ultima":true}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        val repo = ProductoRepositorioOffline(local, crearApiConEngine(mockEngine), TestProveedorHora())

        // 1. Registro sin red
        val guardadoOffline = repo.registrar(Producto(0L, "Amoxicilina 500mg", 18.0, 30, true))
        assertTrue(guardadoOffline.id < 0L)
        assertEquals(1, local.pendientes().size)

        // 2. Reconexión y sincronización
        redActiva = true
        val syncResult = repo.sincronizar()

        assertTrue(syncResult.isSuccess)
        assertEquals(0, local.pendientes().size, "No deben quedar pendientes tras subir al reconectar")
        val listaFinal = repo.listar()
        assertEquals(1, listaFinal.size)
        assertEquals(202L, listaFinal[0].id, "El ID temporal debe reemplazarse por el ID positivo del backend")
    }

    @Test
    fun prueba4_la_bajada_no_borra_lo_pendiente() = runTest {
        val db = crearBaseAislada()
        val local = ProductoLocal(db)

        // Producto pendiente registrado previamente sin red
        val pendienteLocal = local.guardarLocalPendiente(
            Producto(id = 0L, nombre = "Antigripal Noche", precio = 6.0, stock = 15, activo = true)
        )
        assertTrue(pendienteLocal.id < 0L)

        // Descarga de catálogo remoto que trae productos distintos
        val mockEngine = MockEngine { request ->
            if (request.method.value == "GET") {
                respond(
                    content = """{"contenido":[{"id":305,"nombre":"Omeprazol 20mg","precio":22.0,"stock":50,"estado":true,"categoriaId":1,"categoriaNombre":"General","images":[]}],"pagina":0,"tamanio":100,"totalElementos":1,"totalPaginas":1,"ultima":true}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            } else {
                respond("", HttpStatusCode.NotFound)
            }
        }
        val repo = ProductoRepositorioOffline(local, crearApiConEngine(mockEngine), TestProveedorHora())

        // Simulamos la bajada de productos remotos
        local.guardarRemotos(listOf(Producto(305L, "Omeprazol 20mg", 22.0, 50, true)))

        val listaActual = local.listarActual()
        assertEquals(2, listaActual.size, "Deben convivir el producto remoto y el pendiente local")
        assertNotNull(listaActual.firstOrNull { it.id == pendienteLocal.id }, "El producto pendiente no debe ser borrado por la bajada")
        assertNotNull(listaActual.firstOrNull { it.id == 305L }, "El catálogo remoto debe estar presente")
        assertEquals(1, local.pendientes().size)
    }

    @Test
    fun prueba5_identificadores_sin_colision_conviven() = runTest {
        val db = crearBaseAislada()
        val local = ProductoLocal(db)

        // Insertar un producto pendiente local (ID negativo: -1L)
        val pendiente = local.guardarLocalPendiente(
            Producto(id = 0L, nombre = "Vitamina C 1g", precio = 15.0, stock = 10, activo = true)
        )

        // Insertar un producto remoto sincronizado (ID positivo: 1L)
        local.actualizar(
            Producto(id = 1L, nombre = "Aspirina 100mg", precio = 4.0, stock = 80, activo = true),
            pendiente = false
        )

        val todos = local.listarActual()
        assertEquals(2, todos.size)
        assertTrue(todos.any { it.id < 0L }, "Debe existir el identificador negativo")
        assertTrue(todos.any { it.id > 0L }, "Debe existir el identificador positivo del servidor")
        assertNotEquals(pendiente.id, 1L)
    }

    @Test
    fun prueba6_sincronizar_dos_veces_no_duplica_filas() = runTest {
        val db = crearBaseAislada()
        val local = ProductoLocal(db)
        val mockEngine = MockEngine { request ->
            val ruta = request.url.encodedPath
            if (ruta.endsWith("/productos") && request.method.value == "GET") {
                respond(
                    content = """{"contenido":[{"id":501,"nombre":"Loratadina 10mg","precio":8.0,"stock":35,"estado":true,"categoriaId":1,"categoriaNombre":"Antialérgicos","images":[]}],"pagina":0,"tamanio":100,"totalElementos":1,"totalPaginas":1,"ultima":true}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            } else {
                respond("", HttpStatusCode.NotFound)
            }
        }
        val repo = ProductoRepositorioOffline(local, crearApiConEngine(mockEngine), TestProveedorHora())

        // Primera sincronización
        val sync1 = repo.sincronizar()
        assertTrue(sync1.isSuccess)
        val conteo1 = local.listarActual().size
        assertEquals(1, conteo1)

        // Segunda sincronización consecutiva
        val sync2 = repo.sincronizar()
        assertTrue(sync2.isSuccess)
        val conteo2 = local.listarActual().size
        assertEquals(1, conteo2, "Sincronizar repetidamente debe ser idempotente y no duplicar filas")
    }

    @Test
    fun pruebaExtra_sincronizacion_fallida_no_actualiza_timestamp_previo() = runTest {
        val db = crearBaseAislada()
        val local = ProductoLocal(db)
        val proveedorHora = TestProveedorHora(ahora = 2_000_000L)

        // Persistimos un timestamp previo de una sincronización correcta anterior
        local.guardarTimestampSincronizacion(1_000_000L)
        assertEquals(1_000_000L, local.obtenerUltimoSyncTimestamp())

        // Simulamos falla de red al intentar sincronizar
        val mockEngine = MockEngine { _ ->
            throw IOException("Falla de red en sincronización")
        }
        val repo = ProductoRepositorioOffline(local, crearApiConEngine(mockEngine), proveedorHora)

        val resultado = repo.sincronizar()

        assertTrue(resultado.isFailure, "La sincronización fallida debe retornar isFailure")
        assertEquals(
            1_000_000L,
            local.obtenerUltimoSyncTimestamp(),
            "El timestamp previo debe permanecer intacto si la sincronización falla"
        )
    }
}
