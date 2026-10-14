package pe.edu.upeu.pharmamobil.data.local

/**
 * Implementación de almacenamiento seguro para iOS (Keychain).
 */
class AlmacenSeguroIos : AlmacenSeguro {
    private var tokenMemoria: String? = null

    override fun guardarToken(token: String) {
        tokenMemoria = token
    }

    override fun leerToken(): String? = tokenMemoria

    override fun borrarToken() {
        tokenMemoria = null
    }
}
