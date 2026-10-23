package pe.edu.upeu.pharmamobil.data.local

/**
 * Contrato multiplataforma para persistencia segura de tokens de sesión.
 */
interface AlmacenSeguro {
    fun guardarToken(token: String)
    fun leerToken(): String?
    fun borrarToken()

    fun guardarIdentidad(usuario: String, nombreCompleto: String, permisos: Set<String>)
    fun leerIdentidad(): Triple<String, String, Set<String>>?
    fun borrarIdentidad()
}
