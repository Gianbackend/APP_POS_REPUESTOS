package com.example.posapp.data.repository

import com.example.posapp.data.local.dao.ProductoDao
import com.example.posapp.data.local.dao.CategoriaDao
import com.example.posapp.domain.model.Producto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductoRepository @Inject constructor(
    private val productoDao: ProductoDao,      // Para acceder a productos
    private val categoriaDao: CategoriaDao     // Para obtener nombre de categoría
) {

    // Obtener todos los productos activos
    // Flow = datos reactivos (se actualizan automáticamente)
    fun getAllProductos(): Flow<List<Producto>> {
        return productoDao.getAllActive()
            .map { entities ->
                // Convertir cada ProductoEntity a Producto (model)
                entities.map { entity ->
                    // Buscar nombre de categoría
                    val categoria = categoriaDao.getById(entity.categoriaId)
                    entity.toDomain(categoria?.nombre ?: "Sin categoría")
                }
            }
    }

    // Buscar productos por texto (nombre, código, marca, modelo)
    fun buscarProductos(query: String): Flow<List<Producto>> {
        return productoDao.buscar(query)
            .map { entities ->
                entities.map { entity ->
                    val categoria = categoriaDao.getById(entity.categoriaId)
                    entity.toDomain(categoria?.nombre ?: "Sin categoría")
                }
            }
    }

    // Filtrar productos por categoría
    fun getProductosByCategoria(categoriaId: Long): Flow<List<Producto>> {
        return productoDao.getByCategoria(categoriaId)
            .map { entities ->
                entities.map { entity ->
                    val categoria = categoriaDao.getById(entity.categoriaId)
                    entity.toDomain(categoria?.nombre ?: "Sin categoría")
                }
            }
    }

    // Obtener productos con stock bajo (alerta)
    fun getProductosStockBajo(): Flow<List<Producto>> {
        return productoDao.getStockBajo()
            .map { entities ->
                entities.map { entity ->
                    val categoria = categoriaDao.getById(entity.categoriaId)
                    entity.toDomain(categoria?.nombre ?: "Sin categoría")
                }
            }
    }

    // Buscar producto por código de barras
    suspend fun getProductoByCodigo(codigo: String): Producto? {
        val entity = productoDao.getByCodigo(codigo)
        return if (entity != null) {
            val categoria = categoriaDao.getById(entity.categoriaId)
            entity.toDomain(categoria?.nombre ?: "Sin categoría")
        } else null
    }
    // Obtener un producto por ID
    suspend fun getProductoById(id: Long): Producto? {
        val entity = productoDao.getById(id)
        return if (entity != null) {
            val categoria = categoriaDao.getById(entity.categoriaId)
            entity.toDomain(categoria?.nombre ?: "Sin categoría")
        } else null
    }
}
