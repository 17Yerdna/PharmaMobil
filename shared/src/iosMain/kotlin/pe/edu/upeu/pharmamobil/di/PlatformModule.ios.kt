package pe.edu.upeu.pharmamobil.di

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Implementación del motor HTTP para iOS con Darwin (NSURLSession).
 */
actual val platformModule: Module = module {
    single<HttpClientEngine> { Darwin.create() }
}
