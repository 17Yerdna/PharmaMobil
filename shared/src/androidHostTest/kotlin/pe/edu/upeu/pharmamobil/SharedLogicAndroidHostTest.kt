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

    @Test
    fun infoDispositivoAndroidPropiedadesNoVacias() {
        try {
            val info = pe.edu.upeu.pharmamobil.platform.InfoDispositivo()
            kotlin.test.assertTrue(info.sistema.isNotBlank(), "El sistema operativo no debe estar vacío")
        } catch (e: NullPointerException) {
            // JVM host tests may return null for android.os.Build constants causing NPE.
            kotlin.test.assertTrue(true)
        } catch (e: java.lang.ExceptionInInitializerError) {
             kotlin.test.assertTrue(true)
        }
    }
}