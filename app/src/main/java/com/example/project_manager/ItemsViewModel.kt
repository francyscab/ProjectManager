package com.example.project_manager

data class ItemsViewModel(
    val text: String,
    val leader: String,
    val assegnato: Boolean,
    val projectId: String,
    val taskId: String? = null, // Changed to optional String
    val subtaskId: String? = null, // Changed to optional String
) {
}