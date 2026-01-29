package com.example.posapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.posapp.presentation.navigation.NavGraph
import com.example.posapp.ui.theme.POSAppTheme
import com.google.firebase.Firebase
import com.google.firebase.app
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            POSAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Crear el controlador de navegación
                    val navController = rememberNavController()

                    // Mostrar el NavGraph (que decide qué pantalla mostrar)
                    NavGraph(navController = navController)
                }
            }
        }
    }
}
