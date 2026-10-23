package pe.edu.upeu.pharmamobil.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Implementación de almacenamiento seguro para Android utilizando EncryptedSharedPreferences
 * con cifrado AES-256 respaldado por Android Keystore.
 */
class AlmacenSeguroAndroid(contexto: Context) : AlmacenSeguro {

    private val llave = MasterKey.Builder(contexto)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        contexto,
        "sesion_segura",
        llave,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    override fun guardarToken(token: String) {
        prefs.edit().putString(CLAVE, token).apply()
    }

    override fun leerToken(): String? = prefs.getString(CLAVE, null)

    override fun borrarToken() {
        prefs.edit().remove(CLAVE).apply()
    }

    override fun guardarIdentidad(usuario: String, nombreCompleto: String, permisos: Set<String>) {
        prefs.edit()
            .putString(CLAVE_USUARIO, usuario)
            .putString(CLAVE_NOMBRE, nombreCompleto)
            .putString(CLAVE_PERMISOS, permisos.joinToString(","))
            .apply()
    }

    override fun leerIdentidad(): Triple<String, String, Set<String>>? {
        val usuario = prefs.getString(CLAVE_USUARIO, null) ?: return null
        val nombre = prefs.getString(CLAVE_NOMBRE, null) ?: usuario
        val permisosStr = prefs.getString(CLAVE_PERMISOS, "") ?: ""
        val permisos = if (permisosStr.isBlank()) emptySet() else permisosStr.split(",").toSet()
        return Triple(usuario, nombre, permisos)
    }

    override fun borrarIdentidad() {
        prefs.edit()
            .remove(CLAVE_USUARIO)
            .remove(CLAVE_NOMBRE)
            .remove(CLAVE_PERMISOS)
            .apply()
    }

    private companion object {
        const val CLAVE = "jwt"
        const val CLAVE_USUARIO = "cached_usuario"
        const val CLAVE_NOMBRE = "cached_nombre"
        const val CLAVE_PERMISOS = "cached_permisos"
    }
}
