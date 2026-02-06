package com.example.posapp.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// Extensión para crear el DataStore
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Llaves para guardar datos
    private object PreferencesKeys {
        val USER_ID = stringPreferencesKey("user_id") // ✅ CAMBIO: Long → String
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val USER_ROL = stringPreferencesKey("user_rol")
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
    }

    // Leer sesión del usuario (Flow = datos reactivos)
    val userSession: Flow<UserSession> = context.dataStore.data.map { preferences ->
        UserSession(
            userId = preferences[PreferencesKeys.USER_ID] ?: "", // ✅ CAMBIO: 0L → ""
            userName = preferences[PreferencesKeys.USER_NAME] ?: "",
            userEmail = preferences[PreferencesKeys.USER_EMAIL] ?: "",
            userRol = preferences[PreferencesKeys.USER_ROL] ?: "",
            isLoggedIn = preferences[PreferencesKeys.IS_LOGGED_IN] ?: false
        )
    }

    // Guardar sesión cuando el usuario hace login
    suspend fun saveUserSession(
        userId: String, // ✅ CAMBIO: Long → String
        userName: String,
        userEmail: String,
        userRol: String
    ) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_ID] = userId
            preferences[PreferencesKeys.USER_NAME] = userName
            preferences[PreferencesKeys.USER_EMAIL] = userEmail
            preferences[PreferencesKeys.USER_ROL] = userRol
            preferences[PreferencesKeys.IS_LOGGED_IN] = true
        }
    }

    // Borrar sesión cuando el usuario hace logout
    suspend fun clearUserSession() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}

// Modelo de datos de la sesión
data class UserSession(
    val userId: String, // ✅ CAMBIO: Long → String
    val userName: String,
    val userEmail: String,
    val userRol: String,
    val isLoggedIn: Boolean
)
