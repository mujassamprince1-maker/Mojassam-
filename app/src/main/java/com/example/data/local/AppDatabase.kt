package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chats ORDER BY pinningOrder DESC, lastMessageTime DESC")
    fun getAllChats(): Flow<List<Chat>>

    @Query("SELECT * FROM chats WHERE id = :chatId")
    suspend fun getChatById(chatId: Long): Chat?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: Chat): Long

    @Update
    suspend fun updateChat(chat: Chat)

    @Query("DELETE FROM chats WHERE id = :chatId")
    suspend fun deleteChatById(chatId: Long)

    @Query("SELECT * FROM messages WHERE chatId = :chatId AND isScheduled = 0 ORDER BY timestamp ASC")
    fun getMessagesForChat(chatId: Long): Flow<List<Message>>

    @Query("SELECT * FROM messages WHERE isScheduled = 1 ORDER BY scheduledAt ASC")
    fun getScheduledMessages(): Flow<List<Message>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message): Long

    @Update
    suspend fun updateMessage(message: Message)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteMessageById(id: Long)

    @Query("DELETE FROM messages WHERE chatId = :chatId")
    suspend fun clearMessagesForChat(chatId: Long)

    @Query("SELECT * FROM contacts ORDER BY name ASC")
    fun getAllContacts(): Flow<List<Contact>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: Contact)

    @Update
    suspend fun updateContact(contact: Contact)

    @Query("DELETE FROM contacts WHERE phone = :phone")
    suspend fun deleteContactByPhone(phone: String)

    @Query("SELECT * FROM stories ORDER BY timestamp DESC")
    fun getAllStories(): Flow<List<Story>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStory(story: Story)

    @Query("DELETE FROM stories WHERE timestamp < :expiryTime")
    suspend fun deleteExpiredStories(expiryTime: Long)

    @Query("SELECT * FROM calls ORDER BY timestamp DESC")
    fun getAllCallRecords(): Flow<List<CallRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallRecord(callRecord: CallRecord)

    @Query("DELETE FROM calls")
    suspend fun clearCallHistory()
}

@Database(
    entities = [Contact::class, Chat::class, Message::class, Story::class, CallRecord::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
}
