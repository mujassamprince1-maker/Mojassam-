package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts")
data class Contact(
    @PrimaryKey val phone: String,
    val name: String,
    val username: String,
    val statusText: String = "Hey there! I am using Privora.",
    val avatarColor: Int, // Hex value or code
    val isSpam: Boolean = false,
    val isFake: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "chats")
data class Chat(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isGroup: Boolean = false,
    val isSecret: Boolean = false,
    val avatarColor: Int,
    val lastMessage: String = "",
    val lastMessageTime: Long = System.currentTimeMillis(),
    val unreadCount: Int = 0,
    val pinningOrder: Int = 0 // 0 means unpinned
)

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val chatId: Long,
    val senderPhone: String, // Empty means system or self
    val senderName: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "SENT", // "SENT" (single tick), "DELIVERED" (double tick), "READ" (blue double tick)
    val type: String = "TEXT", // "TEXT", "IMAGE", "VOICE", "FILE"
    val mediaUri: String? = null,
    val reaction: String? = null,
    val isPinned: Boolean = false,
    val isSelfDestruct: Boolean = false,
    val selfDestructLimit: Int = 0, // In seconds, 0 means no limit
    val selfDestructTriggeredAt: Long = 0,
    val isScheduled: Boolean = false,
    val scheduledAt: Long = 0,
    val translatedText: String? = null
)

@Entity(tableName = "stories")
data class Story(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val phone: String,
    val name: String,
    val textContent: String,
    val bgHex: Int,
    val mediaUri: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "calls")
data class CallRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String,
    val avatarColor: Int,
    val isVideo: Boolean = false,
    val isIncoming: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val duration: Long = 0 // in seconds
)
