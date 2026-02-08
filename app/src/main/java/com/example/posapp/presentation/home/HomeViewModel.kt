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

    private var isCleaningDatabase = false
    private var isSyncing = false // 🔥 NUEVO: Evitar sincronizaciones simultáneas

    init {
        Log.d(TAG, "🟢 ViewModel inicializado")

        viewModelScope.launch {
            FirebaseAuth.getInstance().currentUser?.let {
                Log.d(TAG, "✅ Usuario autenticado: ${it.email}")

                // 🔥 VERIFICAR SI YA HAY DATOS ANTES DE SINCRONIZAR
                val productosExistentes = productoDao.getAllActive().first()
                if (productosExistentes.isNotEmpty()) {
                    Log.d(TAG, "📦 Ya hay ${productosExistentes.size} productos en cache")
                    _state.update {
                        it.copy(
                            syncCompleted = true,
                            productosCount = productosExistentes.size
                        )
                    }
                } else {
                    Log.d(TAG, "🔄 No hay productos, iniciando sincronización...")
                    syncProductos()
                }
            } ?: run {
                Log.e(TAG, "❌ Usuario NO autenticado")
            }
        }
    }

    fun syncProductos() {
        if (isCleaningDatabase) {
            Log.w(TAG, "⚠️ Ya se está limpiando la base de datos, ignorando...")
            return
        }

        if (isSyncing) {
            Log.w(TAG, "⚠️ Ya hay una sincronización en curso")
            return
        }

        isSyncing = true

        viewModelScope.launch {
            _state.update { it.copy(isSyncing = true, syncError = null) }

            try {
                // 🔥 VERIFICAR SI HAY CATEGORÍAS PRIMERO
                val categoriasExistentes = categoriaDao.getAll().first()

                if (categoriasExistentes.isEmpty()) {
                    Log.d(TAG, "📂 No hay categorías, sincronizando desde Firebase...")
                }

                productoSyncRepository.syncProductos()
                    .onSuccess {
                        val count = productoDao.getAllActive().first().size
                        Log.d(TAG, "✅ Sincronización exitosa: $count productos")
                        _state.update {
                            it.copy(
                                isSyncing = false,
                                syncCompleted = true,
                                productosCount = count
                            )
                        }
                        isSyncing = false
                    }
                    .onFailure { error ->
                        Log.e(TAG, "❌ Error: ${error.message}", error)

                        if (error.message?.contains("FOREIGN KEY") == true && !isCleaningDatabase) {
                            Log.w(TAG, "⚠️ Error de integridad, limpiando DB...")
                            isSyncing = false
                            limpiarBaseDatos()
                        } else {
                            _state.update {
                                it.copy(
                                    isSyncing = false,
                                    syncError = error.message
                                )
                            }
                            isSyncing = false
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error inesperado: ${e.message}", e)

                if (e.message?.contains("FOREIGN KEY") == true && !isCleaningDatabase) {
                    isSyncing = false
                    limpiarBaseDatos()
                } else {
                    _state.update { it.copy(isSyncing = false, syncError = e.message) }
                    isSyncing = false
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
                Log.d(TAG, "🗑️ Limpiando base de datos en orden correcto...")

                // 🔥 ORDEN CORRECTO: De dependientes a independientes
                Log.d(TAG, "🗑️ Paso 1: Eliminando detalles de venta...")
                detalleVentaDao.deleteAll()

                Log.d(TAG, "🗑️ Paso 2: Eliminando ventas...")
                ventaDao.deleteAll()

                Log.d(TAG, "🗑️ Paso 3: Eliminando productos...")
                productoDao.deleteAll()

                Log.d(TAG, "🗑️ Paso 4: Eliminando categorías...")
                categoriaDao.deleteAll()

                delay(500) // Reducido a 500ms

                Log.d(TAG, "✅ Base de datos limpiada correctamente")
                isCleaningDatabase = false

                // 🔥 ESPERAR UN POCO MÁS ANTES DE REINTENTAR
                delay(1000)

                Log.d(TAG, "🔄 Reintentando sincronización...")
                syncProductos()
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error limpiando DB: ${e.message}", e)
                isCleaningDatabase = false
                isSyncing = false
                _state.update {
                    it.copy(
                        isSyncing = false,
                        syncError = "Error al limpiar base de datos: ${e.message}"
                    )
                }
            }
        }
    }

    fun retrySyncProductos() {
        isCleaningDatabase = false
        isSyncing = false
        _state.update { it.copy(syncError = null) }
        syncProductos()
    }
}
