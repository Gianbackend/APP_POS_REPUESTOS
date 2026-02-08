package com.example.posapp.data.firebase

import android.net.Uri
import android.util.Log
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.ktx.storage
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

            // ✅ Crear metadatos sin el DSL deprecated
            val metadata = StorageMetadata.Builder()
                .setContentType("application/pdf")
                .setCustomMetadata("customerEmail", clienteEmail)
                .setCustomMetadata("ticketNumber", numeroVenta)
                .setCustomMetadata("totalAmount", total.toString())
                .setCustomMetadata("saleDate", fecha)
                .build()

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
