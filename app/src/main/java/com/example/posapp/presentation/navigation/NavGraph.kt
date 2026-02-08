package com.example.posapp.presentation.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.posapp.presentation.home.HomeScreen
import com.example.posapp.presentation.login.LoginScreen
import com.example.posapp.presentation.productos.ProductosScreen
import com.example.posapp.presentation.productos.ProductoDetailScreen
import com.example.posapp.presentation.venta.VentaScreen
import com.example.posapp.presentation.ticket.TicketScreen

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
                onNavigateToCatalogo = {
                    Log.d("NavGraph", "🚀 Navegando a productos...")
                    navController.navigate(Screen.Productos.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                    Log.d("NavGraph", "✅ Navegación ejecutada")
                }
            )
        }

        composable(route = Screen.Productos.route) {
            Log.d("NavGraph", "📦 ProductosScreen CARGADO")
            ProductosScreen(
                onProductoClick = { productoId ->
                    navController.navigate(Screen.ProductoDetail.createRoute(productoId))
                },
                onNavigateToCarrito = {
                    navController.navigate(Screen.Carrito.route)
                }
            )
        }

        composable(
            route = Screen.ProductoDetail.route,
            arguments = listOf(
                navArgument("productoId") { type = NavType.LongType }
            )
        ) { _ ->

            ProductoDetailScreen(

                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(route = Screen.Carrito.route) {
            VentaScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onVentaCompletada = { ventaId ->
                    navController.navigate(Screen.Ticket.createRoute(ventaId)) {
                        popUpTo(Screen.Productos.route) { inclusive = false }
                    }
                }
            )
        }

        composable(
            route = Screen.Ticket.route,
            arguments = listOf(
                navArgument("ventaId") { type = NavType.LongType }
            )
        ) { _ ->

            TicketScreen(

                onNavigateToCatalogo = {
                    navController.navigate(Screen.Productos.route) {
                        popUpTo(Screen.Productos.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
