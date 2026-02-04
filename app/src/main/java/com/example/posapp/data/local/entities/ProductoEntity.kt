package com.example.posapp.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.posapp.domain.model.Producto

@Entity(
    tableName = "productos",
    foreignKeys = [
        ForeignKey(
            entity = CategoriaEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoriaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["codigo"], unique = true),
        Index(value = ["categoriaId"])
    ]
)
data class ProductoEntity(
    @PrimaryKey(autoGenerate = true)
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
    val imagenUrl: String? = null,
    val ubicacion: String? = null,
    val activo: Boolean = true,
    val fechaCreacion: Long = System.currentTimeMillis(),
    // 🆕 NUEVOS CAMPOS PARA SINCRONIZACIÓN
    val firebaseId: String? = null,           // ID en Firebase
    val sincronizado: Boolean = false,        // ¿Ya está en la nube?
    val ultimaSincronizacion: Long? = null    // Timestamp última sync
) {
    fun toDomain(categoriaNombre: String = "") = Producto(
        id = id,
        codigo = codigo,
        nombre = nombre,
        descripcion = descripcion,
        marca = marca,
        modelo = modelo,
        precio = precio,
        stock = stock,
        stockMinimo = stockMinimo,
        categoriaId = categoriaId,
        categoriaNombre = categoriaNombre,
        imagenUrl = imagenUrl,
        ubicacion = ubicacion,
        activo = activo
    )
    // 🆕 NUEVO: Convertir a Firebase
    fun toFirebase() = hashMapOf(
        "codigo" to codigo,
        "nombre" to nombre,
        "descripcion" to descripcion,
        "marca" to marca,
        "modelo" to modelo,
        "precio" to precio,
        "stock" to stock,
        "stockMinimo" to stockMinimo,
        "categoriaId" to categoriaId,
        "imagenUrl" to imagenUrl,
        "ubicacion" to ubicacion,
        "activo" to activo,
        "fechaCreacion" to fechaCreacion
    )
}