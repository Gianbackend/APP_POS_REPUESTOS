package com.example.posapp.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "detalle_ventas",
    foreignKeys = [
        ForeignKey(
            entity = VentaEntity::class,
            parentColumns = ["id"],
            childColumns = ["ventaId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ProductoEntity::class,
            parentColumns = ["id"],
            childColumns = ["productoId"]
        )
    ],
    indices = [
        Index(value = ["ventaId"]),
        Index(value = ["productoId"])
    ]
)
data class DetalleVentaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val ventaId: Long,
    val productoId: Long,
    val cantidad: Int,
    val precioUnitario: Double,
    val subtotal: Double
)