package com.example.project_manager.repository

import android.content.ContentValues.TAG
import android.util.Log
import com.example.project_manager.models.ItemsViewModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class TaskRepository {

    val db=FirebaseFirestore.getInstance()
    val projectRepository = ProjectRepository()

    suspend fun uploadTask(projectId: String, taskData: Map<String, Any>): String {
        return try {
            val documentReference = db.collection("progetti")
                .document(projectId)
                .collection("task")
                .add(taskData)
                .await()
            documentReference.id
        } catch (e: Exception) {
            Log.e(TAG, "Error creating task", e)
            throw e
        }
    }

    suspend fun loadAllTaskByProjectId(projectId: String): ArrayList<ItemsViewModel> {
        val tasks = ArrayList<ItemsViewModel>()

        try {
            val projectDocument = db.collection("progetti").document(projectId).get().await()
            if (!projectDocument.exists()) {
                Log.w(TAG, "Project not found: $projectId")
                return tasks // Se il progetto non esiste, restituisce una lista vuota
            }

            // Recupera tutti i task nella sottocollezione "tasks" del progetto
            val taskDocuments = db.collection("progetti").document(projectId)
                .collection("task").get().await()

            for (document in taskDocuments) {
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

                tasks.add(ItemsViewModel(title, assignedTo,creator, deadline, priority,description,progress ,comment,rating,valutato, createdAt, completedAt,sollecitato,projectId, document.id))
            }
        } catch (exception: Exception) {
            Log.e(TAG, "Error getting tasks for project: $projectId", exception)
        }
        return tasks
    }

    suspend fun updateTaskProgress(projectId: String, taskId: String, progress: Int): Boolean {
        return try {
            db.collection("progetti")
                .document(projectId)
                .collection("task")
                .document(taskId)
                .update("progress", progress)
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating task progress", e)
            false
        }
    }

    public suspend fun loadTaskById(taskId: String, projectId: String): ItemsViewModel {
        return try {
            val taskDoc = db.collection("progetti")
                .document(projectId)
                .collection("task")
                .document(taskId)
                .get()
                .await()

            if (taskDoc.exists()) {
                val title = taskDoc.getString("title") ?: ""
                val assignedTo = taskDoc.getString("assignedTo") ?: ""
                val creator = taskDoc.getString("creator") ?: ""
                val deadline = taskDoc.getString("deadline") ?: ""
                val priority = taskDoc.getString("priority") ?: ""
                val description = taskDoc.getString("description") ?: ""
                val progress = taskDoc.getLong("progress")?.toInt() ?: 0
                val comment = taskDoc.getString("comment") ?: ""
                val rating = taskDoc.getLong("rating")?.toInt() ?: 0
                val valutato = taskDoc.getBoolean("valutato") ?: false
                val createdAt = taskDoc.getLong("createdAt")?:0
                val completedAt = taskDoc.getLong("completedAt")?:-1
                val sollecitato = taskDoc.getBoolean("sollecitato") ?: false

                ItemsViewModel(title, assignedTo, creator, deadline, priority,description, progress,comment,rating,valutato,createdAt,completedAt,sollecitato, projectId, taskDoc.id)
            } else {
                Log.w("Firestore", "Task non trovato: $taskId")
                throw NoSuchElementException("Task non trovato con ID: $taskId")
            }
        } catch (e: Exception) {
            Log.e("Firestore", "Errore nel caricamento del task $taskId", e)
            throw e
        }
    }

    public suspend fun getTaskProgress(taskId: String, projectId: String): Int {
        return try {
            val taskDoc = db.collection("progetti")
                .document(projectId)
                .collection("task")
                .document(taskId)
                .get()
                .await()

            if (taskDoc.exists()) {
                taskDoc.getLong("progress")?.toInt() ?: 0
            } else {
                Log.w("Firestore", "Task non trovato: $taskId")
                throw NoSuchElementException("Task non trovato con ID: $taskId")
            }
        } catch (e: Exception) {
            Log.e("Firestore", "Errore nel caricamento del progresso del task $taskId", e)
            throw e
        }
    }


    suspend fun updateTask(projectId: String, taskId: String, updates: Map<String, Any>) {
        try {
            db.collection("progetti")
                .document(projectId)
                .collection("task")
                .document(taskId)
                .update(updates)
                .await()
            Log.d(TAG, "Task updated successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating task", e)
            throw e
        }
    }

    suspend fun getMyTasks(userId: String): ArrayList<ItemsViewModel> {
        val tasks = ArrayList<ItemsViewModel>()

        try {
            // Get all projects
            val projects = projectRepository.loadProjectData()

            // For each project, get its tasks
            for (project in projects) {
                val projectTasks = db.collection("progetti")
                    .document(project.projectId)
                    .collection("task")
                    .get()
                    .await()

                // Filter tasks assigned to the user
                for (taskDoc in projectTasks) {
                    val assignedTo = taskDoc.getString("assignedTo") ?: ""
                    if (assignedTo == userId) {
                        val task = ItemsViewModel(
                            title = taskDoc.getString("title") ?: "",
                            assignedTo = assignedTo,
                            creator = taskDoc.getString("creator") ?: "",
                            deadline = taskDoc.getString("deadline") ?: "",
                            priority = taskDoc.getString("priority") ?: "",
                            description = taskDoc.getString("description") ?: "",
                            progress = taskDoc.getLong("progress")?.toInt() ?: 0,
                            comment = taskDoc.getString("comment") ?: "",
                            rating = taskDoc.getLong("rating")?.toInt() ?: 0,
                            valutato = taskDoc.getBoolean("valutato") ?: false,
                            createdAt = taskDoc.getLong("createdAt") ?: 0,
                            completedAt = taskDoc.getLong("completedAt") ?: -1,
                            sollecitato = taskDoc.getBoolean("sollecitato") ?: false,
                            projectId = project.projectId,
                            taskId = taskDoc.id

                        )
                        tasks.add(task)
                    }
                }
            }

            Log.d(TAG, "Found ${tasks.size} tasks for user $userId")

        } catch (e: Exception) {
            Log.e(TAG, "Error getting tasks for user $userId", e)
            // Return empty list in case of error
        }

        return tasks
    }

    suspend fun saveFeedback(projectId: String, taskId: String, rating: Int, comment: String): Boolean {
        return try {
            val feedbackData = mapOf(
                "rating" to rating,
                "comment" to comment,
                "valutato" to true
            )
            db.collection("progetti")
                .document(projectId)
                .collection("task")
                .document(taskId)
                .update(feedbackData)
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving task feedback", e)
            false
        }
    }

    suspend fun getFeedback(projectId: String, taskId: String): Triple<Int, String, Boolean>? {
        return try {
            val doc = db.collection("progetti")
                .document(projectId)
                .collection("task")
                .document(taskId)
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
            Log.e(TAG, "Error getting task feedback", e)
            null
        }
    }

    suspend fun deleteTask(projectId: String, taskId: String): Boolean {
        return try {
            db.collection("progetti")
                .document(projectId)
                .collection("task")
                .document(taskId)
                .delete()
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting task", e)
            false
        }
    }
}



