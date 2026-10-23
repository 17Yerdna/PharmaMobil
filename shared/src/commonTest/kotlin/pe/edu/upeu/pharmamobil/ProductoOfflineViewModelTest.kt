package pe.edu.upeu.pharmamobil

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import pe.edu.upeu.pharmamobil.domain.error.ErrorApi
import pe.edu.upeu.pharmamobil.domain.error.ErrorApiException
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.platform.Compartidor
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository
import pe.edu.upeu.pharmamobil.domain.usecase.ActualizarProductoUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.EliminarProductoUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.ObservarProductosUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.RegistrarProductoUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.SincronizarProductosUseCase
import pe.edu.upeu.pharmamobil.platform.PlatformConfig
import pe.edu.upeu.pharmamobil.platform.ProveedorHora
import pe.edu.upeu.pharmamobil.presentation.producto.FaseInventario
import pe.edu.upeu.pharmamobil.presentation.producto.ProductoViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ProductoOfflineViewModelTest {

    private class FakeHora(var millis: Long = 1729600000000L) : ProveedorHora {
        override fun ahoraMillis(): Long = millis
        override fun obtenerHoraActualFormateada(): String = "10:30:00"
    }

    private class RepositorioDeEscenarios(productosIniciales: List<Producto> = emptyList()) : ProductoRepository {
        private val productosLocales = MutableStateFlow(productosIniciales)
        var conectado = false
        var hora: String? = null
        var timestamp: Long? = null

        override fun observar() = productosLocales.asStateFlow()
        override suspend fun sincronizar(): Result<Unit> {
            if (!conectado) return Result.failure(ErrorApiException(ErrorApi.SinConexion))
            productosLocales.value = productosLocales.value.map { producto ->
                if (producto.id < 0L) producto.copy(id = 101L) else producto
            }
            hora = "10:30:00"
            timestamp = 1729600000000L
            return Result.success(Unit)
        }
        override fun obtenerUltimaHoraSincronizada(): String? = hora
        override fun obtenerUltimoSyncTimestamp(): Long? = timestamp
        override suspend fun listar() = productosLocales.value
        override suspend fun obtener(id: Long) = productosLocales.value.first { it.id == id }
        override suspend fun registrar(producto: Producto): Producto {
            val guardado = producto.copy(id = if (conectado) 101L else -1L)
            productosLocales.value = productosLocales.value + guardado
            return guardado
        }
        override suspend fun actualizar(producto: Producto): Producto = producto
        override suspend fun eliminar(id: Long) { productosLocales.value = productosLocales.value.filterNot { it.id == id } }
    }

    private fun crearViewModel(repo: ProductoRepository, proveedorHora: ProveedorHora = FakeHora()) = ProductoViewModel(
        observarProductosUseCase = ObservarProductosUseCase(repo),
        sincronizarProductosUseCase = SincronizarProductosUseCase(repo),
        registrarProductoUseCase = RegistrarProductoUseCase(repo),
        actualizarProductoUseCase = ActualizarProductoUseCase(repo),
        eliminarProductoUseCase = EliminarProductoUseCase(repo),
        compartidor = object : Compartidor { override fun compartir(texto: String) {} },
        platformConfig = PlatformConfig(esDepuracion = true, urlBase = "http://localhost/"),
        proveedorHora = proveedorHora
    )

    @Test
    fun sinRed_muestraCatalogoLocalYAvisoDeConexion() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repo = RepositorioDeEscenarios(listOf(Producto(8L, "Paracetamol", 10.0, 5, true)))
            val vm = crearViewModel(repo)
            advanceUntilIdle()

            assertTrue(vm.uiState.value.avisoRed)
            val fase = vm.uiState.value.fase as FaseInventario.ConProductos
            assertEquals("Paracetamol", fase.productos.single().producto.nombre)
            assertEquals("Nunca se ha sincronizado", vm.uiState.value.ultimaSincronizacion)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun altaSinRed_apareceEnElFlujoLocalComoPendiente() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val vm = crearViewModel(RepositorioDeEscenarios())
            advanceUntilIdle()
            vm.iniciarCreacion()
            vm.onNombreChanged("Ibuprofeno")
            vm.onPrecioChanged("15.50")
            vm.onStockChanged("4")
            vm.guardarProducto()
            advanceUntilIdle()

            assertEquals(1, vm.uiState.value.totalPendientes)
            assertTrue(vm.uiState.value.mensajeExito.orEmpty().contains("pendiente"))
            assertEquals(-1L, (vm.uiState.value.fase as FaseInventario.ConProductos).productos.single().id)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun reconectar_reemplazaPendienteYActualizaHora() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val fakeHora = FakeHora(1729600000000L)
            val repo = RepositorioDeEscenarios(listOf(Producto(-1L, "Ibuprofeno", 15.5, 4, true)))
            val vm = crearViewModel(repo, fakeHora)
            advanceUntilIdle()
            assertEquals(1, vm.uiState.value.totalPendientes)

            repo.conectado = true
            vm.sincronizar()
            advanceUntilIdle()

            assertFalse(vm.uiState.value.avisoRed)
            assertEquals(0, vm.uiState.value.totalPendientes)
            assertEquals("Actualizado hace unos momentos", vm.uiState.value.ultimaSincronizacion)
            assertEquals(101L, (vm.uiState.value.fase as FaseInventario.ConProductos).productos.single().id)

            // Verificación de cálculo de lenguaje natural para diferentes antigüedades
            assertEquals("Nunca se ha sincronizado", vm.calcularAntiguedad(null, fakeHora.millis))
            assertEquals("Actualizado hace 2 horas", vm.calcularAntiguedad(fakeHora.millis - 2 * 3600 * 1000L, fakeHora.millis))
            assertEquals("Actualizado hace más de un día", vm.calcularAntiguedad(fakeHora.millis - 25 * 3600 * 1000L, fakeHora.millis))
        } finally {
            Dispatchers.resetMain()
        }
    }
}
