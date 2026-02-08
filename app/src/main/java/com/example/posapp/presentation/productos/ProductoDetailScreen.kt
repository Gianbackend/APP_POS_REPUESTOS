package com.example.posapp.presentation.productos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductoDetailScreen(
    onNavigateBack: () -> Unit,  // Para volver atrás
    viewModel: ProductoDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

        // Mostrar Snackbar cuando se agrega al carrito
    LaunchedEffect(state.agregadoAlCarrito) {
        if (state.agregadoAlCarrito) {
            // Esperar un momento y resetear
            kotlinx.coroutines.delay(1500)
            viewModel.onResetAgregado()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle del Producto") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        snackbarHost = {
            // Snackbar para mostrar "Agregado al carrito"
            if (state.agregadoAlCarrito) {
                Snackbar(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text("✓ Agregado al carrito")
                }
            }
        }
    ) { paddingValues ->

        when {
            state.isLoading -> {
                // Mostrar loading
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            state.error != null -> {
                // Mostrar error
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
                            text = state.error ?: "Error",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onNavigateBack) {
                            Text("Volver")
                        }
                    }
                }
            }

            state.producto != null -> {
                // Mostrar producto
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    val producto = state.producto!!

                    // Código del producto
                    Text(
                        text = producto.codigo,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Nombre
                    Text(
                        text = producto.nombre,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Card con info básica
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            // Precio
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Precio:", fontSize = 16.sp)
                                Text(
                                    text = "$${String.format("%.2f", producto.precio)}",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Stock
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Stock disponible:", fontSize = 16.sp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (producto.stockBajo) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = "Stock bajo",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = "${producto.stock} unidades",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (producto.stockBajo)
                                            MaterialTheme.colorScheme.error
                                        else
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Detalles
                    Text(
                        text = "Detalles",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    InfoRow("Marca:", producto.marca)
                    InfoRow("Modelo:", producto.modelo)
                    InfoRow("Categoría:", producto.categoriaNombre)
                    if (producto.ubicacion != null) {
                        InfoRow("Ubicación:", "📍 ${producto.ubicacion}")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Descripción
                    Text(
                        text = "Descripción",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = producto.descripcion,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Selector de cantidad
                    Text(
                        text = "Cantidad",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Botón -
                        IconButton(
                            onClick = viewModel::onDecrementCantidad,
                            enabled = state.cantidad > 1
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Disminuir")
                        }

                        // Cantidad actual
                        Text(
                            text = "${state.cantidad}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )

                        // Botón +
                        IconButton(
                            onClick = viewModel::onIncrementCantidad,
                            enabled = state.cantidad < producto.stock
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Aumentar")
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Botón Agregar al carrito
                    Button(
                        onClick = viewModel::onAgregarAlCarrito,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = producto.stock > 0
                    ) {
                        Text(
                            text = if (producto.stock > 0)
                                "Agregar al carrito (${state.cantidad})"
                            else
                                "Sin stock",
                            fontSize = 18.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

// Componente auxiliar para mostrar filas de información
@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}