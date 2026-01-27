package com.example.posapp.data.local.dao

import androidx.room.*
import com.example.posapp.data.local.entities.ProductoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductoDao {

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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(producto: ProductoEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(productos: List<ProductoEntity>)

    @Update
    suspend fun update(producto: ProductoEntity)

    @Query("UPDATE productos SET stock = stock - :cantidad WHERE id = :id")
    suspend fun reducirStock(id: Long, cantidad: Int)

    @Query("UPDATE productos SET stock = stock + :cantidad WHERE id = :id")
    suspend fun aumentarStock(id: Long, cantidad: Int)
}