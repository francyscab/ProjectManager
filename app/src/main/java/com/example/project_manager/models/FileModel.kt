package com.example.project_manager.models

data class FileModel(
    val name: String,
    val downloadUrl: String,
    val uploadedAt: Long,
    val size: Long
)