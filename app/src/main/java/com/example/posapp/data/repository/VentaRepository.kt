package com.example.posapp.data.repository

import com.example.posapp.data.local.dao.ClienteDao
import com.example.posapp.data.local.dao.DetalleVentaDao
import com.example.posapp.data.local.dao.ProductoDao
import com.example.posapp.data.local.dao.VentaDao
import com.example.posapp.data.local.entities.ClienteEntity
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
    private val productoDao: ProductoDao,
    private val clienteDao: ClienteDao,// Para reducir stock
    private val userPreferences: UserPreferences  // Para obtener usuario actual
) {

    // Procesar y guardar venta completa
    suspend fun procesarVenta(
        items: List<ItemCarrito>,
        metodoPago: String,
        clienteNombre: String,       // ← NUEVO
        clienteDocumento: String,    // ← NUEVO
        clienteTelefono: String = "", // ← NUEVO
        clienteEmail: String = "",   // ← NUEVO
        descuento: Double = 0.0,
        impuesto: Double = 18.0
    ): Result<Long> {
        return try {
            // 1. Usuario
            val session = userPreferences.userSession.first()
            val usuarioId = session.userId

            if (usuarioId == 0L) {
                return Result.failure(Exception("Usuario no autenticado"))
            }
            // 2. Guardar o buscar cliente
            var clienteId: Long? = null

            if (clienteNombre.isNotBlank() && clienteDocumento.isNotBlank()) {
                // Buscar si el cliente ya existe por documento
                val clienteExistente = clienteDao.getByDocumento(clienteDocumento)

                clienteId = if (clienteExistente != null) {
                    // Cliente existe, usar su ID
                    clienteExistente.id
                } else {
                    // Cliente nuevo, guardarlo
                    val nuevoCliente = ClienteEntity(
                        nombre = clienteNombre,
                        documento = clienteDocumento,
                        telefono = clienteTelefono,
                        email = clienteEmail,
                        direccion = "",
                        tipo = "MINORISTA"
                    )
                    clienteDao.insert(nuevoCliente)
                }
            }
            // 2. Calcular totales (precio ya incluye IVA)
            val totalConIVA = items.sumOf { it.subtotal }
            val descuentoAplicado = totalConIVA * (descuento / 100)
            val total = totalConIVA - descuentoAplicado

            // Desglose del IVA (para guardar en BD)
            val subtotalSinIVA = total / (1 + impuesto / 100)
            //val montoIVA = total - subtotalSinIVA

            // 3. Número de venta
            val numeroVenta = generarNumeroVenta()

            // 4. Crear venta
            val ventaEntity = VentaEntity(
                numeroVenta = numeroVenta,
                usuarioId = usuarioId,
                clienteId = null,  // Por ahora null, luego lo agregamos
                subtotal = subtotalSinIVA,  // Subtotal sin IVA
                descuento = descuento,
                impuesto = impuesto,
                total = total,  // Total con IVA
                metodoPago = metodoPago,
                estado = "COMPLETADA",
                fechaVenta = System.currentTimeMillis(),
                sincronizado = false
            )

            // 5-8. Guardar venta, detalles, reducir stock (igual que antes)
            val ventaId = ventaDao.insert(ventaEntity)

            val detalles = items.map { item ->
                DetalleVentaEntity(
                    ventaId = ventaId,
                    productoId = item.producto.id,
                    cantidad = item.cantidad,
                    precioUnitario = item.producto.precio,
                    subtotal = item.subtotal
                )
            }

            detalleVentaDao.insertAll(detalles)

            items.forEach { item ->
                productoDao.reducirStock(item.producto.id, item.cantidad)
            }

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