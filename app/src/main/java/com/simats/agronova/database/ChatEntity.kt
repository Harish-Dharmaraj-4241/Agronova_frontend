package com.simats.agronova.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val text: String,
    val isUser: Boolean,
    val timestampLabel: String,
    val attachedImageUri: String? = null,
    val replyToMessage: String? = null // New: To store the text you swiped to reply to!
)