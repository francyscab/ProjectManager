package com.example.project_manager.repository

import android.content.ContentValues.TAG
import android.net.Uri
import android.util.Log
import com.example.project_manager.models.Role
import com.example.project_manager.models.User
import com.example.project_manager.services.TaskService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.logger.Logger
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val logger = Logger.getLogger("Storage")
    private val db= FirebaseFirestore.getInstance()

    private val auth = FirebaseAuth.getInstance()

    val taskService= TaskService()


    public fun sign_in (email:String,pw:String,onSuccess: (Boolean) -> Unit, onFailure: (Exception) -> Unit) {
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, pw)
            .addOnSuccessListener {
                onSuccess(true)
            }.addOnFailureListener { e ->
                logger.error("Error creating user in Firebase Auth", e)
                onFailure(e)
            }
    }

    public fun createUser(name:String,surname:String,role:Role,email:String,filepath:Uri,profile_image_url:String, onSuccess: (Boolean) -> Unit, onFailure: (Exception) -> Unit){
        val currentUser = FirebaseAuth.getInstance().currentUser
        if(currentUser!==null){
            val newUser= User(name=name,surname=surname,role=role,email=email,uid=currentUser.uid,profile_image_url=profile_image_url)
            db.collection("utenti").document(currentUser.uid).set(newUser)
                .addOnSuccessListener {
                    Log.d(TAG, "User data added to Firestore with ID: ${currentUser.uid}")
                    onSuccess(true)
                }.addOnFailureListener() {e->
                    logger.error("Error creating user in DB", e)
                    onFailure(e)
                }
        }
        else{
            logger.error("Current user is null")
            onFailure(Exception("Current user is null"))
        }


    }

    public fun isLogged():Boolean{
        val currentUser = FirebaseAuth.getInstance().currentUser
        if(currentUser!==null){
            return true;
        }else{
            return false
        }
    }

    fun login(email: String, pw: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        Log.d("Login", "Tentativo di login con email: $email")

        try {
            auth.signInWithEmailAndPassword(email, pw)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("Login", "Accesso riuscito")
                        onSuccess()
                    } else {
                        val exception = task.exception ?: Exception("Unknown error occurred")
                        Log.e("Login", "Errore durante il login", exception)

                        // Aggiungi un log specifico prima di chiamare onFailure
                        Log.e("Login", "Chiamando onFailure...")

                        try {
                            onFailure(exception)

                            // Aggiungi un log dopo per verificare che onFailure è stata eseguita completamente
                            Log.e("Login", "onFailure eseguita con successo")
                        } catch (e: Exception) {
                            // Cattura eventuali eccezioni che si verificano durante l'esecuzione di onFailure
                            Log.e("Login", "Errore durante l'esecuzione di onFailure", e)
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e("Login", "Eccezione non gestita", e)
            try {
                onFailure(e)
            } catch (e2: Exception) {
                Log.e("Login", "Errore chiamando onFailure per eccezione non gestita", e2)
            }
        }
    }

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    suspend fun getCurrentUser(): User? {
        val uid = getCurrentUserId() ?: return null

        return try {
            val document = db.collection("utenti").document(uid).get().await()
            if (document.exists()) {
                User(
                    name = document.getString("name") ?: "",
                    surname = document.getString("surname") ?: "",
                    email = document.getString("email") ?: "",
                    role = Role.valueOf(document.getString("role") ?: "Developer"),
                    uid = uid,
                    profile_image_url = document.getString("profile_image_url") ?: ""
                )
            } else {
                Log.w(TAG, "User not found with UID: $uid")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving user", e)
            null
        }
    }

    suspend fun getUserNameById(userId: String): String? {
        try {
            val document = db.collection("utenti").document(userId).get().await()
            if (document.exists()) {
                val name = document.getString("name") ?: ""
                val surname = document.getString("surname") ?: ""
                return "$name $surname".trim()
            }
            return null
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving user's full name", e)
            return null
        }
    }

    private suspend fun getAllUsers(): ArrayList<User> {
        return try {
            val result = db.collection("utenti").get().await()
            val userList = result.documents.mapNotNull { it.toObject(User::class.java) }
            ArrayList(userList)
        } catch (e: Exception) {
            Log.e("UserRepository", "Errore nel recuperare gli utenti", e)
            throw e
        }
    }


    public suspend fun getUsersByRole(role: Role): ArrayList<User> {
        return try {
            val allUsers = getAllUsers()
            val filteredUsers = allUsers.filter { it.role == role } // Filtra gli utenti
            ArrayList(filteredUsers) // Converte la lista filtrata in ArrayList
        } catch (e: Exception) {
            Log.e("UserRepository", "Errore nel recuperare gli utenti con ruolo $role", e)
            throw e
        }
    }


    suspend fun getUserById(userId: String): User? {
        return try {
            val document = db.collection("utenti")
                .document(userId)
                .get()
                .await()

            if (document.exists()) {
                document.toObject(User::class.java)
            } else {
                Log.w("UserRepository", "Utente non trovato con ID: $userId")
                null
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Errore nel recuperare l'utente con ID: $userId", e)
            throw e
        }
    }

    public suspend fun getMyLeaders(userId: String): List<User> {
        val myTasks = taskService.getMyTask(userId)
        val leaderIds = mutableSetOf<String>()
        val leaders = mutableListOf<User>()

        try {
            for (task in myTasks) {
                    val leaderId = task.creator
                    if (leaderId.isNotEmpty() && !leaderIds.contains(leaderId)) {
                        leaderIds.add(leaderId)
                        // Get leader user details
                        getUserById(leaderId)?.let { leader ->
                            leaders.add(leader)
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting leaders for user $userId", e)
        }
        return leaders
    }


}