package com.example.posapp.data.repository

import android.util.Log
import com.example.posapp.data.local.dao.ProductoDao
import com.example.posapp.data.local.entities.ProductoEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductoSyncRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val productoDao: ProductoDao
) {
    private val TAG = "ProductoSync"

    /**
     * Sincroniza productos desde Firestore a Room
     * Se ejecuta solo al iniciar sesión
     */
    suspend fun syncProductos(): Result<Int> {
        return try {
            Log.d(TAG, "🔄 Iniciando sincronización...")
            Log.d(TAG, "📡 Conectando a Firestore...")

            // 1️⃣ Descargar de Firestore
            val snapshot = firestore.collection("productos")
                .get()
                .await()

            Log.d(TAG, "📦 Documentos recibidos: ${snapshot.size()}")

            if (snapshot.isEmpty) {
                Log.w(TAG, "⚠️ No hay productos en Firestore")
                return Result.success(0)
            }

            // 2️⃣ Convertir a ProductoEntity
            val productos = snapshot.documents.mapNotNull { doc ->
                try {
                    Log.d(TAG, "📝 Procesando: ${doc.id}")
                    ProductoEntity(
                        id = 0,
                        codigo = doc.getString("codigo") ?: return@mapNotNull null,
                        nombre = doc.getString("nombre") ?: return@mapNotNull null,
                        descripcion = doc.getString("descripcion") ?: "",
                        marca = doc.getString("marca") ?: "",
                        modelo = doc.getString("modelo") ?: "",
                        precio = doc.getDouble("precio") ?: 0.0,
                        stock = doc.getLong("stock")?.toInt() ?: 0,
                        stockMinimo = doc.getLong("stockMinimo")?.toInt() ?: 5,
                        categoriaId = doc.getLong("categoriaId") ?: 1,
                        imagenUrl = doc.getString("imagenUrl"),
                        ubicacion = doc.getString("ubicacion"),
                        activo = doc.getBoolean("activo") ?: true,
                        fechaCreacion = (doc.get("fechaCreacion") as? Timestamp)?.toDate()?.time
                            ?: System.currentTimeMillis(),
                        firebaseId = doc.id,
                        sincronizado = true,
                        ultimaSincronizacion = System.currentTimeMillis()
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error parseando ${doc.id}: ${e.message}")
                    null
                }
            }

            Log.d(TAG, "✅ Productos válidos: ${productos.size}")

            // 3️⃣ Reemplazar todo en Room
            Log.d(TAG, "🗑️ Limpiando base de datos local...")
            productoDao.deleteAll()

            Log.d(TAG, "💾 Guardando ${productos.size} productos...")
            productoDao.insertAll(productos)

            Log.d(TAG, "✅ ${productos.size} productos sincronizados correctamente")
            Result.success(productos.size)

        } catch (e: Exception) {
            Log.e(TAG, "❌ ERROR CRÍTICO: ${e.message}", e)
            Log.e(TAG, "Stack trace: ${e.stackTraceToString()}")
            Result.failure(e)
        }
    }
}
