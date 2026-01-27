package com.example.posapp.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.posapp.data.repository.AuthRepository
import com.example.posapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel // Hilt lo crea automáticamente e inyecta AuthRepository
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository // Inyectado por Hilt
) : ViewModel() {

    // Estado privado (solo el ViewModel puede modificarlo)
    private val _state = MutableStateFlow(LoginState())
    // Estado público (la UI solo puede leerlo)
    val state = _state.asStateFlow()

    // Cuando el usuario escribe en el campo Email
    fun onEmailChange(email: String) {
        _state.update { it.copy(email = email, error = null) }
        // update = actualiza el estado
        // copy = crea una copia del estado actual con cambios
        // error = null = limpia el error cuando el usuario escribe
    }

    // Cuando el usuario escribe en el campo Password
    fun onPasswordChange(password: String) {
        _state.update { it.copy(password = password, error = null) }
    }

    // Cuando el usuario presiona el botón "Iniciar Sesión"
    fun onLogin() {
        val email = _state.value.email.trim() // Quita espacios
        val password = _state.value.password

        // Validaciones básicas
        if (email.isEmpty()) {
            _state.update { it.copy(error = "Ingrese el email") }
            return
        }

        if (password.isEmpty()) {
            _state.update { it.copy(error = "Ingrese la contraseña") }
            return
        }

        // Intentar login
        viewModelScope.launch { // Ejecuta en background
            authRepository.login(email, password).collect { result ->
                // collect = observa los estados que emite el Repository
                when (result) {
                    is Resource.Loading -> {
                        // Mostrar loading
                        _state.update { it.copy(isLoading = true, error = null) }
                    }
                    is Resource.Success -> {
                        // Login exitoso
                        _state.update {
                            it.copy(
                                isLoading = false,
                                loginSuccess = true,
                                error = null
                            )
                        }
                    }
                    is Resource.Error -> {
                        // Mostrar error
                        _state.update {
                            it.copy(
                                isLoading = false,
                                error = result.message,
                                loginSuccess = false
                            )
                        }
                    }
                }
            }
        }
    }
}