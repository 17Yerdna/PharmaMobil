package pe.edu.upeu.pharmamobil

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import androidx.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.KoinContext
import org.koin.compose.viewmodel.koinViewModel
import pe.edu.upeu.pharmamobil.navigation.Screen
import pe.edu.upeu.pharmamobil.navigation.tituloPantalla
import pe.edu.upeu.pharmamobil.presentation.cliente.ClienteScreen
import pe.edu.upeu.pharmamobil.presentation.inicio.InicioScreen
import pe.edu.upeu.pharmamobil.presentation.pedido.PedidoScreen
import pe.edu.upeu.pharmamobil.presentation.producto.ProductoScreen
import pe.edu.upeu.pharmamobil.presentation.producto.ProductoViewModel
import pe.edu.upeu.pharmamobil.presentation.theme.AppIcons
import pe.edu.upeu.pharmamobil.presentation.theme.PharmaMobilTheme
import pe.edu.upeu.pharmamobil.presentation.theme.SemanticSuccess
import pharmamobil.shared.generated.resources.Res
import pharmamobil.shared.generated.resources.pharmamobil_logo

/**
 * Contenedor principal de PharmaMobil con navegación adaptativa moderna:
 * - Teléfono (< 600dp): Scaffold con TopBar estilizada, Bottom Navigation Bar interactivo y Drawer institucional.
 * - Tablet (>= 600dp): NavigationRail lateral de alta densidad con Scaffold central.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun App() {
    // Estado del tema (Light / Dark)
    var darkTheme by remember { mutableStateOf(false) }

    // Estado de navegación y destino actual
    var pantallaActual by remember { mutableStateOf<Screen>(Screen.Inicio) }

    // Estado del Drawer y CoroutineScope
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    KoinContext {
        PharmaMobilTheme(darkTheme = darkTheme) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val esPantallaMedianaOAmplia = maxWidth >= 600.dp

                if (esPantallaMedianaOAmplia) {
                    // Modo Tablet / Pantalla Amplia (NavigationRail)
                    Row(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                        NavigationRail(
                            modifier = Modifier.width(90.dp),
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            header = {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(top = 16.dp, bottom = 12.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Image(
                                                painter = painterResource(Res.drawable.pharmamobil_logo),
                                                contentDescription = "Logo PharmaMobil",
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        ) {
                            Spacer(modifier = Modifier.height(8.dp))

                            NavigationRailItem(
                                selected = pantallaActual is Screen.Inicio,
                                onClick = { pantallaActual = Screen.Inicio },
                                icon = { Icon(AppIcons.Home, contentDescription = "Inicio") },
                                label = { Text("Inicio", fontSize = 11.sp, fontWeight = FontWeight.Medium) }
                            )
                            NavigationRailItem(
                                selected = pantallaActual is Screen.Productos,
                                onClick = { pantallaActual = Screen.Productos },
                                icon = { Icon(AppIcons.ShoppingCart, contentDescription = "Productos") },
                                label = { Text("Inventario", fontSize = 11.sp, fontWeight = FontWeight.Medium) }
                            )
                            NavigationRailItem(
                                selected = pantallaActual is Screen.Clientes,
                                onClick = { pantallaActual = Screen.Clientes },
                                icon = { Icon(AppIcons.Person, contentDescription = "Clientes") },
                                label = { Text("Clientes", fontSize = 11.sp, fontWeight = FontWeight.Medium) }
                            )
                            NavigationRailItem(
                                selected = pantallaActual is Screen.Pedidos,
                                onClick = { pantallaActual = Screen.Pedidos },
                                icon = { Icon(AppIcons.List, contentDescription = "Pedidos") },
                                label = { Text("Pedidos", fontSize = 11.sp, fontWeight = FontWeight.Medium) }
                            )

                            Spacer(modifier = Modifier.weight(1f))

                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.padding(bottom = 16.dp)
                            ) {
                                IconButton(
                                    onClick = { darkTheme = !darkTheme },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = if (darkTheme) AppIcons.DarkMode else AppIcons.LightMode,
                                        contentDescription = "Cambiar tema",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        VerticalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            thickness = 1.dp
                        )

                        // Scaffold central para Tablet
                        Scaffold(
                            topBar = {
                                TopAppBar(
                                    title = {
                                        Column {
                                            Text(
                                                text = tituloPantalla(pantallaActual),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 20.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .clip(CircleShape)
                                                        .background(SemanticSuccess)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Ktor REST Conectado",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    },
                                    actions = {
                                        IconButton(onClick = { darkTheme = !darkTheme }) {
                                            Icon(
                                                imageVector = if (darkTheme) AppIcons.DarkMode else AppIcons.LightMode,
                                                contentDescription = "Alternar Modo Claro/Oscuro",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    },
                                    colors = TopAppBarDefaults.topAppBarColors(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        titleContentColor = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        ) { paddingValues ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(paddingValues)
                                    .background(MaterialTheme.colorScheme.background)
                            ) {
                                AnimatedContent(
                                    targetState = pantallaActual,
                                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                                    label = "ScreenTransitionTablet"
                                ) { screen ->
                                    ContenidoDestino(
                                        pantallaActual = screen,
                                        onNavigate = { pantallaActual = it }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Modo Móvil con ModalNavigationDrawer + BottomNavigationBar
                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        drawerContent = {
                            ModalDrawerSheet(
                                modifier = Modifier.width(310.dp),
                                drawerContainerColor = MaterialTheme.colorScheme.surface,
                                drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
                            ) {
                                // Encabezado institucional con gradiente
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    MaterialTheme.colorScheme.primaryContainer,
                                                    MaterialTheme.colorScheme.surface
                                                )
                                            )
                                        )
                                        .padding(horizontal = 20.dp, vertical = 24.dp)
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = MaterialTheme.colorScheme.surface,
                                                shadowElevation = 2.dp,
                                                modifier = Modifier.size(48.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Image(
                                                        painter = painterResource(Res.drawable.pharmamobil_logo),
                                                        contentDescription = "Logo PharmaMobil",
                                                        modifier = Modifier.size(32.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(14.dp))
                                            Column {
                                                Text(
                                                    text = "PharmaMobil",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "Gestión Farmacéutica",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(14.dp))

                                        // Badge de estado Ktor REST
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .clip(CircleShape)
                                                        .background(SemanticSuccess)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "API REST Ktor Online · v1.0",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }

                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                // Opciones de Navegación
                                NavigationDrawerItem(
                                    icon = { Icon(AppIcons.Home, contentDescription = "Inicio") },
                                    label = { Text("Inicio", fontWeight = FontWeight.Medium) },
                                    selected = pantallaActual is Screen.Inicio,
                                    onClick = {
                                        pantallaActual = Screen.Inicio
                                        scope.launch { drawerState.close() }
                                    },
                                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                )

                                NavigationDrawerItem(
                                    icon = { Icon(AppIcons.ShoppingCart, contentDescription = "Productos") },
                                    label = { Text("Inventario de Productos", fontWeight = FontWeight.Medium) },
                                    selected = pantallaActual is Screen.Productos,
                                    onClick = {
                                        pantallaActual = Screen.Productos
                                        scope.launch { drawerState.close() }
                                    },
                                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                )

                                NavigationDrawerItem(
                                    icon = { Icon(AppIcons.Person, contentDescription = "Clientes") },
                                    label = { Text("Directorio de Clientes", fontWeight = FontWeight.Medium) },
                                    selected = pantallaActual is Screen.Clientes,
                                    onClick = {
                                        pantallaActual = Screen.Clientes
                                        scope.launch { drawerState.close() }
                                    },
                                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                )

                                NavigationDrawerItem(
                                    icon = { Icon(AppIcons.List, contentDescription = "Pedidos") },
                                    label = { Text("Control de Pedidos", fontWeight = FontWeight.Medium) },
                                    selected = pantallaActual is Screen.Pedidos,
                                    onClick = {
                                        pantallaActual = Screen.Pedidos
                                        scope.launch { drawerState.close() }
                                    },
                                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                )

                                Spacer(modifier = Modifier.weight(1f))
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                )

                                // Control de Tema Claro / Oscuro en Drawer
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (darkTheme) AppIcons.DarkMode else AppIcons.LightMode,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = if (darkTheme) "Modo Oscuro" else "Modo Claro",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Switch(
                                        checked = darkTheme,
                                        onCheckedChange = { darkTheme = it }
                                    )
                                }
                            }
                        }
                    ) {
                        Scaffold(
                            topBar = {
                                TopAppBar(
                                    title = {
                                        Column {
                                            Text(
                                                text = tituloPantalla(pantallaActual),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 18.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .clip(CircleShape)
                                                        .background(SemanticSuccess)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Online · Ktor REST",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    },
                                    navigationIcon = {
                                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                            Icon(
                                                imageVector = AppIcons.Menu,
                                                contentDescription = "Abrir Menú de Navegación",
                                                tint = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    },
                                    actions = {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier.padding(end = 8.dp)
                                        ) {
                                            IconButton(
                                                onClick = { darkTheme = !darkTheme },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (darkTheme) AppIcons.DarkMode else AppIcons.LightMode,
                                                    contentDescription = "Alternar Modo Claro/Oscuro",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    },
                                    colors = TopAppBarDefaults.topAppBarColors(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        titleContentColor = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            },
                            bottomBar = {
                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 6.dp
                                ) {
                                    NavigationBarItem(
                                        selected = pantallaActual is Screen.Inicio,
                                        onClick = { pantallaActual = Screen.Inicio },
                                        icon = { Icon(AppIcons.Home, contentDescription = "Inicio") },
                                        label = { Text("Inicio", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                        )
                                    )
                                    NavigationBarItem(
                                        selected = pantallaActual is Screen.Productos,
                                        onClick = { pantallaActual = Screen.Productos },
                                        icon = { Icon(AppIcons.ShoppingCart, contentDescription = "Productos") },
                                        label = { Text("Inventario", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                        )
                                    )
                                    NavigationBarItem(
                                        selected = pantallaActual is Screen.Clientes,
                                        onClick = { pantallaActual = Screen.Clientes },
                                        icon = { Icon(AppIcons.Person, contentDescription = "Clientes") },
                                        label = { Text("Clientes", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                        )
                                    )
                                    NavigationBarItem(
                                        selected = pantallaActual is Screen.Pedidos,
                                        onClick = { pantallaActual = Screen.Pedidos },
                                        icon = { Icon(AppIcons.List, contentDescription = "Pedidos") },
                                        label = { Text("Pedidos", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                        )
                                    )
                                }
                            }
                        ) { paddingValues ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(paddingValues)
                                    .background(MaterialTheme.colorScheme.background)
                            ) {
                                AnimatedContent(
                                    targetState = pantallaActual,
                                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                                    label = "ScreenTransitionMobile"
                                ) { screen ->
                                    ContenidoDestino(
                                        pantallaActual = screen,
                                        onNavigate = { pantallaActual = it }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Renderizado desacoplado del contenido dinámico de la aplicación.
 */
@Composable
private fun ContenidoDestino(
    pantallaActual: Screen,
    onNavigate: (Screen) -> Unit
) {
    when (pantallaActual) {
        is Screen.Inicio -> InicioScreen(
            onNavigateToProductos = { onNavigate(Screen.Productos) },
            onNavigateToClientes = { onNavigate(Screen.Clientes) },
            onNavigateToPedidos = { onNavigate(Screen.Pedidos) }
        )
        is Screen.Productos -> {
            val viewModel = koinViewModel<ProductoViewModel>()
            ProductoScreen(viewModel = viewModel)
        }
        is Screen.Clientes -> ClienteScreen()
        is Screen.Pedidos -> PedidoScreen()
    }
}