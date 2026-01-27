package com.example.posapp.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.posapp.data.local.entities.ClienteEntity
import com.example.posapp.data.local.entities.UsuarioEntity

@Entity(
    tableName = "ventas",
    foreignKeys = [
        ForeignKey(
            entity = UsuarioEntity::class,
            parentColumns = ["id"],
            childColumns = ["usuarioId"]
        ),
        ForeignKey(
            entity = ClienteEntity::class,
            parentColumns = ["id"],
            childColumns = ["clienteId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["numeroVenta"], unique = true),
        Index(value = ["usuarioId"]),
        Index(value = ["clienteId"])
    ]
)
data class VentaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val numeroVenta: String,
    val usuarioId: Long,
    val clienteId: Long? = null,
    val subtotal: Double,
    val descuento: Double = 0.0,
    val impuesto: Double,
    val total: Double,
    val metodoPago: String,
    val estado: String = "COMPLETADA",
    val fechaVenta: Long = System.currentTimeMillis(),
    val sincronizado: Boolean = false
)