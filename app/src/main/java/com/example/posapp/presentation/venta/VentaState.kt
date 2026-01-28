package com.example.posapp.presentation.venta

import com.example.posapp.domain.model.ItemCarrito

data class VentaState(
    val items: List<ItemCarrito> = emptyList(),
    val subtotal: Double = 0.0,
    val descuento: Double = 0.0,
    val impuesto: Double = 18.0,
    val total: Double = 0.0,
    val metodoPago: String = "EFECTIVO",
    val isProcessing: Boolean = false,
    val ventaCompletada: Boolean = false,
    val ventaId: Long? = null,  // ← NUEVO: ID de la venta guardada
    val error: String? = null
) {
    fun calcularTotal(): Double {
        val descuentoAplicado = subtotal * (descuento / 100)
        val subtotalConDescuento = subtotal - descuentoAplicado
        val impuestoAplicado = subtotalConDescuento * (impuesto / 100)
        return subtotalConDescuento + impuestoAplicado
    }
}