package com.example.posapp.presentation.venta

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.posapp.data.repository.CarritoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VentaViewModel @Inject constructor(
    private val carritoRepository: CarritoRepository  // Observa el carrito
) : ViewModel() {

    private val _state = MutableStateFlow(VentaState())
    val state = _state.asStateFlow()

    init {
        // Observar cambios en el carrito
        observarCarrito()
    }

    // Observar items del carrito y actualizar estado
    private fun observarCarrito() {
        viewModelScope.launch {
            carritoRepository.items.collect { items ->
                val subtotal = items.sumOf { it.subtotal }

                _state.update {
                    it.copy(
                        items = items,
                        subtotal = subtotal,
                        total = it.calcularTotal()
                    )
                }
            }
        }
    }

    // Actualizar cantidad de un item
    fun onActualizarCantidad(productoId: Long, nuevaCantidad: Int) {
        carritoRepository.actualizarCantidad(productoId, nuevaCantidad)
    }

    // Eliminar item del carrito
    fun onEliminarItem(productoId: Long) {
        carritoRepository.eliminarProducto(productoId)
    }

    // Cambiar método de pago
    fun onMetodoPagoChange(metodo: String) {
        _state.update { it.copy(metodoPago = metodo) }
    }

    // Aplicar descuento
    fun onDescuentoChange(descuento: Double) {
        _state.update {
            it.copy(
                descuento = descuento,
                total = it.calcularTotal()
            )
        }
    }

    // Procesar venta (por ahora solo limpia el carrito)
    fun onProcesarVenta() {
        if (_state.value.items.isEmpty()) {
            _state.update { it.copy(error = "El carrito está vacío") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isProcessing = true) }

            // Aquí más adelante guardaremos en la BD
            // Por ahora solo limpiamos el carrito

            kotlinx.coroutines.delay(1000)  // Simular procesamiento

            carritoRepository.limpiarCarrito()

            _state.update {
                it.copy(
                    isProcessing = false,
                    ventaCompletada = true
                )
            }
        }
    }

    // Resetear venta completada
    fun onResetVentaCompletada() {
        _state.update { it.copy(ventaCompletada = false) }
    }
}