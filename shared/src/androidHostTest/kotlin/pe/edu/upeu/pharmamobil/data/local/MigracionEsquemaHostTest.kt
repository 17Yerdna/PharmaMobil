package pe.edu.upeu.pharmamobil.data.local

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import pe.edu.upeu.pharmamobil.db.PharmaDb
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Prueba unitaria de migración de esquema SQLite (versión 1 -> 2) en host JVM.
 * Verifica que al migrar una base instalada previamente:
 * 1. La nueva tabla [metadata_sincronizacion] se crea correctamente y es funcional.
 * 2. Todos los registros previos de la tabla [producto] se preservan intactos sin pérdida de datos.
 */
class MigracionEsquemaHostTest {

    @Test
    fun testMigracionEsquema1a2_creaMetadataYConservaProductosExistentes() {
        // 1. Inicializar driver en memoria con esquema de versión 1
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        driver.execute(
            identifier = null,
            sql = """
                CREATE TABLE producto (
                    id INTEGER NOT NULL PRIMARY KEY,
                    nombre TEXT NOT NULL,
                    precio REAL NOT NULL,
                    stock INTEGER NOT NULL,
                    activo INTEGER NOT NULL DEFAULT 1,
                    imagen TEXT NOT NULL DEFAULT '',
                    categoria TEXT NOT NULL DEFAULT '',
                    pendiente INTEGER NOT NULL DEFAULT 0
                );
            """.trimIndent(),
            parameters = 0
        )

        // 2. Insertar registros iniciales en la versión 1
        driver.execute(
            identifier = null,
            sql = "INSERT INTO producto (id, nombre, precio, stock, activo, imagen, categoria, pendiente) VALUES (1, 'Amoxicilina 500mg', 18.0, 30, 1, '', 'Antibióticos', 0);",
            parameters = 0
        )
        driver.execute(
            identifier = null,
            sql = "INSERT INTO producto (id, nombre, precio, stock, activo, imagen, categoria, pendiente) VALUES (2, 'Ibuprofeno 400mg', 7.5, 20, 1, '', 'Analgésicos', 0);",
            parameters = 0
        )

        // 3. Ejecutar la migración formal 1 -> 2 gestionada por SQLDelight (1.sqm)
        PharmaDb.Schema.migrate(driver, 1, 2)

        // 4. Instanciar PharmaDb sobre el esquema migrado
        val db = PharmaDb(driver)

        // 5. Verificar que la tabla metadata_sincronizacion existe y opera correctamente
        db.metadataSincronizacionQueries.guardarTimestamp("ultimo_sync", 1729600000000L)
        val timestampPersistido = db.metadataSincronizacionQueries.obtenerTimestamp("ultimo_sync").executeAsOneOrNull()
        assertNotNull(timestampPersistido)
        assertEquals(1729600000000L, timestampPersistido)

        // 6. Verificar que los productos preexistentes de la v1 siguen intactos
        val productos = db.productoQueries.listarTodos().executeAsList()
        assertEquals(2, productos.size, "La migración no debe alterar ni borrar los productos preexistentes")
        assertEquals(1L, productos[0].id)
        assertEquals("Amoxicilina 500mg", productos[0].nombre)
        assertEquals(2L, productos[1].id)
        assertEquals("Ibuprofeno 400mg", productos[1].nombre)
    }
}
