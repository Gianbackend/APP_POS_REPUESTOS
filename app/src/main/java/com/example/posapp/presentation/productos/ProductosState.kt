package com.example.posapp.presentation.productos

import com.example.posapp.domain.model.Categoria
import com.example.posapp.domain.model.Producto

data class ProductosState(
    val productos: List<Producto> = emptyList(),           // Lista de productos a mostrar
    val categorias: List<Categoria> = emptyList(),         // Categorías para filtro
    val isLoading: Boolean = true,                         // ¿Está cargando?
    val searchQuery: String = "",                          // Texto de búsqueda
    val selectedCategoriaId: Long? = null,                 // Categoría seleccionada (null = todas)
    val showStockBajo: Boolean = false,                    // ¿Mostrar solo stock bajo?
    val error: String? = null                              // Mensaje de error si hay
)