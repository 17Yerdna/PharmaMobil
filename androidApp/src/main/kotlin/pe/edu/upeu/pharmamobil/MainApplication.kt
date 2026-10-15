package pe.edu.upeu.pharmamobil

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.logger.Level
import pe.edu.upeu.pharmamobil.di.initKoin

/**
 * Punto de entrada de la aplicación en Android.
 * Inicializa Koin inyectando el contexto de la aplicación.
 */
class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        initKoin {
            androidLogger(Level.ERROR)
            androidContext(this@MainApplication)
        }
    }
}
