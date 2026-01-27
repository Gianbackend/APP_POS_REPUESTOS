package com.example.posapp.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.posapp.presentation.home.HomeScreen
import com.example.posapp.presentation.login.LoginScreen
import com.example.posapp.presentation.productos.ProductosScreen
import com.example.posapp.presentation.productos.ProductoDetailScreen  // ← NUEVO

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Login.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(route = Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(route = Screen.Home.route) {
            HomeScreen(
                onNavigateToProductos = {
                    navController.navigate(Screen.Productos.route)
                }
            )
        }

        composable(route = Screen.Productos.route) {
            ProductosScreen(
                onProductoClick = { productoId ->
                    // Navegar al detalle con el ID
                    navController.navigate(Screen.ProductoDetail.createRoute(productoId))
                }
            )
        }

        // NUEVA RUTA: Detalle de producto
        composable(route = Screen.ProductoDetail.route) {
            ProductoDetailScreen(
                onNavigateBack = {
                    navController.popBackStack()  // Volver atrás
                }
            )
        }
    }
}