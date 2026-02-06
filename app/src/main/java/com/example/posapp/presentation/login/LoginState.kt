package com.example.posapp.presentation.login

data class LoginState(
    val email: String = "",  // Vacío
    val password: String = "", // Vacío (será el PIN)
    val isLoading: Boolean = false,
    val error: String? = null,
    val loginSuccess: Boolean = false
)