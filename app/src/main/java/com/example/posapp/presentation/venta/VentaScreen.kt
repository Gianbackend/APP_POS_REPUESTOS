package com.example.posapp.presentation.venta

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.posapp.domain.model.ItemCarrito

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VentaScreen(
    onNavigateBack: () -> Unit,
    onVentaCompletada: (Long) -> Unit,
    viewModel: VentaViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.ventaCompletada) {
        if (state.ventaCompletada && state.ventaId != null) {
            onVentaCompletada(state.ventaId!!)
            viewModel.onResetVentaCompletada()
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

        if (state.items.isEmpty() && !state.ventaCompletada && !state.isProcessing) {
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
                // Lista de items
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

                // Resumen de totales
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Subtotal (sin IVA):", fontSize = 16.sp)
                            Text(
                                text = "$${String.format("%.2f", state.calcularSubtotalSinIVA())}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("IVA (${state.impuesto.toInt()}%):", fontSize = 16.sp)
                            Text(
                                text = "$${String.format("%.2f", state.calcularMontoIVA())}",
                                fontSize = 16.sp
                            )
                        }

                        Divider(modifier = Modifier.padding(vertical = 12.dp))

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

                        Button(
                            onClick = viewModel::onMostrarFormCliente,
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
                                Text("Continuar con la venta", fontSize = 18.sp)
                            }
                        }
                    }
                }
            }
        }

        // Diálogo de datos del cliente (AGREGAR ESTO)
        if (state.mostrarFormCliente) {
            ClienteFormDialog(
                clienteForm = state.clienteForm,
                isProcessing = state.isProcessing,
                error = state.error,
                onDismiss = viewModel::onOcultarFormCliente,
                onNombreChange = viewModel::onNombreClienteChange,
                onDocumentoChange = viewModel::onDocumentoClienteChange,
                onTelefonoChange = viewModel::onTelefonoClienteChange,
                onEmailChange = viewModel::onEmailClienteChange,
                onConfirmar = viewModel::onProcesarVenta
            )
        }
    }
}

// Card individual de item
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

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "$${String.format("%.2f", item.subtotal)}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
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

                    IconButton(
                        onClick = onDecrementar,
                        modifier = Modifier.size(32.dp),
                        enabled = item.cantidad > 1
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Quitar")
                    }

                    Text(
                        text = "${item.cantidad}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

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

// Diálogo del formulario de cliente
@Composable
private fun ClienteFormDialog(
    clienteForm: ClienteFormState,
    isProcessing: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onNombreChange: (String) -> Unit,
    onDocumentoChange: (String) -> Unit,
    onTelefonoChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onConfirmar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        title = {
            Text(
                text = "Datos del Cliente",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = clienteForm.nombre,
                    onValueChange = onNombreChange,
                    label = { Text("Nombre *") },
                    placeholder = { Text("Juan Pérez") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isProcessing
                )

                OutlinedTextField(
                    value = clienteForm.documento,
                    onValueChange = onDocumentoChange,
                    label = { Text("DNI / RUC *") },
                    placeholder = { Text("12345678") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isProcessing,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    )
                )

                OutlinedTextField(
                    value = clienteForm.telefono,
                    onValueChange = onTelefonoChange,
                    label = { Text("Teléfono") },
                    placeholder = { Text("987654321") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isProcessing,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone
                    )
                )

                OutlinedTextField(
                    value = clienteForm.email,
                    onValueChange = onEmailChange,
                    label = { Text("Email") },
                    placeholder = { Text("cliente@mail.com") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isProcessing,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email
                    )
                )

                if (error != null) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp
                    )
                }

                Text(
                    text = "* Campos obligatorios",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmar,
                enabled = !isProcessing &&
                        clienteForm.nombre.isNotBlank() &&
                        clienteForm.documento.isNotBlank()
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Finalizar Venta")
                }
            }
        },
        dismissButton = {
            if (!isProcessing) {
                TextButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
            }
        }
    )
}