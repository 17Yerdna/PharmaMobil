package pe.edu.upeu.pharmamobil.di

import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.bind
import org.koin.dsl.module
import pe.edu.upeu.pharmamobil.data.remote.ProductoApi
import pe.edu.upeu.pharmamobil.data.remote.crearHttpClient
import pe.edu.upeu.pharmamobil.data.repository.ProductoRepositorioRest
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository
import pe.edu.upeu.pharmamobil.domain.usecase.RegistrarProductoUseCase
import pe.edu.upeu.pharmamobil.presentation.producto.ProductoViewModel

/**
 * Módulo de la capa de datos: Registra HttpClient, ProductoApi y ProductoRepositorioRest.
 */
val dataModule = module {
    single { crearHttpClient(get()) }
    singleOf(::ProductoApi)
    singleOf(::ProductoRepositorioRest) bind ProductoRepository::class
}

/**
 * Módulo de la capa de dominio: Registra los casos de uso.
 */
val domainModule = module {
    factoryOf(::RegistrarProductoUseCase)
}

/**
 * Módulo de la capa de presentación: Registra el ViewModel.
 */
val viewModelModule = module {
    viewModelOf(::ProductoViewModel)
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
