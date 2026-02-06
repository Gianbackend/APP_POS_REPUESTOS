package com.example.posapp.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.posapp.data.local.dao.CategoriaDao
import com.example.posapp.data.local.dao.ClienteDao
import com.example.posapp.data.local.dao.DetalleVentaDao
import com.example.posapp.data.local.dao.ProductoDao
import com.example.posapp.data.local.dao.UsuarioDao
import com.example.posapp.data.local.dao.VentaDao
import com.example.posapp.data.local.entities.CategoriaEntity
import com.example.posapp.data.local.entities.ClienteEntity
import com.example.posapp.data.local.entities.DetalleVentaEntity
import com.example.posapp.data.local.entities.ProductoEntity
import com.example.posapp.data.local.entities.UsuarioEntity
import com.example.posapp.data.local.entities.VentaEntity


@Database(
    entities = [
        UsuarioEntity::class,
        CategoriaEntity::class,
        ProductoEntity::class,
        ClienteEntity::class,
        VentaEntity::class,
        DetalleVentaEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class POSDatabase : RoomDatabase() {

    abstract fun usuarioDao(): UsuarioDao
    abstract fun categoriaDao(): CategoriaDao
    abstract fun productoDao(): ProductoDao
    abstract fun clienteDao(): ClienteDao
    abstract fun ventaDao(): VentaDao
    abstract fun detalleVentaDao(): DetalleVentaDao

    companion object {
        const val DATABASE_NAME = "pos_database"

        // 🆕 MIGRACIÓN 1 → 2: Agregar campos de sincronización
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Agregar columnas para sincronización con Firebase
                database.execSQL("""
                    ALTER TABLE productos 
                    ADD COLUMN firebaseId TEXT
                """)

                database.execSQL("""
                    ALTER TABLE productos 
                    ADD COLUMN sincronizado INTEGER NOT NULL DEFAULT 0
                """)

                database.execSQL("""
                    ALTER TABLE productos 
                    ADD COLUMN ultimaSincronizacion INTEGER
                """)
            }
        }
        // Migración 2 → 3: usuarios Long → String
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
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

                database.execSQL("""
                    INSERT INTO usuarios_new (id, nombre, email, passwordHash, rol, activo, fechaCreacion)
                    SELECT CAST(id AS TEXT), nombre, email, passwordHash, rol, activo, fechaCreacion
                    FROM usuarios
                """)

                database.execSQL("DROP TABLE usuarios")
                database.execSQL("ALTER TABLE usuarios_new RENAME TO usuarios")
            }
        }

        // ✅ NUEVA: Migración 3 → 4: ventas.usuarioId Long → String
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Crear tabla temporal
                database.execSQL("""
                    CREATE TABLE ventas_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        numeroVenta TEXT NOT NULL,
                        usuarioId TEXT NOT NULL,
                        clienteId INTEGER,
                        subtotal REAL NOT NULL,
                        descuento REAL NOT NULL DEFAULT 0.0,
                        impuesto REAL NOT NULL DEFAULT 18.0,
                        total REAL NOT NULL,
                        metodoPago TEXT NOT NULL,
                        estado TEXT NOT NULL DEFAULT 'COMPLETADA',
                        fechaVenta INTEGER NOT NULL,
                        sincronizado INTEGER NOT NULL DEFAULT 0,
                        firebaseId TEXT,
                        ultimaSincronizacion INTEGER,
                        FOREIGN KEY(usuarioId) REFERENCES usuarios(id) ON DELETE CASCADE,
                        FOREIGN KEY(clienteId) REFERENCES clientes(id) ON DELETE SET NULL
                    )
                """)

                // Copiar datos (convertir usuarioId a String)
                database.execSQL("""
                    INSERT INTO ventas_new 
                    SELECT id, numeroVenta, CAST(usuarioId AS TEXT), clienteId, 
                           subtotal, descuento, impuesto, total, metodoPago, 
                           estado, fechaVenta, sincronizado, firebaseId, ultimaSincronizacion
                    FROM ventas
                """)

                // Eliminar tabla vieja
                database.execSQL("DROP TABLE ventas")

                // Renombrar
                database.execSQL("ALTER TABLE ventas_new RENAME TO ventas")

                // Recrear índices
                database.execSQL("CREATE INDEX index_ventas_usuarioId ON ventas(usuarioId)")
                database.execSQL("CREATE INDEX index_ventas_clienteId ON ventas(clienteId)")
                database.execSQL("CREATE UNIQUE INDEX index_ventas_numeroVenta ON ventas(numeroVenta)")
            }
        }
    }
}

