package com.example.posapp.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.posapp.domain.model.Usuario

@Entity(tableName = "usuarios")
data class UsuarioEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nombre: String,
    val email: String,
    val passwordHash: String,
    val rol: String,
    val activo: Boolean = true,
    val fechaCreacion: Long = System.currentTimeMillis()
) {
    fun toDomain() = Usuario(
        id = id,
        nombre = nombre,
        email = email,
        rol = rol,
        activo = activo
    )
}