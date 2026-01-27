package com.example.posapp.presentation.login

data class LoginState(
    val email: String = "admin@pos.com",              // Email ingresado
    val password: String = "admin123",           // Password ingresado
    val isLoading: Boolean = false,      // ¿Está procesando el login?
    val error: String? = null,           // Mensaje de error (si hay)
    val loginSuccess: Boolean = false    // ¿Login exitoso?
)