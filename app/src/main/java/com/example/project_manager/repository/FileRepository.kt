package com.example.project_manager.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.example.project_manager.R
import com.example.project_manager.models.FileModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.logger.Logger
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class FileRepository {
    private val storage = FirebaseStorage.getInstance()
    private val logger = Logger.getLogger("Storage")
    private val db= FirebaseFirestore.getInstance()
    var auth = FirebaseAuth.getInstance()

    fun uploadProfileImage(imageUri: Uri, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        uploadFile("profile_images", imageUri, getTodayDate(), onSuccess, onFailure)
    }

    fun uploadProjectDocument(imageUri: Uri, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        val fileName = getFileName(imageUri) + getTodayDate()

        uploadFile("project_documents", imageUri, fileName, onSuccess, onFailure)
    }

    fun uploadFile(path: String, imageUri: Uri, imageName: String, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        val storageReference = storage.reference

        val profileImagesRef = storageReference.child("${path}/${imageName}")

        profileImagesRef.putFile(imageUri).addOnSuccessListener {
            profileImagesRef.downloadUrl.addOnSuccessListener { uri ->
                onSuccess(uri.toString())
            }.addOnFailureListener { e ->
                logger.error("Error getting download URL", e)
                onFailure(e)
            }
        }.addOnFailureListener{ e ->
            logger.error("Error uploading image to Firebase Storage", e)
            onFailure(e)
        }
    }

    suspend fun getImageUrlFromStorage(imageUrl: String): Uri? {
        return try {
            if (imageUrl.isNotEmpty()) {
                val storageRef = storage.getReferenceFromUrl(imageUrl)
                storageRef.downloadUrl.await()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("FileRepository", "Error getting image URL from storage", e)
            null
        }
    }

    suspend fun loadProfileImage(context: Context, imageView: ImageView, imageUrl: String) {
        try {
            val downloadUrl = getImageUrlFromStorage(imageUrl)
            if (downloadUrl != null) {
                Glide.with(context)
                    .load(downloadUrl)
                    .placeholder(R.drawable.username)
                    .error(R.drawable.username)
                    .circleCrop()
                    .into(imageView)
            } else {
                imageView.setImageResource(R.drawable.username)
            }
        } catch (e: Exception) {
            Log.e("FileRepository", "Error loading profile image", e)
            imageView.setImageResource(R.drawable.username)
        }
    }

    private fun getTodayDate(): String {
        val formatter = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.getDefault())
        return formatter.format(Date())
    }

    private fun getFileName(uri: Uri): String {
        return uri.path?.substring(uri.path!!.lastIndexOf('/') + 1) ?: ""
    }

    suspend fun getTaskFiles(projectId: String, taskId: String): ArrayList<FileModel> {
        //Log.d("FileRepository", "Getting task files for project $projectId and task $taskId")
        val files = ArrayList<FileModel>()
        try {
            val storageRef = storage.reference
                .child("projects/$projectId/tasks/$taskId/files")

            val result = storageRef.listAll().await()

            result.items.forEach { item ->
                val downloadUrl = item.downloadUrl.await()
                val metadata = item.metadata.await()

                files.add(
                    FileModel(
                    name = item.name,
                    downloadUrl = downloadUrl.toString(),
                    uploadedAt = metadata.creationTimeMillis,
                    size = metadata.sizeBytes
                )
                )
            }
        } catch (e: Exception) {
            Log.e("FileRepository", "Error getting task files", e)
            throw e
        }
        return files
    }

    suspend fun saveFileMetadata(projectId: String, taskId: String, fileName: String, path: String): Boolean {
        return try {
            val fileData = hashMapOf(
                "name" to fileName,
                "uploadedAt" to System.currentTimeMillis(),
                "path" to "projects/$projectId/tasks/$taskId/files/$fileName"
            )

            db.collection("progetti")
                .document(projectId)
                .collection("task")
                .document(taskId)
                .collection("files")
                .add(fileData)
                .await()

            true
        } catch (e: Exception) {
            Log.e("FileRepository", "Error saving file metadata to database", e)
            false
        }
    }

}