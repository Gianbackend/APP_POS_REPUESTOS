package com.example.posapp.presentation.productos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.posapp.data.repository.CarritoRepository
import com.example.posapp.data.repository.CategoriaRepository
import com.example.posapp.data.repository.ProductoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductosViewModel @Inject constructor(
    private val productoRepository: ProductoRepository,
    private val categoriaRepository: CategoriaRepository,
    val carritoRepository: CarritoRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ProductosState())
    val state = _state.asStateFlow()

    private var loadProductosJob: Job? = null  // ← NUEVO: Para cancelar el Job anterior

    init {
        loadCategorias()
        loadProductos()
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
        // CANCELAR el Job anterior si existe
        loadProductosJob?.cancel()

        // Crear nuevo Job
        loadProductosJob = viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true) }

                val productosFlow = when {
                    // Caso 1: Solo búsqueda
                    _state.value.searchQuery.isNotEmpty() -> {
                        productoRepository.buscarProductos(_state.value.searchQuery)
                    }
                    // ✅ NUEVO: Stock Bajo + Categoría (AND)
                    _state.value.showStockBajo && _state.value.selectedCategoriaId != null -> {
                        productoRepository.getProductosStockBajoPorCategoria(_state.value.selectedCategoriaId!!)
                    }
                    // Caso 3: Solo Stock Bajo
                    _state.value.showStockBajo -> {
                        productoRepository.getProductosStockBajo()
                    }
                    // Caso 4: Solo Categoría
                    _state.value.selectedCategoriaId != null -> {
                        productoRepository.getProductosByCategoria(_state.value.selectedCategoriaId!!)
                    }
                    // Caso 5: Sin filtros
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
                //showStockBajo = false,
                isLoading = true
            )
        }
        loadProductos()
    }

    fun onToggleStockBajo() {
        _state.update {
            it.copy(
                showStockBajo = !it.showStockBajo,
                //selectedCategoriaId = null,
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

    // Obtener cantidad en el carrito
    fun getCantidadCarrito(): Flow<Int> {
        return carritoRepository.items.map { items ->
            items.sumOf { it.cantidad }
        }
    }

    // Limpiar al destruir el ViewModel
    override fun onCleared() {
        super.onCleared()
        loadProductosJob?.cancel()
    }
}