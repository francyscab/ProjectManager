package com.example.project_manager.repository

import android.content.ContentValues.TAG
import android.util.Log
import com.example.project_manager.models.ItemsViewModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ProjectRepository {
    val db = FirebaseFirestore.getInstance()

    suspend fun uploadProject(projectData: Map<String, Any>): String {
        return try {
            val documentReference = db.collection("progetti").add(projectData).await()
            documentReference.id
        } catch (e: Exception) {
            Log.e(TAG, "Error creating project", e)
            throw e
        }
    }

    suspend fun loadProjectData(): ArrayList<ItemsViewModel> {
        val data = ArrayList<ItemsViewModel>()
        try {
            val result = db.collection("progetti").get().await()
            for (document in result) {
                val creator = document.getString("creator") ?: ""
                val title = document.getString("title") ?: ""
                val leader = document.getString("assignedTo") ?: ""
                val deadline = document.getString("deadline") ?: ""
                val priority = document.getString("priority") ?: ""
                val description = document.getString("description") ?: ""
                val progress = document.getLong("progress")?.toInt() ?: 0
                val comment = document.getString("comment") ?: ""
                val rating = document.getLong("rating")?.toInt() ?: 0
                val valutato = document.getBoolean("valutato") ?: false
                val createdAt = document.getLong("createdAt")?: 0
                val completedAt = document.getLong("completedAt") ?:-1
                val sollecitato = document.getBoolean("sollecitato") ?: false

                data.add(ItemsViewModel(title, leader,creator, deadline,priority,description,progress,comment,rating,valutato,createdAt, completedAt,sollecitato,document.id))
            }
        } catch (exception: Exception) {
            Log.w(TAG, "Error getting project.", exception)
        }
        return data
    }

    public suspend fun getProjectProgress(projectId: String): Int {
        return try {
            val projectDoc = db.collection("progetti")
                .document(projectId)
                .get()
                .await()

            if (projectDoc.exists()) {
                projectDoc.getLong("progress")?.toInt() ?: 0
            } else {
                Log.w("Firestore", "Project non trovato: $projectId")
                throw NoSuchElementException("Project non trovato con ID: $projectId")
            }
        } catch (e: Exception) {
            Log.e("Firestore", "Errore nel caricamento del progresso del project $projectId", e)
            throw e
        }
    }

    public suspend fun getProjectById(projectId: String): ItemsViewModel? {
        Log.d(TAG, "Attempting to fetch project with ID: $projectId")

        return try {
            // Verifica che l'ID non sia vuoto
            if (projectId.isBlank()) {
                Log.e(TAG, "Project ID is blank or empty")
                return null
            }

            // Aggiungi log per debuggare
            val documentRef = db.collection("progetti").document(projectId)
            Log.d(TAG, "Document reference created for path: ${documentRef.path}")

            val document = documentRef.get().await()
            Log.d(TAG, "Document exists: ${document.exists()}")

            if (document.exists()) {
                val creator = document.getString("creator") ?: ""
                val title = document.getString("title") ?: ""
                val assignedTo = document.getString("assignedTo") ?: ""
                val deadline = document.getString("deadline") ?: ""
                val priority = document.getString("priority") ?: ""
                val description = document.getString("description") ?: ""
                val progress = document.getLong("progress")?.toInt() ?: 0
                val comment = document.getString("comment") ?: ""
                val rating = document.getLong("rating")?.toInt() ?: 0
                val valutato = document.getBoolean("valutato") ?: false
                val createdAt = document.getLong("createdAt") ?: 0
                val completedAt = document.getLong("completedAt") ?: -1
                val sollecitato = document.getBoolean("sollecitato") ?: false

                // Log dei dati recuperati
                Log.d(TAG, """
                Project data retrieved:
                - Title: $title
                - Creator: $creator
                - AssignedTo: $assignedTo 
                - Progress: $progress
            """.trimIndent())

                ItemsViewModel(
                    title = title,
                    assignedTo = assignedTo,
                    creator = creator,
                    deadline = deadline,
                    priority = priority,
                    description = description,
                    progress = progress,
                    comment = comment,
                    rating = rating,
                    valutato = valutato,
                    createdAt = createdAt,
                    completedAt = completedAt,
                    sollecitato = sollecitato,
                    projectId = document.id
                )
            } else {
                Log.w(TAG, "No document found with ID: $projectId")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting project with ID: $projectId", e)
            null
        }
    }


    suspend fun updateProjectProgress(projectId: String, progress: Int): Boolean {
        return try {
            db.collection("progetti")
                .document(projectId)
                .update("progress", progress)
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating project progress", e)
            false
        }
    }

    suspend fun updateProject(projectId: String, updates: Map<String, Any>) {
        try {
            db.collection("progetti")
                .document(projectId)
                .update(updates)
                .await()
            Log.d(TAG, "Project updated successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating project", e)
            throw e
        }
    }

    suspend fun saveFeedback(projectId: String, rating: Int, comment: String): Boolean {
        return try {
            val feedbackData = mapOf(
                "rating" to rating,
                "comment" to comment,
                "valutato" to true
            )
            db.collection("progetti")
                .document(projectId)
                .update(feedbackData)
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving project feedback", e)
            false
        }
    }

    suspend fun getFeedback(projectId: String): Triple<Int, String, Boolean>? {
        return try {
            val doc = db.collection("progetti")
                .document(projectId)
                .get()
                .await()

            if (doc.exists()) {
                Triple(
                    doc.getLong("rating")?.toInt() ?: 0,
                    doc.getString("comment") ?: "",
                    doc.getBoolean("valutato") ?: false
                )
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting project feedback", e)
            null
        }

    }

    suspend fun deleteProject(projectId: String): Boolean {
        return try {
            db.collection("progetti")
                .document(projectId)
                .delete()
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting project", e)
            false
        }
    }

    suspend fun sollecita(projectId: String,valore :Boolean): Boolean {
        return try {
            db.collection("progetti")
                .document(projectId)
                .update("sollecitato", valore)
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating project progress", e)
            false
        }
    }

}