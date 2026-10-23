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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import androidx.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.KoinContext
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import pe.edu.upeu.pharmamobil.domain.model.Sesion
import pe.edu.upeu.pharmamobil.domain.session.SesionEstado
import pe.edu.upeu.pharmamobil.domain.session.SessionCoordinator
import pe.edu.upeu.pharmamobil.navigation.Screen
import pe.edu.upeu.pharmamobil.navigation.tituloPantalla
import pe.edu.upeu.pharmamobil.presentation.cliente.ClienteScreen
import pe.edu.upeu.pharmamobil.presentation.inicio.InicioScreen
import pe.edu.upeu.pharmamobil.presentation.pedido.PedidoScreen
import pe.edu.upeu.pharmamobil.presentation.producto.ProductoScreen
import pe.edu.upeu.pharmamobil.presentation.producto.ProductoViewModel
import pe.edu.upeu.pharmamobil.presentation.sesion.LoginScreen
import pe.edu.upeu.pharmamobil.presentation.sesion.SesionViewModel
import pe.edu.upeu.pharmamobil.presentation.theme.AppIcons
import pe.edu.upeu.pharmamobil.presentation.theme.PharmaMobilTheme
import pe.edu.upeu.pharmamobil.presentation.theme.SemanticSuccess
import pharmamobil.shared.generated.resources.Res
import pharmamobil.shared.generated.resources.pharmamobil_logo

