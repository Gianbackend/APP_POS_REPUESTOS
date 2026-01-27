package com.example.posapp.data.repository

import com.example.posapp.domain.model.ItemCarrito
import com.example.posapp.domain.model.Producto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton  // Una sola instancia compartida en toda la app
class CarritoRepository @Inject constructor() {

    // Lista privada del carrito
    private val _items = MutableStateFlow<List<ItemCarrito>>(emptyList())
    // Lista pública (solo lectura)
    val items: StateFlow<List<ItemCarrito>> = _items.asStateFlow()

    // Agregar o actualizar producto en el carrito
    fun agregarProducto(producto: Producto, cantidad: Int) {
        _items.update { currentItems ->
            // Buscar si el producto ya está en el carrito
            val itemExistente = currentItems.find { it.producto.id == producto.id }

            if (itemExistente != null) {
                // Si existe, SUMAR la cantidad
                currentItems.map { item ->
                    if (item.producto.id == producto.id) {
                        item.copy(cantidad = item.cantidad + cantidad)
                    } else {
                        item
                    }
                }
            } else {
                // Si NO existe, agregarlo
                currentItems + ItemCarrito(producto, cantidad)
            }
        }
    }

    // Actualizar cantidad de un producto (sin sumar, reemplazar)
    fun actualizarCantidad(productoId: Long, nuevaCantidad: Int) {
        _items.update { currentItems ->
            if (nuevaCantidad <= 0) {
                // Si la cantidad es 0 o menos, eliminar del carrito
                currentItems.filter { it.producto.id != productoId }
            } else {
                // Actualizar cantidad
                currentItems.map { item ->
                    if (item.producto.id == productoId) {
                        item.copy(cantidad = nuevaCantidad)
                    } else {
                        item
                    }
                }
            }
        }
    }

    // Eliminar producto del carrito
    fun eliminarProducto(productoId: Long) {
        _items.update { currentItems ->
            currentItems.filter { it.producto.id != productoId }
        }
    }

    // Limpiar todo el carrito
    fun limpiarCarrito() {
        _items.value = emptyList()
    }

    // Calcular total del carrito
    fun calcularTotal(): Double {
        return _items.value.sumOf { it.subtotal }
    }

    // Obtener cantidad total de items (para mostrar badge)
    fun getCantidadTotal(): Int {
        return _items.value.sumOf { it.cantidad }
    }
}