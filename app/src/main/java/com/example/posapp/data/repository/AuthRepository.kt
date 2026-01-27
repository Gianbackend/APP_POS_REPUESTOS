package com.example.posapp.data.repository

import com.example.posapp.data.local.dao.UsuarioDao
import com.example.posapp.data.preferences.UserPreferences
import com.example.posapp.domain.model.Usuario
import com.example.posapp.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val usuarioDao: UsuarioDao,        // Para buscar usuario en BD
    private val userPreferences: UserPreferences // Para guardar sesión
) {

    // Función de login que retorna estados (Loading, Success, Error)
    fun login(email: String, password: String): Flow<Resource<Usuario>> = flow {
        try {
            // 1. Emitir estado Loading
            emit(Resource.Loading())

            // 2. Buscar usuario por email en la base de datos
            val usuarioEntity = usuarioDao.getByEmail(email)

            // 3. Validar que el usuario existe
            if (usuarioEntity == null) {
                emit(Resource.Error("Usuario no encontrado"))
                return@flow
            }

            // 4. Verificar contraseña (simple hash - en producción usa BCrypt)
            val passwordHash = password.hashCode().toString()
            if (usuarioEntity.passwordHash != passwordHash) {
                emit(Resource.Error("Contraseña incorrecta"))
                return@flow
            }

            // 5. Verificar que el usuario esté activo
            if (!usuarioEntity.activo) {
                emit(Resource.Error("Usuario inactivo. Contacte al administrador"))
                return@flow
            }

            // 6. Guardar sesión en DataStore
            userPreferences.saveUserSession(
                userId = usuarioEntity.id,
                userName = usuarioEntity.nombre,
                userEmail = usuarioEntity.email,
                userRol = usuarioEntity.rol
            )

            // 7. Login exitoso - emitir Success con el usuario
            emit(Resource.Success(usuarioEntity.toDomain()))

        } catch (e: Exception) {
            // Si hay algún error, emitir Error
            emit(Resource.Error(e.localizedMessage ?: "Error desconocido"))
        }
    }

    // Cerrar sesión
    suspend fun logout() {
        userPreferences.clearUserSession()
    }

    // Exponer sesión actual (para verificar si está logueado)
    val userSession = userPreferences.userSession
}