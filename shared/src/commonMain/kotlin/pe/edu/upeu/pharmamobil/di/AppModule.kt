package pe.edu.upeu.pharmamobil.di

import org.koin.core.context.startKoin
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module
import pe.edu.upeu.pharmamobil.data.local.CreadorDeDriver
import pe.edu.upeu.pharmamobil.data.local.ProductoLocal
import pe.edu.upeu.pharmamobil.data.remote.ProductoApi
import pe.edu.upeu.pharmamobil.data.remote.crearHttpClient
import pe.edu.upeu.pharmamobil.data.repository.AuthRepositorioRest
import pe.edu.upeu.pharmamobil.data.repository.ProductoRepositorioOffline
import pe.edu.upeu.pharmamobil.db.PharmaDb
import pe.edu.upeu.pharmamobil.domain.repository.AuthRepository
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository
import pe.edu.upeu.pharmamobil.domain.session.SessionCoordinator
import pe.edu.upeu.pharmamobil.domain.usecase.*
import pe.edu.upeu.pharmamobil.presentation.producto.ProductoViewModel
import pe.edu.upeu.pharmamobil.presentation.sesion.SesionViewModel

private const val CATEGORIA_POR_DEFECTO = 1L

/**
 * Módulo de la capa de datos: Registra HttpClient, BD SQLite, ProductoApi, AuthRepositorioRest y ProductoRepositorioOffline.
 */
val dataModule = module {
    single { crearHttpClient(get(), get(), get(), get()) }
    single { PharmaDb(get<CreadorDeDriver>().crear()) }
    singleOf(::ProductoLocal)
    singleOf(::ProductoApi)
    single<AuthRepository> {
        AuthRepositorioRest(get(), get())
    }
    single {
        ProductoRepositorioOffline(
            local = get(),
            api = get(),
            proveedorHora = get(),
            categoriaPorDefecto = CATEGORIA_POR_DEFECTO
        )
    }
    single<ProductoRepository> {
        get<ProductoRepositorioOffline>()
    }
}

/**
 * Módulo de la capa de dominio: Registra SessionCoordinator y los casos de uso.
 */
val domainModule = module {
    singleOf(::SessionCoordinator)
    factoryOf(::IniciarSesionUseCase)
    factoryOf(::CerrarSesionUseCase)
    factoryOf(::SesionGuardadaUseCase)
    factoryOf(::RevalidarSesionUseCase)
    factoryOf(::ObservarProductosUseCase)
    factoryOf(::SincronizarProductosUseCase)
    factoryOf(::RegistrarProductoUseCase)
    factoryOf(::ActualizarProductoUseCase)
    factoryOf(::EliminarProductoUseCase)
}

/**
 * Módulo de la capa de presentación: Registra los ViewModels.
 */
val viewModelModule = module {
    viewModelOf(::ProductoViewModel)
    viewModelOf(::SesionViewModel)
}

/**
 * Inicializador global de Koin para todas las plataformas.
 */
fun initKoin(appDeclaration: KoinAppDeclaration = {}) =
    startKoin {
        appDeclaration()
        modules(
            platformModule,
            dataModule,
            domainModule,
            viewModelModule
        )
    }
