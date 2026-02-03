package com.example.posapp.presentation.productos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.posapp.presentation.productos.components.ProductoCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductosScreen(
    onProductoClick: (Long) -> Unit,
    onNavigateToCarrito: () -> Unit,
    viewModel: ProductosViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    // Observar cantidad en el carrito (simplificado)
    val cantidadCarrito by viewModel.getCantidadCarrito().collectAsState(initial = 0)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Catálogo de Repuestos") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            if (cantidadCarrito > 0) {
                FloatingActionButton(
                    onClick = onNavigateToCarrito,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "Carrito"
                        )
                        Text(
                            text = "($cantidadCarrito)",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            SearchBar(
                query = state.searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )

            FilterChips(
                categorias = state.categorias,
                selectedCategoriaId = state.selectedCategoriaId,
                showStockBajo = state.showStockBajo,
                onCategoriaSelected = viewModel::onCategoriaSelected,
                onToggleStockBajo = viewModel::onToggleStockBajo,
                onClearFilters = viewModel::onClearFilters,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                state.error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = state.error ?: "Error desconocido",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                state.productos.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No se encontraron productos",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),  // ← TU VERSIÓN (está bien)
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = state.productos,
                            key = { it.id }
                        ) { producto ->
                            ProductoCard(
                                producto = producto,
                                onClick = { onProductoClick(producto.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ... resto igual (SearchBar, FilterChips)

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        label = { Text("Buscar por material o código...") },  // ← CAMBIA placeholder por label
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                contentDescription = "Buscar",
                modifier = Modifier.size(18.dp)
            )
        },
        modifier = modifier
            .height(56.dp)  // ← Aumenta a 56dp para el label
            .fillMaxWidth(),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium
    )

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterChips(
    categorias: List<com.example.posapp.domain.model.Categoria>,
    selectedCategoriaId: Long?,
    showStockBajo: Boolean,
    onCategoriaSelected: (Long?) -> Unit,
    onToggleStockBajo: () -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    // ✅ TODO en un solo LazyRow
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        // ✅ Stock Bajo primero
        item {
            FilterChip(
                selected = showStockBajo,
                onClick = onToggleStockBajo,
                label = { Text("Stock Bajo") },
                leadingIcon = if (showStockBajo) {
                    { Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null
            )
        }

        // ✅ Limpiar filtros
        if (selectedCategoriaId != null || showStockBajo) {
            item {
                FilterChip(
                    selected = false,
                    onClick = onClearFilters,
                    label = { Text("Todos") }
                )
            }
        }

        // ✅ Categorías
        items(categorias) { categoria ->
            FilterChip(
                selected = selectedCategoriaId == categoria.id,
                onClick = {
                    if (selectedCategoriaId == categoria.id) {
                        onCategoriaSelected(null)
                    } else {
                        onCategoriaSelected(categoria.id)
                    }
                },
                label = { Text(categoria.nombre) }
            )
        }
    }
}
