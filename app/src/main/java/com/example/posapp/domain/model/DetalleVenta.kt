package com.example.posapp.domain.model

data class DetalleVenta(
    val id: Long = 0,
    val ventaId: Long = 0,
    val productoId: Long,
    val productoCodigo: String,
    val productoNombre: String,
    val cantidad: Int,
    val precioUnitario: Double,
    val subtotal: Double = cantidad * precioUnitario
)