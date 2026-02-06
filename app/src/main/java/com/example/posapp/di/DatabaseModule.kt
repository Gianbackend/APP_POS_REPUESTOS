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

    // ✅ ACTUALIZADO: Datos iniciales con id String
    private suspend fun insertarDatosIniciales(context: Context) {
        val db = Room.databaseBuilder(
            context,
            POSDatabase::class.java,
            POSDatabase.DATABASE_NAME
        ).build()

        try {
            // ✅ Usuario admin con ID String (simulando Firebase UID)
            val adminHash = "admin123".hashCode().toString()
            db.usuarioDao().insert(
                UsuarioEntity(
                    id = "admin_local_001", // ✅ ID String
                    nombre = "Administrador",
                    email = "admin@pos.com",
                    passwordHash = adminHash,
                    rol = "ADMIN"
                )
            )

            // Categorías (sin cambios)
            val categorias = listOf(
                CategoriaEntity(nombre = "Frenos", descripcion = "Sistema de frenado", color = "#E53935"),
                CategoriaEntity(nombre = "Motor", descripcion = "Repuestos de motor", color = "#1E88E5"),
                CategoriaEntity(nombre = "Filtros", descripcion = "Filtros de aire y aceite", color = "#00ACC1"),
                CategoriaEntity(nombre = "Lubricantes", descripcion = "Aceites y grasas", color = "#FDD835"),
                CategoriaEntity(nombre = "Transmisión", descripcion = "Cadenas y piñones", color = "#FB8C00")
            )
            db.categoriaDao().insertAll(categorias)

            // Productos (sin cambios)
            val productos = listOf(
                ProductoEntity(
                    codigo = "MOTO-F001",
                    nombre = "Pastilla de freno delantera",
                    descripcion = "Pastilla semimetálica alta fricción",
                    marca = "Vesrah",
                    modelo = "Honda CB 190R",
                    precio = 12.00,
                    stock = 30,
                    stockMinimo = 8,
                    categoriaId = 1,
                    ubicacion = "A-1"
                ),
                ProductoEntity(
                    codigo = "MOTO-F002",
                    nombre = "Disco de freno delantero",
                    descripcion = "Disco ventilado 220mm",
                    marca = "Sunstar",
                    modelo = "Pulsar NS 200",
                    precio = 25.00,
                    stock = 4,
                    stockMinimo = 5,
                    categoriaId = 1,
                    ubicacion = "A-2"
                ),
                ProductoEntity(
                    codigo = "MOTO-M001",
                    nombre = "Bujía NGK iridium",
                    descripcion = "Bujía de alto rendimiento",
                    marca = "NGK",
                    modelo = "Universal 4T",
                    precio = 5.00,
                    stock = 60,
                    stockMinimo = 20,
                    categoriaId = 2,
                    ubicacion = "B-1"
                ),
                ProductoEntity(
                    codigo = "MOTO-M002",
                    nombre = "Kit de aros",
                    descripcion = "Aros de pistón standard",
                    marca = "RIK",
                    modelo = "Honda CG 150",
                    precio = 13.50,
                    stock = 12,
                    stockMinimo = 5,
                    categoriaId = 2,
                    ubicacion = "B-2"
                ),
                ProductoEntity(
                    codigo = "MOTO-M003",
                    nombre = "Pistón completo",
                    descripcion = "Pistón con bulón",
                    marca = "Takasago",
                    modelo = "Yamaha YBR 125",
                    precio = 21.50,
                    stock = 2,
                    stockMinimo = 4,
                    categoriaId = 2,
                    ubicacion = "B-3"
                ),
                ProductoEntity(
                    codigo = "MOTO-FI001",
                    nombre = "Filtro de aceite",
                    descripcion = "Filtro spin-on",
                    marca = "HiFlo",
                    modelo = "Honda CB 190",
                    precio = 4.00,
                    stock = 45,
                    stockMinimo = 15,
                    categoriaId = 3,
                    ubicacion = "F-1"
                ),
                ProductoEntity(
                    codigo = "MOTO-FI002",
                    nombre = "Filtro de aire",
                    descripcion = "Filtro espuma lavable",
                    marca = "Twin Air",
                    modelo = "Yamaha FZ 150",
                    precio = 9.50,
                    stock = 20,
                    stockMinimo = 8,
                    categoriaId = 3,
                    ubicacion = "F-2"
                ),
                ProductoEntity(
                    codigo = "MOTO-FI003",
                    nombre = "Filtro de aire K&N",
                    descripcion = "Filtro deportivo alto flujo",
                    marca = "K&N",
                    modelo = "Pulsar NS 200",
                    precio = 25.50,
                    stock = 3,
                    stockMinimo = 4,
                    categoriaId = 3,
                    ubicacion = "F-3"
                ),
                ProductoEntity(
                    codigo = "MOTO-L001",
                    nombre = "Aceite 20W-50 mineral",
                    descripcion = "Aceite mineral 4T - 1L",
                    marca = "Motul",
                    modelo = "3000 20W-50",
                    precio = 8.00,
                    stock = 35,
                    stockMinimo = 12,
                    categoriaId = 4,
                    ubicacion = "G-1"
                ),
                ProductoEntity(
                    codigo = "MOTO-L002",
                    nombre = "Aceite 10W-40 sintético",
                    descripcion = "Aceite sintético 4T - 1L",
                    marca = "Castrol",
                    modelo = "Power1 10W-40",
                    precio = 13.50,
                    stock = 25,
                    stockMinimo = 10,
                    categoriaId = 4,
                    ubicacion = "G-2"
                ),
                ProductoEntity(
                    codigo = "MOTO-L003",
                    nombre = "Aceite 2T sintético",
                    descripcion = "Aceite 2 tiempos - 1L",
                    marca = "Yamalube",
                    modelo = "2T sintético",
                    precio = 9.50,
                    stock = 18,
                    stockMinimo = 8,
                    categoriaId = 4,
                    ubicacion = "G-3"
                ),
                ProductoEntity(
                    codigo = "MOTO-L004",
                    nombre = "Grasa cadena spray",
                    descripcion = "Lubricante aerosol",
                    marca = "Motul",
                    modelo = "Chain Lube 400ml",
                    precio = 6.50,
                    stock = 22,
                    stockMinimo = 10,
                    categoriaId = 4,
                    ubicacion = "G-4"
                ),
                ProductoEntity(
                    codigo = "MOTO-T001",
                    nombre = "Cadena 428H x 134L",
                    descripcion = "Cadena con retenes",
                    marca = "DID",
                    modelo = "Pulsar 180",
                    precio = 25.50,
                    stock = 12,
                    stockMinimo = 5,
                    categoriaId = 5,
                    ubicacion = "D-1"
                ),
                ProductoEntity(
                    codigo = "MOTO-T002",
                    nombre = "Piñón 14T",
                    descripcion = "Piñón delantero",
                    marca = "JT",
                    modelo = "Yamaha FZ 150",
                    precio = 6.50,
                    stock = 18,
                    stockMinimo = 8,
                    categoriaId = 5,
                    ubicacion = "D-2"
                ),
                ProductoEntity(
                    codigo = "MOTO-T003",
                    nombre = "Corona 43T",
                    descripcion = "Corona trasera",
                    marca = "JT",
                    modelo = "Honda CB 190R",
                    precio = 13.50,
                    stock = 14,
                    stockMinimo = 6,
                    categoriaId = 5,
                    ubicacion = "D-3"
                ),
                ProductoEntity(
                    codigo = "MOTO-T004",
                    nombre = "Kit transmisión",
                    descripcion = "Cadena + piñón + corona",
                    marca = "RK",
                    modelo = "KTM Duke 200",
                    precio = 54.00,
                    stock = 1,
                    stockMinimo = 3,
                    categoriaId = 5,
                    ubicacion = "D-4"
                )
            )

            db.productoDao().insertAll(productos)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
