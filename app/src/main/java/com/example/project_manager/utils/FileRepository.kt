package com.example.project_manager.utils

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.logger.Logger
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class FileRepository {
    private val storage = FirebaseStorage.getInstance()
    private val logger = Logger.getLogger("Storage")
    private val db= FirebaseFirestore.getInstance()
    var auth = FirebaseAuth.getInstance()

    public fun uploadProfileImage(imageUri: Uri, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        uploadFile("profile_images", imageUri, getTodayDate(), onSuccess, onFailure)
    }

    public fun uploadProjectDocument(imageUri: Uri, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        val fileName = getFileName(imageUri) + getTodayDate()

        uploadFile("project_documents", imageUri, fileName, onSuccess, onFailure)
    }

    private fun uploadFile(path: String, imageUri: Uri, imageName: String, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
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

    private fun getTodayDate(): String {
        val formatter = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.getDefault())
        return formatter.format(Date())
    }

    private fun getFileName(uri: Uri): String {
        return uri.path?.substring(uri.path!!.lastIndexOf('/') + 1) ?: ""
    }
}