package com.example.posapp.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
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
    version = 1,
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
    }
}