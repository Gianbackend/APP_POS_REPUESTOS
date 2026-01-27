package com.example.posapp.presentation.venta

import com.example.posapp.domain.model.ItemCarrito

data class VentaState(
    val items: List<ItemCarrito> = emptyList(),  // Items del carrito
    val subtotal: Double = 0.0,                  // Suma de productos
    val descuento: Double = 0.0,                 // % de descuento
    val impuesto: Double = 13.0,                 // IVA 13%
    val total: Double = 0.0,                     // Total final
    val metodoPago: String = "EFECTIVO",         // EFECTIVO, TARJETA, TRANSFERENCIA
    val isProcessing: Boolean = false,           // ¿Está procesando la venta?
    val ventaCompletada: Boolean = false,        // ¿Se completó la venta?
    val error: String? = null                    // Error si hay
) {
    // Calcular total con descuento e impuesto
    fun calcularTotal(): Double {
        val descuentoAplicado = subtotal * (descuento / 100)
        val subtotalConDescuento = subtotal - descuentoAplicado
        val impuestoAplicado = subtotalConDescuento * (impuesto / 100)
        return subtotalConDescuento + impuestoAplicado
    }
}