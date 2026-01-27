package com.example.posapp.domain.model

data class ItemCarrito(
    val producto: Producto,      // Producto completo
    val cantidad: Int            // Cantidad en el carrito
) {
    // Calcular subtotal de este item
    val subtotal: Double
        get() = producto.precio * cantidad
}