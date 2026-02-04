package com.example.posapp.data.repository

import android.util.Log
import com.example.posapp.data.local.converter.ProductoVentaDto
import com.example.posapp.data.local.dao.ClienteDao
import com.example.posapp.data.local.dao.DetalleVentaDao
import com.example.posapp.data.local.dao.ProductoDao
import com.example.posapp.data.local.dao.VentaDao
import com.example.posapp.data.local.dao.VentaPendienteDao
import com.example.posapp.data.local.entities.ClienteEntity
import com.example.posapp.data.local.entities.DetalleVentaEntity
import com.example.posapp.data.local.entities.VentaEntity
import com.example.posapp.data.local.entity.VentaPendienteEntity
import com.example.posapp.data.preferences.UserPreferences
import com.example.posapp.domain.model.ItemCarrito
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
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
    private val ventaPendienteDao: VentaPendienteDao,
    private val userPreferences: UserPreferences,
    private val firestore: FirebaseFirestore,
    private val gson: Gson
) {

    companion object {
        private const val TAG = "VentaRepository"
        private const val COLLECTION_VENTAS = "ventas"
        private const val MAX_REINTENTOS = 3
    }

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
            val session = userPreferences.userSession.first()
            val usuarioId = session.userId

            if (usuarioId == 0L) {
                return Result.failure(Exception("Usuario no autenticado"))
            }

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
                    clienteDao.insert(nuevoCliente)
                }
            }

            val totalConIVA = items.sumOf { it.subtotal }
            val descuentoAplicado = totalConIVA * (descuento / 100)
            val total = totalConIVA - descuentoAplicado
            val subtotalSinIVA = total / (1 + impuesto / 100)

            val numeroVenta = generarNumeroVenta()

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

            guardarVentaPendiente(items, total, metodoPago, clienteNombre, clienteDocumento)

            Result.success(ventaId)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun guardarVentaPendiente(
        items: List<ItemCarrito>,
        total: Double,
        metodoPago: String,
        clienteNombre: String?,
        clienteDocumento: String?
    ) {
        try {
            val productosDto = items.map { item ->
                ProductoVentaDto(
                    productoId = item.producto.id,
                    codigo = item.producto.codigo,
                    nombre = item.producto.nombre,
                    cantidad = item.cantidad,
                    precioUnitario = item.producto.precio,
                    subtotal = item.subtotal
                )
            }

            val ventaPendiente = VentaPendienteEntity(
                fecha = System.currentTimeMillis(),
                total = total,
                metodoPago = metodoPago,
                clienteNombre = clienteNombre,
                clienteDocumento = clienteDocumento,
                productosJson = gson.toJson(productosDto),
                sincronizado = false
            )

            val ventaId = ventaPendienteDao.insert(ventaPendiente)
            sincronizarVenta(ventaId)

        } catch (e: Exception) {
            Log.e(TAG, "Error guardando venta pendiente", e)
        }
    }

    suspend fun sincronizarVenta(ventaId: Long): Result<String> {
        return try {
            val venta = ventaPendienteDao.getById(ventaId)
                ?: return Result.failure(Exception("Venta no encontrada"))

            if (venta.sincronizado) {
                return Result.success(venta.firebaseId ?: "")
            }

            val ventaData = hashMapOf(
                "fecha" to venta.fecha,
                "total" to venta.total,
                "metodoPago" to venta.metodoPago,
                "clienteNombre" to venta.clienteNombre,
                "clienteDocumento" to venta.clienteDocumento,
                "productos" to gson.fromJson(
                    venta.productosJson,
                    Array<ProductoVentaDto>::class.java
                ).toList(),
                "sincronizadoEn" to System.currentTimeMillis()
            )

            val docRef = firestore.collection(COLLECTION_VENTAS)
                .add(ventaData)
                .await()

            ventaPendienteDao.update(
                venta.copy(
                    sincronizado = true,
                    firebaseId = docRef.id,
                    errorSincronizacion = null
                )
            )

            Log.d(TAG, "✅ Venta sincronizada: ${docRef.id}")
            Result.success(docRef.id)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error sincronizando venta $ventaId", e)

            val venta = ventaPendienteDao.getById(ventaId)
            venta?.let {
                ventaPendienteDao.update(
                    it.copy(
                        intentosSincronizacion = it.intentosSincronizacion + 1,
                        ultimoIntento = System.currentTimeMillis(),
                        errorSincronizacion = e.message
                    )
                )
            }

            Result.failure(e)
        }
    }

    suspend fun sincronizarVentasPendientes(): Result<Int> {
        return try {
            val ventasPendientes = ventaPendienteDao.getVentasPendientes()
            var sincronizadas = 0

            ventasPendientes.forEach { venta ->
                if (venta.intentosSincronizacion < MAX_REINTENTOS) {
                    val resultado = sincronizarVenta(venta.id)
                    if (resultado.isSuccess) sincronizadas++
                }
            }

            Log.d(TAG, "✅ Sincronizadas $sincronizadas de ${ventasPendientes.size}")
            Result.success(sincronizadas)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error sincronizando ventas", e)
            Result.failure(e)
        }
    }

    fun observarVentasPendientes(): Flow<List<VentaPendienteEntity>> {
        return ventaPendienteDao.observarVentasPendientes()
    }

    suspend fun contarVentasPendientes(): Int {
        return ventaPendienteDao.contarVentasPendientes()
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
