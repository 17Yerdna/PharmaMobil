package pe.edu.upeu.pharmamobil.di

import android.content.pm.ApplicationInfo
import org.koin.android.ext.koin.androidContext
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import pe.edu.upeu.pharmamobil.domain.platform.Compartidor
import pe.edu.upeu.pharmamobil.platform.CompartidorAndroid
import pe.edu.upeu.pharmamobil.platform.PlatformConfig
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Implementación del módulo de dependencias nativas para Android.
 */
actual val platformModule: Module = module {
    single<HttpClientEngine> { OkHttp.create() }
    single<Compartidor> { CompartidorAndroid(androidContext()) }
    single<PlatformConfig> {
        val contexto = androidContext()
        val esDebug = (contexto.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        PlatformConfig(
            urlBase = "http://10.0.2.2:8080/api/v1/",
            esDepuracion = esDebug
        )
    }
}
