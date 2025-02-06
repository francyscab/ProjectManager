package com.example.project_manager.services

import android.content.ContentValues.TAG
import android.util.Log
import com.example.project_manager.models.ItemsViewModel
import com.example.project_manager.utils.ProjectRepository
import com.example.project_manager.utils.SubTaskRepository
import com.example.project_manager.utils.TaskRepository


class TaskService {

    private val projectService by lazy { ProjectService() }
    val taskRepository = TaskRepository()
    val projectRepository = ProjectRepository()
    val subtaskRepository = SubTaskRepository()

    suspend fun getTaskById(projectId: String, taskId: String): ItemsViewModel? {
        return taskRepository.loadTaskById(taskId, projectId)
    }

    suspend fun getAllTaskByProjectId(projectId: String): ArrayList<ItemsViewModel> {
        return taskRepository.loadAllTaskByProjectId(projectId)
    }

    suspend fun updateTaskProgress(projectId: String, taskId: String): Boolean {
        return try {
            val progress = calculateTaskProgress(projectId, taskId)
            val success = taskRepository.updateTaskProgress(projectId, taskId, progress)
            if (success) {
                // Trigger project progress update
                projectService.updateProjectProgress(projectId)
            }
            success
        } catch (e: Exception) {
            Log.e(TAG, "Error in task progress update", e)
            false
        }
    }

    suspend fun calculateTaskProgress(projectId: String, taskId: String): Int {
        return try {
            val subtasks = subtaskRepository.loadAllSubtaskByTaskId(projectId, taskId)

            if (subtasks.isEmpty()) return 0

            val totalProgress = subtasks.sumOf { it.progress }
            totalProgress / subtasks.size
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating task progress", e)
            0
        }
    }
    suspend fun filterTaskByLeader(leaderId: String): ArrayList<ItemsViewModel> {
        val taskData = ArrayList<ItemsViewModel>()
        val projectData = projectRepository.loadProjectData()

        for (project in projectData) {
            val tasks = taskRepository.loadAllTaskByProjectId(project.projectId)
            tasks.filter { task -> task.creator == leaderId }
            taskData.addAll(tasks)
        }

        return taskData
    }

    suspend fun filterTaskByDeveloper(developerId: String): ArrayList<ItemsViewModel> {
        val taskData = ArrayList<ItemsViewModel>()
        val projectData = projectRepository.loadProjectData()

        for (project in projectData) {
            val tasks = taskRepository.loadAllTaskByProjectId(project.projectId)
            tasks.filter { task -> task.assignedTo == developerId }
            taskData.addAll(tasks)
        }
        return taskData
    }

    suspend fun filterTasksByProgress(
        tasks: ArrayList<ItemsViewModel>,
        filter: String // "completed" o "incompleted"
    ): ArrayList<ItemsViewModel> {
        val filteredTasks = ArrayList<ItemsViewModel>()

        for (task in tasks) {
            try {
                val progress: Int? = if (!task.taskId.isNullOrEmpty()) {
                    taskRepository.getTaskProgress(task.taskId, task.projectId)
                } else {
                    throw Exception("taskid mancante")
                }

                when (filter) {
                    "completed" -> if (progress == 100) filteredTasks.add(task)
                    "incompleted" -> if ((progress ?: 0) < 100) filteredTasks.add(task)
                    else -> throw IllegalArgumentException("Filtro non valido: $filter. Usa 'completed' o 'incompleted'.")
                }
            } catch (e: Exception) {
                throw Exception("Errore nel filtrare il task con ID: ${task.taskId}", e)
            }
        }

        return filteredTasks
    }

    suspend fun updateTask(projectId: String, taskId: String, title: String, description: String, developer: String? = null) {
        val updates = mutableMapOf<String, Any>(
            "title" to title,
            "description" to description
        )
        developer?.let { updates["developer"] = it }

        taskRepository.updateTask(projectId, taskId, updates)
    }

    suspend fun uploadNewTask(
        projectId: String,
        title: String,
        description: String,
        deadline: String,
        priority: String,
        creator: String,
        assignedTo: String
    ): String {
        val subtaskData = mapOf(
            "title" to title,
            "description" to description,
            "deadline" to deadline,
            "priority" to priority,
            "creator" to creator,
            "assignedTo" to assignedTo,
            "progress" to 0,
            "valutato" to false
        )
        return try {
            taskRepository.uploadTask(projectId, subtaskData)
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading new subtask", e)
            throw e
        }
    }

    suspend fun getMyTask(userId: String): ArrayList<ItemsViewModel> {
        return taskRepository.getMyTasks(userId)
    }

    suspend fun getDeveloperCollegue(userId: String): Set<String> {
        val developerIds = mutableSetOf<String>()

        try {
            // Get all tasks assigned to the user
            val userTasks = filterTaskByDeveloper(userId)

            // For each task, get all tasks from the same project
            for (task in userTasks) {
                val projectTasks = getAllTaskByProjectId(task.projectId)

                // Extract developer IDs (assignedTo field) from project tasks
                for (projectTask in projectTasks) {
                    val assignedTo = projectTask.assignedTo
                    if (!assignedTo.isNullOrEmpty() && assignedTo != userId) {
                        developerIds.add(assignedTo)
                    }
                }
            }

            Log.d(TAG, "Found ${developerIds.size} developer colleagues for user $userId")
        } catch (e: Exception) {
            Log.e(TAG, "Error getting developer colleagues for user $userId", e)
            throw e
        }

        return developerIds
    }

    suspend fun saveFeedback(projectId: String, taskId: String, rating: Int, comment: String): Boolean {
        return taskRepository.saveFeedback(projectId, taskId, rating, comment)
    }

    suspend fun getFeedback(projectId: String, taskId: String): Triple<Int, String, Boolean>? {
        return taskRepository.getFeedback(projectId, taskId)
    }
}

