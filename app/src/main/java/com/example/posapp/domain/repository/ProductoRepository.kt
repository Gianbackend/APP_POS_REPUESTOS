package com.example.posapp.domain.repository

import com.example.posapp.domain.model.Producto
import kotlinx.coroutines.flow.Flow

interface ProductoRepository {

    // ========== LECTURA ==========

    fun getAllProductos(): Flow<List<Producto>>

    fun getProductoById(id: Long): Flow<Producto?>

    fun getProductosByCategoriaId(categoriaId: Long): Flow<List<Producto>>

    fun searchProductos(query: String): Flow<List<Producto>>

    fun getProductosBajoStock(): Flow<List<Producto>>

    // ========== ESCRITURA ==========

    suspend fun insertProducto(producto: Producto): Long

    suspend fun updateProducto(producto: Producto)

    suspend fun deleteProducto(producto: Producto)

    suspend fun updateStock(productoId: Long, nuevoStock: Int)
}
