package com.example.posapp.data.repository

import com.example.posapp.data.preferences.UserPreferences
import com.example.posapp.domain.model.Usuario
import com.example.posapp.util.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val userPreferences: UserPreferences // Para guardar sesión
) {
    // ✅ NUEVO: Instancias de Firebase
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    // ✅ ACTUALIZADO: Login con Firebase Auth
    fun login(email: String, pin: String): Flow<Resource<Usuario>> = flow {
        try {
            // 1. Emitir estado Loading
            emit(Resource.Loading())

            // 2. Validar formato de email
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emit(Resource.Error("Email inválido"))
                return@flow
            }

            // 3. Validar PIN (6 dígitos)
            if (pin.length != 6 || !pin.all { it.isDigit() }) {
                emit(Resource.Error("El PIN debe tener 6 dígitos"))
                return@flow
            }

            // 4. Login con Firebase Auth
            val result = auth.signInWithEmailAndPassword(email, pin).await()
            val uid = result.user?.uid ?: throw Exception("Usuario no encontrado")

            // 5. Obtener datos del usuario desde Firestore
            val userDoc = firestore.collection("usuarios")
                .document(uid)
                .get()
                .await()

            if (!userDoc.exists()) {
                auth.signOut()
                emit(Resource.Error("Usuario no registrado en el sistema"))
                return@flow
            }

            // 6. Verificar estado del usuario
            val activo = userDoc.getBoolean("activo") ?: false
            val bloqueado = userDoc.getBoolean("bloqueado") ?: false

            when {
                bloqueado -> {
                    auth.signOut()
                    emit(Resource.Error("Usuario bloqueado. Contacte al administrador"))
                    return@flow
                }
                !activo -> {
                    auth.signOut()
                    emit(Resource.Error("Usuario inactivo"))
                    return@flow
                }
            }

            // 7. Crear objeto Usuario
            val usuario = Usuario(
                id = uid,
                nombre = userDoc.getString("nombre") ?: "Usuario",
                email = userDoc.getString("email") ?: email,
                rol = userDoc.getString("rol") ?: "vendedor",
                activo = activo
            )

            // 8. Guardar sesión en DataStore
            userPreferences.saveUserSession(
                userId = usuario.id,
                userName = usuario.nombre,
                userEmail = usuario.email,
                userRol = usuario.rol
            )

            // 9. Login exitoso
            emit(Resource.Success(usuario))

        } catch (e: FirebaseAuthInvalidUserException) {
            emit(Resource.Error("Usuario no existe"))
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            emit(Resource.Error("PIN incorrecto"))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Error desconocido"))
        }
    }

    // ✅ ACTUALIZADO: Cerrar sesión con Firebase
    suspend fun logout() {
        auth.signOut()
        userPreferences.clearUserSession()
    }

    // ✅ NUEVO: Verificar si hay sesión activa
    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    // Exponer sesión actual
    val userSession = userPreferences.userSession
}
