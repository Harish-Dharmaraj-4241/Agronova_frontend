package com.simats.agronova.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.simats.agronova.database.ChatDatabase
import com.simats.agronova.database.ChatEntity
import com.simats.agronova.service.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

// Updated ChatMessage model to include IDs and Reply references
data class ChatMessage(
    val id: Int = 0,
    val text: String,
    val isUser: Boolean,
    val timestampLabel: String,
    val attachedImageUri: Uri? = null,
    val replyToMessage: String? = null
)

class AssistantViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AuthRepository()
    private val chatDao = ChatDatabase.getDatabase(application).chatDao()

    val chatHistory = mutableStateListOf<ChatMessage>()
    var isAiTyping = mutableStateOf(false)
    var latestVoiceResponse = mutableStateOf<String?>(null)

    var selectedLanguage = mutableStateOf("English")
    val availableLanguages = listOf("English", "Tamil", "Hindi", "Telugu", "Malayalam", "Kannada")

    init {
        // Automatically listen to local Room database for chat history
        viewModelScope.launch {
            chatDao.getAllMessages().collectLatest { entities ->
                chatHistory.clear()
                chatHistory.addAll(entities.map {
                    ChatMessage(
                        id = it.id,
                        text = it.text,
                        isUser = it.isUser,
                        timestampLabel = it.timestampLabel,
                        attachedImageUri = it.attachedImageUri?.let { uriStr -> Uri.parse(uriStr) },
                        replyToMessage = it.replyToMessage
                    )
                })

                // Add default greeting ONLY if the database is completely empty
                if (chatHistory.isEmpty()) {
                    val greeting = ChatEntity(
                        text = "Hello! I am AgroNova AI. How can I help your farm today?",
                        isUser = false,
                        timestampLabel = getCurrentTime()
                    )
                    chatDao.insertMessage(greeting)
                }
            }
        }
    }

    fun sendMessage(context: Context, userText: String, imageUri: Uri?, replyTo: String?) {
        val textToSend = userText.ifEmpty { "Analyze this image." }
        val time = getCurrentTime()

        viewModelScope.launch {
            // 1. Save user message to Local Database instantly
            val userEntity = ChatEntity(
                text = textToSend,
                isUser = true,
                timestampLabel = time,
                attachedImageUri = imageUri?.toString(),
                replyToMessage = replyTo
            )
            chatDao.insertMessage(userEntity)
            isAiTyping.value = true

            // Grab location from SharedPreferences
            val prefs = context.getSharedPreferences("AgroNovaPrefs", Context.MODE_PRIVATE)
            val location = prefs.getString("FARM_LOCATION", "Tap to set location") ?: "Tap to set location"

            try {
                var base64Image: String? = null
                if (imageUri != null) {
                    base64Image = withContext(Dispatchers.IO) { encodeImageToBase64(context, imageUri) }
                }

                // FIX: Inject location directly into the prompt so AI can't ignore it
                val systemContext = if (location != "Tap to set location") "Location: $location. " else "Location: Not provided yet. "
                val promptWithContext = systemContext + (if (replyTo != null) "In reply to: \"$replyTo\". User says: $textToSend" else textToSend)

                val response = repository.askAssistant(promptWithContext, selectedLanguage.value, location, base64Image)
                if (response.isSuccessful && response.body() != null) {
                    val aiReply = response.body()?.reply ?: "I couldn't process that."

                    // Save AI Reply to Local Database
                    val aiEntity = ChatEntity(
                        text = aiReply,
                        isUser = false,
                        timestampLabel = getCurrentTime()
                    )
                    chatDao.insertMessage(aiEntity)
                    latestVoiceResponse.value = aiReply
                } else {
                    chatDao.insertMessage(ChatEntity(text = "Sorry, the server is unreachable right now.", isUser = false, timestampLabel = "SYSTEM ERROR"))
                }
            } catch (e: Exception) {
                chatDao.insertMessage(ChatEntity(text = "Network Error: Make sure your Flask server is running.", isUser = false, timestampLabel = "SYSTEM ERROR"))
            } finally {
                isAiTyping.value = false
            }
        }
    }

    fun deleteSelectedMessages(messageIds: List<Int>) {
        viewModelScope.launch(Dispatchers.IO) {
            val entitiesToDelete = messageIds.map { ChatEntity(id = it, text = "", isUser = false, timestampLabel = "") }
            chatDao.deleteMessages(entitiesToDelete)
        }
    }

    fun deleteAllMessages() {
        viewModelScope.launch(Dispatchers.IO) {
            chatDao.deleteAllMessages()
            val greeting = ChatEntity(
                text = "Hello! I am AgroNova AI. How can I help your farm today?",
                isUser = false,
                timestampLabel = getCurrentTime()
            )
            chatDao.insertMessage(greeting)
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