package pe.edu.upeu.pharmamobil.di

import org.koin.android.ext.koin.androidContext
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import pe.edu.upeu.pharmamobil.domain.platform.Compartidor
import pe.edu.upeu.pharmamobil.platform.CompartidorAndroid
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Implementación del motor HTTP para Android con OkHttp.
 */
actual val platformModule: Module = module {
    single<HttpClientEngine> { OkHttp.create() }
    single<Compartidor> { CompartidorAndroid(androidContext()) }
}
