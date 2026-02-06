package com.example.posapp.data.repository

import android.util.Log
import com.example.posapp.data.local.dao.ProductoDao
import com.example.posapp.data.local.entities.ProductoEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.Timestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductoSyncRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val productoDao: ProductoDao
) {
    private var realtimeListener: ListenerRegistration? = null
    private val TAG = "ProductoSync"

    /**
     * Sincronización inicial: Trae todos los productos de Firestore
     */
    suspend fun syncInitial(): Result<Int> {
        return try {
            val snapshot = firestore.collection("productos").get().await()

            val productos = snapshot.documents.mapNotNull { doc ->
                try {
                    ProductoEntity(
                        id = doc.id.toLongOrNull() ?: 0L,
                        codigo = doc.getString("codigo") ?: "",
                        nombre = doc.getString("nombre") ?: "",
                        descripcion = doc.getString("descripcion") ?: "",
                        marca = doc.getString("marca") ?: "",
                        modelo = doc.getString("modelo") ?: "",
                        precio = doc.getDouble("precio") ?: 0.0,
                        stock = doc.getLong("stock")?.toInt() ?: 0,
                        stockMinimo = doc.getLong("stockMinimo")?.toInt() ?: 5,
                        categoriaId = doc.getLong("categoriaId") ?: 0L,
                        ubicacion = doc.getString("ubicacion"),
                        imagenUrl = doc.getString("imagenUrl"),
                        activo = doc.getBoolean("activo") ?: true,
                        fechaCreacion = (doc.get("fechaCreacion") as? Timestamp)?.toDate()?.time ?: System.currentTimeMillis(),
                        firebaseId = doc.id,
                        sincronizado = true,
                        ultimaSincronizacion = System.currentTimeMillis()
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parseando producto ${doc.id}: ${e.message}")
                    null
                }
            }

            if (productos.isNotEmpty()) {
                productoDao.insertAll(productos)
                Log.d(TAG, "✅ Sincronizados ${productos.size} productos")
            }

            Result.success(productos.size)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en syncInitial: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Listener en tiempo real para cambios en Firestore
     */
    fun startRealtimeSync(onError: (Exception) -> Unit) {
        realtimeListener = firestore.collection("productos")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "❌ Error en listener: ${error.message}")
                    onError(error)
                    return@addSnapshotListener
                }

                snapshot?.documentChanges?.forEach { change ->
                    try {
                        val doc = change.document
                        val producto = ProductoEntity(
                            id = doc.id.toLongOrNull() ?: 0L,
                            codigo = doc.getString("codigo") ?: "",
                            nombre = doc.getString("nombre") ?: "",
                            descripcion = doc.getString("descripcion") ?: "",
                            marca = doc.getString("marca") ?: "",
                            modelo = doc.getString("modelo") ?: "",
                            precio = doc.getDouble("precio") ?: 0.0,
                            stock = doc.getLong("stock")?.toInt() ?: 0,
                            stockMinimo = doc.getLong("stockMinimo")?.toInt() ?: 5,
                            categoriaId = doc.getLong("categoriaId") ?: 0L,
                            ubicacion = doc.getString("ubicacion"),
                            imagenUrl = doc.getString("imagenUrl"),
                            activo = doc.getBoolean("activo") ?: true,
                            fechaCreacion = (doc.get("fechaCreacion") as? Timestamp)?.toDate()?.time ?: System.currentTimeMillis(),
                            firebaseId = doc.id,
                            sincronizado = true,
                            ultimaSincronizacion = System.currentTimeMillis()
                        )

                        when (change.type) {
                            com.google.firebase.firestore.DocumentChange.Type.ADDED,
                            com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                                CoroutineScope(Dispatchers.IO).launch {
                                    productoDao.insert(producto)
                                    Log.d(TAG, "✅ ${producto.nombre} → $${producto.precio}")
                                }
                            }
                            com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                                CoroutineScope(Dispatchers.IO).launch {
                                    productoDao.delete(producto)
                                    Log.d(TAG, "🗑️ Eliminado: ${producto.nombre}")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Error procesando cambio: ${e.message}", e)
                    }
                }
            }
    }

    fun stopRealtimeSync() {
        realtimeListener?.remove()
        realtimeListener = null
        Log.d(TAG, "🛑 Listener detenido")
    }
}