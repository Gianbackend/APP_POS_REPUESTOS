package com.example.posapp.presentation.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.posapp.data.repository.ProductoSyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val productoSyncRepository: ProductoSyncRepository
) : ViewModel() {

    private val TAG = "HomeViewModel"

    private val _state = MutableStateFlow(HomeState())
    val state = _state.asStateFlow()

    init {
        Log.d(TAG, "🟢 ViewModel inicializado")
        Log.d(TAG, "🟢 Repository: $productoSyncRepository")
    }

    /**
     * Sincroniza productos desde Firestore
     */
    fun syncProductos() {
        Log.d(TAG, "🔵 syncProductos() LLAMADO")

        viewModelScope.launch {
            Log.d(TAG, "🟢 Dentro de viewModelScope.launch")

            _state.update {
                Log.d(TAG, "📝 Actualizando estado: isSyncing=true")
                it.copy(isSyncing = true, syncError = null)
            }

            Log.d(TAG, "📡 Llamando a productoSyncRepository.syncProductos()...")

            productoSyncRepository.syncProductos()
                .onSuccess { count ->
                    Log.d(TAG, "✅ Sincronización exitosa: $count productos")
                    _state.update {
                        it.copy(
                            isSyncing = false,
                            syncCompleted = true,
                            productosCount = count
                        )
                    }
                }
                .onFailure { error ->
                    Log.e(TAG, "❌ Error en sincronización: ${error.message}", error)
                    _state.update {
                        it.copy(
                            isSyncing = false,
                            syncError = error.message ?: "Error desconocido"
                        )
                    }
                }
        }
    }

    /**
     * Reintentar sincronización
     */
    fun retrySyncProductos() {
        Log.d(TAG, "🔄 Reintentando sincronización...")
        _state.update { it.copy(syncError = null) }
        syncProductos()
    }
}
