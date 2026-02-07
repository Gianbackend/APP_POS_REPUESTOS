package com.example.posapp.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
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

    // ✅ NUEVA: Migración 2 → 3 (cambiar id de Long a String en usuarios)
    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // 1. Crear tabla temporal con id String
            database.execSQL("""
                CREATE TABLE usuarios_new (
                    id TEXT PRIMARY KEY NOT NULL,
                    nombre TEXT NOT NULL,
                    email TEXT NOT NULL,
                    passwordHash TEXT NOT NULL,
                    rol TEXT NOT NULL,
                    activo INTEGER NOT NULL DEFAULT 1,
                    fechaCreacion INTEGER NOT NULL
                )
            """)

            // 2. Copiar datos existentes (convertir id a String)
            database.execSQL("""
                INSERT INTO usuarios_new (id, nombre, email, passwordHash, rol, activo, fechaCreacion)
                SELECT CAST(id AS TEXT), nombre, email, passwordHash, rol, activo, fechaCreacion
                FROM usuarios
            """)

            // 3. Eliminar tabla vieja
            database.execSQL("DROP TABLE usuarios")

            // 4. Renombrar tabla nueva
            database.execSQL("ALTER TABLE usuarios_new RENAME TO usuarios")
        }
    }

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
                POSDatabase.MIGRATION_1_2,
                POSDatabase.MIGRATION_2_3,
                POSDatabase.MIGRATION_3_4 // ✅ AGREGAR
            )
            .fallbackToDestructiveMigration() // ✅ Si falla, borra todo
            .build()
    }

    @Provides
    fun provideUsuarioDao(database: POSDatabase): UsuarioDao {
        return database.usuarioDao()
    }

    @Provides
    fun provideCategoriaDao(database: POSDatabase): CategoriaDao {
        return database.categoriaDao()
    }

    @Provides
    fun provideProductoDao(database: POSDatabase): ProductoDao {
        return database.productoDao()
    }

    @Provides
    fun provideClienteDao(database: POSDatabase): ClienteDao {
        return database.clienteDao()
    }

    @Provides
    fun provideVentaDao(database: POSDatabase): VentaDao {
        return database.ventaDao()
    }

    @Provides
    fun provideDetalleVentaDao(database: POSDatabase): DetalleVentaDao {
        return database.detalleVentaDao()
    }

    @Provides
    @Singleton
    fun provideFirebaseStorageManager(): FirebaseStorageManager {
        return FirebaseStorageManager()
    }

    @Provides
    @Singleton
    fun provideProductoSyncRepository(
        firestore: FirebaseFirestore,
        productoDao: ProductoDao
    ): ProductoSyncRepository {
        return ProductoSyncRepository(firestore, productoDao)
    }


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

            // ✅ Categorías (necesarias para relación FK)
            val categorias = listOf(
                CategoriaEntity(nombre = "Frenos", descripcion = "Sistema de frenado", color = "#E53935"),
                CategoriaEntity(nombre = "Motor", descripcion = "Repuestos de motor", color = "#1E88E5"),
                CategoriaEntity(nombre = "Filtros", descripcion = "Filtros de aire y aceite", color = "#00ACC1"),
                CategoriaEntity(nombre = "Lubricantes", descripcion = "Aceites y grasas", color = "#FDD835"),
                CategoriaEntity(nombre = "Transmisión", descripcion = "Cadenas y piñones", color = "#FB8C00")
            )
            db.categoriaDao().insertAll(categorias)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
