package com.example.posapp.presentation.venta

data class ClienteFormState(
    val nombre: String = "",
    val documento: String = "",
    val telefono: String = "",
    val email: String = "",
    val tipo: String = "MINORISTA"  // MINORISTA, MAYORISTA, TALLER
)