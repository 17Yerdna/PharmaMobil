package pe.edu.upeu.pharmamobil.platform

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProveedorHoraAndroid : ProveedorHora {
    override fun obtenerHoraActualFormateada(): String =
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
}
