package pe.edu.upeu.pharmamobil.platform

/**
 * Proveedor multiplataforma para la obtención de la hora actual formateada.
 * Utilizado para registrar la frescura temporal de sincronizaciones offline.
 */
interface ProveedorHora {
    fun obtenerHoraActualFormateada(): String
}
