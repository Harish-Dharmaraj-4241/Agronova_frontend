package com.simats.agronova.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simats.agronova.model.ChemicalScanRequest
import com.simats.agronova.model.ChemicalScanResponse
import com.simats.agronova.service.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class ChemicalTranslatorViewModel : ViewModel() {
    val scanResult = mutableStateOf<ChemicalScanResponse?>(null)
    val isLoading = mutableStateOf(false)
    val errorMessage = mutableStateOf<String?>(null)

    fun analyzeChemicalLabel(context: Context, imageUri: Uri, language: String) {
        isLoading.value = true
        errorMessage.value = null
        scanResult.value = null

        viewModelScope.launch {
            try {
                val base64Image = withContext(Dispatchers.IO) { encodeImageToBase64(context, imageUri) }
                if (base64Image != null) {
                    val response = RetrofitClient.apiService.translateChemical(ChemicalScanRequest(base64Image, language))
                    if (response.isSuccessful && response.body() != null) {
                        scanResult.value = response.body()
                    } else {
                        errorMessage.value = "Failed to read label. Please ensure the text is clear."
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace() // This will print the exact error in your Android Studio Logcat
                // Temporarily display the exact system error on the phone screen so we can debug it
                errorMessage.value = "Error: ${e.message}"
            } finally {
                isLoading.value = false
            }
        }
    }

    private fun encodeImageToBase64(context: Context, imageUri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
        } catch (e: Exception) { null }
    }
}