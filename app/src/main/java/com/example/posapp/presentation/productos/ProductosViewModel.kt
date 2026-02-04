package com.example.posapp.presentation.productos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.posapp.data.repository.CarritoRepository
import com.example.posapp.data.repository.CategoriaRepository
import com.example.posapp.data.repository.ProductoRepository
import com.example.posapp.data.repository.ProductoRepositoryImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductosViewModel @Inject constructor(
    private val productoRepository: ProductoRepository,
    private val categoriaRepository: CategoriaRepository,
    val carritoRepository: CarritoRepository,
    private val repositoryImpl: ProductoRepositoryImpl
) : ViewModel() {

    private val _state = MutableStateFlow(ProductosState())
    val state = _state.asStateFlow()

    private var loadProductosJob: Job? = null

    init {
        loadCategorias()
        loadProductos()
        sincronizarAlIniciar() // ← 🆕 Sincronización silenciosa al iniciar
    }

    // 🆕 SINCRONIZACIÓN AUTOMÁTICA AL INICIAR
    private fun sincronizarAlIniciar() {
        viewModelScope.launch {
            try {
                // Intenta descargar productos de Firebase
                repositoryImpl.descargarDesdeFirebase()

                // Intenta subir cambios pendientes
                repositoryImpl.sincronizarPendientes()

            } catch (e: Exception) {
                // Si falla, continúa con datos locales (modo offline silencioso)
                // No mostramos error al usuario
            }
        }
    }

    private fun loadCategorias() {
        viewModelScope.launch {
            categoriaRepository.getAllCategorias()
                .catch { e ->
                    _state.update { it.copy(error = e.message) }
                }
                .collect { categorias ->
                    _state.update { it.copy(categorias = categorias) }
                }
        }
    }

    private fun loadProductos() {
        loadProductosJob?.cancel()

        loadProductosJob = viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true) }

                val productosFlow = when {
                    _state.value.searchQuery.isNotEmpty() -> {
                        productoRepository.buscarProductos(_state.value.searchQuery)
                    }
                    _state.value.showStockBajo && _state.value.selectedCategoriaId != null -> {
                        productoRepository.getProductosStockBajoPorCategoria(_state.value.selectedCategoriaId!!)
                    }
                    _state.value.showStockBajo -> {
                        productoRepository.getProductosStockBajo()
                    }
                    _state.value.selectedCategoriaId != null -> {
                        productoRepository.getProductosByCategoria(_state.value.selectedCategoriaId!!)
                    }
                    else -> {
                        productoRepository.getAllProductos()
                    }
                }

                productosFlow
                    .catch { e ->
                        _state.update { it.copy(error = e.message, isLoading = false) }
                    }
                    .collect { productos ->
                        _state.update {
                            it.copy(
                                productos = productos,
                                isLoading = false,
                                error = null
                            )
                        }
                    }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        error = e.message,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _state.update { it.copy(searchQuery = query, isLoading = true) }
        loadProductos()
    }

    fun onCategoriaSelected(categoriaId: Long?) {
        _state.update {
            it.copy(
                selectedCategoriaId = categoriaId,
                searchQuery = "",
                isLoading = true
            )
        }
        loadProductos()
    }

    fun onToggleStockBajo() {
        _state.update {
            it.copy(
                showStockBajo = !it.showStockBajo,
                searchQuery = "",
                isLoading = true
            )
        }
        loadProductos()
    }

    fun onClearFilters() {
        _state.update {
            it.copy(
                searchQuery = "",
                selectedCategoriaId = null,
                showStockBajo = false,
                isLoading = true
            )
        }
        loadProductos()
    }

    fun getCantidadCarrito(): Flow<Int> {
        return carritoRepository.items.map { items ->
            items.sumOf { it.cantidad }
        }
    }

    override fun onCleared() {
        super.onCleared()
        loadProductosJob?.cancel()
    }
}
