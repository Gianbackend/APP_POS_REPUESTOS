package com.example.posapp.data.local.dao

import androidx.room.*
import com.example.posapp.data.local.entity.VentaPendienteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VentaPendienteDao {

    @Query("SELECT * FROM ventas_pendientes WHERE sincronizado = 0 ORDER BY fecha ASC")
    fun getVentasPendientes(): Flow<List<VentaPendienteEntity>>

    @Query("SELECT * FROM ventas_pendientes WHERE sincronizado = 0 ORDER BY fecha ASC")
    suspend fun getVentasPendientesList(): List<VentaPendienteEntity>

    @Query("SELECT COUNT(*) FROM ventas_pendientes WHERE sincronizado = 0")
    fun contarVentasPendientes(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVentaPendiente(venta: VentaPendienteEntity): Long

    @Update
    suspend fun updateVentaPendiente(venta: VentaPendienteEntity)

    @Query("UPDATE ventas_pendientes SET sincronizado = 1, firebaseId = :firebaseId WHERE id = :id")
    suspend fun marcarComoSincronizada(id: Long, firebaseId: String)

    @Query("UPDATE ventas_pendientes SET intentosSincronizacion = :intentos, ultimoIntento = :timestamp, errorSincronizacion = :error WHERE id = :id")
    suspend fun registrarIntentoFallido(id: Long, intentos: Int, timestamp: Long, error: String)

    @Delete
    suspend fun deleteVentaPendiente(venta: VentaPendienteEntity)

    @Query("DELETE FROM ventas_pendientes WHERE sincronizado = 1")
    suspend fun limpiarVentasSincronizadas()

    @Query("SELECT * FROM ventas_pendientes ORDER BY fecha DESC")
    fun getAllVentas(): Flow<List<VentaPendienteEntity>>
}
