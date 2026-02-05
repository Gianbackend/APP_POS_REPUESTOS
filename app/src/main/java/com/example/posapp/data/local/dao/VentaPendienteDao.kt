package com.example.posapp.data.local.dao

import androidx.room.*
import com.example.posapp.data.local.entity.VentaPendienteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VentaPendienteDao {

    // ============================================
    // 🔵 MÉTODOS BÁSICOS (INSERT, UPDATE, DELETE)
    // ============================================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(venta: VentaPendienteEntity): Long

    @Update
    suspend fun update(venta: VentaPendienteEntity)

    @Delete
    suspend fun delete(venta: VentaPendienteEntity)

    @Query("DELETE FROM ventas_pendientes WHERE id = :id")
    suspend fun deleteById(id: Long)

    // ============================================
    // 🔵 CONSULTAS BÁSICAS
    // ============================================

    @Query("SELECT * FROM ventas_pendientes WHERE id = :id")
    suspend fun getById(id: Long): VentaPendienteEntity?

    @Query("SELECT * FROM ventas_pendientes")
    suspend fun getAll(): List<VentaPendienteEntity>

    @Query("SELECT * FROM ventas_pendientes ORDER BY fecha DESC")
    fun getAllVentas(): Flow<List<VentaPendienteEntity>>

    // ============================================
    // 🔵 VENTAS PENDIENTES DE SINCRONIZACIÓN
    // ============================================

    @Query("SELECT * FROM ventas_pendientes WHERE sincronizado = 0 ORDER BY fecha ASC")
    suspend fun getVentasPendientes(): List<VentaPendienteEntity>

    @Query("SELECT * FROM ventas_pendientes WHERE sincronizado = 0 ORDER BY fecha ASC")
    fun observarVentasPendientes(): Flow<List<VentaPendienteEntity>>

    @Query("SELECT COUNT(*) FROM ventas_pendientes WHERE sincronizado = 0")
    suspend fun contarVentasPendientes(): Int

    // ============================================
    // 🔵 SINCRONIZACIÓN DE VENTAS
    // ============================================

    @Query("UPDATE ventas_pendientes SET sincronizado = 1, firebaseId = :firebaseId WHERE id = :id")
    suspend fun marcarComoSincronizado(id: Long, firebaseId: String)

    @Query("UPDATE ventas_pendientes SET intentosSincronizacion = :intentos, ultimoIntento = :timestamp, errorSincronizacion = :error WHERE id = :id")
    suspend fun actualizarIntentoSincronizacion(id: Long, intentos: Int, timestamp: Long, error: String?)

    @Query("""
        SELECT * FROM ventas_pendientes 
        WHERE sincronizado = 0 
        AND intentosSincronizacion < :maxReintentos
        ORDER BY fecha DESC
    """)
    suspend fun getVentasConError(maxReintentos: Int): List<VentaPendienteEntity>

    // ============================================
    // 🆕 SINCRONIZACIÓN DE PDFs
    // ============================================

    @Query("SELECT * FROM ventas_pendientes WHERE pdfRutaLocal IS NOT NULL AND pdfSubido = 0")
    suspend fun getVentasConPdfsPendientes(): List<VentaPendienteEntity>

    @Query("SELECT * FROM ventas_pendientes WHERE pdfSubido = 1 AND emailEnviado = 0")
    suspend fun getVentasConEmailsPendientes(): List<VentaPendienteEntity>

    @Query("UPDATE ventas_pendientes SET pdfSubido = 1, pdfUrlStorage = :pdfUrl WHERE id = :id")
    suspend fun marcarPdfComoSubido(id: Long, pdfUrl: String)

    @Query("UPDATE ventas_pendientes SET emailEnviado = 1 WHERE id = :id")
    suspend fun marcarEmailComoEnviado(id: Long)

    // ============================================
    // 🔵 LIMPIEZA Y MANTENIMIENTO
    // ============================================

    @Query("DELETE FROM ventas_pendientes WHERE sincronizado = 1")
    suspend fun limpiarVentasSincronizadas()

    @Query("""
        DELETE FROM ventas_pendientes 
        WHERE sincronizado = 1 
        AND fecha < :timestamp
    """)
    suspend fun eliminarSincronizadasAntiguas(timestamp: Long): Int

    @Query("DELETE FROM ventas_pendientes WHERE sincronizado = 1 AND pdfSubido = 1 AND emailEnviado = 1")
    suspend fun limpiarVentasCompletamenteSync(): Int

    // ============================================
    // 🔵 MÉTODOS LEGACY (mantener compatibilidad)
    // ============================================

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
}
