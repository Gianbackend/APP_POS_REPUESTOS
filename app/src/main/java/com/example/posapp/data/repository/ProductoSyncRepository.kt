package com.example.posapp.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.example.posapp.data.local.dao.ProductoDao
import com.example.posapp.data.local.entities.ProductoEntity
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
    private val TAG = "ProductoSync"
    private var listener: ListenerRegistration? = null //r

    /**
     * Sincronización inicial: descarga todos los productos de Firestore //
     */
    suspend fun syncInitial(): Result<Int> {
        return try {
            val snapshot = firestore.collection("productos")
                .get()
                .await()

            val productos = snapshot.documents.mapNotNull { doc ->
                try {
                    ProductoEntity(
                        id = 0L, // AutoGenerate
                        codigo = doc.getString("codigo") ?: "",
                        nombre = doc.getString("nombre") ?: "",
                        descripcion = doc.getString("descripcion") ?: "",
                        marca = doc.getString("marca") ?: "",
                        modelo = doc.getString("modelo") ?: "",
                        precio = doc.getDouble("precio") ?: 0.0,
                        stock = doc.getLong("stock")?.toInt() ?: 0,
                        stockMinimo = doc.getLong("stockMinimo")?.toInt() ?: 0,
                        categoriaId = doc.getLong("categoriaId") ?: 0L,
                        ubicacion = doc.getString("ubicacion") ?: "",
                        imagenUrl = doc.getString("imagenUrl"),
                        activo = doc.getBoolean("activo") ?: true,
                        fechaCreacion = doc.getLong("fechaCreacion") ?: System.currentTimeMillis()
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parseando producto ${doc.id}", e)
                    null
                }
            }

            // Guardar en local
            productoDao.insertAll(productos)

            Log.d(TAG, "Sincronizados ${productos.size} productos")
            Result.success(productos.size)

        } catch (e: Exception) {
            Log.e(TAG, "Error en sync inicial", e)
            Result.failure(e)
        }
    }

    /**
     * Escuchar cambios en tiempo real
     */
    fun startRealtimeSync(onError: (Exception) -> Unit = {}) {
        listener = firestore.collection("productos")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error en listener", error)
                    onError(error)
                    return@addSnapshotListener
                }

                snapshot?.documentChanges?.forEach { change ->
                    val doc = change.document

                    try {
                        val producto = ProductoEntity(
                            id = 0L,
                            codigo = doc.getString("codigo") ?: "",
                            nombre = doc.getString("nombre") ?: "",
                            descripcion = doc.getString("descripcion") ?: "",
                            marca = doc.getString("marca") ?: "",
                            modelo = doc.getString("modelo") ?: "",
                            precio = doc.getDouble("precio") ?: 0.0,
                            stock = doc.getLong("stock")?.toInt() ?: 0,
                            stockMinimo = doc.getLong("stockMinimo")?.toInt() ?: 0,
                            categoriaId = doc.getLong("categoriaId") ?: 0L,
                            ubicacion = doc.getString("ubicacion") ?: "",
                            imagenUrl = doc.getString("imagenUrl"),
                            activo = doc.getBoolean("activo") ?: true,
                            fechaCreacion = doc.getLong("fechaCreacion") ?: System.currentTimeMillis()
                        )

                        when (change.type) {
                            com.google.firebase.firestore.DocumentChange.Type.ADDED,
                            com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                                CoroutineScope(Dispatchers.IO).launch {
                                    productoDao.insert(producto)
                                    Log.d(TAG, "Producto actualizado: ${producto.nombre}")
                                }
                            }
                            com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                                CoroutineScope(Dispatchers.IO).launch {
                                    productoDao.delete(producto)
                                    Log.d(TAG, "Producto eliminado: ${producto.nombre}")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error procesando cambio", e)
                    }
                }
            }
    }

    /**
     * Detener sincronización en tiempo real
     */
    fun stopRealtimeSync() {
        listener?.remove()
        listener = null
        Log.d(TAG, "Listener detenido")
    }

    /**
     * Forzar sincronización manual
     */
    suspend fun forceSync(): Result<String> {
        return try {
            val result = syncInitial()
            if (result.isSuccess) {
                Result.success("Sincronización completada: ${result.getOrNull()} productos")
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Error desconocido"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}