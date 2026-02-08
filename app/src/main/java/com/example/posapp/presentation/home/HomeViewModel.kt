package com.example.posapp.presentation.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.posapp.data.local.dao.CategoriaDao
import com.example.posapp.data.local.dao.DetalleVentaDao
import com.example.posapp.data.local.dao.ProductoDao
import com.example.posapp.data.local.dao.VentaDao
import com.example.posapp.data.repository.ProductoSyncRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val productoSyncRepository: ProductoSyncRepository,
    private val productoDao: ProductoDao,
    private val categoriaDao: CategoriaDao,
    private val ventaDao: VentaDao,
    private val detalleVentaDao: DetalleVentaDao
) : ViewModel() {

    private val TAG = "HomeViewModel"
    private val _state = MutableStateFlow(HomeState())
    val state = _state.asStateFlow()

    private var isCleaningDatabase = false // ✅ Flag para evitar bucle

    init {
        Log.d(TAG, "🟢 ViewModel inicializado")

        viewModelScope.launch {
            FirebaseAuth.getInstance().currentUser?.let {
                Log.d(TAG, "✅ Usuario autenticado: ${it.email}")
                syncProductos()
            } ?: run {
                Log.e(TAG, "❌ Usuario NO autenticado")
                // ✅ NO hacer nada, el HomeScreen mostrará el botón de sync
            }
        }
    }

    fun syncProductos() {
        if (isCleaningDatabase) {
            Log.w(TAG, "⚠️ Ya se está limpiando la base de datos, ignorando...")
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSyncing = true, syncError = null) }

            try {
                productoSyncRepository.syncProductos()
                    .onSuccess {
                        // Obtener el conteo real de productos activos
                        val count = productoDao.getAllActive().first().size
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
                        Log.e(TAG, "❌ Error: ${error.message}", error)

                        if (error.message?.contains("FOREIGN KEY") == true && !isCleaningDatabase) {
                            Log.w(TAG, "⚠️ Limpiando DB corrupta...")
                            limpiarBaseDatos()
                        }

                        _state.update {
                            it.copy(
                                isSyncing = false,
                                syncError = error.message
                            )
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error inesperado: ${e.message}", e)

                if (e.message?.contains("FOREIGN KEY") == true && !isCleaningDatabase) {
                    limpiarBaseDatos()
                } else {
                    _state.update { it.copy(isSyncing = false, syncError = e.message) }
                }
            }
        }
    }

    private fun limpiarBaseDatos() {
        if (isCleaningDatabase) {
            Log.w(TAG, "⚠️ Ya se está limpiando la base de datos")
            return
        }

        isCleaningDatabase = true

        viewModelScope.launch {
            try {
                Log.d(TAG, "🗑️ Paso 1: Eliminando detalles de venta...")
                detalleVentaDao.deleteAll()

                Log.d(TAG, "🗑️ Paso 2: Eliminando ventas...")
                ventaDao.deleteAll()

                Log.d(TAG, "🗑️ Paso 3: Eliminando productos...")
                productoDao.deleteAll()

                Log.d(TAG, "🗑️ Paso 4: Eliminando categorías...")
                categoriaDao.deleteAll()

                delay(1000)

                Log.d(TAG, "✅ Base de datos limpiada")
                isCleaningDatabase = false

                Log.d(TAG, "🔄 Reintentando sincronización...")
                syncProductos()
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error limpiando DB: ${e.message}", e)
                isCleaningDatabase = false
                _state.update {
                    it.copy(
                        isSyncing = false,
                        syncError = "Error al limpiar base de datos"
                    )
                }
            }
        }
    }

    fun retrySyncProductos() {
        isCleaningDatabase = false
        _state.update { it.copy(syncError = null) }
        syncProductos()
    }
}
