package pe.edu.upeu.pharmamobil.di

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import pe.edu.upeu.pharmamobil.data.local.AlmacenSeguro
import pe.edu.upeu.pharmamobil.data.local.AlmacenSeguroIos
import pe.edu.upeu.pharmamobil.data.local.CreadorDeDriver
import pe.edu.upeu.pharmamobil.data.local.CreadorDeDriverIos
import pe.edu.upeu.pharmamobil.domain.platform.Compartidor
import pe.edu.upeu.pharmamobil.platform.CompartidorIos
import pe.edu.upeu.pharmamobil.platform.PlatformConfig
import pe.edu.upeu.pharmamobil.platform.ProveedorHora
import pe.edu.upeu.pharmamobil.platform.ProveedorHoraIos
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Implementación del motor HTTP y configuración nativa para iOS con Darwin.
 * La URL localhost aplica para ejecuciones en Simulador iOS.
 */
actual val platformModule: Module = module {
    single<HttpClientEngine> { Darwin.create() }
    single<Compartidor> { CompartidorIos() }
    single<AlmacenSeguro> { AlmacenSeguroIos() }
    single<CreadorDeDriver> { CreadorDeDriverIos() }
    single<ProveedorHora> { ProveedorHoraIos() }
    single<PlatformConfig> {
        PlatformConfig(
            urlBase = "http://localhost:8080/api/v1/",
            esDepuracion = false
        )
    }
}
