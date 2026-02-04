package com.example.posapp.data.local.dao

import androidx.room.*
import com.example.posapp.data.local.entities.ProductoEntity
import com.example.posapp.data.local.entities.ProductoWithCategoria
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductoDao {

    // ========== QUERIES BÁSICAS (sin relaciones) ==========

    @Query("""
        SELECT * FROM productos 
        WHERE activo = 1 
        ORDER BY nombre ASC
    """)
    fun getAllActive(): Flow<List<ProductoEntity>>

    @Query("""
        SELECT * FROM productos 
        WHERE categoriaId = :categoriaId AND activo = 1 
        ORDER BY nombre ASC
    """)
    fun getByCategoria(categoriaId: Long): Flow<List<ProductoEntity>>

    @Query("""
        SELECT * FROM productos 
        WHERE (nombre LIKE '%' || :query || '%' 
        OR codigo LIKE '%' || :query || '%'
        OR marca LIKE '%' || :query || '%'
        OR modelo LIKE '%' || :query || '%')
        AND activo = 1
        ORDER BY nombre ASC
    """)
    fun buscar(query: String): Flow<List<ProductoEntity>>

    @Query("SELECT * FROM productos WHERE codigo = :codigo AND activo = 1 LIMIT 1")
    suspend fun getByCodigo(codigo: String): ProductoEntity?

    @Query("SELECT * FROM productos WHERE id = :id")
    suspend fun getById(id: Long): ProductoEntity?

    @Query("SELECT * FROM productos WHERE stock <= stockMinimo AND activo = 1")
    fun getStockBajo(): Flow<List<ProductoEntity>>

    @Query("""
        SELECT * FROM productos 
        WHERE activo = 1 
        AND categoriaId = :categoriaId 
        AND stock <= stockMinimo 
        ORDER BY nombre ASC
    """)
    fun getStockBajoPorCategoria(categoriaId: Long): Flow<List<ProductoEntity>>

    // ========== QUERIES CON RELACIONES ==========

    @Transaction
    @Query("""
        SELECT * FROM productos 
        ORDER BY nombre ASC
    """)
    fun getAllProductosWithCategoria(): Flow<List<ProductoWithCategoria>>

    @Transaction
    @Query("""
        SELECT * FROM productos 
        WHERE id = :id
    """)
    fun getProductoByIdWithCategoria(id: Long): Flow<ProductoWithCategoria?>

    @Transaction
    @Query("""
        SELECT * FROM productos 
        WHERE categoriaId = :categoriaId 
        ORDER BY nombre ASC
    """)
    fun getProductosByCategoriaWithCategoria(categoriaId: Long): Flow<List<ProductoWithCategoria>>

    @Transaction
    @Query("""
        SELECT * FROM productos 
        WHERE nombre LIKE '%' || :query || '%' 
           OR codigo LIKE '%' || :query || '%'
        ORDER BY nombre ASC
    """)
    fun searchProductosWithCategoria(query: String): Flow<List<ProductoWithCategoria>>

    @Transaction
    @Query("""
        SELECT * FROM productos 
        WHERE stock <= stockMinimo 
        ORDER BY stock ASC
    """)
    fun getProductosBajoStockWithCategoria(): Flow<List<ProductoWithCategoria>>

    // ========== SINCRONIZACIÓN ==========

    @Query("SELECT * FROM productos WHERE sincronizado = 0")
    suspend fun getProductosNoSincronizados(): List<ProductoEntity>

    @Query("SELECT * FROM productos WHERE firebaseId = :firebaseId LIMIT 1")
    suspend fun getProductoByFirebaseId(firebaseId: String): ProductoEntity?

    @Query("SELECT * FROM productos WHERE id = :id LIMIT 1")
    suspend fun getProductoById(id: Long): ProductoEntity?

    // ========== OPERACIONES CRUD (UNA SOLA VEZ) ==========

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(producto: ProductoEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(productos: List<ProductoEntity>)

    @Update
    suspend fun update(producto: ProductoEntity)

    @Delete
    suspend fun delete(producto: ProductoEntity)

    // ========== OPERACIONES DE STOCK ==========

    @Query("UPDATE productos SET stock = :nuevoStock WHERE id = :productoId")
    suspend fun updateStock(productoId: Long, nuevoStock: Int)

    @Query("UPDATE productos SET stock = stock - :cantidad WHERE id = :id")
    suspend fun reducirStock(id: Long, cantidad: Int)

    @Query("UPDATE productos SET stock = stock + :cantidad WHERE id = :id")
    suspend fun aumentarStock(id: Long, cantidad: Int)

    @Query("DELETE FROM productos")
    suspend fun deleteAll()
}
