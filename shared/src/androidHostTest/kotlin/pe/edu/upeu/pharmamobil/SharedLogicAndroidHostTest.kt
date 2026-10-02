package pe.edu.upeu.pharmamobil

import pe.edu.upeu.pharmamobil.domain.model.Cliente
import kotlin.test.Test
import kotlin.test.assertEquals

class SharedLogicAndroidHostTest {

    @Test
    fun clienteTelefono(){
        val cliente= Cliente(
            id=1L,
            nombre="Farmacia Laufarma",
            correo = "ventas@laufarma.pe",
            telefono = "987654321"
        )
        val resultado = cliente.obtenerTelefono()

        assertEquals(
            expected = "987654321",
            actual = resultado
        )
    }
}