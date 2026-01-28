package com.example.posapp.presentation.venta

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.posapp.data.repository.CarritoRepository
import com.example.posapp.data.repository.VentaRepository  // ← NUEVO
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VentaViewModel @Inject constructor(
    private val carritoRepository: CarritoRepository,
    private val ventaRepository: VentaRepository  // ← NUEVO
) : ViewModel() {

    private val _state = MutableStateFlow(VentaState())
    val state = _state.asStateFlow()

    init {
        observarCarrito()
    }

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

    fun onActualizarCantidad(productoId: Long, nuevaCantidad: Int) {
        carritoRepository.actualizarCantidad(productoId, nuevaCantidad)
    }

    fun onEliminarItem(productoId: Long) {
        carritoRepository.eliminarProducto(productoId)
    }

    fun onMetodoPagoChange(metodo: String) {
        _state.update { it.copy(metodoPago = metodo) }
    }

    fun onDescuentoChange(descuento: Double) {
        _state.update {
            it.copy(
                descuento = descuento,
                total = it.calcularTotal()
            )
        }
    }

    // ACTUALIZADO: Ahora guarda en la BD
    fun onProcesarVenta() {
        if (_state.value.items.isEmpty()) {
            _state.update { it.copy(error = "El carrito está vacío") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isProcessing = true, error = null) }

            // Procesar y guardar venta
            val result = ventaRepository.procesarVenta(
                items = _state.value.items,
                metodoPago = _state.value.metodoPago,
                descuento = _state.value.descuento,
                impuesto = _state.value.impuesto
            )

            if (result.isSuccess) {
                // Venta exitosa
                val ventaId = result.getOrNull()

                // Limpiar carrito
                carritoRepository.limpiarCarrito()

                // Actualizar estado
                _state.update {
                    it.copy(
                        isProcessing = false,
                        ventaCompletada = true,
                        ventaId = ventaId  // Guardar ID de la venta
                    )
                }
            } else {
                // Error al procesar
                val error = result.exceptionOrNull()?.message ?: "Error al procesar la venta"

                _state.update {
                    it.copy(
                        isProcessing = false,
                        error = error
                    )
                }
            }
        }
    }

    fun onResetVentaCompletada() {
        _state.update { it.copy(ventaCompletada = false, ventaId = null) }
    }
}