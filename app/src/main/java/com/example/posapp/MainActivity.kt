package com.example.posapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.posapp.data.repository.AuthRepository
import com.example.posapp.presentation.navigation.NavGraph // ✅ IMPORTAR
import com.example.posapp.presentation.navigation.Screen // ✅ IMPORTAR
import com.example.posapp.ui.theme.POSAppTheme
import com.google.firebase.FirebaseApp
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)

        setContent {
            POSAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    // ✅ Determinar ruta inicial
                    val isLoggedIn = authRepository.isUserLoggedIn()
                    val startDestination = if (isLoggedIn) {
                        Screen.Home.route
                    } else {
                        Screen.Login.route
                    }

                    // ✅ USAR NavGraph en lugar de AppNavigation
                    NavGraph(
                        navController = navController,
                        startDestination = startDestination
                    )
                }
            }
        }
    }
}
