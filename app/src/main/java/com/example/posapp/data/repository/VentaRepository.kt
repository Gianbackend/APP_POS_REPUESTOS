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
import kotlinx.coroutines.withTimeoutOrNull
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
        impuesto: Double = 18.0,
        pdfRutaLocal: String? = null
    ): Result<Long> {
        return try {
            Log.d(TAG, "🟢 Iniciando procesarVenta")

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

            Log.d(TAG, "🟢 Guardando venta en Room")

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
            Log.d(TAG, "✅ Venta guardada en Room con ID: $ventaId")

            Log.d(TAG, "🟢 Guardando detalles de venta")
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
            Log.d(TAG, "✅ Detalles guardados")

            Log.d(TAG, "🟢 Reduciendo stock")
            items.forEach { item ->
                productoDao.reducirStock(item.producto.id, item.cantidad)
            }
            Log.d(TAG, "✅ Stock actualizado")

            // 🔥 CAMBIO CRÍTICO: Guardar venta pendiente SIN sincronizar inmediatamente
            guardarVentaPendienteSinSincronizar(
                items = items,
                total = total,
                metodoPago = metodoPago,
                clienteNombre = clienteNombre,
                clienteDocumento = clienteDocumento,
                clienteEmail = clienteEmail,    // 🆕
                numeroVenta = numeroVenta,       // 🆕
                pdfRutaLocal = pdfRutaLocal     // 🆕
            )

            Log.d(TAG, "✅ procesarVenta completado exitosamente")
            Result.success(ventaId)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en procesarVenta", e)
            Result.failure(e)
        }
    }

    // 🆕 Función mejorada: Guarda venta pendiente CON información del PDF
    private suspend fun guardarVentaPendienteSinSincronizar(
        items: List<ItemCarrito>,
        total: Double,
        metodoPago: String,
        clienteNombre: String?,
        clienteDocumento: String?,
        clienteEmail: String? = null,  // 🆕 Parámetro nuevo
        numeroVenta: String,            // 🆕 Parámetro nuevo
        pdfRutaLocal: String? = null    // 🆕 Parámetro nuevo
    ) {
        try {
            Log.d(TAG, "🟢 Guardando venta pendiente (sin sincronizar)")

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
                clienteEmail = clienteEmail,      // 🆕
                numeroVenta = numeroVenta,         // 🆕
                productosJson = gson.toJson(productosDto),
                sincronizado = false,
                pdfRutaLocal = pdfRutaLocal,      // 🆕
                pdfSubido = false,                 // 🆕
                emailEnviado = false               // 🆕
            )

            val ventaId = ventaPendienteDao.insert(ventaPendiente)
            Log.d(TAG, "✅ Venta pendiente guardada con ID: $ventaId")
            Log.d(TAG, "📄 PDF local: $pdfRutaLocal")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error guardando venta pendiente", e)
        }
    }


    // 🆕 Función para intentar sincronizar CON timeout
    suspend fun intentarSincronizarVenta(ventaId: Long): Result<String> {
        return withTimeoutOrNull(5000) { // 5 segundos máximo
            try {
                sincronizarVenta(ventaId)
            } catch (e: Exception) {
                Log.d(TAG, "⚠️ Error al sincronizar venta $ventaId: ${e.message}")
                Result.failure(e)
            }
        } ?: run {
            Log.d(TAG, "⏱️ Timeout al sincronizar venta $ventaId")
            Result.failure(Exception("Timeout en sincronización"))
        }
    }

    suspend fun sincronizarVenta(ventaId: Long): Result<String> {
        return try {
            Log.d(TAG, "🟢 Sincronizando venta $ventaId con Firebase")

            val venta = ventaPendienteDao.getById(ventaId)
                ?: return Result.failure(Exception("Venta no encontrada"))

            if (venta.sincronizado) {
                Log.d(TAG, "✅ Venta ya sincronizada: ${venta.firebaseId}")
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
            Log.d(TAG, "🟢 Sincronizando ventas pendientes")

            val ventasPendientes = ventaPendienteDao.getVentasPendientes()
            var sincronizadas = 0

            ventasPendientes.forEach { venta ->
                if (venta.intentosSincronizacion < MAX_REINTENTOS) {
                    val resultado = intentarSincronizarVenta(venta.id)
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

    // 🆕 Actualizar la ruta del PDF en la venta pendiente
    suspend fun actualizarPdfRutaLocal(ventaId: Long, pdfRutaLocal: String) {
        try {
            Log.d(TAG, "🟢 Actualizando PDF ruta local para venta $ventaId")

            val ventaPendiente = ventaPendienteDao.getAll()
                .firstOrNull { it.id == ventaId }

            if (ventaPendiente != null) {
                val actualizada = ventaPendiente.copy(
                    pdfRutaLocal = pdfRutaLocal
                )
                ventaPendienteDao.update(actualizada)
                Log.d(TAG, "✅ PDF ruta actualizada: $pdfRutaLocal")
            } else {
                Log.e(TAG, "❌ No se encontró venta pendiente con ID: $ventaId")
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error actualizando PDF ruta", e)
        }
    }

    // 🆕 Marcar PDF como subido exitosamente
    suspend fun marcarPdfComoSubido(ventaId: Long, pdfUrl: String) {
        try {
            Log.d(TAG, "🟢 Marcando PDF como subido para venta $ventaId")

            val ventaPendiente = ventaPendienteDao.getAll()
                .firstOrNull { it.id == ventaId }

            if (ventaPendiente != null) {
                val actualizada = ventaPendiente.copy(
                    pdfSubido = true,
                    pdfUrlStorage = pdfUrl
                )
                ventaPendienteDao.update(actualizada)
                Log.d(TAG, "✅ PDF marcado como subido: $pdfUrl")
            } else {
                Log.e(TAG, "❌ No se encontró venta pendiente con ID: $ventaId")
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error marcando PDF como subido", e)
        }
    }

    // 🆕 Obtener ventas con PDFs pendientes de subir
    suspend fun getVentasConPdfsPendientes(): List<VentaPendienteEntity> {
        return try {
            ventaPendienteDao.getVentasConPdfsPendientes()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error obteniendo ventas con PDFs pendientes", e)
            emptyList()
        }
    }
}
