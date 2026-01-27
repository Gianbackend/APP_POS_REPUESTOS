package com.example.posapp.domain.model

data class Usuario(
    val id: Long = 0,
    val nombre: String,
    val email: String,
    val rol: String, // "VENDEDOR", "ADMIN", "CAJERO"
    val activo: Boolean = true
)