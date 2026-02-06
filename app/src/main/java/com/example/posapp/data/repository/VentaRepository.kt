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
    private val ventaDao: VentaDao,
    private val detalleVentaDao: DetalleVentaDao,
    private val productoDao: ProductoDao,
    private val clienteDao: ClienteDao,
    private val userPreferences: UserPreferences
) {

    // Procesar y guardar venta completa
    suspend fun procesarVenta(
        items: List<ItemCarrito>,
        metodoPago: String,
        clienteNombre: String,
        clienteDocumento: String,
        clienteTelefono: String = "",
        clienteEmail: String = "",
        descuento: Double = 0.0,
        impuesto: Double = 18.0
    ): Result<Long> {
        return try {
            // 1. Usuario
            val session = userPreferences.userSession.first()
            val usuarioId = session.userId

            // ✅ CAMBIO: Ahora userId es String
            if (usuarioId.isEmpty()) {
                return Result.failure(Exception("Usuario no autenticado"))
            }

            // 2. Guardar o buscar cliente PRIMERO
            var clienteId: Long? = null

            if (clienteNombre.isNotBlank() && clienteDocumento.isNotBlank()) {
                val clienteExistente = clienteDao.getByDocumento(clienteDocumento)

                clienteId = if (clienteExistente != null) {
                    clienteExistente.id
                } else {
                    // Guardar cliente ANTES de la venta
                    val nuevoCliente = ClienteEntity(
                        nombre = clienteNombre,
                        documento = clienteDocumento,
                        telefono = clienteTelefono,
                        email = clienteEmail,
                        direccion = "",
                        tipo = "MINORISTA"
                    )
                    val nuevoId = clienteDao.insert(nuevoCliente)

                    // ✅ VERIFICAR que se guardó
                    if (nuevoId <= 0) {
                        throw Exception("Error al guardar cliente")
                    }

                    nuevoId
                }
            }

            // 3. Calcular totales
            val totalConIVA = items.sumOf { it.subtotal }
            val descuentoAplicado = totalConIVA * (descuento / 100)
            val total = totalConIVA - descuentoAplicado
            val subtotalSinIVA = total / (1 + impuesto / 100)

            // 4. Número de venta
            val numeroVenta = generarNumeroVenta()

            // 5. Crear venta CON clienteId válido
            val ventaEntity = VentaEntity(
                numeroVenta = numeroVenta,
                usuarioId = usuarioId, // ✅ String (UID de Firebase)
                clienteId = clienteId,
                subtotal = subtotalSinIVA,
                descuento = descuento,
                impuesto = impuesto,
                total = total,
                metodoPago = metodoPago,
                estado = "COMPLETADA",
                fechaVenta = System.currentTimeMillis(),
                sincronizado = false
            )

            // 6. Guardar venta
            val ventaId = ventaDao.insert(ventaEntity)

            // 7. Guardar detalles
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

            // 8. Reducir stock
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
