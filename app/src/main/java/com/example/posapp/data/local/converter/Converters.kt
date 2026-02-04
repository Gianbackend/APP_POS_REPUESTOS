package com.example.posapp.data.local.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {

    private val gson = Gson()

    // Converter para List<ProductoVentaDto>
    @TypeConverter
    fun fromProductoVentaList(value: List<ProductoVentaDto>?): String? {
        return value?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toProductoVentaList(value: String?): List<ProductoVentaDto>? {
        return value?.let {
            val type = object : TypeToken<List<ProductoVentaDto>>() {}.type
            gson.fromJson(it, type)
        }
    }
}

// DTO para productos en la venta
data class ProductoVentaDto(
    val productoId: Long,
    val codigo: String,
    val nombre: String,
    val cantidad: Int,
    val precioUnitario: Double,
    val subtotal: Double
)
