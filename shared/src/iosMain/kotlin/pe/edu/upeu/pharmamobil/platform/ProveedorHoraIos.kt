package pe.edu.upeu.pharmamobil.platform

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter

class ProveedorHoraIos : ProveedorHora {
    override fun ahoraMillis(): Long = (NSDate().timeIntervalSince1970 * 1000.0).toLong()

    override fun obtenerHoraActualFormateada(): String {
        val formatter = NSDateFormatter().apply {
            dateFormat = "HH:mm:ss"
        }
        return formatter.stringFromDate(NSDate())
    }
}
