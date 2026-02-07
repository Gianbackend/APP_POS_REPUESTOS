package com.example.posapp.presentation.home

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
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

    Log.d(TAG, "🟦 HomeScreen COMPOSABLE ejecutado")

    val state by viewModel.state.collectAsState()

    Log.d(TAG, "🟦 Estado actual: isSyncing=${state.isSyncing}, error=${state.syncError}, completed=${state.syncCompleted}")

    // 🔥 Sincronizar al entrar
    LaunchedEffect(Unit) {
        Log.d(TAG, "🚀 LaunchedEffect INICIADO")
        try {
            Log.d(TAG, "📞 Llamando a viewModel.syncProductos()...")
            viewModel.syncProductos()
            Log.d(TAG, "✅ viewModel.syncProductos() ejecutado")
        } catch (e: Exception) {
            Log.e(TAG, "❌ EXCEPCIÓN al llamar syncProductos: ${e.message}", e)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bienvenido") }
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
                    Log.d(TAG, "🔄 Mostrando indicador de carga")
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
                    Log.e(TAG, "❌ Mostrando error: ${state.syncError}")
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

                // ✅ Sincronización exitosa
                state.syncCompleted -> {
                    Log.d(TAG, "✅ Sincronización completada: ${state.productosCount} productos")
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text(
                            text = "✅ Sincronización completada",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${state.productosCount} productos sincronizados",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Button(
                            onClick = onNavigateToCatalogo,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Ir al Catálogo")
                        }
                    }
                }

                // 🟡 Estado inicial
                else -> {
                    Log.d(TAG, "🟡 Estado inicial - esperando...")
                    Text("Iniciando...")
                }
            }
        }
    }
}
