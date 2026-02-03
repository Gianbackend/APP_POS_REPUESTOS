package com.example.posapp.data.firebase

import android.net.Uri
import android.util.Log
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.google.firebase.storage.ktx.storageMetadata
import kotlinx.coroutines.tasks.await
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseStorageManager @Inject constructor() {

    private val storage = Firebase.storage
    private val ticketsRef = storage.reference.child("tickets")

    suspend fun subirTicket(
        file: File,
        numeroVenta: String,
        clienteEmail: String,
        total: Double,
        fecha: String
    ): Result<String> {
        return try {
            val fileName = "ticket_$numeroVenta.pdf"
            val fileRef = ticketsRef.child(fileName)

            // Crear metadatos usando el DSL de Kotlin
            val metadata = storageMetadata {
                contentType = "application/pdf"
                setCustomMetadata("customerEmail", clienteEmail)  // ✅ era "email"
                setCustomMetadata("ticketNumber", numeroVenta)    // ✅ era "numeroVenta"
                setCustomMetadata("totalAmount", total.toString()) // ✅ era "total"
                setCustomMetadata("saleDate", fecha)              // ✅ era "fecha"
            }

            // Subir archivo
            val uri = Uri.fromFile(file)
            fileRef.putFile(uri, metadata).await()

            // Obtener URL de descarga
            val downloadUrl = fileRef.downloadUrl.await().toString()

            // Eliminar archivo temporal
            file.delete()

            Result.success(downloadUrl)
        } catch (e: Exception) {
            Log.e("FirebaseStorage", "Error al subir ticket: ${e.message}", e)
            Result.failure(e)
        }
    }
}
