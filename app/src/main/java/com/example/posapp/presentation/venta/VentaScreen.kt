package com.example.posapp.presentation.venta

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.posapp.domain.model.ItemCarrito

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VentaScreen(
    onNavigateBack: () -> Unit,        // Volver al catálogo
    onVentaCompletada: () -> Unit,     // Cuando termina la venta
    viewModel: VentaViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    // Navegar cuando la venta se completa
    LaunchedEffect(state.ventaCompletada) {
        if (state.ventaCompletada) {
            kotlinx.coroutines.delay(1500)
            viewModel.onResetVentaCompletada()
            onVentaCompletada()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Carrito de Compras") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->

        if (state.items.isEmpty()) {
            // Carrito vacío
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🛒",
                        fontSize = 64.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "El carrito está vacío",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = onNavigateBack) {
                        Text("Ir al catálogo")
                    }
                }
            }
        } else {
            // Carrito con items
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Lista de items (ocupa el espacio disponible)
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = state.items,
                        key = { it.producto.id }
                    ) { item ->
                        ItemCarritoCard(
                            item = item,
                            onIncrementar = {
                                viewModel.onActualizarCantidad(
                                    item.producto.id,
                                    item.cantidad + 1
                                )
                            },
                            onDecrementar = {
                                viewModel.onActualizarCantidad(
                                    item.producto.id,
                                    item.cantidad - 1
                                )
                            },
                            onEliminar = {
                                viewModel.onEliminarItem(item.producto.id)
                            }
                        )
                    }
                }

                // Resumen de totales (fijo abajo)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        // Subtotal
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Subtotal:", fontSize = 16.sp)
                            Text(
                                text = "$${String.format("%.2f", state.subtotal)}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Impuesto
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("IVA (${state.impuesto.toInt()}%):", fontSize = 16.sp)
                            Text(
                                text = "$${String.format("%.2f", state.subtotal * (state.impuesto / 100))}",
                                fontSize = 16.sp
                            )
                        }

                        Divider(modifier = Modifier.padding(vertical = 12.dp))

                        // Total
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TOTAL:",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$${String.format("%.2f", state.calcularTotal())}",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Botón Finalizar Venta
                        Button(
                            onClick = viewModel::onProcesarVenta,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            enabled = !state.isProcessing
                        ) {
                            if (state.isProcessing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text("Finalizar Venta", fontSize = 18.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Card individual de cada item del carrito
@Composable
private fun ItemCarritoCard(
    item: ItemCarrito,
    onIncrementar: () -> Unit,
    onDecrementar: () -> Unit,
    onEliminar: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Info del producto
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.producto.nombre,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$${String.format("%.2f", item.producto.precio)} c/u",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Controles de cantidad
            Column(
                horizontalAlignment = Alignment.End
            ) {
                // Subtotal
                Text(
                    text = "$${String.format("%.2f", item.subtotal)}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Cantidad con botones
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Botón eliminar
                    IconButton(
                        onClick = onEliminar,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Botón -
                    IconButton(
                        onClick = onDecrementar,
                        modifier = Modifier.size(32.dp),
                        enabled = item.cantidad > 1
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Quitar")
                    }

                    // Cantidad
                    Text(
                        text = "${item.cantidad}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    // Botón +
                    IconButton(
                        onClick = onIncrementar,
                        modifier = Modifier.size(32.dp),
                        enabled = item.cantidad < item.producto.stock
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Agregar")
                    }
                }
            }
        }
    }
}