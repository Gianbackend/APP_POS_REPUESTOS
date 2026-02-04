package com.example.posapp.di

import com.example.posapp.data.local.dao.ProductoDao
import com.example.posapp.data.repository.ProductoRepositoryImpl
import com.example.posapp.domain.repository.ProductoRepository
import com.google.firebase.firestore.FirebaseFirestore
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
    ): ProductoRepository {
        return ProductoRepositoryImpl(productoDao, firestore)
    }
}
