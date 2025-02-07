package com.example.project_manager.services

import android.util.Log
import com.example.project_manager.models.Chat
import com.example.project_manager.models.Message
import com.example.project_manager.repository.ChatRepository


class ChatService {
    val chatRepository = ChatRepository()
    val TAG = "ChatService"
    val userService= UserService()

    suspend fun getCurrentUserChats(): List<Chat> {
        val currentUserId = userService.getCurrentUserId().toString()
        return chatRepository.getUserChats(currentUserId)
    }

    suspend fun getChatMessages(chatId: String): List<Message> {
        return chatRepository.getChatMessages(chatId)
    }

    suspend fun sendMessage(chatId: String, messageText: String): Boolean {
        val currentUserId = userService.getCurrentUserId().toString()

        val message = Message(
            senderId = currentUserId,
            text = messageText,
            timestamp = System.currentTimeMillis()
        )

        return chatRepository.sendMessage(chatId, message)
    }

    suspend fun resetUnreadCounter(chatId: String): Boolean {
        return try {
            val currentUserId = userService.getCurrentUserId() ?: return false
            val chat = chatRepository.getChat(chatId) ?: return false
            if (chat.senderId == currentUserId) {
                return true
            }
            chatRepository.resetUnreadCounter(chatId)
        } catch (e: Exception) {
            Log.e("ChatService", "Error resetting unread counter", e)
            false
        }
    }

    suspend fun startChatWithUser(otherUserId: String): String? {
        val currentUserId = userService.getCurrentUserId().toString()

        // Generate unique chat ID
        val chatId = generateChatId(currentUserId, otherUserId)

        // Create new chat
        val chat = Chat(
            chatId = chatId,
            lastMessage = "",
            timestamp = System.currentTimeMillis(),
            unreadCount = 0,
            user1 = currentUserId,
            user2 = otherUserId
        )

        return if (chatRepository.createChat(chat)) chatId else null
    }

    private fun generateChatId(id1: String, id2: String): String {
        return listOf(id1, id2)
            .sorted()
            .joinToString("_")
    }
}