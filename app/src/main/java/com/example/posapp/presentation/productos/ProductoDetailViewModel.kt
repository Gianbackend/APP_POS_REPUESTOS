package com.example.posapp.presentation.productos

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.posapp.data.repository.ProductoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductoDetailViewModel @Inject constructor(
    private val productoRepository: ProductoRepository,
    savedStateHandle: SavedStateHandle  // Para recibir el ID desde navegación
) : ViewModel() {

    private val _state = MutableStateFlow(ProductoDetailState())
    val state = _state.asStateFlow()

    // Obtener el ID del producto desde los argumentos de navegación
    private val productoId: Long = savedStateHandle.get<String>("productoId")?.toLongOrNull() ?: 0L

    init {
        // Cargar producto automáticamente al crear el ViewModel
        loadProducto()
    }

    // Cargar producto desde el repository
    private fun loadProducto() {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true) }

                // Buscar producto por ID
                val producto = productoRepository.getProductoById(productoId)

                if (producto != null) {
                    _state.update {
                        it.copy(
                            producto = producto,
                            isLoading = false,
                            error = null
                        )
                    }
                } else {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = "Producto no encontrado"
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Error desconocido"
                    )
                }
            }
        }
    }

    // Aumentar cantidad
    fun onIncrementCantidad() {
        val producto = _state.value.producto ?: return
        val nuevaCantidad = _state.value.cantidad + 1

        // No permitir más que el stock disponible
        if (nuevaCantidad <= producto.stock) {
            _state.update { it.copy(cantidad = nuevaCantidad) }
        }
    }

    // Disminuir cantidad
    fun onDecrementCantidad() {
        val nuevaCantidad = _state.value.cantidad - 1

        // Mínimo 1
        if (nuevaCantidad >= 1) {
            _state.update { it.copy(cantidad = nuevaCantidad) }
        }
    }

    // Agregar al carrito (por ahora solo marca como agregado, luego lo conectaremos con el carrito real)
    fun onAgregarAlCarrito() {
        _state.update { it.copy(agregadoAlCarrito = true) }
    }

    // Resetear estado de "agregado" (para poder agregar más veces)
    fun onResetAgregado() {
        _state.update { it.copy(agregadoAlCarrito = false) }
    }
}