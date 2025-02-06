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
            "title" to title,
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

}