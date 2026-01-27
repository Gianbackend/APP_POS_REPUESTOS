package com.example.posapp.presentation.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Home : Screen("home")
    object Productos : Screen("productos")
    object ProductoDetail : Screen("producto_detail/{productoId}") {  // ← NUEVO
        fun createRoute(productoId: Long) = "producto_detail/$productoId"
    }
}