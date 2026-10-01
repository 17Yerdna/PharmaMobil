package pe.edu.upeu.pharmamobil.presentation.producto

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import pe.edu.upeu.pharmamobil.presentation.components.EstadoVacio
import pe.edu.upeu.pharmamobil.presentation.components.MensajeExito
import pe.edu.upeu.pharmamobil.presentation.components.SkeletonGrid
import pe.edu.upeu.pharmamobil.presentation.components.ValidatedTextField
import pe.edu.upeu.pharmamobil.presentation.components.pressFeedback

@Composable
fun ProductoScreen(
    viewModel: ProductoViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Cabecera superior con botón de acción
        CabeceraCatalogo(
            formularioVisible = uiState.formularioVisible,
            onToggleFormulario = viewModel::toggleFormulario
        )

        // Formulario expandible con animación fluida
        AnimatedVisibility(
            visible = uiState.formularioVisible,
            enter = fadeIn(tween(250)) + expandVertically(tween(300)),
            exit = fadeOut(tween(200)) + shrinkVertically(tween(250))
        ) {
            FormularioProductoCard(
                formulario = uiState.formulario,
                registrando = uiState.registrando,
                onNombreChange = viewModel::onNombreChange,
                onPrecioChange = viewModel::onPrecioChange,
                onStockChange = viewModel::onStockChange,
                onRegistrar = viewModel::registrar,
                onCancelar = viewModel::toggleFormulario
            )
        }

        uiState.mensajeExito?.let {
            MensajeExito(it)
        }

        // Pestañas de filtrado de inventario
        PestañasFiltro(
            filtroActual = uiState.filtroSeleccionado,
            onSeleccionarFiltro = viewModel::setFiltro
        )

        // Área central con estados de carga, error y lista en cuadrícula
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (val fase = uiState.fase) {
                ProductoUiState.Fase.Cargando -> {
                    SkeletonGrid(
                        modifier = Modifier.fillMaxSize(),
                        itemsCount = 6
                    )
                }

                ProductoUiState.Fase.SinProductos -> {
                    EstadoVacio(
                        icono = Icons.Default.Inventory2,
                        titulo = "Sin medicamentos registrados",
                        descripcion = "Utiliza el botón superior para dar de alta el primer medicamento en el catálogo.",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is ProductoUiState.Fase.ConProductos -> {
                    val productosFiltrados = when (uiState.filtroSeleccionado) {
                        FiltroInventario.TODOS -> fase.productos
                        FiltroInventario.DISPONIBLES -> fase.productos.filter { !it.requiereReposicion && !it.estaAgotado }
                        FiltroInventario.BAJO_STOCK -> fase.productos.filter { it.requiereReposicion }
                        FiltroInventario.AGOTADOS -> fase.productos.filter { it.estaAgotado }
                    }

                    if (productosFiltrados.isEmpty()) {
                        EstadoVacio(
                            icono = Icons.Default.WarningAmber,
                            titulo = "No hay productos en esta categoría",
                            descripcion = "No se encontraron ítems para el filtro \"${uiState.filtroSeleccionado.titulo}\".",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        AnimatedContent(
                            targetState = productosFiltrados,
                            transitionSpec = {
                                fadeIn(tween(200)) togetherWith fadeOut(tween(150))
                            },
                            label = "GridProductosTransition"
                        ) { lista ->
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(
                                    items = lista,
                                    key = { it.id }
                                ) { producto ->
                                    ProductoCard(producto)
                                }
                            }
                        }
                    }
                }

                is ProductoUiState.Fase.Error -> {
                    EstadoVacio(
                        icono = Icons.Default.CloudOff,
                        titulo = "Conexión interrumpida",
                        descripcion = fase.mensaje,
                        colorIcono = MaterialTheme.colorScheme.error,
                        colorFondoIcono = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        modifier = Modifier.align(Alignment.Center),
                        accion = {
                            FilledTonalButton(
                                onClick = viewModel::cargarProductos,
                                modifier = Modifier.pressFeedback()
                            ) {
                                Text("Reintentar conexión")
                            }
                        }
                    )
                }
            }
        }

        // Barra inferior de paginación flotante
        if (uiState.fase is ProductoUiState.Fase.ConProductos || uiState.paginaActual > 1) {
            BarraPaginacion(
                paginaActual = uiState.paginaActual,
                hayMas = uiState.hayMasProductos,
                cargando = uiState.fase is ProductoUiState.Fase.Cargando,
                onAnterior = viewModel::paginaAnterior,
                onSiguiente = viewModel::paginaSiguiente
            )
        }
    }
}

