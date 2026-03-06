package com.simats.agronova.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simats.agronova.service.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestampLabel: String,
    val attachedImageUri: Uri? = null
)

class AssistantViewModel : ViewModel() {
    private val repository = AuthRepository()

    val chatHistory = mutableStateListOf<ChatMessage>()
    var isAiTyping = mutableStateOf(false)
    var latestVoiceResponse = mutableStateOf<String?>(null)

    var selectedLanguage = mutableStateOf("English")
    val availableLanguages = listOf("English", "Tamil", "Hindi", "Telugu", "Malayalam", "Kannada")

    init {
        chatHistory.add(ChatMessage("Hello! I am AgroNova AI. How can I help your farm today?", false, getCurrentTime()))
    }

    fun sendMessage(context: Context, userText: String, imageUri: Uri?) {
        val textToSend = userText.ifEmpty { "Analyze this image." }
        chatHistory.add(ChatMessage(textToSend, true, "YOU • ${getCurrentTime()}", imageUri))
        isAiTyping.value = true

        // Grab location from SharedPreferences
        val prefs = context.getSharedPreferences("AgroNovaPrefs", Context.MODE_PRIVATE)
        val location = prefs.getString("FARM_LOCATION", "")

        viewModelScope.launch {
            try {
                var base64Image: String? = null
                if (imageUri != null) {
                    base64Image = withContext(Dispatchers.IO) { encodeImageToBase64(context, imageUri) }
                }

                val response = repository.askAssistant(textToSend, selectedLanguage.value, location, base64Image)
                if (response.isSuccessful && response.body() != null) {
                    val aiReply = response.body()?.reply ?: "I couldn't process that."
                    chatHistory.add(ChatMessage(aiReply, false, "AGRONOVA AI • ${getCurrentTime()}"))
                    latestVoiceResponse.value = aiReply
                } else {
                    chatHistory.add(ChatMessage("Sorry, the server is unreachable right now.", false, "SYSTEM ERROR"))
                }
            } catch (e: Exception) {
                chatHistory.add(ChatMessage("Network Error: Make sure your Flask server is running.", false, "SYSTEM ERROR"))
            } finally {
                isAiTyping.value = false
            }
        }
    }

    private fun encodeImageToBase64(context: Context, imageUri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            val byteArray = outputStream.toByteArray()
            Base64.encodeToString(byteArray, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getCurrentTime(): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return sdf.format(Date())
    }
}