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
    // 🆕 NUEVOS CAMPOS PARA PDF
    val pdfRutaLocal: String? = null,  // Ruta del PDF en el dispositivo
    val pdfSubido: Boolean = false,     // Si el PDF ya se subió a Storage
    val pdfUrlStorage: String? = null,  // URL del PDF en Firebase Storage
    val emailEnviado: Boolean = false,  // Si el email ya se envió
    val clienteEmail: String? = null,   // Email del cliente
    val numeroVenta: String? = null,     // Número de venta (V-2026-007)

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
