package com.example.posapp.data.di

import com.example.posapp.data.local.dao.*
import com.example.posapp.data.preferences.UserPreferences
import com.example.posapp.data.repository.ProductoRepositoryImpl
import com.example.posapp.data.repository.VentaRepository
import com.example.posapp.domain.repository.ProductoRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideProductoRepository(
        productoDao: ProductoDao,
        firestore: FirebaseFirestore
        // ❌ QUITAR: storage: FirebaseStorage
    ): ProductoRepository {
        // ✅ CORREGIDO: Solo 2 parámetros
        return ProductoRepositoryImpl(productoDao, firestore)
    }

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return Gson()
    }

    @Provides
    @Singleton
    fun provideVentaRepository(
        ventaDao: VentaDao,
        detalleVentaDao: DetalleVentaDao,
        productoDao: ProductoDao,
        clienteDao: ClienteDao,
        ventaPendienteDao: VentaPendienteDao,
        userPreferences: UserPreferences,
        firestore: FirebaseFirestore,
        gson: Gson
    ): VentaRepository {
        return VentaRepository(
            ventaDao = ventaDao,
            detalleVentaDao = detalleVentaDao,
            productoDao = productoDao,
            clienteDao = clienteDao,
            ventaPendienteDao = ventaPendienteDao,
            userPreferences = userPreferences,
            firestore = firestore,
            gson = gson
        )
    }
}
