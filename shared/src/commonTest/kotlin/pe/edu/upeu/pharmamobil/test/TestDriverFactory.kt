package pe.edu.upeu.pharmamobil.test

import app.cash.sqldelight.db.SqlDriver

/**
 * Fábrica multiplataforma para crear instancias aisladas de [SqlDriver] en memoria
 * para pruebas unitarias sin acoplamiento a JVM o iOS en commonTest.
 */
expect fun crearDriverEnMemoria(): SqlDriver
