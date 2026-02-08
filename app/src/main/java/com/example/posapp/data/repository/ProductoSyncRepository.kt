package com.example.posapp.data.repository

import android.util.Log
import com.example.posapp.data.local.dao.CategoriaDao
import com.example.posapp.data.local.dao.ProductoDao
import com.example.posapp.data.local.entities.CategoriaEntity
import com.example.posapp.data.local.entities.ProductoEntity
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductoSyncRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val productoDao: ProductoDao,
    private val categoriaDao: CategoriaDao
) {
    private val TAG = "ProductoSync"

    suspend fun syncProductos(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔄 Iniciando sincronización...")

            // Obtener productos de Firebase
            val productosSnapshot = firestore
                .collection("productos")
                .get()
                .await()

            Log.d(TAG, "📊 Productos obtenidos: ${productosSnapshot.size()}")

            if (productosSnapshot.isEmpty) {
                Log.w(TAG, "⚠️ No hay productos en Firebase")
                return@withContext Result.success(Unit)
            }

            // Extraer categorías únicas
            val categoriasUnicas = productosSnapshot.documents
                .mapNotNull { it.getString("categoria") }
                .distinct()

            Log.d(TAG, "📊 Categorías únicas: ${categoriasUnicas.size}")

            // Guardar categorías
            val categorias = categoriasUnicas.mapIndexed { index, nombreCategoria ->
                CategoriaEntity(
                    id = (index + 1).toLong(),
                    nombre = nombreCategoria,
                    firebaseId = nombreCategoria.hashCode().toString()
                )
            }
            categoriaDao.insertAll(categorias)

            // Guardar productos
            val productos = productosSnapshot.documents.mapNotNull { doc ->
                try {
                    val categoriaStr = doc.getString("categoria") ?: ""
                    val categoriaIndex = categoriasUnicas.indexOf(categoriaStr)

                    // 🔥 MANEJO FLEXIBLE DE fechaCreacion
                    val fechaCreacion = when (val fecha = doc.get("fechaCreacion")) {
                        is Long -> fecha
                        is Timestamp -> fecha.toDate().time
                        is String -> fecha.toLongOrNull() ?: System.currentTimeMillis()
                        else -> System.currentTimeMillis()
                    }

                    ProductoEntity(
                        id = 0L,
                        codigo = doc.getString("codigo") ?: "",
                        nombre = doc.getString("nombre") ?: "",
                        descripcion = doc.getString("descripcion") ?: "",
                        marca = doc.getString("marca") ?: "",
                        modelo = doc.getString("modelo") ?: "",
                        precio = doc.getDouble("precio") ?: 0.0,
                        stock = doc.getLong("stock")?.toInt() ?: 0,
                        stockMinimo = doc.getLong("stockMinimo")?.toInt() ?: 5,
                        categoriaId = (categoriaIndex + 1).toLong(),
                        imagenUrl = doc.getString("imagenUrl"),
                        ubicacion = doc.getString("ubicacion"),
                        activo = doc.getBoolean("activo") ?: true,
                        fechaCreacion = fechaCreacion,
                        firebaseId = doc.id,
                        sincronizado = true,
                        ultimaSincronizacion = System.currentTimeMillis()
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error procesando producto: ${doc.id}", e)
                    null
                }
            }

            productoDao.insertAll(productos)
            Log.d(TAG, "✅ Sincronización completada: ${productos.size} productos")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en sincronización", e)
            Result.failure(e)
        }
    }
}
