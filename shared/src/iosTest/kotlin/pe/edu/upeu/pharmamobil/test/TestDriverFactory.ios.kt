package pe.edu.upeu.pharmamobil.test

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.inMemoryDriver
import pe.edu.upeu.pharmamobil.db.PharmaDb

actual fun crearDriverEnMemoria(): SqlDriver = inMemoryDriver(PharmaDb.Schema)
