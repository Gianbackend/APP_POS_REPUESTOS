package com.example.posapp.presentation.productos

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.posapp.data.repository.CarritoRepository  // ← NUEVO
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
    private val carritoRepository: CarritoRepository,  // ← NUEVO
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(ProductoDetailState())
    val state = _state.asStateFlow()

    private val productoId: Long = savedStateHandle.get<String>("productoId")?.toLongOrNull() ?: 0L

    init {
        loadProducto()
    }

    private fun loadProducto() {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true) }

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

    fun onIncrementCantidad() {
        val producto = _state.value.producto ?: return
        val nuevaCantidad = _state.value.cantidad + 1

        if (nuevaCantidad <= producto.stock) {
            _state.update { it.copy(cantidad = nuevaCantidad) }
        }
    }

    fun onDecrementCantidad() {
        val nuevaCantidad = _state.value.cantidad - 1

        if (nuevaCantidad >= 1) {
            _state.update { it.copy(cantidad = nuevaCantidad) }
        }
    }

    // ACTUALIZADO: Ahora realmente agrega al carrito
    fun onAgregarAlCarrito() {
        val producto = _state.value.producto ?: return
        val cantidad = _state.value.cantidad

        // Agregar al carrito (acumula si ya existe)
        carritoRepository.agregarProducto(producto, cantidad)

        // Mostrar mensaje de confirmación
        _state.update { it.copy(agregadoAlCarrito = true) }
    }

    fun onResetAgregado() {
        _state.update { it.copy(agregadoAlCarrito = false, cantidad = 1) }  // ← También resetea cantidad a 1
    }
}