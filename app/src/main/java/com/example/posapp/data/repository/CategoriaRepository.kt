package com.example.posapp.data.repository

import com.example.posapp.data.local.dao.CategoriaDao
import com.example.posapp.domain.model.Categoria
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoriaRepository @Inject constructor(
    private val categoriaDao: CategoriaDao
) {

    // Obtener todas las categorías
    fun getAllCategorias(): Flow<List<Categoria>> {
        return categoriaDao.getAll()
            .map { entities ->
                // Convertir entities a models
                entities.map { it.toDomain() }
            }
    }
}