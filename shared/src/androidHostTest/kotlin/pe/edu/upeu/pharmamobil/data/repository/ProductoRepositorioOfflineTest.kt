package pe.edu.upeu.pharmamobil.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import pe.edu.upeu.pharmamobil.data.local.AlmacenSeguro
import pe.edu.upeu.pharmamobil.data.local.ProductoLocal
import pe.edu.upeu.pharmamobil.data.remote.ProductoApi
import pe.edu.upeu.pharmamobil.data.remote.crearHttpClient
import pe.edu.upeu.pharmamobil.db.PharmaDb
import pe.edu.upeu.pharmamobil.domain.error.ErrorApi
import pe.edu.upeu.pharmamobil.domain.error.ErrorApiException
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.session.SessionCoordinator
import pe.edu.upeu.pharmamobil.platform.PlatformConfig
import pe.edu.upeu.pharmamobil.platform.ProveedorHora
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.*

/**
 * Suite de pruebas unitarias para [ProductoRepositorioOffline] en host JVM.
 * Verifica la resiliencia offline-first, transiciones atómicas de ID temporal a remoto,
 * exclusión mutua en sincronización y bloqueo de mutaciones para productos pendientes.
 */
class ProductoRepositorioOfflineTest {

    private class FakeAlmacenSeguro : AlmacenSeguro {
        override fun guardarToken(token: String) {}
        override fun leerToken(): String? = "jwt.test.token"
        override fun borrarToken() {}
    }

    private class FakeProveedorHora(var hora: String = "10:30:00") : ProveedorHora {
        override fun obtenerHoraActualFormateada(): String = hora
    }

    private val configTest = PlatformConfig(
        esDepuracion = true,
        urlBase = "http://localhost:8080/api/v1/"
    )

    private fun crearBaseDatosEnMemoria(): PharmaDb {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        PharmaDb.Schema.create(driver)
        return PharmaDb(driver)
    }

    @Test
    fun test1_guardarOffline_generaIdNegativoYSeEmiteEnLocal() = runTest {
        val db = crearBaseDatosEnMemoria()
        val local = ProductoLocal(db)
        val mockEngine = MockEngine { _ ->
            throw IOException("Sin red simulada")
        }
        val api = ProductoApi(crearHttpClient(mockEngine, FakeAlmacenSeguro(), SessionCoordinator(), configTest))
        val repo = ProductoRepositorioOffline(local, api, FakeProveedorHora())

        val producto = Producto(
            id = 0L,
            nombre = "Paracetamol 500mg",
            precio = 10.50,
            stock = 15,
            activo = true
        )

        val resultado = repo.registrar(producto)

        assertTrue(resultado.id < 0L, "El ID del producto registrado sin conexión debe ser negativo")
        val listaLocal = repo.listar()
        assertEquals(1, listaLocal.size)
        assertEquals(resultado.id, listaLocal[0].id)
        assertEquals("Paracetamol 500mg", listaLocal[0].nombre)
        assertEquals(1, local.pendientes().size, "Debe existir un producto en la cola de pendientes")
    }

