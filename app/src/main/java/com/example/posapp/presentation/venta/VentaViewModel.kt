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
        // Validar que haya items
        if (_state.value.items.isEmpty()) {
            _state.update { it.copy(error = "El carrito está vacío") }
            return
        }

        // Validar datos del cliente
        val clienteForm = _state.value.clienteForm
        if (clienteForm.nombre.isBlank() || clienteForm.documento.isBlank()) {
            _state.update { it.copy(error = "Nombre y DNI son obligatorios") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isProcessing = true, error = null) }

            // Procesar venta con datos del cliente
            val result = ventaRepository.procesarVenta(
                items = _state.value.items,
                metodoPago = _state.value.metodoPago,
                clienteNombre = clienteForm.nombre,      // ← NUEVO
                clienteDocumento = clienteForm.documento, // ← NUEVO
                clienteTelefono = clienteForm.telefono,  // ← NUEVO
                clienteEmail = clienteForm.email,        // ← NUEVO
                descuento = _state.value.descuento,
                impuesto = _state.value.impuesto
            )

            if (result.isSuccess) {
                val ventaId = result.getOrNull()

                carritoRepository.limpiarCarrito()

                _state.update {
                    it.copy(
                        isProcessing = false,
                        ventaCompletada = true,
                        ventaId = ventaId,
                        mostrarFormCliente = false  // ← Cerrar el diálogo
                    )
                }
            } else {
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
    // Mostrar formulario de cliente
    fun onMostrarFormCliente() {
        _state.update { it.copy(mostrarFormCliente = true) }
    }

    // Ocultar formulario
    fun onOcultarFormCliente() {
        _state.update { it.copy(mostrarFormCliente = false) }
    }

    // Actualizar nombre
    fun onNombreClienteChange(nombre: String) {
        _state.update {
            it.copy(
                clienteForm = it.clienteForm.copy(nombre = nombre)
            )
        }
    }

    // Actualizar documento
    fun onDocumentoClienteChange(documento: String) {
        _state.update {
            it.copy(
                clienteForm = it.clienteForm.copy(documento = documento)
            )
        }
    }

    // Actualizar teléfono
    fun onTelefonoClienteChange(telefono: String) {
        _state.update {
            it.copy(
                clienteForm = it.clienteForm.copy(telefono = telefono)
            )
        }
    }

    // Actualizar email
    fun onEmailClienteChange(email: String) {
        _state.update {
            it.copy(
                clienteForm = it.clienteForm.copy(email = email)
            )
        }
    }


}