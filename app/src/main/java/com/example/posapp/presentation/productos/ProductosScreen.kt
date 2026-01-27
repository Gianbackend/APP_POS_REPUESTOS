package com.example.posapp.presentation.productos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.posapp.presentation.productos.components.ProductoCard

@OptIn(ExperimentalMaterial3Api::class)  // ← Cubre toda la pantalla
@Composable
fun ProductosScreen(
    onProductoClick: (Long) -> Unit,
    viewModel: ProductosViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Catálogo de Repuestos") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
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
                    .padding(16.dp)
            )

            FilterChips(
                categorias = state.categorias,
                selectedCategoriaId = state.selectedCategoriaId,
                showStockBajo = state.showStockBajo,
                onCategoriaSelected = viewModel::onCategoriaSelected,
                onToggleStockBajo = viewModel::onToggleStockBajo,
                onClearFilters = viewModel::onClearFilters,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp)  // ← Cambia el Spacer por padding bottom
            )

// ELIMINA el Spacer(modifier = Modifier.height(8.dp))

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
                        contentPadding = PaddingValues(16.dp),
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

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Buscar por nombre, código, marca...") },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = "Buscar")
        },
        modifier = modifier,
        singleLine = true
    )
}

@OptIn(ExperimentalMaterial3Api::class)  // ← Para los FilterChips
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
    Column(modifier = modifier) {
        // Primera fila: Stock Bajo + Limpiar filtros
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            FilterChip(
                selected = showStockBajo,
                onClick = onToggleStockBajo,
                label = { Text("Stock Bajo") },
                leadingIcon = if (showStockBajo) {
                    { Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null
            )

            if (selectedCategoriaId != null || showStockBajo) {
                FilterChip(
                    selected = false,
                    onClick = onClearFilters,
                    label = { Text("Todos") }
                )
            }
        }

        // Segunda fila: Categorías con scroll horizontal
        if (categorias.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))

            // CAMBIO: LazyRow en vez de Row para scroll horizontal
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
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
    }
}