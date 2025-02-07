package com.example.project_manager.services

import android.content.ContentValues.TAG
import android.util.Log
import com.example.project_manager.models.ItemsViewModel
import com.example.project_manager.repository.ProjectRepository
import com.example.project_manager.repository.SubTaskRepository
import com.example.project_manager.repository.TaskRepository

class SubTaskService {
    val projectService = ProjectService()
    val taskRepository = TaskRepository()
    val projectRepository = ProjectRepository()
    val subtaskRepository = SubTaskRepository()
    val taskService = TaskService()


    suspend fun getSubTaskById(projectId: String, taskId: String, subtaskId: String): ItemsViewModel? {
        return subtaskRepository.loadSubTaskById(projectId, taskId,subtaskId)
    }

    suspend fun getSubTaskProgress(projectId: String, taskId: String, subtaskId: String): Int {
        return subtaskRepository.getSubTaskProgress(projectId, taskId, subtaskId)
    }

    suspend fun updateSubTaskProgress(projectId: String, taskId: String, subtaskId: String, progress: Int): Boolean {
        return try{
            subtaskRepository.updateSubTaskProgress(projectId, taskId, subtaskId, progress)
            taskService.updateTaskProgress(projectId, taskId)
        } catch (e: Exception) {
            Log.e(TAG, "Error in subtask progress update", e)
            false
        }
    }

    suspend fun uploadNewSubTask(
        projectId: String,
        taskId: String,
        title: String,
        description: String,
        deadline: String,
        priority: String,
        creator: String,
        assignedTo: String
    ): String {
        val subtaskData = mapOf(
            "title" to title.capitalizeFirstLetter(),
            "description" to description,
            "deadline" to deadline,
            "priority" to priority,
            "creator" to creator,
            "assignedTo" to assignedTo,
            "progress" to 0,
            "valutato" to false,
            "createdAt" to System.currentTimeMillis(),
            "completedAt" to -1,
            "sollecitato" to false
        )
        return try {
            subtaskRepository.uploadSubTask(projectId, taskId, subtaskData)
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading new subtask", e)
            throw e
        }
    }

    public suspend fun getAllSubTaskByTaskId(projectId: String, taskId: String): ArrayList<ItemsViewModel> {
        return subtaskRepository.loadAllSubtaskByTaskId(projectId, taskId)
    }

    // SubTaskService
    suspend fun deleteSubTask(projectId: String, taskId: String, subtaskId: String): Boolean {
        return try {
            // Delete subtask
            val success = subtaskRepository.deleteSubTask(projectId, taskId, subtaskId)
            if (success) {
                // Update task progress after subtask deletion
                taskService.updateTaskProgress(projectId, taskId)
            }
            success
        } catch (e: Exception) {
            Log.e(TAG, "Error in subtask deletion", e)
            false
        }
    }

    suspend fun updateSubTask(projectId: String, taskId: String, subtaskId: String,title: String, description: String, developer: String? = null) {
        val updates = mutableMapOf<String, Any>(
            "title" to title.capitalizeFirstLetter(),
            "description" to description
        )
        developer?.let { updates["developer"] = it }

        subtaskRepository.updateSubTask(projectId, taskId,subtaskId, updates)
    }

    suspend fun filterSubTasksByProgress(
        subtasks: ArrayList<ItemsViewModel>,
        filter: String // "completed" o "incompleted"
    ): ArrayList<ItemsViewModel> {
        val filteredSubTasks = ArrayList<ItemsViewModel>()

        for (subtask in subtasks) {
            try {
                val progress: Int = if (!subtask.taskId.isNullOrEmpty()) {
                    subtaskRepository.getSubTaskProgress( subtask.projectId,subtask.taskId,subtask.subtaskId!!)
                } else {
                    throw Exception("subtaskid mancante")
                }

                when (filter) {
                    "completed" -> if (progress == 100) filteredSubTasks.add(subtask)
                    "incompleted" -> if ((progress ?: 0) < 100) filteredSubTasks.add(subtask)
                    else -> throw IllegalArgumentException("Filtro non valido: $filter. Usa 'completed' o 'incompleted'.")
                }
            } catch (e: Exception) {
                throw Exception("Errore nel filtrare il task con ID: ${subtask.subtaskId}", e)
            }
        }

        return filteredSubTasks
    }

    fun String.capitalizeFirstLetter(): String {
        return this.firstOrNull()?.uppercase()?.plus(this.substring(1)) ?: ""
    }
}