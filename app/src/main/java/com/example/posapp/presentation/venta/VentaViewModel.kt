package com.example.posapp.presentation.venta

import android.content.Context
import com.example.posapp.data.pdf.TicketPdfGenerator
import com.example.posapp.data.firebase.FirebaseStorageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.posapp.data.repository.CarritoRepository
import com.example.posapp.data.repository.VentaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.flow.combine

@HiltViewModel
class VentaViewModel @Inject constructor(
    private val carritoRepository: CarritoRepository,
    private val ventaRepository: VentaRepository,
    private val firebaseStorageManager: FirebaseStorageManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(VentaState())
    val state = _state.asStateFlow()

    private var observerJob: Job? = null
    private var hasNavigated = false
    private val _shouldObserve = MutableStateFlow(true)
    private val shouldObserve = _shouldObserve.asStateFlow()

    // 🆕 Observar ventas pendientes (opcional - para mostrar badge)
    val ventasPendientes = ventaRepository.observarVentasPendientes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            shouldObserve.collect { value ->
                android.util.Log.d("VentaVM", "🟡🟡🟡 shouldObserve cambió a: $value")
            }
        }
        observarCarrito()
    }

    private fun observarCarrito() {
        observerJob?.cancel()

        observerJob = viewModelScope.launch {
            combine(
                carritoRepository.items,
                shouldObserve
            ) { items, shouldObs ->
                items to shouldObs
            }.collect { (items, shouldObs) ->
                android.util.Log.d("VentaVM", "🟣🟣🟣 NUEVA VERSIÓN DEL OBSERVER")

                android.util.Log.d("VentaVM", "Observer recibió: ${items.size} items, shouldObserve=$shouldObs, isProcessing=${_state.value.isProcessing}, ventaCompletada=${_state.value.ventaCompletada}, hasNavigated=$hasNavigated")

                if (shouldObs &&
                    !_state.value.isProcessing &&
                    !_state.value.ventaCompletada &&
                    !hasNavigated) {

                    android.util.Log.d("VentaVM", "✅ Actualizando state con ${items.size} items")

                    val subtotal = items.sumOf { it.subtotal }

                    _state.update {
                        it.copy(
                            items = items,
                            subtotal = subtotal,
                            total = it.calcularTotal()
                        )
                    }
                } else {
                    android.util.Log.d("VentaVM", "🚫 Observer BLOQUEADO - shouldObserve=$shouldObs")
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

    fun onProcesarVenta() {
        android.util.Log.d("VentaVM", "🔵🔵🔵 onProcesarVenta INICIADO")

        _shouldObserve.value = false

        if (_state.value.items.isEmpty()) {
            android.util.Log.d("VentaVM", "❌ Carrito vacío")
            _state.update { it.copy(error = "El carrito está vacío") }
            return
        }

        val clienteForm = _state.value.clienteForm
        if (clienteForm.nombre.isBlank() || clienteForm.documento.isBlank()) {
            android.util.Log.d("VentaVM", "❌ Datos de cliente incompletos")
            _state.update { it.copy(error = "Nombre y DNI son obligatorios") }
            return
        }

        if (clienteForm.email.isBlank()) {
            android.util.Log.d("VentaVM", "❌ Email es obligatorio")
            _state.update { it.copy(error = "El email es obligatorio para enviar el ticket") }
            return
        }

        android.util.Log.d("VentaVM", "✅ Validaciones OK - Procesando venta")

        viewModelScope.launch {
            val itemsSnapshot = _state.value.items.toList()

            _state.update { it.copy(isProcessing = true, error = null) }

            val result = ventaRepository.procesarVenta(
                items = itemsSnapshot,
                metodoPago = _state.value.metodoPago,
                clienteNombre = clienteForm.nombre,
                clienteDocumento = clienteForm.documento,
                clienteTelefono = clienteForm.telefono,
                clienteEmail = clienteForm.email,
                descuento = _state.value.descuento,
                impuesto = _state.value.impuesto
            )

            if (result.isSuccess) {
                val ventaId = result.getOrNull()
                android.util.Log.d("VentaVM", "✅ Venta procesada exitosamente, ventaId=$ventaId")

                try {
                    val numeroVenta = "V-${SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())}-${String.format("%03d", ventaId)}"
                    val fecha = Date()

                    val pdfGenerator = TicketPdfGenerator(context)
                    val pdfFile = pdfGenerator.generarTicket(
                        numeroVenta = numeroVenta,
                        fecha = fecha,
                        metodoPago = _state.value.metodoPago,
                        clienteNombre = clienteForm.nombre,
                        clienteDocumento = clienteForm.documento,
                        clienteTelefono = clienteForm.telefono,
                        clienteEmail = clienteForm.email,
                        items = itemsSnapshot,
                        subtotalSinIVA = _state.value.calcularSubtotalSinIVA(),
                        montoIVA = _state.value.calcularMontoIVA(),
                        total = _state.value.calcularTotal(),
                        impuestoPorcentaje = _state.value.impuesto
                    )

                    android.util.Log.d("VentaVM", "✅ PDF generado: ${pdfFile.absolutePath}")

                    val uploadResult = firebaseStorageManager.subirTicket(
                        file = pdfFile,
                        numeroVenta = numeroVenta,
                        clienteEmail = clienteForm.email,
                        total = _state.value.calcularTotal(),
                        fecha = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(fecha)
                    )

                    if (uploadResult.isSuccess) {
                        android.util.Log.d("VentaVM", "✅ PDF subido a Storage, URL: ${uploadResult.getOrNull()}")
                        android.util.Log.d("VentaVM", "📧 Cloud Function enviará el email automáticamente")
                    } else {
                        android.util.Log.e("VentaVM", "❌ Error al subir PDF: ${uploadResult.exceptionOrNull()?.message}")
                    }

                } catch (e: Exception) {
                    android.util.Log.e("VentaVM", "❌ Error al generar/subir PDF: ${e.message}")
                }

                _state.update {
                    it.copy(
                        isProcessing = true,
                        ventaCompletada = true,
                        ventaId = ventaId,
                        mostrarFormCliente = false,
                        items = itemsSnapshot
                    )
                }
                android.util.Log.d("VentaVM", "✅ State actualizado con ventaCompletada=true")

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

    fun onPreNavigate() {
        hasNavigated = true
        observerJob?.cancel()
    }

    fun onResetVentaCompletada() {
        android.util.Log.d("VentaVM", "🔵 onResetVentaCompletada INICIADO")

        carritoRepository.limpiarCarrito()

        _state.update {
            it.copy(
                isProcessing = false,
                ventaCompletada = false,
                ventaId = null,
                items = emptyList(),
                clienteForm = ClienteFormState()
            )
        }

        hasNavigated = false

        android.util.Log.d("VentaVM", "🟢 Cambiando shouldObserve a true en onResetVentaCompletada")
        _shouldObserve.value = true
        observarCarrito()
    }

    fun onMostrarFormCliente() {
        android.util.Log.d("VentaVM", "📋 Mostrando formulario de cliente")
        _state.update { it.copy(mostrarFormCliente = true) }
    }

    fun onOcultarFormCliente() {
        _state.update { it.copy(mostrarFormCliente = false) }
    }

    fun onNombreClienteChange(nombre: String) {
        _state.update {
            it.copy(
                clienteForm = it.clienteForm.copy(nombre = nombre)
            )
        }
    }

    fun onDocumentoClienteChange(documento: String) {
        _state.update {
            it.copy(
                clienteForm = it.clienteForm.copy(documento = documento)
            )
        }
    }

    fun onTelefonoClienteChange(telefono: String) {
        _state.update {
            it.copy(
                clienteForm = it.clienteForm.copy(telefono = telefono)
            )
        }
    }

    fun onEmailClienteChange(email: String) {
        _state.update {
            it.copy(
                clienteForm = it.clienteForm.copy(email = email)
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        observerJob?.cancel()
    }
}
