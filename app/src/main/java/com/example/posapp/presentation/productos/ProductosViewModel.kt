package com.example.posapp.presentation.productos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.posapp.data.repository.CategoriaRepository
import com.example.posapp.data.repository.ProductoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductosViewModel @Inject constructor(
    private val productoRepository: ProductoRepository,
    private val categoriaRepository: CategoriaRepository
) : ViewModel() {

    // Estado privado
    private val _state = MutableStateFlow(ProductosState())
    // Estado público (solo lectura)
    val state = _state.asStateFlow()

    init {
        // Al crear el ViewModel, cargar datos automáticamente
        loadCategorias()
        loadProductos()
    }

    // Cargar categorías para el filtro
    private fun loadCategorias() {
        viewModelScope.launch {
            categoriaRepository.getAllCategorias()
                .catch { e ->
                    // Si hay error, actualizar estado con error
                    _state.update { it.copy(error = e.message) }
                }
                .collect { categorias ->
                    // Actualizar estado con las categorías
                    _state.update { it.copy(categorias = categorias) }
                }
        }
    }

    // Cargar productos según filtros activos
    private fun loadProductos() {
        viewModelScope.launch {
            // Determinar qué Flow usar según los filtros
            val productosFlow = when {
                // Si está activado "mostrar stock bajo"
                _state.value.showStockBajo -> {
                    productoRepository.getProductosStockBajo()
                }
                // Si hay una categoría seleccionada
                _state.value.selectedCategoriaId != null -> {
                    productoRepository.getProductosByCategoria(_state.value.selectedCategoriaId!!)
                }
                // Si hay texto de búsqueda
                _state.value.searchQuery.isNotEmpty() -> {
                    productoRepository.buscarProductos(_state.value.searchQuery)
                }
                // Por defecto, mostrar todos
                else -> {
                    productoRepository.getAllProductos()
                }
            }

            // Observar el Flow y actualizar estado
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
        }
    }

    // Cuando el usuario escribe en la búsqueda
    fun onSearchQueryChange(query: String) {
        _state.update { it.copy(searchQuery = query, isLoading = true) }
        loadProductos() // Recargar con el nuevo filtro
    }

    // Cuando el usuario selecciona una categoría
    fun onCategoriaSelected(categoriaId: Long?) {
        _state.update {
            it.copy(
                selectedCategoriaId = categoriaId,
                searchQuery = "", // Limpiar búsqueda
                showStockBajo = false, // Desactivar filtro de stock bajo
                isLoading = true
            )
        }
        loadProductos()
    }

    // Cuando el usuario activa/desactiva "mostrar solo stock bajo"
    fun onToggleStockBajo() {
        _state.update {
            it.copy(
                showStockBajo = !it.showStockBajo,
                selectedCategoriaId = null, // Limpiar filtro de categoría
                searchQuery = "", // Limpiar búsqueda
                isLoading = true
            )
        }
        loadProductos()
    }

    // Limpiar todos los filtros
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
}