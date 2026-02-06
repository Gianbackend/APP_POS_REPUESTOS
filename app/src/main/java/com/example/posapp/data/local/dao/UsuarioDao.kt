package com.example.posapp.data.local.dao

import androidx.room.*
import com.example.posapp.data.local.entities.UsuarioEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UsuarioDao {

    @Query("SELECT * FROM usuarios WHERE email = :email AND activo = 1 LIMIT 1")
    suspend fun getByEmail(email: String): UsuarioEntity?

    @Query("SELECT * FROM usuarios WHERE id = :id")
    suspend fun getById(id: String): UsuarioEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(usuario: UsuarioEntity): Long

    @Query("SELECT * FROM usuarios WHERE activo = 1")
    fun getAllActive(): Flow<List<UsuarioEntity>>

    @Update
    suspend fun update(usuario: UsuarioEntity)
}