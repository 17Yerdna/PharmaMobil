package pe.edu.upeu.pharmamobil

import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.presentation.producto.comoTextoParaCompartir
import kotlin.test.Test
import kotlin.test.assertContains

class CompartirProductoTest {
    @Test
    fun textoParaCompartir_incluyeDatosDelProductoYMonedaLocal() {
        val producto = Producto(
            id = 9L,
            nombre = "Paracetamol 500 mg",
            precio = 12.5,
            stock = 7
        )

        val texto = producto.comoTextoParaCompartir()

        assertContains(texto, "Paracetamol 500 mg")
        assertContains(texto, "S/")
        assertContains(texto, "Stock: 7")
    }
}
