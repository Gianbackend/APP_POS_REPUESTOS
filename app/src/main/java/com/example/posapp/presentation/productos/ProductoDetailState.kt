package com.example.posapp.presentation.productos

import com.example.posapp.domain.model.Producto

data class ProductoDetailState(
    val producto: Producto? = null,        // Producto a mostrar
    val isLoading: Boolean = true,         // ¿Está cargando?
    val error: String? = null,             // Error si no se encuentra
    val cantidad: Int = 1,                 // Cantidad a agregar al carrito
    val agregadoAlCarrito: Boolean = false // ¿Se agregó al carrito?
)