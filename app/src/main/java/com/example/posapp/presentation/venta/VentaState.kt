package com.example.posapp.presentation.venta

import com.example.posapp.domain.model.ItemCarrito

data class VentaState(
    val items: List<ItemCarrito> = emptyList(),
    val subtotal: Double = 0.0,
    val descuento: Double = 0.0,
    val impuesto: Double = 18.0,  // ← IVA 18%
    val total: Double = 0.0,
    val metodoPago: String = "EFECTIVO",
    val clienteForm: ClienteFormState = ClienteFormState(),  // ← NUEVO
    val mostrarFormCliente: Boolean = false,  // ← NUEVO: mostrar diálogo
    val isProcessing: Boolean = false,
    val ventaCompletada: Boolean = false,
    val ventaId: Long? = null,
    val error: String? = null
) {
    // Calcular total (precio ya incluye IVA)
    fun calcularTotal(): Double {
        // El total es la suma de precios (que ya incluyen IVA)
        val totalConIVA = items.sumOf { it.subtotal }

        // Aplicar descuento si hay
        val descuentoAplicado = totalConIVA * (descuento / 100)

        return totalConIVA - descuentoAplicado
    }

    // Calcular subtotal SIN IVA (desglose)
    fun calcularSubtotalSinIVA(): Double {
        val totalConIVA = calcularTotal()
        // Precio con IVA / 1.18 = Precio sin IVA
        return totalConIVA / (1 + impuesto / 100)
    }

    // Calcular monto del IVA (desglose)
    fun calcularMontoIVA(): Double {
        return calcularTotal() - calcularSubtotalSinIVA()
    }
}