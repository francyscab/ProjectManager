package com.example.project_manager.services

import android.net.Uri
import android.util.Log
import com.example.project_manager.models.FileModel
import com.example.project_manager.models.ItemsViewModel
import com.example.project_manager.repository.FileRepository
import kotlinx.coroutines.launch
import java.io.File

class FileService {

    val fileRepository=FileRepository()

    fun filterFiles(query: String?, files: ArrayList<FileModel>): ArrayList<FileModel> {
        return if (query.isNullOrEmpty()) {
            files
        } else {
            ArrayList(files.filter {
                it.name.contains(query, ignoreCase = true)
            })
        }
    }


    fun uploadTaskFile(projectId: String, taskId: String, fileUri: Uri, fileName: String, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        val path = "projects/$projectId/tasks/$taskId/files"

        // Prima carichiamo il file nello storage
        fileRepository.uploadFile(path, fileUri, fileName, { downloadUrl ->
            // Salviamo i metadati in modo asincrono
            kotlinx.coroutines.GlobalScope.launch {
                try {
                    fileRepository.saveFileMetadata(
                        projectId = projectId,
                        taskId = taskId,
                        fileName = fileName,
                        path = path
                    )
                } catch (e: Exception) {
                    Log.e("FileService", "Error saving file metadata", e)
                }
            }

            // Chiamiamo onSuccess quando il file è caricato
            onSuccess(downloadUrl)
        }, onFailure)
    }

    suspend fun getTaskFiles(projectId: String, taskId: String): ArrayList<FileModel> {
        return fileRepository.getTaskFiles(projectId, taskId)
    }



}