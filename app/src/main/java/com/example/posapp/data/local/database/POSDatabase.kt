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
    version = 2,
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
    }
}