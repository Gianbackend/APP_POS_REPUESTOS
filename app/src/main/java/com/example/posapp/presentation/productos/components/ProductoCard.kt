package com.example.posapp.presentation.productos.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.posapp.domain.model.Producto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductoCard(
    producto: Producto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),  // ← 16 → 12
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = producto.codigo,
                    fontSize = 11.sp,  // ← 12 → 11
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(2.dp))  // ← 4 → 2

                Text(
                    text = producto.nombre,
                    fontSize = 15.sp,  // ← 16 → 15
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))  // ← 4 → 2

                Text(
                    text = "${producto.marca} - ${producto.modelo}",
                    fontSize = 13.sp,  // ← 14 → 13
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))  // ← 4 → 3

                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = producto.categoriaNombre,
                        fontSize = 11.sp,  // ← 12 → 11
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)  // ← Reducido
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))  // ← 16 → 12

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "$ ${String.format("%.2f", producto.precio)}",
                    fontSize = 17.sp,  // ← 18 → 17
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(6.dp))  // ← 8 → 6

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (producto.stockBajo) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Stock bajo",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(14.dp)  // ← 16 → 14
                        )
                        Spacer(modifier = Modifier.width(3.dp))  // ← 4 → 3
                    }

                    Text(
                        text = "Stock: ${producto.stock}",
                        fontSize = 13.sp,  // ← 14 → 13
                        color = if (producto.stockBajo) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }

                if (producto.ubicacion != null) {
                    Spacer(modifier = Modifier.height(3.dp))  // ← 4 → 3
                    Text(
                        text = "📍 ${producto.ubicacion}",
                        fontSize = 11.sp,  // ← 12 → 11
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
