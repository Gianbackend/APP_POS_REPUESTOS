package com.example.posapp.domain.model

data class Cliente(
    val id: Long = 0,
    val nombre: String,
    val documento: String,
    val telefono: String = "",
    val email: String = "",
    val direccion: String = "",
    val tipo: String = "MINORISTA"
)