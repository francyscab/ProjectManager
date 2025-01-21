package com.example.project_manager

data class Chat(
    val chatId: String = "",
    val lastMessage: String = "",
    val timestamp: Long = 0L,
    val unreadCount: Int = 0,
    val user1: String = "",
    val user2: String = ""
)