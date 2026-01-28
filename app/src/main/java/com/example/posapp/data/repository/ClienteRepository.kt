package com.example.posapp.data.repository

import com.example.posapp.data.local.dao.ClienteDao
import com.example.posapp.data.local.entities.ClienteEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClienteRepository @Inject constructor(
    private val clienteDao: ClienteDao
) {
    suspend fun getClienteById(id: Long): ClienteEntity? {
        return clienteDao.getById(id)
    }
}