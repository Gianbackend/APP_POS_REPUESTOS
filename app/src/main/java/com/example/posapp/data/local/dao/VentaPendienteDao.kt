package com.example.posapp.data.local.dao

import androidx.room.*
import com.example.posapp.data.local.entity.VentaPendienteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VentaPendienteDao {

    // ✅ AGREGAR: Método insert (alias)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(venta: VentaPendienteEntity): Long

    // ✅ AGREGAR: Método update (alias)
    @Update
    suspend fun update(venta: VentaPendienteEntity)

    // ✅ AGREGAR: Método getById
    @Query("SELECT * FROM ventas_pendientes WHERE id = :id")
    suspend fun getById(id: Long): VentaPendienteEntity?

    // ✅ Cambiar: getVentasPendientes debe retornar List, no Flow
    @Query("SELECT * FROM ventas_pendientes WHERE sincronizado = 0 ORDER BY fecha ASC")
    suspend fun getVentasPendientes(): List<VentaPendienteEntity>

    // ✅ AGREGAR: observarVentasPendientes (Flow)
    @Query("SELECT * FROM ventas_pendientes WHERE sincronizado = 0 ORDER BY fecha ASC")
    fun observarVentasPendientes(): Flow<List<VentaPendienteEntity>>

    // ✅ Cambiar: contarVentasPendientes debe retornar Int, no Flow
    @Query("SELECT COUNT(*) FROM ventas_pendientes WHERE sincronizado = 0")
    suspend fun contarVentasPendientes(): Int

    // ✅ Mantener tus métodos originales
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

    // ✅ AGREGAR: Métodos adicionales útiles
    @Query("""
        SELECT * FROM ventas_pendientes 
        WHERE sincronizado = 0 
        AND intentosSincronizacion < :maxReintentos
        ORDER BY fecha DESC
    """)
    suspend fun getVentasConError(maxReintentos: Int): List<VentaPendienteEntity>

    @Query("""
        DELETE FROM ventas_pendientes 
        WHERE sincronizado = 1 
        AND fecha < :timestamp
    """)
    suspend fun eliminarSincronizadasAntiguas(timestamp: Long): Int
}
