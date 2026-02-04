package com.example.posapp.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.posapp.data.local.dao.CategoriaDao
import com.example.posapp.data.local.dao.ClienteDao
import com.example.posapp.data.local.dao.DetalleVentaDao
import com.example.posapp.data.local.dao.ProductoDao
import com.example.posapp.data.local.dao.UsuarioDao
import com.example.posapp.data.local.dao.VentaDao
import com.example.posapp.data.local.dao.VentaPendienteDao
import com.example.posapp.data.local.entities.CategoriaEntity
import com.example.posapp.data.local.entities.ClienteEntity
import com.example.posapp.data.local.entities.DetalleVentaEntity
import com.example.posapp.data.local.entities.ProductoEntity
import com.example.posapp.data.local.entities.UsuarioEntity
import com.example.posapp.data.local.entities.VentaEntity
import com.example.posapp.data.local.entity.VentaPendienteEntity
import com.example.posapp.data.local.converter.Converters

@Database(
    entities = [
        UsuarioEntity::class,
        CategoriaEntity::class,
        ProductoEntity::class,
        ClienteEntity::class,
        VentaEntity::class,
        DetalleVentaEntity::class,
        VentaPendienteEntity::class  // ← ✅ YA LO TIENES
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class POSDatabase : RoomDatabase() {

    abstract fun usuarioDao(): UsuarioDao
    abstract fun categoriaDao(): CategoriaDao
    abstract fun productoDao(): ProductoDao
    abstract fun clienteDao(): ClienteDao
    abstract fun ventaDao(): VentaDao
    abstract fun detalleVentaDao(): DetalleVentaDao
    abstract fun ventaPendienteDao(): VentaPendienteDao  // ← ✅ YA LO TIENES

    companion object {
        const val DATABASE_NAME = "pos_database"

        // 🆕 MIGRACIÓN 1 → 2: Agregar tabla ventas_pendientes + campos sync en productos
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {

                // 1️⃣ Crear tabla ventas_pendientes
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS ventas_pendientes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        fecha INTEGER NOT NULL,
                        total REAL NOT NULL,
                        metodoPago TEXT NOT NULL,
                        clienteNombre TEXT,
                        clienteDocumento TEXT,
                        productosJson TEXT NOT NULL,
                        intentosSincronizacion INTEGER NOT NULL DEFAULT 0,
                        ultimoIntento INTEGER,
                        errorSincronizacion TEXT,
                        sincronizado INTEGER NOT NULL DEFAULT 0,
                        firebaseId TEXT
                    )
                """.trimIndent())

                // 2️⃣ Agregar columnas de sincronización a productos
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
    }
}
