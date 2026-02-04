package com.example.posapp.data.local.entities

import androidx.room.Embedded
import androidx.room.Relation
import com.example.posapp.domain.model.Producto

data class ProductoWithCategoria(
    @Embedded val producto: ProductoEntity,

    @Relation(
        parentColumn = "categoriaId",
        entityColumn = "id"
    )
    val categoria: CategoriaEntity
) {
    fun toDomain(): Producto {
        return producto.toDomain(categoria.nombre)
    }
}
