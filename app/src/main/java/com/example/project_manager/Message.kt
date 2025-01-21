package com.example.project_manager

data class Message(
    val senderId: String = "",
    val text: String = "",
    val timestamp: Long = 0L
)