@Composable
private fun CabeceraCatalogo(
    formularioVisible: Boolean,
    onToggleFormulario: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Catálogo Farmacéutico",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Inventario sincronizado con API REST",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Button(
            onClick = onToggleFormulario,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (formularioVisible) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primary,
                contentColor = if (formularioVisible) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier.pressFeedback()
        ) {
            Icon(
                imageVector = if (formularioVisible) Icons.Default.Close else Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (formularioVisible) "Cerrar" else "Nuevo",
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun PestañasFiltro(
    filtroActual: FiltroInventario,
    onSeleccionarFiltro: (FiltroInventario) -> Unit
) {
    ScrollableTabRow(
        selectedTabIndex = filtroActual.ordinal,
        edgePadding = 0.dp,
        containerColor = Color.Transparent,
        divider = {},
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 2.dp)
    ) {
        FiltroInventario.entries.forEach { filtro ->
            val seleccionado = filtro == filtroActual
            Tab(
                selected = seleccionado,
                onClick = { onSeleccionarFiltro(filtro) },
                modifier = Modifier
                    .padding(end = 6.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (seleccionado) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerHigh
                    )
            ) {
                Text(
                    text = filtro.titulo,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Medium,
                    color = if (seleccionado) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun ProductoCard(
    producto: ProductoUi
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pressFeedback(scaleDown = 0.97f),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Contenedor de Imagen con overlay de categoría
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.1f)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                if (producto.imagenUrl.isNotBlank()) {
                    AsyncImage(
                        model = producto.imagenUrl,
                        contentDescription = producto.nombre,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Medication,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }

                // Tag de categoría sutil sobre la esquina superior izquierda
                if (producto.categoria.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = producto.categoria,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Detalles del producto
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = producto.nombre,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = producto.precio,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Badge de estado de stock semántico
                BadgeStock(
                    stockTexto = producto.stock,
                    requiereReposicion = producto.requiereReposicion,
                    estaAgotado = producto.estaAgotado
                )
            }
        }
    }
}

@Composable
private fun BadgeStock(
    stockTexto: String,
    requiereReposicion: Boolean,
    estaAgotado: Boolean
) {
    val (fondo, textoColor, label) = when {
        estaAgotado -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            "Agotado"
        )
        requiereReposicion -> Triple(
            Color(0xFFFFE082),
            Color(0xFF5D4037),
            "Bajo Stock ($stockTexto)"
        )
        else -> Triple(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            MaterialTheme.colorScheme.onPrimaryContainer,
            "Stock: $stockTexto"
        )
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = fondo,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = textoColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun BarraPaginacion(
    paginaActual: Int,
    hayMas: Boolean,
    cargando: Boolean,
    onAnterior: () -> Unit,
    onSiguiente: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            FilledTonalButton(
                onClick = onAnterior,
                enabled = paginaActual > 1 && !cargando,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.pressFeedback()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Anterior",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Anterior", fontWeight = FontWeight.SemiBold)
            }

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Text(
                    text = "Pág. $paginaActual",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            FilledTonalButton(
                onClick = onSiguiente,
                enabled = hayMas && !cargando,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.pressFeedback()
            ) {
                Text("Siguiente", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Siguiente",
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun FormularioProductoCard(
    formulario: FormularioProducto,
    registrando: Boolean,
    onNombreChange: (String) -> Unit,
    onPrecioChange: (String) -> Unit,
    onStockChange: (String) -> Unit,
    onRegistrar: () -> Unit,
    onCancelar: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Registrar Nuevo Medicamento",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            ValidatedTextField(
                value = formulario.nombre,
                onValueChange = onNombreChange,
                label = "Nombre del medicamento",
                error = formulario.nombreError,
                leadingIcon = Icons.Default.Medication,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ValidatedTextField(
                    value = formulario.precio,
                    onValueChange = onPrecioChange,
                    label = "Precio (S/)",
                    error = formulario.precioError,
                    ayuda = "Ej. 14.50",
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f)
                )

                ValidatedTextField(
                    value = formulario.stock,
                    onValueChange = onStockChange,
                    label = "Stock inicial",
                    error = formulario.stockError,
                    ayuda = "Unidades",
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilledTonalButton(
                    onClick = onCancelar,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).pressFeedback()
                ) {
                    Text("Cancelar")
                }

                Button(
                    onClick = onRegistrar,
                    enabled = !registrando,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1.5f).pressFeedback()
                ) {
                    Text(
                        text = if (registrando) "Guardando…" else "Guardar Medicamento",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
