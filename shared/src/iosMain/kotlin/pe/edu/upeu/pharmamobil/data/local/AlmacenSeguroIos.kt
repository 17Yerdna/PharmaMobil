package pe.edu.upeu.pharmamobil.data.local

/**
 * Adaptador de desarrollo en memoria para iOS.
 *
 * NOTA DE ARQUITECTURA / ENTORNO ACADÉMICO:
 * Debido a que el desarrollo y compilación se realizan en un entorno Windows sin acceso
 * a las herramientas de Apple (macOS / Xcode SDK / Security framework), esta implementación
 * mantiene el token en memoria únicamente para propósitos de compilación multiplataforma.
 *
 * La integración con Keychain Services (SecItemAdd, SecItemCopyMatching, SecItemDelete)
 * está planificada para ser implementada y validada al disponer de un entorno macOS oficial.
 * No proporciona persistencia segura entre reinicios de la aplicación en este estado.
 */
class AlmacenSeguroIos : AlmacenSeguro {
    private var tokenMemoria: String? = null
    private var identidadMemoria: Triple<String, String, Set<String>>? = null

    override fun guardarToken(token: String) {
        tokenMemoria = token
    }

    override fun leerToken(): String? = tokenMemoria

    override fun borrarToken() {
        tokenMemoria = null
    }

    override fun guardarIdentidad(usuario: String, nombreCompleto: String, permisos: Set<String>) {
        identidadMemoria = Triple(usuario, nombreCompleto, permisos)
    }

    override fun leerIdentidad(): Triple<String, String, Set<String>>? = identidadMemoria

    override fun borrarIdentidad() {
        identidadMemoria = null
    }
}
