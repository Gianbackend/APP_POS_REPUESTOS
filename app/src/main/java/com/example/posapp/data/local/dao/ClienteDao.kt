package com.example.posapp.data.local.dao

import androidx.room.*
import com.example.posapp.data.local.entities.ClienteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClienteDao {

    @Query("SELECT * FROM clientes ORDER BY nombre ASC")
    fun getAll(): Flow<List<ClienteEntity>>

    @Query("SELECT * FROM clientes WHERE nombre LIKE '%' || :query || '%' OR documento LIKE '%' || :query || '%'")
    fun buscar(query: String): Flow<List<ClienteEntity>>

    @Query("SELECT * FROM clientes WHERE documento = :documento LIMIT 1")
    suspend fun getByDocumento(documento: String): ClienteEntity?

    @Query("SELECT * FROM clientes WHERE id = :id")
    suspend fun getById(id: Long): ClienteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cliente: ClienteEntity): Long

    @Update
    suspend fun update(cliente: ClienteEntity)
}