package com.example.posapp.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.posapp.domain.model.Categoria

@Entity(
    tableName = "categorias",
    indices = [Index(value = ["nombre"], unique = true)]
)
data class CategoriaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nombre: String,
    val descripcion: String = "",
    val icono: String = "category",
    val color: String = "#2196F3"
) {
    fun toDomain() = Categoria(
        id = id,
        nombre = nombre,
        descripcion = descripcion,
        icono = icono,
        color = color
    )
}