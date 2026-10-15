package pe.edu.upeu.pharmamobil.presentation.pedido

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.edu.upeu.pharmamobil.domain.model.*
import pe.edu.upeu.pharmamobil.presentation.theme.*

@Composable
fun PedidoScreen(
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var filtroSeleccionado by remember { mutableStateOf(0) } // 0: Todos, 1: Pendiente, 2: Procesando, 3: Entregado, 4: Rechazado

    val listaPedidos = remember {
        val prod1 = Producto(1L, "Paracetamol 500 mg", 8.50, 100)
        val prod2 = Producto(2L, "Amoxicilina 500 mg", 18.50, 45)
        val prod3 = Producto(3L, "Loratadina 10 mg", 10.00, 30)

        val cliente1 = Cliente(1L, "Botica San Jerónimo", "contacto@sanjeronimo.pe", "987654321")
        val cliente2 = Cliente(2L, "Farmacia Central UPeU", "farmacia@upeu.edu.pe", "951234567")
        val cliente3 = Cliente(3L, "Policlínico Los Ángeles", "compras@losangeles.com", "912345678")

        listOf(
            Pedido(
                id = 101L,
                cliente = cliente1,
                detalles = listOf(DetallePedido(prod1, 10), DetallePedido(prod2, 5)),
                estado = EstadoPedido.Entregado
            ),
            Pedido(
                id = 102L,
                cliente = cliente2,
                detalles = listOf(DetallePedido(prod2, 2), DetallePedido(prod3, 4)),
                estado = EstadoPedido.Procesando
            ),
            Pedido(
                id = 103L,
                cliente = cliente3,
                detalles = listOf(DetallePedido(prod1, 20)),
                estado = EstadoPedido.Pendiente
            ),
            Pedido(
                id = 104L,
                cliente = cliente1,
                detalles = listOf(DetallePedido(prod3, 15)),
                estado = EstadoPedido.Rechazado("Falta de comprobante de pago bancario")
            )
        )
    }

    val pedidosFiltrados = remember(filtroSeleccionado) {
        when (filtroSeleccionado) {
            1 -> listaPedidos.filter { it.estado is EstadoPedido.Pendiente }
            2 -> listaPedidos.filter { it.estado is EstadoPedido.Procesando }
            3 -> listaPedidos.filter { it.estado is EstadoPedido.Entregado }
            4 -> listaPedidos.filter { it.estado is EstadoPedido.Rechazado }
            else -> listaPedidos
        }
    }

    val filtros = listOf("Todos", "Pendientes", "Procesando", "Entregados", "Rechazados")

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // ==========================================
        // HEADER DEL CONTROL DE PEDIDOS
        // ==========================================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Control de Pedidos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Órdenes de despacho y facturación",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer
            ) {
                Text(
                    text = "4 Órdenes",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ==========================================
        // FILTRO DE ESTADOS (CHIPS HORIZONTALES)
        // ==========================================
        val chipScrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(chipScrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filtros.forEachIndexed { index, nombre ->
                val isSelected = filtroSeleccionado == index
                FilterChip(
                    selected = isSelected,
                    onClick = { filtroSeleccionado = index },
                    label = {
                        Text(
                            text = nombre,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ==========================================
        // LISTADO DE PEDIDOS
        // ==========================================
        if (pedidosFiltrados.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = AppIcons.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "No hay órdenes con este estado",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        } else {
            pedidosFiltrados.forEach { pedido ->
                PedidoItemCard(pedido = pedido)
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
fun PedidoItemCard(
    pedido: Pedido,
    modifier: Modifier = Modifier
) {
    val total = remember(pedido) {
        pedido.detalles.sumOf { it.producto.precio * it.cantidad }
    }
    val totalFormateado = remember(total) {
        val centavos = ((total * 100).toLong() % 100).let { if (it < 10) "0$it" else "$it" }
        "${total.toLong()}.$centavos"
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(18.dp)
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header de la orden: ID y Chip de Estado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "ORDEN #${pedido.id}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                EstadoChip(estado = pedido.estado)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Cliente
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = AppIcons.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = pedido.cliente.nombre,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Desglose de ítems
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    pedido.detalles.forEach { detalle ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "• ${detalle.producto.nombre} (x${detalle.cantidad})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "S/. ${((detalle.producto.precio * detalle.cantidad * 100).toLong() / 100.0)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Totalizador y Estado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${pedido.detalles.size} producto(s) incluidos",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "Total: S/. $totalFormateado",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            if (pedido.estado is EstadoPedido.Rechazado) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SemanticDangerContainerLight,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Icon(
                            imageVector = AppIcons.Warning,
                            contentDescription = null,
                            tint = SemanticDanger,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Motivo: ${(pedido.estado as EstadoPedido.Rechazado).motivo}",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSemanticDangerLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EstadoChip(
    estado: EstadoPedido
) {
    val (texto, colorFondo, colorTexto, puntoColor) = when (estado) {
        is EstadoPedido.Pendiente -> Quadruple(
            "Pendiente",
            SemanticInfoContainerLight,
            OnSemanticInfoLight,
            SemanticInfo
        )
        is EstadoPedido.Procesando -> Quadruple(
            "Procesando",
            SemanticWarningContainerLight,
            OnSemanticWarningLight,
            SemanticWarning
        )
        is EstadoPedido.Entregado -> Quadruple(
            "Entregado",
            SemanticSuccessContainerLight,
            OnSemanticSuccessLight,
            SemanticSuccess
        )
        is EstadoPedido.Rechazado -> Quadruple(
            "Rechazado",
            SemanticDangerContainerLight,
            OnSemanticDangerLight,
            SemanticDanger
        )
    }

    Surface(
        color = colorFondo,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(puntoColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = texto,
                color = colorTexto,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
