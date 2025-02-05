package com.example.project_manager.utils

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

                tasks.add(ItemsViewModel(title, assignedTo,creator, deadline, priority,description,progress ,comment,rating, projectId, document.id))
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

                ItemsViewModel(title, assignedTo, creator, deadline, priority,description, progress,comment,rating, projectId, taskDoc.id)
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



}