package com.example.posapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.example.posapp.presentation.navigation.NavGraph
import com.example.posapp.ui.theme.POSAppTheme
import com.example.posapp.ui.viewmodel.SyncViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // ViewModel para sincronización
    private val syncViewModel: SyncViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ UI carga INSTANTÁNEAMENTE
        setContent {
            POSAppTheme(dynamicColor = false) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavGraph(navController = navController)
                }
            }
        }

        // ✅ Sincronización en segundo plano (NO bloquea UI)
        observeSyncState()
    }

    private fun observeSyncState() {
        lifecycleScope.launch {
            syncViewModel.syncState.collect { state ->
                when (state) {
                    is SyncViewModel.SyncState.Loading -> {
                        // Opcional: Log para debug
                        android.util.Log.d("MainActivity", "Sincronizando productos...")
                    }
                    is SyncViewModel.SyncState.Success -> {
                        android.util.Log.d("MainActivity", "✅ ${state.count} productos sincronizados")
                    }
                    is SyncViewModel.SyncState.Error -> {
                        android.util.Log.e("MainActivity", "❌ Error sync: ${state.message}")
                    }
                    else -> {}
                }
            }
        }
    }
}