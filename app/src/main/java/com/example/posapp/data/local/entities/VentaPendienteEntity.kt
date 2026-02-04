package com.example.posapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.posapp.data.local.converter.Converters

@Entity(tableName = "ventas_pendientes")
@TypeConverters(Converters::class)
data class VentaPendienteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val fecha: Long,                    // timestamp
    val total: Double,
    val metodoPago: String,             // "EFECTIVO", "TARJETA", "TRANSFERENCIA"
    val clienteNombre: String? = null,
    val clienteDocumento: String? = null,

    // Productos de la venta (JSON)
    val productosJson: String,          // Lista de ProductoVentaDto serializada

    // Control de sincronización
    val intentosSincronizacion: Int = 0,
    val ultimoIntento: Long? = null,
    val errorSincronizacion: String? = null,

    // Estado
    val sincronizado: Boolean = false,
    val firebaseId: String? = null      // ID en Firebase después de sincronizar
)

// DTO para los productos de la venta
data class ProductoVentaDto(
    val productoId: Long,
    val codigo: String,
    val nombre: String,
    val cantidad: Int,
    val precioUnitario: Double,
    val subtotal: Double
)
