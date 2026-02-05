package com.example.posapp.data.sync

import android.util.Log
import com.example.posapp.data.firebase.FirebaseStorageManager
import com.example.posapp.data.repository.VentaRepository
import com.example.posapp.util.NetworkUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    private val ventaRepository: VentaRepository,
    private val firebaseStorageManager: FirebaseStorageManager,
    private val networkUtils: NetworkUtils
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "SyncManager"
    }

    fun sincronizarAlIniciar() {
        scope.launch {
            try {
                if (!networkUtils.isInternetAvailable()) {
                    Log.d(TAG, "⚠️ Sin conexión a internet, sincronización pospuesta")
                    return@launch
                }

                val cantidadPendientes = ventaRepository.contarVentasPendientes()

                if (cantidadPendientes == 0) {
                    Log.d(TAG, "✅ No hay ventas pendientes para sincronizar")
                    return@launch
                }

                Log.d(TAG, "🔄 Iniciando sincronización de $cantidadPendientes ventas...")

                // 1️⃣ Sincronizar ventas con Firebase
                val resultado = ventaRepository.sincronizarVentasPendientes()

                resultado.fold(
                    onSuccess = { sincronizadas ->
                        Log.d(TAG, "✅ Sincronizadas $sincronizadas ventas exitosamente")
                    },
                    onFailure = { error ->
                        Log.e(TAG, "❌ Error sincronizando ventas: ${error.message}", error)
                    }
                )

                // 2️⃣ 🆕 Sincronizar PDFs pendientes
                sincronizarPdfsPendientes()

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error inesperado en sincronización", e)
            }
        }
    }

    suspend fun sincronizarAhora(): Result<Int> {
        return try {
            if (!networkUtils.isInternetAvailable()) {
                Result.failure(Exception("Sin conexión a internet"))
            } else {
                // 1️⃣ Sincronizar ventas
                val resultado = ventaRepository.sincronizarVentasPendientes()

                // 2️⃣ 🆕 Sincronizar PDFs
                sincronizarPdfsPendientes()

                resultado
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 🆕 Función para sincronizar PDFs pendientes
    private suspend fun sincronizarPdfsPendientes() {
        try {
            Log.d(TAG, "🔄 Buscando PDFs pendientes de subir")

            val ventasConPdfs = ventaRepository.getVentasConPdfsPendientes()

            if (ventasConPdfs.isEmpty()) {
                Log.d(TAG, "✅ No hay PDFs pendientes")
                return
            }

            Log.d(TAG, "📄 PDFs pendientes: ${ventasConPdfs.size}")

            ventasConPdfs.forEach { venta ->
                try {
                    val pdfPath = venta.pdfRutaLocal

                    if (pdfPath != null) {
                        val pdfFile = File(pdfPath)

                        if (pdfFile.exists()) {
                            Log.d(TAG, "📤 Subiendo PDF: ${venta.numeroVenta}")

                            val uploadResult = firebaseStorageManager.subirTicket(
                                file = pdfFile,
                                numeroVenta = venta.numeroVenta ?: "V-UNKNOWN",
                                clienteEmail = venta.clienteEmail ?: "",
                                total = venta.total,
                                fecha = SimpleDateFormat(
                                    "dd/MM/yyyy HH:mm",
                                    Locale.getDefault()
                                ).format(Date(venta.fecha))
                            )

                            if (uploadResult.isSuccess) {
                                val pdfUrl = uploadResult.getOrNull()
                                Log.d(TAG, "✅ PDF subido: $pdfUrl")
                                Log.d(TAG, "📧 Cloud Function enviará el email automáticamente")

                                // Marcar como subido
                                ventaRepository.marcarPdfComoSubido(venta.id, pdfUrl!!)
                            } else {
                                Log.e(TAG, "❌ Error subiendo PDF: ${uploadResult.exceptionOrNull()?.message}")
                            }
                        } else {
                            Log.e(TAG, "❌ PDF no existe: $pdfPath")
                        }
                    } else {
                        Log.d(TAG, "⚠️ Venta ${venta.id} no tiene PDF local")
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error subiendo PDF de venta ${venta.id}", e)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error sincronizando PDFs", e)
        }
    }
}
