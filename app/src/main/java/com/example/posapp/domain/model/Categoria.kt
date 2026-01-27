package com.example.posapp.domain.model

data class Categoria(
    val id: Long = 0,
    val nombre: String,
    val descripcion: String = "",
    val icono: String = "category",
    val color: String = "#2196F3"
)