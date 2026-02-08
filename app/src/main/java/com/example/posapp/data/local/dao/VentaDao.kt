package com.example.posapp.data.local.dao

import androidx.room.*
import com.example.posapp.data.local.entities.VentaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VentaDao {

    @Query("SELECT * FROM ventas ORDER BY fechaVenta DESC")
    fun getAll(): Flow<List<VentaEntity>>

    @Query("SELECT * FROM ventas WHERE id = :id")
    suspend fun getById(id: Long): VentaEntity?

    @Query("SELECT * FROM ventas WHERE sincronizado = 0")
    suspend fun getNoSincronizadas(): List<VentaEntity>

    @Query("""
        SELECT * FROM ventas 
        WHERE fechaVenta BETWEEN :fechaInicio AND :fechaFin 
        ORDER BY fechaVenta DESC
    """)
    fun getByRangoFechas(fechaInicio: Long, fechaFin: Long): Flow<List<VentaEntity>>

    @Query("SELECT * FROM ventas WHERE usuarioId = :usuarioId ORDER BY fechaVenta DESC")
    fun getByUsuario(usuarioId: Long): Flow<List<VentaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(venta: VentaEntity): Long

    @Update
    suspend fun update(venta: VentaEntity)

    @Query("UPDATE ventas SET sincronizado = 1 WHERE id = :id")
    suspend fun marcarComoSincronizada(id: Long)

    @Query("DELETE FROM ventas")
    suspend fun deleteAll()
}