package com.example.posapp.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.posapp.data.local.dao.*
import com.example.posapp.data.local.entities.*

@Database(
    entities = [
        UsuarioEntity::class,
        CategoriaEntity::class,
        ProductoEntity::class,
        ClienteEntity::class,
        VentaEntity::class,
        DetalleVentaEntity::class
    ],
    version = 5,
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

        // Migración 3 → 4: ventas.usuarioId Long → String
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
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

                database.execSQL("""
                    INSERT INTO ventas_new 
                    SELECT id, numeroVenta, CAST(usuarioId AS TEXT), clienteId, 
                           subtotal, descuento, impuesto, total, metodoPago, 
                           estado, fechaVenta, sincronizado, firebaseId, ultimaSincronizacion
                    FROM ventas
                """)

                database.execSQL("DROP TABLE ventas")
                database.execSQL("ALTER TABLE ventas_new RENAME TO ventas")
                database.execSQL("CREATE INDEX index_ventas_usuarioId ON ventas(usuarioId)")
                database.execSQL("CREATE INDEX index_ventas_clienteId ON ventas(clienteId)")
                database.execSQL("CREATE UNIQUE INDEX index_ventas_numeroVenta ON ventas(numeroVenta)")
            }
        }

        // ✅ NUEVA: Migración 4 → 5: Categorías con IDs fijos
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 1. Eliminar productos (por foreign key)
                database.execSQL("DELETE FROM productos")

                // 2. Eliminar categorías viejas
                database.execSQL("DELETE FROM categorias")

                // 3. Recrear tabla categorías sin autoGenerate
                database.execSQL("DROP TABLE categorias")
                database.execSQL("""
                    CREATE TABLE categorias (
                        id INTEGER PRIMARY KEY NOT NULL,
                        nombre TEXT NOT NULL,
                        descripcion TEXT NOT NULL DEFAULT '',
                        icono TEXT NOT NULL DEFAULT 'category',
                        color TEXT NOT NULL DEFAULT '#2196F3',
                        activo INTEGER NOT NULL DEFAULT 1,
                        fechaCreacion INTEGER NOT NULL
                    )
                """)

                // 4. Insertar categorías con IDs fijos
                val timestamp = System.currentTimeMillis()
                database.execSQL("""
                    INSERT INTO categorias (id, nombre, descripcion, color, activo, fechaCreacion) VALUES
                    (1, 'Lubricantes', 'Aceites y grasas', '#FDD835', 1, $timestamp),
                    (2, 'Filtros', 'Filtros de aire y aceite', '#00ACC1', 1, $timestamp),
                    (3, 'Sistema Eléctrico', 'Baterías y bujías', '#1E88E5', 1, $timestamp),
                    (4, 'Sistema de Frenos', 'Pastillas y discos', '#E53935', 1, $timestamp),
                    (5, 'Suspensión', 'Amortiguadores', '#FB8C00', 1, $timestamp),
                    (6, 'Transmisión', 'Embrague y cadenas', '#8E24AA', 1, $timestamp)
                """)

                // 5. Recrear índice
                database.execSQL("CREATE UNIQUE INDEX index_categorias_nombre ON categorias(nombre)")
            }
        }
    }
}
