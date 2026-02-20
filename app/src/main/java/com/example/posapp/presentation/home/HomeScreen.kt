package com.example.posapp.presentation.home

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToCatalogo: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val TAG = "HomeScreen"
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        Log.d(TAG, "🚀 Iniciando sincronización")
        viewModel.syncProductos()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("POS Repuestos") }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            when {
                // 🔄 Sincronizando
                state.isSyncing -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = "Sincronizando productos...",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                // ❌ Error
                state.syncError != null -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text(
                            text = "Error al sincronizar",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = state.syncError ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = { viewModel.retrySyncProductos() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Reintentar")
                        }
                    }
                }

                // ✅ Login exitoso
                state.syncCompleted -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                        modifier = Modifier.padding(horizontal = 32.dp)
                    ) {
                        // Ícono de éxito
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )

                        // Título
                        Text(
                            text = "Login exitoso",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Bienvenida
                        Text(
                            text = "Bienvenido",
                            style = MaterialTheme.typography.titleLarge
                        )

                        // Productos sincronizados
                        Text(
                            text = "${state.productosCount} productos sincronizados",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.height(8.dp))

                        // Botón principal
                        Button(
                            onClick = onNavigateToCatalogo,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Ir al Catálogo",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }

                // 🟡 Estado inicial
                else -> {
                    CircularProgressIndicator()
                }
            }
        }
    }
}
