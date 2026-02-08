package com.example.posapp.data.repository

import android.util.Log
import com.example.posapp.data.local.dao.ClienteDao
import com.example.posapp.data.local.dao.DetalleVentaDao
import com.example.posapp.data.local.dao.ProductoDao
import com.example.posapp.data.local.dao.VentaDao
import com.example.posapp.data.local.entities.ClienteEntity
import com.example.posapp.data.local.entities.DetalleVentaEntity
import com.example.posapp.data.local.entities.VentaEntity
import com.example.posapp.data.preferences.UserPreferences
import com.example.posapp.domain.model.ItemCarrito
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
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
    private val userPreferences: UserPreferences,
    private val firestore: FirebaseFirestore // ✅ Agregar
) {

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

            if (usuarioId.isEmpty()) {
                return Result.failure(Exception("Usuario no autenticado"))
            }

            // 2. Guardar o buscar cliente
            var clienteId: Long? = null

            if (clienteNombre.isNotBlank() && clienteDocumento.isNotBlank()) {
                val clienteExistente = clienteDao.getByDocumento(clienteDocumento)

                clienteId = if (clienteExistente != null) {
                    clienteExistente.id
                } else {
                    val nuevoCliente = ClienteEntity(
                        nombre = clienteNombre,
                        documento = clienteDocumento,
                        telefono = clienteTelefono,
                        email = clienteEmail,
                        direccion = "",
                        tipo = "MINORISTA"
                    )
                    val nuevoId = clienteDao.insert(nuevoCliente)

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

            // 5. Crear venta
            val ventaEntity = VentaEntity(
                numeroVenta = numeroVenta,
                usuarioId = usuarioId,
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

            // ✅ 8. Reducir stock
            items.forEach { item ->
                productoDao.reducirStock(item.producto.id, item.cantidad)
            }
            // ✅ 9. Actualizar stock en FIREBASE
            items.forEach { item ->
                val productoActualizado = productoDao.getById(item.producto.id)
                val firebaseId = productoActualizado?.firebaseId

                if (productoActualizado != null && !firebaseId.isNullOrEmpty()) {
                    try {
                        firestore.collection("productos")
                            .document(firebaseId) // ✅ Ya es String no nullable
                            .update("stock", productoActualizado.stock)
                            .await()
                    } catch (e: Exception) {
                        Log.e("VentaRepository", "Error actualizando stock en Firebase: ${e.message}")
                    }
                }
            }

            Result.success(ventaId)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun generarNumeroVenta(): String {
        val year = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())
        val ventas = ventaDao.getAll().first()
        val ventasEsteAno = ventas.filter { it.numeroVenta.startsWith("V-$year") }
        val siguienteNumero = ventasEsteAno.size + 1
        return "V-$year-${String.format("%03d", siguienteNumero)}"
    }

    suspend fun getVentaById(id: Long): VentaEntity? {
        return ventaDao.getById(id)
    }

    suspend fun getDetallesVenta(ventaId: Long): List<DetalleVentaEntity> {
        return detalleVentaDao.getByVenta(ventaId)
    }
}
