package com.example.project_manager.services

import android.content.ContentValues.TAG
import android.util.Log
import com.example.project_manager.models.ItemsViewModel
import com.example.project_manager.repository.ProjectRepository

class ProjectService {
    val projectRepository= ProjectRepository()
    val taskService = TaskService()

    public suspend fun getProjectById(projectId: String): ItemsViewModel? {
        Log.d(TAG, "ProjectService: Getting project with ID: $projectId")
        return try {
            val project = projectRepository.getProjectById(projectId)
            if (project == null) {
                Log.w(TAG, "ProjectService: No project found with ID: $projectId")
            } else {
                Log.d(TAG, "ProjectService: Successfully retrieved project: ${project.title}")
            }
            project
        } catch (exception: Exception) {
            Log.e(TAG, "ProjectService: Error getting project", exception)
            null
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
            projectRepository.uploadProject(projectData)
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading new project", e)
            throw e
        }
    }

    suspend fun updateProject(projectId: String, title: String, description: String) {
        val updates = mutableMapOf<String, Any>(
            "title" to title.capitalizeFirstLetter(),
            "description" to description,

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

    suspend fun deleteProject(projectId: String): Boolean {
        return try {
            projectRepository.deleteProject(projectId)
        } catch (e: Exception) {
            Log.e(TAG, "Error in project deletion", e)
            false
        }
    }

    public suspend fun sollecita(projectId: String): Boolean {
        return projectRepository.sollecita(projectId,true)
    }

    public suspend fun elimina_sollecita(projectId: String): Boolean {
        return projectRepository.sollecita(projectId,false)
    }

    fun String.capitalizeFirstLetter(): String {
        return this.firstOrNull()?.uppercase()?.plus(this.substring(1)) ?: ""
    }

}