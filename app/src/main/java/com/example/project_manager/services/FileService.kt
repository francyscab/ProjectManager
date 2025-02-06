package com.example.project_manager.services

import android.net.Uri
import com.example.project_manager.models.FileModel
import com.example.project_manager.utils.FileRepository
import java.nio.file.Path

class FileService {

    val fileRepository=FileRepository()

    public fun uploadFile(path: String, fileUri: Uri,fileName: String, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit){
        return fileRepository.uploadFile(path,fileUri,fileName,onSuccess,onFailure)
    }

    suspend fun getTaskFiles(projectId: String, taskId: String): List<FileModel> {
        return fileRepository.getTaskFiles(projectId, taskId)
    }



}