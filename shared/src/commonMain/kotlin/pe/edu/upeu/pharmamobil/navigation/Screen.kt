package pe.edu.upeu.pharmamobil.navigation

/**
 * Representa los destinos tipados de navegación en PharmaMobil.
 */
sealed class Screen {
    data object Inicio : Screen()
    data object Productos : Screen()
    data object Clientes : Screen()
    data object Pedidos : Screen()
}

/**
 * Retorna el título contextual para la TopAppBar según el destino actual.
 */
fun tituloPantalla(screen: Screen): String = when (screen) {
    is Screen.Inicio -> "Inicio - PharmaMobil"
    is Screen.Productos -> "Registro de Productos"
    is Screen.Clientes -> "Gestión de Clientes"
    is Screen.Pedidos -> "Control de Pedidos"
}
