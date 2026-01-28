package com.example.posapp.presentation.ticket

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.posapp.data.repository.ProductoRepository
import com.example.posapp.data.repository.VentaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TicketViewModel @Inject constructor(
    private val ventaRepository: VentaRepository,
    private val productoRepository: ProductoRepository,
    savedStateHandle: SavedStateHandle  // Para recibir ventaId desde navegación
) : ViewModel() {

    private val _state = MutableStateFlow(TicketState())
    val state = _state.asStateFlow()

    // Obtener ID de la venta desde argumentos de navegación
    private val ventaId: Long = savedStateHandle.get<String>("ventaId")?.toLongOrNull() ?: 0L

    init {
        loadVenta()
    }

    // Cargar datos de la venta
    private fun loadVenta() {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true) }

                // Obtener venta
                val venta = ventaRepository.getVentaById(ventaId)

                if (venta == null) {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = "Venta no encontrada"
                        )
                    }
                    return@launch
                }

                // Obtener detalles de la venta
                val detalles = ventaRepository.getDetallesVenta(ventaId)

                // Convertir detalles a ItemTicket (con nombre del producto)
                val items = detalles.map { detalle ->
                    val producto = productoRepository.getProductoById(detalle.productoId)

                    ItemTicket(
                        nombreProducto = producto?.nombre ?: "Producto desconocido",
                        cantidad = detalle.cantidad,
                        precioUnitario = detalle.precioUnitario,
                        subtotal = detalle.subtotal
                    )
                }

                // Actualizar estado con todos los datos
                _state.update {
                    it.copy(
                        numeroVenta = venta.numeroVenta,
                        fechaVenta = venta.fechaVenta,
                        items = items,
                        subtotal = venta.subtotal,
                        descuento = venta.descuento,
                        impuesto = venta.impuesto,
                        total = venta.total,
                        metodoPago = venta.metodoPago,
                        isLoading = false,
                        error = null
                    )
                }

            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Error al cargar la venta"
                    )
                }
            }
        }
    }
}