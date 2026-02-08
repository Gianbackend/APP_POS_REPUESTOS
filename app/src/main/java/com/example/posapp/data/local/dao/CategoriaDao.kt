package com.example.posapp.data.local.dao

import androidx.room.*
import com.example.posapp.data.local.entities.CategoriaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoriaDao {

    @Query("SELECT * FROM categorias ORDER BY nombre ASC")
    fun getAll(): Flow<List<CategoriaEntity>>

    @Query("SELECT * FROM categorias WHERE id = :id")
    suspend fun getById(id: Long): CategoriaEntity?

    @Query("SELECT * FROM categorias WHERE firebaseId = :firebaseId")
    suspend fun getByFirebaseId(firebaseId: String): CategoriaEntity? // ✅ Agregado tipo de retorno

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(categoria: CategoriaEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categorias: List<CategoriaEntity>)

    @Update
    suspend fun update(categoria: CategoriaEntity)

    @Delete
    suspend fun delete(categoria: CategoriaEntity)

    @Query("DELETE FROM categorias")
    suspend fun deleteAll()
}
