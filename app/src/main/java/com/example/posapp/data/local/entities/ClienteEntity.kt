package com.example.posapp.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.posapp.domain.model.Cliente

@Entity(
    tableName = "clientes",
    indices = [Index(value = ["documento"], unique = true)]
)
data class ClienteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nombre: String,
    val documento: String,
    val telefono: String = "",
    val email: String = "",
    val direccion: String = "",
    val tipo: String = "MINORISTA",
    val fechaCreacion: Long = System.currentTimeMillis()
) {
    fun toDomain() = Cliente(
        id = id,
        nombre = nombre,
        documento = documento,
        telefono = telefono,
        email = email,
        direccion = direccion,
        tipo = tipo
    )
}