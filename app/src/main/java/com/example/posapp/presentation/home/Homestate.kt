
package com.example.posapp.presentation.home

data class HomeState(
    val isSyncing: Boolean = false,
    val syncCompleted: Boolean = false,
    val syncError: String? = null,
    val productosCount: Int = 0
)
