package com.example.posapp.presentation.sync

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.posapp.data.repository.ProductoSyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SyncViewModel @Inject constructor(
    private val productoSyncRepository: ProductoSyncRepository
) : ViewModel() {

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private var syncJob: Job? = null
    private val TAG = "SyncViewModel"

    init {
        Log.d(TAG, "✅ SyncViewModel inicializado correctamente")
        // ❌ COMENTADO: No iniciar listener automáticamente
        // startRealtimeSync()
    }

    /**
     * Sincronización inicial manual
     */
    fun syncProductos() {
        syncJob?.cancel()

        syncJob = viewModelScope.launch {
            _syncState.value = SyncState.Loading
            Log.d(TAG, "🔄 Iniciando sincronización...")

            try {
                val result = productoSyncRepository.syncInitial()

                result.fold(
                    onSuccess = { count ->
                        _syncState.value = SyncState.Success("✅ $count productos sincronizados")
                        Log.d(TAG, "✅ Sincronización exitosa: $count productos")

                        // Ahora SÍ iniciar el listener en tiempo real
                        startRealtimeSync()
                    },
                    onFailure = { error ->
                        _syncState.value = SyncState.Error(error.message ?: "Error desconocido")
                        Log.e(TAG, "❌ Error en sincronización: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                _syncState.value = SyncState.Error(e.message ?: "Error inesperado")
                Log.e(TAG, "❌ Excepción en syncProductos: ${e.message}", e)
            }
        }
    }

    /**
     * Listener en tiempo real (solo después de sincronización exitosa)
     */
    private fun startRealtimeSync() {
        try {
            productoSyncRepository.startRealtimeSync { error ->
                _syncState.value = SyncState.Error(error.message ?: "Error en tiempo real")
                Log.e(TAG, "❌ Error en listener: ${error.message}")
            }
            Log.d(TAG, "🔄 Listener en tiempo real iniciado")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al iniciar listener: ${e.message}", e)
        }
    }

    override fun onCleared() {
        super.onCleared()
        productoSyncRepository.stopRealtimeSync()
        syncJob?.cancel()
        Log.d(TAG, "🛑 ViewModel limpiado")
    }
}

sealed class SyncState {
    object Idle : SyncState()
    object Loading : SyncState()
    data class Success(val message: String) : SyncState()
    data class Error(val message: String) : SyncState()
}
