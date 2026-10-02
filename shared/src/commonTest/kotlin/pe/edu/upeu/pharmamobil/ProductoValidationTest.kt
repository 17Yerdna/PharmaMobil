package pe.edu.upeu.pharmamobil

import pe.edu.upeu.pharmamobil.data.repository.ProductoRepositorioEnMemoria
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.usecase.RegistrarProductoUseCase
import kotlin.test.*

class ProductoValidationTest {

    private lateinit var repositorio: ProductoRepositorioEnMemoria
    private lateinit var registrarProductoUseCase: RegistrarProductoUseCase

    @BeforeTest
    fun setup() {
        repositorio = ProductoRepositorioEnMemoria()
        registrarProductoUseCase = RegistrarProductoUseCase(repositorio)
    }

    @Test
    fun caso1_registroValido_exito() {
        val resultado = registrarProductoUseCase.validar(
            nombre = "Paracetamol 500 mg",
            precio = "8.50",
            stock = "100"
        )
        assertTrue(resultado.isSuccess, "El registro debería ser exitoso")
        val producto = resultado.getOrNull()
        assertNotNull(producto)
        assertEquals("Paracetamol 500 mg", producto.nombre)
        assertEquals(8.50, producto.precio)
        assertEquals(100, producto.stock)
    }

    @Test
    fun caso2_nombreVacio_errorObligatorio() {
        val resultado = registrarProductoUseCase.validar(
            nombre = "",
            precio = "8.50",
            stock = "100"
        )
        assertTrue(resultado.isFailure)
        assertEquals("El nombre es obligatorio.", resultado.exceptionOrNull()?.message)
    }

    @Test
    fun caso2_nombreEspacios_errorObligatorio() {
        val resultado = registrarProductoUseCase.validar(
            nombre = "   ",
            precio = "8.50",
            stock = "100"
        )
        assertTrue(resultado.isFailure)
        assertEquals("El nombre es obligatorio.", resultado.exceptionOrNull()?.message)
    }

    @Test
    fun caso3_precioNoNumerico_errorFormato() {
        val resultado = registrarProductoUseCase.validar(
            nombre = "Ibuprofeno",
            precio = "abc",
            stock = "50"
        )
        assertTrue(resultado.isFailure)
        assertEquals("Ingrese un precio numérico.", resultado.exceptionOrNull()?.message)
    }

    @Test
    fun caso4_precioCero_errorMayorQueCero() {
        val resultado = registrarProductoUseCase.validar(
            nombre = "Ibuprofeno",
            precio = "0",
            stock = "50"
        )
        assertTrue(resultado.isFailure)
        assertEquals("El precio debe ser mayor que cero.", resultado.exceptionOrNull()?.message)
    }

    @Test
    fun caso5_stockNoEntero_errorFormato() {
        val resultado = registrarProductoUseCase.validar(
            nombre = "Amoxicilina",
            precio = "18.50",
            stock = "abc"
        )
        assertTrue(resultado.isFailure)
        assertEquals("Ingrese un stock entero.", resultado.exceptionOrNull()?.message)
    }

    @Test
    fun caso6_stockNegativo_errorNoNegativo() {
        val resultado = registrarProductoUseCase.validar(
            nombre = "Amoxicilina",
            precio = "18.50",
            stock = "-5"
        )
        assertTrue(resultado.isFailure)
        assertEquals("El stock no puede ser negativo.", resultado.exceptionOrNull()?.message)
    }

    @Test
    fun caso7_stockCero_registroValido() {
        val resultado = registrarProductoUseCase.validar(
            nombre = "Loratadina",
            precio = "10",
            stock = "0"
        )
        assertTrue(resultado.isSuccess, "El registro con stock 0 debe ser válido")
        val producto = resultado.getOrNull()
        assertNotNull(producto)
        assertEquals("Loratadina", producto.nombre)
        assertEquals(10.0, producto.precio)
        assertEquals(0, producto.stock)
        assertTrue(producto.requiereReposicion(), "Con stock 0 debe requerir reposicion")
    }

    @Test
    fun reglaNegocio_requiereReposicion_validacion() {
        val productoBajoStock = Producto(id = 1L, nombre = "Amoxicilina", precio = 25.0, stock = 5)
        val productoStockSuficiente = Producto(id = 2L, nombre = "Ibuprofeno", precio = 18.0, stock = 6)

        assertTrue(productoBajoStock.requiereReposicion(), "Stock <= 5 debe requerir reposicion")
        assertFalse(productoStockSuficiente.requiereReposicion(), "Stock > 5 no debe requerir reposicion")
    }
}
