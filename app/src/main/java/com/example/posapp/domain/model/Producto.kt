package com.example.posapp.domain.model

data class Producto(
    val id: Long = 0,
    val codigo: String,
    val nombre: String,
    val descripcion: String,
    val marca: String,
    val modelo: String,
    val precio: Double,
    val stock: Int,
    val stockMinimo: Int = 5,
    val categoriaId: Long,
    val categoriaNombre: String = "",
    val imagenUrl: String? = null,
    val ubicacion: String? = null,
    val activo: Boolean = true
) {
    val stockBajo: Boolean
        get() = stock <= stockMinimo
}