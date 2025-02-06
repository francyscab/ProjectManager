package com.example.project_manager.repository

import android.content.ContentValues.TAG
import android.util.Log
import com.example.project_manager.models.ItemsViewModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class SubTaskRepository {

    val projectRepository = ProjectRepository()
    val db = FirebaseFirestore.getInstance()

    suspend fun updateSubTaskProgress(projectId: String, taskId: String, subtaskId: String, progress: Int): Boolean {
        return try {
            val collectionPath = "progetti/$projectId/task/$taskId/subtask"
            db.collection(collectionPath)
                .document(subtaskId)
                .update("progress", progress)
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating subtask progress", e)
            false
        }
    }

    suspend fun getSubTaskProgress(projectId: String, taskId: String, subtaskId: String): Int {
        return try {
            val document = db.collection("progetti/$projectId/task/$taskId/subtask")
                .document(subtaskId)
                .get()
                .await()

            document.getLong("progress")?.toInt() ?: 0
        } catch (e: Exception) {
            Log.e(TAG, "Error getting subtask progress", e)
            0
        }
    }

    suspend fun loadAllSubtaskByTaskId(
        projectId: String,
        taskId: String
    ): ArrayList<ItemsViewModel> {
        val subTasks = ArrayList<ItemsViewModel>()

        try {
            val projectDocument =
                db.collection("progetti").document(projectId).collection("task").document(taskId)
                    .get().await()
            if (!projectDocument.exists()) {
                Log.w(TAG, "Project not found: $projectId")
                return subTasks
            }

            val subTaskDocuments =
                db.collection("progetti").document(projectId).collection("task").document(taskId)
                    .collection("subtask")
                    .get().await()

            for (document in subTaskDocuments) {
                val title = document.getString("title") ?: ""
                val assignedTo = document.getString("assignedTo") ?: ""
                val creator = document.getString("creator") ?: ""
                val deadline = document.getString("deadline") ?: ""
                val priority = document.getString("priority") ?: ""
                val description = document.getString("description") ?: ""
                val progress = document.getLong("progress")?.toInt() ?: 0
                val comment = document.getString("comment") ?: ""
                val rating = document.getLong("rating")?.toInt() ?: 0
                val valutato = document.getBoolean("valutato") ?: false
                val createdAt = document.getLong("createdAt")?:0
                val completedAt = document.getLong("completedAt")?:-1
                val sollecitato = document.getBoolean("sollecitato") ?: false

                subTasks.add(
                    ItemsViewModel(
                        title,
                        assignedTo,
                        creator,
                        deadline,
                        priority,
                        description,
                        progress,
                        comment,
                        rating,
                        valutato,
                        createdAt,
                        completedAt,
                        sollecitato,
                        projectId,
                        taskId,
                        document.id
                    )
                )
            }
        } catch (exception: Exception) {
            Log.e(TAG, "Error getting subtask for task: $taskId", exception)
        }
        return subTasks
    }

    public suspend fun loadSubTaskById(
        projectId: String,
        taskId: String,
        subtaskId: String
    ): ItemsViewModel {
        return try {
            val subtaskDoc = db.collection("progetti")
                .document(projectId)
                .collection("task")
                .document(taskId)
                .collection("subtask")
                .document(subtaskId)
                .get()
                .await()

            if (subtaskDoc.exists()) {
                val title = subtaskDoc.getString("title") ?: ""
                val assignedTo = subtaskDoc.getString("assignedTo") ?: ""
                val creator = subtaskDoc.getString("creator") ?: ""
                val deadline = subtaskDoc.getString("deadline") ?: ""
                val priority = subtaskDoc.getString("priority") ?: ""
                val description = subtaskDoc.getString("description") ?: ""
                val progress = subtaskDoc.getLong("progress")?.toInt() ?: 0
                val comment = subtaskDoc.getString("comment") ?: ""
                val rating = subtaskDoc.getLong("rating")?.toInt() ?: 0
                val valutato = subtaskDoc.getBoolean("valutato") ?: false
                val createdAt = subtaskDoc.getLong("createdAt")?:0
                val completedAt = subtaskDoc.getLong("completedAt")?:-1
                val sollecitato = subtaskDoc.getBoolean("sollecitato") ?: false

                ItemsViewModel(
                    title,
                    assignedTo,
                    creator,
                    deadline,
                    priority,
                    description,
                    progress,
                    comment,
                    rating,
                    valutato,
                    createdAt,
                    completedAt,
                    sollecitato,
                    projectId,
                    subtaskDoc.id
                )
            } else {
                Log.w("Firestore", "Subtask non trovato: $subtaskId")
                throw NoSuchElementException("Subtask non trovato con ID: $subtaskId")
            }
        } catch (e: Exception) {
            Log.e("Firestore", "Errore nel caricamento del Subtask $subtaskId", e)
            throw e
        }
    }

    suspend fun uploadSubTask(projectId: String, taskId: String, subtaskData: Map<String, Any>): String {
        return try {
            val documentReference = db.collection("progetti")
                .document(projectId)
                .collection("task")
                .document(taskId)
                .collection("subtask")
                .add(subtaskData)
                .await()
            documentReference.id
        } catch (e: Exception) {
            Log.e(TAG, "Error creating subtask", e)
            throw e
        }
    }
}






