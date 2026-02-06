package com.example.posapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.posapp.data.repository.ProductoSyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SyncViewModel @Inject constructor(
    private val syncRepository: ProductoSyncRepository
) : ViewModel() {

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState

    sealed class SyncState {
        object Idle : SyncState()
        object Loading : SyncState()
        data class Success(val count: Int) : SyncState()
        data class Error(val message: String) : SyncState()
    }

    init {
        // ✅ Sincronización automática al iniciar
        syncProductos()

        // ✅ Listener en tiempo real
        syncRepository.startRealtimeSync { error ->
            _syncState.value = SyncState.Error(error.message ?: "Error desconocido")
        }
    }

    fun syncProductos() {
        viewModelScope.launch {
            _syncState.value = SyncState.Loading

            val result = syncRepository.syncInitial()

            _syncState.value = if (result.isSuccess) {
                SyncState.Success(result.getOrNull() ?: 0)
            } else {
                SyncState.Error(result.exceptionOrNull()?.message ?: "Error al sincronizar")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        syncRepository.stopRealtimeSync()
    }
}