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
    @PrimaryKey // ✅ SIN autoGenerate
    val id: Long, // ✅ ID manual
    val nombre: String,
    val descripcion: String? = null,
    val icono: String = "category",
    val color: String = "#2196F3",
    val activo: Boolean = true,
    val fechaCreacion: Long = System.currentTimeMillis(),
    val firebaseId: String = ""
) {
    fun toDomain() = Categoria(
        id = id,
        nombre = nombre,
        descripcion = descripcion,
        icono = icono,
        color = color
    )
}
