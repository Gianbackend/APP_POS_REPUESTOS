package com.example.posapp.data.repository

import android.util.Log
import com.example.posapp.data.local.dao.CategoriaDao
import com.example.posapp.data.local.entities.CategoriaEntity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoriaSyncRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val categoriaDao: CategoriaDao
) {
    private val TAG = "CategoriaSync"

    suspend fun syncCategorias(): Result<Int> {
        return try {
            Log.d(TAG, "🔄 Sincronizando categorías desde Firestore...")

            // 1️⃣ Descargar de Firestore
            val snapshot = firestore.collection("categorias")
                .get()
                .await()

            Log.d(TAG, "📦 Categorías recibidas: ${snapshot.size()}")

            if (snapshot.isEmpty) {
                Log.w(TAG, "⚠️ No hay categorías en Firestore, creando categoría por defecto...")

                // Crear categoría por defecto
                val categoriaDefault = CategoriaEntity(
                    id = 1,
                    nombre = "General",
                    descripcion = "Categoría general"
                )
                categoriaDao.insert(categoriaDefault)

                return Result.success(1)
            }

            // 2️⃣ Convertir a CategoriaEntity
            val categorias = snapshot.documents.mapNotNull { doc ->
                try {
                    CategoriaEntity(
                        id = doc.getLong("id") ?: return@mapNotNull null,
                        nombre = doc.getString("nombre") ?: return@mapNotNull null,
                        descripcion = doc.getString("descripcion")
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error parseando ${doc.id}: ${e.message}")
                    null
                }
            }

            Log.d(TAG, "✅ Categorías válidas: ${categorias.size}")

            // 3️⃣ Guardar en Room
            categoriaDao.deleteAll()
            categoriaDao.insertAll(categorias)

            Log.d(TAG, "✅ ${categorias.size} categorías sincronizadas")
            Result.success(categorias.size)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error: ${e.message}", e)
            Result.failure(e)
        }
    }
}
