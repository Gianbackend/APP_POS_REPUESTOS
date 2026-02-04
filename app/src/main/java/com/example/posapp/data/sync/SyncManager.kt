package com.example.posapp.data.sync

import android.util.Log
import com.example.posapp.data.repository.VentaRepository
import com.example.posapp.util.NetworkUtils  // ← Cambiado aquí
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    private val ventaRepository: VentaRepository,
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

                val resultado = ventaRepository.sincronizarVentasPendientes()

                resultado.fold(
                    onSuccess = { sincronizadas ->
                        Log.d(TAG, "✅ Sincronizadas $sincronizadas ventas exitosamente")
                    },
                    onFailure = { error ->
                        Log.e(TAG, "❌ Error sincronizando ventas: ${error.message}", error)
                    }
                )

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
                ventaRepository.sincronizarVentasPendientes()
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