    @Test
    fun test2_sincronizar_subePendientesYReemplazaIdNegativoPorRemotoAtomicamente() = runTest {
        val db = crearBaseDatosEnMemoria()
        val local = ProductoLocal(db)

        var modoOnline = false
        val mockEngine = MockEngine { request ->
            if (!modoOnline) {
                throw IOException("Sin red inicial")
            }
            val ruta = request.url.encodedPath
            when {
                ruta.endsWith("/productos") && request.method.value == "POST" -> {
                    respond(
                        content = """{"id":101,"nombre":"Amoxicilina 500mg","precio":18.0,"stock":30,"estado":true,"categoriaId":1,"categoriaNombre":"Antibióticos","images":[]}""",
                        status = HttpStatusCode.Created,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                ruta.endsWith("/productos") && request.method.value == "GET" -> {
                    respond(
                        content = """{"contenido":[{"id":101,"nombre":"Amoxicilina 500mg","precio":18.0,"stock":30,"estado":true,"categoriaId":1,"categoriaNombre":"Antibióticos","images":[]}],"pagina":0,"tamanio":100,"totalElementos":1,"totalPaginas":1,"ultima":true}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        val api = ProductoApi(crearHttpClient(mockEngine, FakeAlmacenSeguro(), SessionCoordinator(), configTest))
        val repo = ProductoRepositorioOffline(local, api, FakeProveedorHora("11:45:00"))

        val guardadoOffline = repo.registrar(
            Producto(id = 0L, nombre = "Amoxicilina 500mg", precio = 18.0, stock = 30, activo = true)
        )
        assertTrue(guardadoOffline.id < 0L)
        assertEquals(1, local.pendientes().size)

        // Activamos red y sincronizamos
        modoOnline = true
        val syncResultado = repo.sincronizar()

        assertTrue(syncResultado.isSuccess, "La sincronización con red debe completarse con éxito")
        assertEquals(0, local.pendientes().size, "No deben quedar productos pendientes tras la sincronización")
        val listaActualizada = repo.listar()
        assertEquals(1, listaActualizada.size)
        assertEquals(101L, listaActualizada[0].id, "El ID negativo debe haber sido reemplazado por el ID 101 del servidor")
        assertEquals("11:45:00", repo.obtenerUltimaHoraSincronizada())
    }

    @Test
    fun test3_sincronizacionConcurrente_protegidaPorMutexNoDuplicaPosts() = runTest {
        val db = crearBaseDatosEnMemoria()
        val local = ProductoLocal(db)

        val contadorPosts = AtomicInteger(0)
        val mockEngine = MockEngine { request ->
            val ruta = request.url.encodedPath
            if (ruta.endsWith("/productos") && request.method.value == "POST") {
                contadorPosts.incrementAndGet()
                delay(80) // Simulamos latencia de red
                respond(
                    content = """{"id":202,"nombre":"Omeprazol 20mg","precio":22.0,"stock":10,"estado":true,"categoriaId":1,"categoriaNombre":"General","images":[]}""",
                    status = HttpStatusCode.Created,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            } else if (ruta.endsWith("/productos") && request.method.value == "GET") {
                respond(
                    content = """{"contenido":[{"id":202,"nombre":"Omeprazol 20mg","precio":22.0,"stock":10,"estado":true,"categoriaId":1,"categoriaNombre":"General","images":[]}],"pagina":0,"tamanio":100,"totalElementos":1,"totalPaginas":1,"ultima":true}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            } else {
                respond("", HttpStatusCode.NotFound)
            }
        }
        val api = ProductoApi(crearHttpClient(mockEngine, FakeAlmacenSeguro(), SessionCoordinator(), configTest))
        val repo = ProductoRepositorioOffline(local, api, FakeProveedorHora())

        // Guardamos directamente un producto pendiente en SQLite local
        local.guardarLocalPendiente(Producto(id = 0L, nombre = "Omeprazol 20mg", precio = 22.0, stock = 10, activo = true))
        assertEquals(1, local.pendientes().size)

        // Disparamos dos sincronizaciones en paralelo
        val deferred1 = async { repo.sincronizar() }
        val deferred2 = async { repo.sincronizar() }
        val resultados = awaitAll(deferred1, deferred2)

        assertTrue(resultados.all { it.isSuccess })
        assertEquals(1, contadorPosts.get(), "El Mutex debe garantizar que el POST se ejecute exactamente una sola vez")
        assertEquals(0, local.pendientes().size)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun registrarMientrasSincroniza_noPublicaDosVecesElMismoPendiente() = runTest {
        val local = ProductoLocal(crearBaseDatosEnMemoria())
        val primerPostIniciado = CompletableDeferred<Unit>()
        val liberarPrimerPost = CompletableDeferred<Unit>()
        val contadorPosts = AtomicInteger(0)
        val mockEngine = MockEngine { request ->
            when (request.method.value) {
                "POST" -> {
                    if (contadorPosts.incrementAndGet() == 1) {
                        primerPostIniciado.complete(Unit)
                        liberarPrimerPost.await()
                    }
                    respond(
                        content = """{"id":303,"nombre":"Producto simultáneo","precio":7.0,"stock":2,"estado":true,"categoriaId":1,"categoriaNombre":"General","images":[]}""",
                        status = HttpStatusCode.Created,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                "GET" -> respond(
                    content = """{"contenido":[{"id":303,"nombre":"Producto simultáneo","precio":7.0,"stock":2,"estado":true,"categoriaId":1,"categoriaNombre":"General","images":[]}],"pagina":0,"tamanio":100,"totalElementos":1,"totalPaginas":1,"ultima":true}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        val api = ProductoApi(crearHttpClient(mockEngine, FakeAlmacenSeguro(), SessionCoordinator(), configTest))
        val repo = ProductoRepositorioOffline(local, api, FakeProveedorHora())

        val alta = async {
            repo.registrar(Producto(0L, "Producto simultáneo", 7.0, 2, true))
        }
        primerPostIniciado.await()
        val sincronizacion = async { repo.sincronizar() }
        runCurrent()
        liberarPrimerPost.complete(Unit)

        alta.await()
        assertTrue(sincronizacion.await().isSuccess)
        assertEquals(1, contadorPosts.get(), "El alta y la sincronización deben compartir exclusión mutua")
        assertEquals(listOf(303L), repo.listar().map { it.id })
    }

    @Test
    fun test4_sincronizarConFallaDeRed_propagaErrorTipadoYConservaPendiente() = runTest {
        val db = crearBaseDatosEnMemoria()
        val local = ProductoLocal(db)
        val mockEngine = MockEngine { _ ->
            throw IOException("Conexión rehusada")
        }
        val api = ProductoApi(crearHttpClient(mockEngine, FakeAlmacenSeguro(), SessionCoordinator(), configTest))
        val repo = ProductoRepositorioOffline(local, api, FakeProveedorHora())

        local.guardarLocalPendiente(Producto(id = 0L, nombre = "Aspirina 100mg", precio = 5.0, stock = 50, activo = true))
        assertEquals(1, local.pendientes().size)

        val resultadoSync = repo.sincronizar()

        assertTrue(resultadoSync.isFailure, "sincronizar() no debe silenciar excepciones de red")
        val error = resultadoSync.exceptionOrNull()
        assertTrue(error is ErrorApiException)
        assertEquals(ErrorApi.SinConexion, (error as ErrorApiException).error)
        assertEquals(1, local.pendientes().size, "El producto debe permanecer como pendiente en SQLite")
    }

    @Test
    fun test5_edicionYEliminacionDeProductoPendiente_bloqueadasConExcepcion() = runTest {
        val db = crearBaseDatosEnMemoria()
        val local = ProductoLocal(db)
        val mockEngine = MockEngine { _ -> respond("", HttpStatusCode.OK) }
        val api = ProductoApi(crearHttpClient(mockEngine, FakeAlmacenSeguro(), SessionCoordinator(), configTest))
        val repo = ProductoRepositorioOffline(local, api, FakeProveedorHora())

        val pendiente = local.guardarLocalPendiente(
            Producto(id = 0L, nombre = "Jarabe Tos", precio = 14.0, stock = 8, activo = true)
        )
        assertTrue(pendiente.id < 0L)

        // Intento de edición
        val falloEdicion = assertFailsWith<IllegalStateException> {
            repo.actualizar(pendiente.copy(nombre = "Jarabe Tos Modificado"))
        }
        assertTrue(falloEdicion.message.orEmpty().contains("pendiente"))

        // Intento de eliminación
        val falloEliminacion = assertFailsWith<IllegalStateException> {
            repo.eliminar(pendiente.id)
        }
        assertTrue(falloEliminacion.message.orEmpty().contains("pendiente"))

        // El registro sigue existiendo intacto en SQLite
        assertEquals(1, local.pendientes().size)
    }

    @Test
    fun test6_guardarRemotos_limpiaSincronizadosYPreservaPendientesLocales() = runTest {
        val db = crearBaseDatosEnMemoria()
        val local = ProductoLocal(db)

        // 1. Insertamos un producto pendiente localmente (ID -1L)
        val pendiente = local.guardarLocalPendiente(
            Producto(id = 0L, nombre = "Pendiente Local", precio = 9.90, stock = 12, activo = true)
        )

        // 2. Simulamos que previamente existía un producto sincronizado (ID 50L)
        local.actualizar(
            Producto(id = 50L, nombre = "Producto Antiguo Sincronizado", precio = 30.0, stock = 5, activo = true),
            pendiente = false
        )

        assertEquals(2, local.listarActual().size)

        // 3. El servidor ahora devuelve un nuevo catálogo donde el producto 50L fue dado de baja y existe el 80L
        val remotosNuevos = listOf(
            Producto(id = 80L, nombre = "Nuevo Catálogo Remoto", precio = 45.0, stock = 20, activo = true)
        )
        local.guardarRemotos(remotosNuevos)

        val catalogoFinal = local.listarActual()
        assertEquals(2, catalogoFinal.size)

        // El producto antiguo sincronizado (50L) fue purgado
        assertNull(catalogoFinal.firstOrNull { it.id == 50L })

        // El nuevo producto remoto (80L) está presente
        assertNotNull(catalogoFinal.firstOrNull { it.id == 80L })

        // El producto pendiente local (-1L) se preservó intacto
        val pendientePreservado = catalogoFinal.firstOrNull { it.id == pendiente.id }
        assertNotNull(pendientePreservado)
        assertEquals("Pendiente Local", pendientePreservado.nombre)
        assertEquals(1, local.pendientes().size)
    }
}
