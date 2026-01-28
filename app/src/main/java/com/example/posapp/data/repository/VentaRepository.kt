package com.example.posapp.data.repository

import com.example.posapp.data.local.dao.DetalleVentaDao
import com.example.posapp.data.local.dao.ProductoDao
import com.example.posapp.data.local.dao.VentaDao
import com.example.posapp.data.local.entities.DetalleVentaEntity
import com.example.posapp.data.local.entities.VentaEntity
import com.example.posapp.data.preferences.UserPreferences
import com.example.posapp.domain.model.ItemCarrito
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VentaRepository @Inject constructor(
    private val ventaDao: VentaDao,              // Para guardar ventas
    private val detalleVentaDao: DetalleVentaDao, // Para guardar items
    private val productoDao: ProductoDao,         // Para reducir stock
    private val userPreferences: UserPreferences  // Para obtener usuario actual
) {

    // Procesar y guardar venta completa
    suspend fun procesarVenta(
        items: List<ItemCarrito>,
        metodoPago: String,
        descuento: Double = 0.0,
        impuesto: Double = 13.0
    ): Result<Long> {
        return try {
            // 1. Obtener usuario actual
            val session = userPreferences.userSession.first()
            val usuarioId = session.userId

            if (usuarioId == 0L) {
                return Result.failure(Exception("Usuario no autenticado"))
            }

            // 2. Calcular totales
            val subtotal = items.sumOf { it.subtotal }
            val descuentoAplicado = subtotal * (descuento / 100)
            val subtotalConDescuento = subtotal - descuentoAplicado
            val impuestoAplicado = subtotalConDescuento * (impuesto / 100)
            val total = subtotalConDescuento + impuestoAplicado

            // 3. Generar número de venta único
            val numeroVenta = generarNumeroVenta()

            // 4. Crear entidad de venta
            val ventaEntity = VentaEntity(
                numeroVenta = numeroVenta,
                usuarioId = usuarioId,
                clienteId = null,  // Por ahora sin cliente
                subtotal = subtotal,
                descuento = descuento,
                impuesto = impuesto,
                total = total,
                metodoPago = metodoPago,
                estado = "COMPLETADA",
                fechaVenta = System.currentTimeMillis(),
                sincronizado = false
            )

            // 5. Guardar venta en BD y obtener ID
            val ventaId = ventaDao.insert(ventaEntity)

            // 6. Crear detalles de venta
            val detalles = items.map { item ->
                DetalleVentaEntity(
                    ventaId = ventaId,
                    productoId = item.producto.id,
                    cantidad = item.cantidad,
                    precioUnitario = item.producto.precio,
                    subtotal = item.subtotal
                )
            }

            // 7. Guardar detalles
            detalleVentaDao.insertAll(detalles)

            // 8. Reducir stock de productos
            items.forEach { item ->
                productoDao.reducirStock(item.producto.id, item.cantidad)
            }

            // Retornar ID de la venta
            Result.success(ventaId)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Generar número de venta único (formato: V-2026-001)
    private suspend fun generarNumeroVenta(): String {
        val year = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())

        // Obtener todas las ventas y contar cuántas hay este año
        val ventas = ventaDao.getAll().first()
        val ventasEsteAno = ventas.filter { it.numeroVenta.startsWith("V-$year") }
        val siguienteNumero = ventasEsteAno.size + 1

        // Formato: V-2026-001
        return "V-$year-${String.format("%03d", siguienteNumero)}"
    }

    // Obtener venta por ID (para mostrar ticket)
    suspend fun getVentaById(id: Long): VentaEntity? {
        return ventaDao.getById(id)
    }

    // Obtener detalles de una venta
    suspend fun getDetallesVenta(ventaId: Long): List<DetalleVentaEntity> {
        return detalleVentaDao.getByVenta(ventaId)
    }
}