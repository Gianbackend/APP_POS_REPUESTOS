package com.example.posapp.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.posapp.data.firebase.FirebaseStorageManager
import com.example.posapp.data.local.dao.*
import com.example.posapp.data.local.database.POSDatabase
import com.example.posapp.data.local.entities.*
import com.example.posapp.data.repository.ProductoSyncRepository
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): POSDatabase {
        return Room.databaseBuilder(
            context,
            POSDatabase::class.java,
            POSDatabase.DATABASE_NAME
        )
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    CoroutineScope(Dispatchers.IO).launch {
                        insertarDatosIniciales(context)
                    }
                }
            })
            .addMigrations(
                POSDatabase.MIGRATION_2_3,
                POSDatabase.MIGRATION_3_4,
                POSDatabase.MIGRATION_4_5 // ✅ AGREGAR
            )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideUsuarioDao(database: POSDatabase): UsuarioDao = database.usuarioDao()

    @Provides
    fun provideCategoriaDao(database: POSDatabase): CategoriaDao = database.categoriaDao()

    @Provides
    fun provideProductoDao(database: POSDatabase): ProductoDao = database.productoDao()

    @Provides
    fun provideClienteDao(database: POSDatabase): ClienteDao = database.clienteDao()

    @Provides
    fun provideVentaDao(database: POSDatabase): VentaDao = database.ventaDao()

    @Provides
    fun provideDetalleVentaDao(database: POSDatabase): DetalleVentaDao = database.detalleVentaDao()

    @Provides
    @Singleton
    fun provideFirebaseStorageManager(): FirebaseStorageManager = FirebaseStorageManager()

    @Provides
    @Singleton
    fun provideProductoSyncRepository(
        firestore: FirebaseFirestore,
        productoDao: ProductoDao
    ): ProductoSyncRepository = ProductoSyncRepository(firestore, productoDao)

    private suspend fun insertarDatosIniciales(context: Context) {
        val db = Room.databaseBuilder(
            context,
            POSDatabase::class.java,
            POSDatabase.DATABASE_NAME
        ).build()

        try {
            // ✅ Usuario admin
            val adminHash = "admin123".hashCode().toString()
            db.usuarioDao().insert(
                UsuarioEntity(
                    id = "admin_local_001",
                    nombre = "Administrador",
                    email = "admin@pos.com",
                    passwordHash = adminHash,
                    rol = "ADMIN"
                )
            )

            // ✅ Categorías con IDs FIJOS
            val categorias = listOf(
                CategoriaEntity(id = 1, nombre = "Lubricantes", descripcion = "Aceites y grasas", color = "#FDD835"),
                CategoriaEntity(id = 2, nombre = "Filtros", descripcion = "Filtros de aire y aceite", color = "#00ACC1"),
                CategoriaEntity(id = 3, nombre = "Sistema Eléctrico", descripcion = "Baterías y bujías", color = "#1E88E5"),
                CategoriaEntity(id = 4, nombre = "Sistema de Frenos", descripcion = "Pastillas y discos", color = "#E53935"),
                CategoriaEntity(id = 5, nombre = "Suspensión", descripcion = "Amortiguadores", color = "#FB8C00"),
                CategoriaEntity(id = 6, nombre = "Transmisión", descripcion = "Embrague y cadenas", color = "#8E24AA")
            )

            db.categoriaDao().insertAll(categorias)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
