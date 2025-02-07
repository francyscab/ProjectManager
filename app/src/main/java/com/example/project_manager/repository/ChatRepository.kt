package com.example.project_manager.repository

import android.util.Log
import com.example.project_manager.models.Chat
import com.example.project_manager.models.Message
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class ChatRepository {
    private val db = FirebaseFirestore.getInstance()
    private val TAG = "ChatRepository"

    suspend fun getChat(chatId: String): Chat? {
        return try {
            val doc = db.collection("chat")
                .document(chatId)
                .get()
                .await()

            if (doc.exists()) {
                Chat(
                    chatId = doc.getString("chatID") ?: "",
                    lastMessage = doc.getString("lastMessage") ?: "",
                    timestamp = doc.getLong("timestamp") ?: 0L,
                    unreadCount = doc.getLong("unreadCount")?.toInt() ?: 0,
                    senderId = doc.getString("senderId") ?: "",
                    user1 = doc.getString("user1") ?: "",
                    user2 = doc.getString("user2") ?: ""
                )
            } else null
        } catch (e: Exception) {
            Log.w(TAG, "Error retrieving chat", e)
            null
        }
    }

    suspend fun getUserChats(userId: String): List<Chat> {
        return try {
            val chatsAsUser1 = db.collection("chat")
                .whereEqualTo("user1", userId)
                .get()
                .await()

            val chatsAsUser2 = db.collection("chat")
                .whereEqualTo("user2", userId)
                .get()
                .await()

            val allChats = mutableListOf<Chat>()

            for (doc in chatsAsUser1.documents + chatsAsUser2.documents) {
                Chat(
                    chatId = doc.getString("chatId") ?: "",
                    lastMessage = doc.getString("lastMessage") ?: "",
                    timestamp = doc.getLong("timestamp") ?: 0L,
                    unreadCount = doc.getLong("unreadCount")?.toInt() ?: 0,
                    senderId = doc.getString("senderId") ?: "",
                    user1 = doc.getString("user1") ?: "",
                    user2 = doc.getString("user2") ?: ""
                ).let { allChats.add(it) }
            }

            allChats.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            Log.w(TAG, "Error retrieving user chats", e)
            emptyList()
        }
    }

    suspend fun getChatMessages(chatId: String): List<Message> {
        return try {
            db.collection("chat")
                .document(chatId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    Message(
                        senderId = doc.getString("senderId") ?: return@mapNotNull null,
                        text = doc.getString("text") ?: return@mapNotNull null,
                        timestamp = doc.getLong("timestamp") ?: return@mapNotNull null
                    )
                }
        } catch (e: Exception) {
            Log.w(TAG, "Error retrieving chat messages", e)
            emptyList()
        }
    }

    suspend fun sendMessage(chatId: String, message: Message): Boolean {
        return try {
            // Get current chat data
            val chatDoc = db.collection("chat")
                .document(chatId)
                .get()
                .await()

            val recipientId = if (chatDoc.getString("user1") == message.senderId) {
                chatDoc.getString("user2")
            } else {
                chatDoc.getString("user1")
            }

            // Add message to messages collection
            db.collection("chat")
                .document(chatId)
                .collection("messages")
                .add(message)
                .await()

            // Update chat's last message and increment unread counter
            db.collection("chat")
                .document(chatId)
                .update(
                    mapOf(
                        "lastMessage" to message.text,
                        "timestamp" to message.timestamp,
                        "senderId" to message.senderId,
                        "unreadCount" to FieldValue.increment(1)  // Increment unread counter
                    )
                )
                .await()

            true
        } catch (e: Exception) {
            Log.w(TAG, "Error sending message", e)
            false
        }
    }

    suspend fun resetUnreadCounter(chatId: String): Boolean {
        return try {
            db.collection("chat")
                .document(chatId)
                .update("unreadCount", 0)
                .await()
            true
        } catch (e: Exception) {
            Log.w(TAG, "Error resetting unread counter", e)
            false
        }
    }

    suspend fun createChat(chat: Chat): Boolean {
        return try {
            db.collection("chat")
                .document(chat.chatId)
                .set(chat)
                .await()
            true
        } catch (e: Exception) {
            Log.w(TAG, "Error creating chat", e)
            false
        }
    }
}