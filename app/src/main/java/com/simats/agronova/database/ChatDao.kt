package com.simats.agronova.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages ORDER BY id ASC")
    fun getAllMessages(): Flow<List<ChatEntity>>

    @Insert
    suspend fun insertMessage(message: ChatEntity)

    @Delete
    suspend fun deleteMessages(messages: List<ChatEntity>)

    @Query("DELETE FROM chat_messages")
    suspend fun deleteAllMessages()
}