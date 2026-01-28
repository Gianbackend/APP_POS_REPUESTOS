package com.example.posapp.presentation.ticket

import com.example.posapp.domain.model.Producto

data class TicketState(
    val numeroVenta: String = "",
    val fechaVenta: Long = 0L,
    val clienteNombre: String = "",
    val items: List<ItemTicket> = emptyList(),
    val subtotal: Double = 0.0,
    val descuento: Double = 0.0,
    val impuesto: Double = 0.0,
    val total: Double = 0.0,
    val metodoPago: String = "",
    val isLoading: Boolean = true,
    val error: String? = null
)

// Item individual del ticket
data class ItemTicket(
    val nombreProducto: String,
    val cantidad: Int,
    val precioUnitario: Double,
    val subtotal: Double
)