/**
 * Contenedor principal de PharmaMobil con guardia de autenticación JWT y RBAC:
 * - SesionEstado.Cargando: Splash de inicialización reactiva.
 * - SesionEstado.NoAutenticado / Expirado: Pantalla de Login con banners contextuales.
 * - SesionEstado.Autenticado: Navegación adaptativa (NavigationRail en Tablet, Drawer en Móvil)
 *   con filtrado de módulos por permisos y control de mutaciones CRUD.
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
        val sesionViewModel = koinViewModel<SesionViewModel>()
        val sessionCoordinator = koinInject<SessionCoordinator>()
        val estadoSesion by sessionCoordinator.estadoSesion.collectAsState()
        val uiStateSesion by sesionViewModel.uiState.collectAsState()

        PharmaMobilTheme(darkTheme = darkTheme) {
            if (uiStateSesion.cargando && estadoSesion is SesionEstado.NoAutenticado) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shadowElevation = 3.dp,
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Image(
                                    painter = painterResource(Res.drawable.pharmamobil_logo),
                                    contentDescription = "Logo PharmaMobil",
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(18.dp))
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            } else {
                when (val estado = estadoSesion) {
                    is SesionEstado.NoAutenticado -> {
                        LoginScreen(
                            viewModel = sesionViewModel,
                            sesionExpirada = false
                        )
                    }

                    is SesionEstado.Expirado -> {
                        LoginScreen(
                            viewModel = sesionViewModel,
                            sesionExpirada = true
                        )
                    }

                    is SesionEstado.Autenticado, is SesionEstado.OfflineSoloLectura -> {
                        val sesion = when (estado) {
                            is SesionEstado.Autenticado -> estado.sesion
                            is SesionEstado.OfflineSoloLectura -> estado.sesion
                            else -> error("Estado no contemplado")
                        }

                    // Si la pantalla activa es Clientes pero el rol no tiene permisos, redirigir a Inicio
                    LaunchedEffect(sesion) {
                        if (pantallaActual is Screen.Clientes && !sesion.puede("CLIENTES_LEER")) {
                            pantallaActual = Screen.Inicio
                        }
                    }

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

                                    if (sesion.puede("CLIENTES_LEER")) {
                                        NavigationRailItem(
                                            selected = pantallaActual is Screen.Clientes,
                                            onClick = { pantallaActual = Screen.Clientes },
                                            icon = { Icon(AppIcons.Person, contentDescription = "Clientes") },
                                            label = { Text("Clientes", fontSize = 11.sp, fontWeight = FontWeight.Medium) }
                                        )
                                    }

                                    NavigationRailItem(
                                        selected = pantallaActual is Screen.Pedidos,
                                        onClick = { pantallaActual = Screen.Pedidos },
                                        icon = { Icon(AppIcons.List, contentDescription = "Pedidos") },
                                        label = { Text("Pedidos", fontSize = 11.sp, fontWeight = FontWeight.Medium) }
                                    )

                                    Spacer(modifier = Modifier.weight(1f))

                                    // Avatar inicial del usuario
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier.size(36.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = sesion.usuario.take(2).uppercase(),
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }

                                    // Botón de alternancia de tema
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    ) {
                                        IconButton(
                                            onClick = { darkTheme = !darkTheme },
                                            modifier = Modifier.size(38.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (darkTheme) AppIcons.DarkMode else AppIcons.LightMode,
                                                contentDescription = "Cambiar tema",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    // Botón de Cerrar Sesión
                                    IconButton(
                                        onClick = { sesionViewModel.cerrarSesion() },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = AppIcons.Logout,
                                            contentDescription = "Cerrar sesión",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
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
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = "${sesion.nombreCompleto} (@${sesion.usuario}) · JWT",
                                                            fontSize = 11.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
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
                                                sesion = sesion,
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
                                        // Encabezado institucional con información de sesión
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

                                                // Tarjeta del usuario autenticado
                                                Surface(
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                                        Text(
                                                            text = sesion.nombreCompleto,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            modifier = Modifier.fillMaxWidth()
                                                        ) {
                                                            Text(
                                                                text = "@${sesion.usuario}",
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = MaterialTheme.colorScheme.primary,
                                                                fontWeight = FontWeight.SemiBold
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
                                                                    text = "Sesión JWT",
                                                                    fontSize = 10.sp,
                                                                    fontWeight = FontWeight.Medium,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 16.dp),
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))

                                        // Opciones de Navegación filtradas por permisos
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

                                        if (sesion.puede("CLIENTES_LEER")) {
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
                                        }

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
                                                .padding(horizontal = 20.dp, vertical = 12.dp),
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

                                        // Botón de Cerrar Sesión en Drawer
                                        NavigationDrawerItem(
                                            icon = {
                                                Icon(
                                                    imageVector = AppIcons.Logout,
                                                    contentDescription = "Cerrar Sesión",
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            },
                                            label = {
                                                Text(
                                                    text = "Cerrar Sesión",
                                                    color = MaterialTheme.colorScheme.error,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            },
                                            selected = false,
                                            onClick = {
                                                scope.launch { drawerState.close() }
                                                sesionViewModel.cerrarSesion()
                                            },
                                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                        )

                                        // Acciones de Diagnóstico en Depuración (Escenario E3 y Efecto 2)
                                        if (sesionViewModel.esDepuracion) {
                                            HorizontalDivider(
                                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                            )
                                            Text(
                                                text = "DIAGNÓSTICO (DEBUG S10)",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                            )
                                            NavigationDrawerItem(
                                                icon = {
                                                    Icon(
                                                        imageVector = AppIcons.Warning,
                                                        contentDescription = "Simular Token Alterado",
                                                        tint = MaterialTheme.colorScheme.error
                                                    )
                                                },
                                                label = {
                                                    Text(
                                                        text = "Simular Token Alterado (E3)",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.error
                                                    )
                                                },
                                                selected = false,
                                                onClick = {
                                                    scope.launch { drawerState.close() }
                                                    sesionViewModel.corromperTokenParaPruebaDebug()
                                                },
                                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                            )
                                            NavigationDrawerItem(
                                                icon = {
                                                    Icon(
                                                        imageVector = AppIcons.Logout,
                                                        contentDescription = "Logout y Probar 401",
                                                        tint = MaterialTheme.colorScheme.outline
                                                    )
                                                },
                                                label = {
                                                    Text(
                                                        text = "Logout + Probar 401 (Efecto 2)",
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                },
                                                selected = false,
                                                onClick = {
                                                    scope.launch { drawerState.close() }
                                                    sesionViewModel.probarPeticionSinTokenPostLogout()
                                                },
                                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(16.dp))
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
                                                            text = "${sesion.nombreCompleto} · JWT",
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
                                                label = { Text("Inicio", fontSize = 10.sp, fontWeight = FontWeight.Medium) }
                                            )
                                            NavigationBarItem(
                                                selected = pantallaActual is Screen.Productos,
                                                onClick = { pantallaActual = Screen.Productos },
                                                icon = { Icon(AppIcons.ShoppingCart, contentDescription = "Productos") },
                                                label = { Text("Inventario", fontSize = 10.sp, fontWeight = FontWeight.Medium) }
                                            )

                                            if (sesion.puede("CLIENTES_LEER")) {
                                                NavigationBarItem(
                                                    selected = pantallaActual is Screen.Clientes,
                                                    onClick = { pantallaActual = Screen.Clientes },
                                                    icon = { Icon(AppIcons.Person, contentDescription = "Clientes") },
                                                    label = { Text("Clientes", fontSize = 10.sp, fontWeight = FontWeight.Medium) }
                                                )
                                            }

                                            NavigationBarItem(
                                                selected = pantallaActual is Screen.Pedidos,
                                                onClick = { pantallaActual = Screen.Pedidos },
                                                icon = { Icon(AppIcons.List, contentDescription = "Pedidos") },
                                                label = { Text("Pedidos", fontSize = 10.sp, fontWeight = FontWeight.Medium) }
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
                                                sesion = sesion,
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
    }
}
}



/**
 * Renderizado desacoplado del contenido dinámico de la aplicación aplicando RBAC.
 */
@Composable
private fun ContenidoDestino(
    pantallaActual: Screen,
    sesion: Sesion,
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
            ProductoScreen(
                viewModel = viewModel,
                puedeEscribir = sesion.puede("CATALOGO_ESCRIBIR")
            )
        }
        is Screen.Clientes -> ClienteScreen()
        is Screen.Pedidos -> PedidoScreen()
    }
}