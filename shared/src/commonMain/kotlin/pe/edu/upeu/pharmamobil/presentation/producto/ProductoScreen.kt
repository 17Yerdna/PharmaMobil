package pe.edu.upeu.pharmamobil.presentation.producto

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.presentation.theme.*

/**
 * Pantalla de Inventario de Productos con CRUD REST completo:
 * - Listado paginado en cuadrícula cuadrada con imágenes y paleta Mostaza.
 * - Formulario reactivo para Crear (POST) y Editar (PUT) con errores del servidor campo por campo.
 * - Diálogo de confirmación para Eliminación (DELETE 204 No Content).
 * - Manejo de estados desacoplado sin parpadeo (FaseInventario vs Operacion).
 */
@Composable
fun ProductoScreen(
    viewModel: ProductoViewModel,
    puedeEscribir: Boolean = true,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    val tabs = listOf(
        "Activos" to uiState.totalActivos,
        "Inactivos" to uiState.totalInactivos,
        "Bajo Stock" to uiState.totalBajoStock
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // ==========================================
        // HEADER CON ACCIÓN PRINCIPAL
        // ==========================================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Inventario de Productos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = uiState.ultimaSincronizacion ?: "Nunca se ha sincronizado",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = { viewModel.sincronizar() },
                    enabled = !uiState.estaSincronizando,
                    modifier = Modifier.size(36.dp)
                ) {
                    if (uiState.estaSincronizando) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            imageVector = AppIcons.CheckCircle,
                            contentDescription = "Sincronizar ahora",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                if (uiState.esDepuracion) {
                    OutlinedButton(
                        onClick = { viewModel.simularOperacion403() },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = AppIcons.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Test 403",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                if (puedeEscribir) {
                    Button(
                        onClick = { viewModel.toggleMostrarFormulario() },
                        shape = RoundedCornerShape(12.dp),
                        colors = if (uiState.mostrarFormulario) {
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        },
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = if (uiState.mostrarFormulario) AppIcons.Close else AppIcons.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (uiState.mostrarFormulario) "Cerrar" else "Nuevo",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Indicador de operación global en curso
        if (uiState.operacion is Operacion.EnCurso) {
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer
            )
        }

        // Banner Offline-First (Modo sin conexión y productos pendientes)
        if (uiState.avisoRed || uiState.totalPendientes > 0) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (uiState.totalPendientes > 0) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = AppIcons.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (uiState.avisoRed) "Modo sin conexión (Offline)" else "Sincronización pendiente",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (uiState.totalPendientes > 0) {
                                Text(
                                    text = "${uiState.totalPendientes} producto(s) guardado(s) localmente en SQLite",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Button(
                        onClick = { viewModel.sincronizar() },
                        enabled = !uiState.estaSincronizando,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        if (uiState.estaSincronizando) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(
                                text = "Sincronizar",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Banner de Éxito
        uiState.mensajeExito?.let { mensaje ->
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SemanticSuccessContainerLight,
                border = androidx.compose.foundation.BorderStroke(1.dp, SemanticSuccess.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = AppIcons.CheckCircle,
                            contentDescription = null,
                            tint = SemanticSuccess,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = mensaje,
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSemanticSuccessLight,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    IconButton(
                        onClick = { viewModel.descartarMensajes() },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = AppIcons.Close,
                            contentDescription = "Cerrar",
                            tint = OnSemanticSuccessLight,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        // Banner de Operación Fallida
        (uiState.operacion as? Operacion.Fallida)?.let { opFallida ->
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = AppIcons.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = opFallida.mensaje,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    IconButton(
                        onClick = { viewModel.descartarMensajes() },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = AppIcons.Close,
                            contentDescription = "Cerrar",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ==========================================
        // BARRA DE BÚSQUEDA REACTIVA
        // ==========================================
        OutlinedTextField(
            value = uiState.busqueda,
            onValueChange = viewModel::onBusquedaChanged,
            placeholder = {
                Text(
                    text = "Buscar por nombre (ej. Paracetamol)...",
                    style = MaterialTheme.typography.bodySmall
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = AppIcons.Search,
                    contentDescription = "Buscar",
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                if (uiState.busqueda.isNotBlank()) {
                    IconButton(onClick = { viewModel.onBusquedaChanged("") }) {
                        Icon(
                            imageVector = AppIcons.Close,
                            contentDescription = "Limpiar búsqueda",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ==========================================
        // TABS SEGMENTADAS CON BADGES NUMÉRICOS
        // ==========================================
        PrimaryTabRow(
            selectedTabIndex = uiState.tabSeleccionada,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(14.dp)
                )
        ) {
            tabs.forEachIndexed { index, (titulo, cantidad) ->
                val isSelected = uiState.tabSeleccionada == index
                Tab(
                    selected = isSelected,
                    onClick = { viewModel.seleccionarTab(index) },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = titulo,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            ) {
                                Text(
                                    text = "$cantidad",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ==========================================
        // FORMULARIO O LISTA DE INVENTARIO
        // ==========================================
        AnimatedVisibility(
            visible = uiState.mostrarFormulario,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            FormularioProductoCard(
                formulario = uiState.formulario,
                onNombreChanged = viewModel::onNombreChanged,
                onPrecioChanged = viewModel::onPrecioChanged,
                onStockChanged = viewModel::onStockChanged,
                onActivoChanged = viewModel::onActivoChanged,
                onGuardarClick = viewModel::guardarProducto,
                onCancelarClick = viewModel::cerrarFormulario
            )
        }

        if (!uiState.mostrarFormulario) {
            when (val fase = uiState.fase) {
                is FaseInventario.Cargando -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Sincronizando inventario con Ktor REST...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                is FaseInventario.SinProductos -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(56.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = AppIcons.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (uiState.busqueda.isNotBlank()) "No se encontraron resultados" else "Sin productos en esta categoría",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (uiState.busqueda.isNotBlank()) {
                                    "Intenta con otro término o limpia la barra de búsqueda."
                                } else {
                                    "Utiliza el botón superior '+ Nuevo' para registrar nuevos ítems en el catálogo."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                is FaseInventario.ConProductos -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(fase.productos, key = { it.id }) { productoUi ->
                            val producto = productoUi.producto
                            ProductoItemCard(
                                producto = producto,
                                precioFormateado = productoUi.precioFormateado,
                                onDetalleClick = { viewModel.mostrarDetalle(productoUi) },
                                onEditarClick = { viewModel.iniciarEdicion(producto) },
                                onEliminarClick = { viewModel.solicitarEliminacion(producto) },
                                habilitado = uiState.operacion !is Operacion.EnCurso,
                                puedeEscribir = puedeEscribir
                            )

                        }
                    }
                }

                is FaseInventario.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .padding(16.dp)
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(18.dp)
                                )
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = AppIcons.Warning,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Error de Conexión REST",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = fase.mensaje,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { viewModel.sincronizar() },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = AppIcons.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Reintentar Conexión", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // DIÁLOGO DE CONFIRMACIÓN DE ELIMINACIÓN
    // ==========================================
    uiState.productoAEliminar?.let { producto ->
        AlertDialog(
            onDismissRequest = { viewModel.cancelarEliminacion() },
            title = {
                Text(
                    text = "Confirmar Eliminación",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "¿Está seguro de eliminar '${producto.nombre}' (#00${producto.id})? Esta acción ejecutará la mutación DELETE en el servidor REST.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmarEliminacion() },
                    colors = ButtonDefaults.buttonColors(containerColor = SemanticDanger),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Eliminar", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { viewModel.cancelarEliminacion() },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancelar")
                }
            },
            shape = RoundedCornerShape(18.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    uiState.productoEnDetalle?.let { productoUi ->
        val producto = productoUi.producto
        AlertDialog(
            onDismissRequest = { viewModel.cerrarDetalle() },
            title = { Text(producto.nombre, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Precio: ${productoUi.precioFormateado}")
                    Text("Stock disponible: ${producto.stock} unidades")
                    Text(if (producto.activo) "Estado: Activo" else "Estado: Inactivo")
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.compartirProducto(producto) }) {
                    Text("Compartir")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.cerrarDetalle() }) {
                    Text("Cerrar")
                }
            },
            shape = RoundedCornerShape(18.dp)
        )
    }
}

/**
 * Componente de formulario de producto con soporte para Crear (POST) y Editar (PUT)
 * y visualización de errores devueltos por el servidor campo por campo.
 */
@Composable
private fun FormularioProductoCard(
    formulario: FormularioProductoState,
    onNombreChanged: (String) -> Unit,
    onPrecioChanged: (String) -> Unit,
    onStockChanged: (String) -> Unit,
    onActivoChanged: (Boolean) -> Unit,
    onGuardarClick: () -> Unit,
    onCancelarClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(bottom = 12.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (formulario.esEdicion) "Editar Producto #${formulario.id}" else "Nuevo Registro Farmacéutico",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = if (formulario.esEdicion) "Ktor PUT" else "Ktor POST",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Campo Nombre con error de validación bajo el input
            OutlinedTextField(
                value = formulario.nombre,
                onValueChange = onNombreChanged,
                label = { Text("Nombre del Producto o Medicamento") },
                placeholder = { Text("Ej. Paracetamol 500 mg") },
                isError = formulario.nombreError != null,
                supportingText = formulario.nombreError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                // Campo Precio con error de validación bajo el input
                OutlinedTextField(
                    value = formulario.precio,
                    onValueChange = onPrecioChanged,
                    label = { Text("Precio (S/.)") },
                    placeholder = { Text("15.50") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = formulario.precioError != null,
                    supportingText = formulario.precioError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(10.dp))
                // Campo Stock con error de validación bajo el input
                OutlinedTextField(
                    value = formulario.stock,
                    onValueChange = onStockChanged,
                    label = { Text("Stock Inicial") },
                    placeholder = { Text("100") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = formulario.stockError != null,
                    supportingText = formulario.stockError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Column {
                        Text(
                            text = if (formulario.activo) "Producto Habilitado (Activo)" else "Producto Deshabilitado (Inactivo)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (formulario.activo) "Disponible para órdenes y pedidos" else "Oculto en catálogo público",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = formulario.activo,
                        onCheckedChange = onActivoChanged
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onCancelarClick,
                    enabled = !formulario.estaGuardando,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("Cancelar")
                }

                Button(
                    onClick = onGuardarClick,
                    enabled = !formulario.estaGuardando,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(2f)
                        .height(48.dp)
                ) {
                    if (formulario.estaGuardando) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = if (formulario.esEdicion) "Guardar Cambios" else "Registrar Producto",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Tarjeta cuadrada de producto en cuadrícula con visualización de imagen remota,
 * pastillas de stock y botones de acción rápida de edición y eliminación.
 */
@Composable
fun ProductoItemCard(
    producto: Producto,
    precioFormateado: String,
    onDetalleClick: () -> Unit = {},
    onEditarClick: () -> Unit = {},
    onEliminarClick: () -> Unit = {},
    habilitado: Boolean = true,
    puedeEscribir: Boolean = true,
    modifier: Modifier = Modifier
) {
    val esBajoStock = producto.requiereReposicion()

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = when {
                    !producto.activo -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    producto.stock == 0 -> SemanticDanger.copy(alpha = 0.35f)
                    esBajoStock -> SemanticWarning.copy(alpha = 0.35f)
                    else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                },
                shape = RoundedCornerShape(18.dp)
            )
            .animateContentSize()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Contenedor Cuadrado / Superior de Imagen
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                if (producto.imagen.isNotBlank()) {
                    AsyncImage(
                        model = producto.imagen,
                        contentDescription = producto.nombre,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = AppIcons.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Tag flotante de Categoría o SKU en la esquina superior
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Text(
                        text = if (producto.categoria.isNotBlank() && producto.categoria != "General") producto.categoria else "#00${producto.id}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Cuerpo de información del producto
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = producto.nombre,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    minLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = precioFormateado,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                    ) {
                        Text(
                            text = "#00${producto.id}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Pastilla semántica de stock compacta
                if (producto.id < 0L) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "⏳ Pendiente de sincronización",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Surface(
                    color = when {
                        !producto.activo -> MaterialTheme.colorScheme.surfaceVariant
                        producto.stock == 0 -> SemanticDangerContainerLight
                        producto.stock <= Producto.STOCK_MINIMO -> SemanticWarningContainerLight
                        else -> SemanticSuccessContainerLight
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        !producto.activo -> MaterialTheme.colorScheme.outline
                                        producto.stock == 0 -> SemanticDanger
                                        producto.stock <= Producto.STOCK_MINIMO -> SemanticWarning
                                        else -> SemanticSuccess
                                    }
                                )
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = when {
                                !producto.activo -> "Inactivo"
                                producto.stock == 0 -> "Agotado (0 u.)"
                                producto.stock <= Producto.STOCK_MINIMO -> "Bajo: ${producto.stock} u."
                                else -> "Stock: ${producto.stock} u."
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            maxLines = 1,
                            color = when {
                                !producto.activo -> MaterialTheme.colorScheme.onSurfaceVariant
                                producto.stock == 0 -> OnSemanticDangerLight
                                producto.stock <= Producto.STOCK_MINIMO -> OnSemanticWarningLight
                                else -> OnSemanticSuccessLight
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Fila de acciones CRUD: Editar y Eliminar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDetalleClick,
                        enabled = habilitado,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = AppIcons.Info,
                            contentDescription = "Ver detalle del producto",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (puedeEscribir) {
                        IconButton(
                            onClick = onEditarClick,
                            enabled = habilitado && producto.id > 0L,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = AppIcons.Edit,
                                contentDescription = "Editar producto",
                                tint = if (producto.id > 0L) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        IconButton(
                            onClick = onEliminarClick,
                            enabled = habilitado && producto.id > 0L,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = AppIcons.Delete,
                                contentDescription = "Eliminar producto",
                                tint = if (producto.id > 0L) SemanticDanger else MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

            }
        }
    }
}
