package com.example.posapp.data.local.dao

import androidx.room.*
import com.example.posapp.data.local.entities.DetalleVentaEntity

@Dao
interface DetalleVentaDao {

    @Query("SELECT * FROM detalle_ventas WHERE ventaId = :ventaId")
    suspend fun getByVenta(ventaId: Long): List<DetalleVentaEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(detalles: List<DetalleVentaEntity>)

    @Query("DELETE FROM detalle_ventas WHERE ventaId = :ventaId")
    suspend fun deleteByVenta(ventaId: Long)

    @Query("DELETE FROM detalle_ventas")
    suspend fun deleteAll()
}