package com.example.posapp.presentation.login

import android.provider.ContactsContract.PinnedPositions.pin
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.posapp.data.repository.AuthRepository
import com.example.posapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.fold
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
        val email = _state.value.email
        val password = _state.value.password // Este es el PIN

        // Validaciones
        if (email.isEmpty()) {
            _state.update { it.copy(error = "Ingrese el email") }
            return
        }

        // NUEVA VALIDACIÓN PARA PIN (6 dígitos)
        if (password.length != 6 || !password.all { it.isDigit() }) {
            _state.update { it.copy(error = "El PIN debe tener 6 dígitos") }
            return
        }

        _state.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            authRepository.login(email, password).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                loginSuccess = true
                            )
                        }
                    }
                    is Resource.Error -> {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                error = result.message ?: "Error al iniciar sesión"
                            )
                        }
                    }
                    is Resource.Loading -> {
                        _state.update { it.copy(isLoading = true) }
                    }
                }
            }
        }
    }





}