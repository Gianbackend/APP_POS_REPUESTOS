package com.example.posapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class POSApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Inicialización de la app
    }
}