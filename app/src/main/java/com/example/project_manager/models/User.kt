package com.example.project_manager.models



data class User(
    val name: String = "",
    val surname: String = "",
    val email: String = "",
    val role: Role = Role.Developer, // Valore predefinito per evitare errori di null
    val uid: String = "",
    val profile_image_url: String = ""
)

