package com.example.project_manager.models

import android.media.audiofx.AudioEffect.Descriptor

data class ItemsViewModel(
    val title: String,
    val assignedTo: String,
    val creator: String,
    val deadline: String,
    val priority: String,
    val description: String,
    val progress: Int,
    val comment: String,
    val rating: Int,
    val valutato: Boolean,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long,
    val sollecitato: Boolean? = false,
    val projectId: String,
    val taskId: String? = null,
    val subtaskId: String? = null, // Changed to optional String
) {
}