package pe.edu.upeu.pharmamobil.data.local

import app.cash.sqldelight.db.SqlDriver

/**
 * Fábrica multiplataforma de drivers SQLite para SQLDelight 2.1.0.
 * Conecta el patrón canónico 'DatabaseDriverFactory' de la documentación oficial
 * con la inyección de dependencias de Koin.
 */
interface CreadorDeDriver {
    fun crear(): SqlDriver
}

typealias DatabaseDriverFactory = CreadorDeDriver
