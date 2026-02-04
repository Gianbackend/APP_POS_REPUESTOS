package com.example.posapp.data.repository

import com.example.posapp.data.local.dao.ProductoDao
import com.example.posapp.data.local.entities.ProductoEntity
import com.example.posapp.domain.model.Producto
import com.example.posapp.domain.repository.ProductoRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductoRepositoryImpl @Inject constructor(
    private val productoDao: ProductoDao,
    private val firestore: FirebaseFirestore
) : ProductoRepository {

    private val productosCollection = firestore.collection("productos")

    // ========== OPERACIONES LOCALES (Room) ==========

    override fun getAllProductos(): Flow<List<Producto>> {
        return productoDao.getAllProductosWithCategoria()
            .map { list -> list.map { it.producto.toDomain(it.categoria.nombre) } }
    }

    override fun getProductoById(id: Long): Flow<Producto?> {
        return productoDao.getProductoByIdWithCategoria(id)
            .map { it?.producto?.toDomain(it.categoria.nombre) }
    }

    override fun getProductosByCategoriaId(categoriaId: Long): Flow<List<Producto>> {
        return productoDao.getProductosByCategoriaWithCategoria(categoriaId)
            .map { list -> list.map { it.producto.toDomain(it.categoria.nombre) } }
    }

    override fun searchProductos(query: String): Flow<List<Producto>> {
        return productoDao.searchProductosWithCategoria(query)
            .map { list -> list.map { it.producto.toDomain(it.categoria.nombre) } }
    }

    override fun getProductosBajoStock(): Flow<List<Producto>> {
        return productoDao.getProductosBajoStockWithCategoria()
            .map { list -> list.map { it.producto.toDomain(it.categoria.nombre) } }
    }

    // ========== OPERACIONES DE ESCRITURA (Room + Firebase) ==========

    override suspend fun insertProducto(producto: Producto): Long {
        // 1. Insertar en Room
        val entity = producto.toEntity()
        val localId = productoDao.insert(entity)

        // 2. Sincronizar con Firebase
        try {
            val docRef = productosCollection.document()
            val productoConId = entity.copy(
                id = localId,
                firebaseId = docRef.id,
                sincronizado = true,
                ultimaSincronizacion = System.currentTimeMillis()
            )

            docRef.set(productoConId.toFirebase()).await()

            // 3. Actualizar Room con firebaseId
            productoDao.update(productoConId)

        } catch (e: Exception) {
            // Si falla Firebase, marcar como no sincronizado
            productoDao.update(
                entity.copy(
                    id = localId,
                    sincronizado = false
                )
            )
        }

        return localId
    }

    override suspend fun updateProducto(producto: Producto) {
        val entity = producto.toEntity()

        // 1. Actualizar en Room
        productoDao.update(entity)

        // 2. Sincronizar con Firebase
        try {
            if (entity.firebaseId != null) {
                productosCollection
                    .document(entity.firebaseId)
                    .set(entity.toFirebase())
                    .await()

                // Marcar como sincronizado
                productoDao.update(
                    entity.copy(
                        sincronizado = true,
                        ultimaSincronizacion = System.currentTimeMillis()
                    )
                )
            }
        } catch (e: Exception) {
            // Marcar como no sincronizado
            productoDao.update(entity.copy(sincronizado = false))
        }
    }

    override suspend fun deleteProducto(producto: Producto) {
        val entity = producto.toEntity()

        // 1. Eliminar de Firebase primero
        try {
            if (entity.firebaseId != null) {
                productosCollection
                    .document(entity.firebaseId)
                    .delete()
                    .await()
            }
        } catch (e: Exception) {
            // Continuar aunque falle Firebase
        }

        // 2. Eliminar de Room
        productoDao.delete(entity)
    }

    override suspend fun updateStock(productoId: Long, nuevoStock: Int) {
        productoDao.updateStock(productoId, nuevoStock)

        // Sincronizar con Firebase
        val producto = productoDao.getProductoById(productoId)
        if (producto?.firebaseId != null) {
            try {
                productosCollection
                    .document(producto.firebaseId)
                    .update("stock", nuevoStock)
                    .await()
            } catch (e: Exception) {
                // Marcar como no sincronizado
                productoDao.update(producto.copy(sincronizado = false))
            }
        }
    }

    // ========== SINCRONIZACIÓN ==========

    suspend fun sincronizarPendientes() {
        val pendientes = productoDao.getProductosNoSincronizados()

        pendientes.forEach { producto ->
            try {
                if (producto.firebaseId == null) {
                    // Crear en Firebase
                    val docRef = productosCollection.document()
                    docRef.set(producto.toFirebase()).await()

                    productoDao.update(
                        producto.copy(
                            firebaseId = docRef.id,
                            sincronizado = true,
                            ultimaSincronizacion = System.currentTimeMillis()
                        )
                    )
                } else {
                    // Actualizar en Firebase
                    productosCollection
                        .document(producto.firebaseId)
                        .set(producto.toFirebase())
                        .await()

                    productoDao.update(
                        producto.copy(
                            sincronizado = true,
                            ultimaSincronizacion = System.currentTimeMillis()
                        )
                    )
                }
            } catch (e: Exception) {
                // Continuar con el siguiente
            }
        }
    }

    suspend fun descargarDesdeFirebase() {
        try {
            val snapshot = productosCollection.get().await()

            snapshot.documents.forEach { doc ->
                val firebaseId = doc.id
                val data = doc.data ?: return@forEach

                // Verificar si ya existe localmente
                val existente = productoDao.getProductoByFirebaseId(firebaseId)

                if (existente == null) {
                    // Insertar nuevo producto
                    val producto = ProductoEntity(
                        codigo = data["codigo"] as String,
                        nombre = data["nombre"] as String,
                        descripcion = data["descripcion"] as String,
                        marca = data["marca"] as String,
                        modelo = data["modelo"] as String,
                        precio = (data["precio"] as? Number)?.toDouble() ?: 0.0,
                        stock = (data["stock"] as? Number)?.toInt() ?: 0,
                        stockMinimo = (data["stockMinimo"] as? Number)?.toInt() ?: 5,
                        categoriaId = (data["categoriaId"] as? Number)?.toLong() ?: 1L,
                        imagenUrl = data["imagenUrl"] as? String,
                        ubicacion = data["ubicacion"] as? String,
                        activo = data["activo"] as? Boolean ?: true,
                        fechaCreacion = (data["fechaCreacion"] as? Number)?.toLong()
                            ?: System.currentTimeMillis(),
                        firebaseId = firebaseId,
                        sincronizado = true,
                        ultimaSincronizacion = System.currentTimeMillis()
                    )

                    productoDao.insert(producto)
                }
            }
        } catch (e: Exception) {
            throw e
        }
    }
}

// Extension function
private fun Producto.toEntity() = ProductoEntity(
    id = id,
    codigo = codigo,
    nombre = nombre,
    descripcion = descripcion,
    marca = marca,
    modelo = modelo,
    precio = precio,
    stock = stock,
    stockMinimo = stockMinimo,
    categoriaId = categoriaId,
    imagenUrl = imagenUrl,
    ubicacion = ubicacion,
    activo = activo
)
