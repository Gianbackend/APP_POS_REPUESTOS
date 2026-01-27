package com.example.posapp.domain.model

import com.example.posapp.domain.model.DetalleVenta

data class Venta(
    val id: Long = 0,
    val numeroVenta: String,
    val usuarioId: Long,
    val usuarioNombre: String = "",
    val clienteId: Long? = null,
    val clienteNombre: String? = null,
    val items: List<DetalleVenta> = emptyList(),
    val subtotal: Double,
    val descuento: Double = 0.0,
    val impuesto: Double,
    val total: Double,
    val metodoPago: String,
    val estado: String = "COMPLETADA",
    val fechaVenta: Long = System.currentTimeMillis(),
    val sincronizado: Boolean = false
)