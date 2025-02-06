package com.example.project_manager.services

import android.content.ContentValues.TAG
import android.util.Log
import com.example.project_manager.models.ItemsViewModel
import com.example.project_manager.utils.ProjectRepository

class ProjectService {
    val projectRepository= ProjectRepository()
    val taskService = TaskService()

    public suspend fun getProjectById(projectId: String): ItemsViewModel? {
        return try {
            projectRepository.getProjectById(projectId)
        } catch (exception: Exception) {
            throw exception
        }
    }

    suspend fun uploadNewProject(
        title: String,
        description: String,
        deadline: String,
        priority: String,
        creator: String,
        assignedTo: String
    ): String {
        val projectData = mapOf(
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
            projectRepository.uploadProject(projectData)
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading new project", e)
            throw e
        }
    }

    suspend fun updateProject(projectId: String, title: String, description: String, assignedTo: String) {
        val updates = mutableMapOf<String, Any>(
            "title" to title,
            "description" to description,
            "assignedTo" to assignedTo
        )
        projectRepository.updateProject(projectId, updates)
    }

    fun filterProjects(query: String?, data: ArrayList<ItemsViewModel>): ArrayList<ItemsViewModel> {
        val filteredData = if (query.isNullOrEmpty()) {
            data
        } else {
            data.filter { project ->
                project.title.contains(query, ignoreCase = true)
            }.toCollection(ArrayList())
        }
        return filteredData
    }

    public suspend fun loadProjectByLeader(leaderId: String): ArrayList<ItemsViewModel> {
        val data=projectRepository.loadProjectData()
        return ArrayList(data.filter { project -> project.assignedTo == leaderId })

    }
    public suspend fun loadProjectForUser(userId: String): ArrayList<ItemsViewModel> {
        val data = projectRepository.loadProjectData()
        Log.d(TAG, "loadProjectForUser: $data")

        return ArrayList(data.filter { project -> project.creator == userId }) // Converte il risultato in ArrayList
    }

    suspend fun filterProjectByProgress(
        projects: ArrayList<ItemsViewModel>,
        filter: String // "completed" o "incompleted"
    ): ArrayList<ItemsViewModel> {
        val filteredProjects = ArrayList<ItemsViewModel>()

        for (project in projects) {
            try {
                val progress: Int? = if (!project.projectId.isNullOrEmpty()) {
                    // Caso: Recupera il progresso del task specifico
                    projectRepository.getProjectProgress(project.projectId)
                } else {
                    throw Exception("projectId mancante")
                }

                when (filter) {
                    "completed" -> if (progress == 100) filteredProjects.add(project)
                    "incompleted" -> if ((progress ?: 0) < 100) filteredProjects.add(project)
                    else -> throw IllegalArgumentException("Filtro non valido: $filter. Usa 'completed' o 'incompleted'.")
                }
            } catch (e: Exception) {
                throw Exception("Errore nel filtrare il task con ID: ${project.projectId}", e)
            }
        }

        return filteredProjects // Ritorna il risultato dopo aver elaborato tutti i task
    }

    suspend fun calculateProjectProgress(projectId: String): Int {
        return try {
            val tasks = taskService.getAllTaskByProjectId(projectId)

            if (tasks.isEmpty()) return 0

            val totalProgress = tasks.sumOf { it.progress }
            totalProgress / tasks.size
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating task progress", e)
            0
        }
    }

    suspend fun updateProjectProgress(projectId: String): Boolean {
        return try {
            val progress = calculateProjectProgress(projectId)
            projectRepository.updateProjectProgress(projectId, progress)
        } catch (e: Exception) {
            Log.e(TAG, "Error in project progress update", e)
            false
        }
    }

    suspend fun saveFeedback(projectId: String, rating: Int, comment: String): Boolean {
        return projectRepository.saveFeedback(projectId, rating, comment)
    }

    suspend fun getFeedback(projectId: String): Triple<Int, String, Boolean>? {
        return projectRepository.getFeedback(projectId)
    }
}