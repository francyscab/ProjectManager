package com.example.project_manager.services

import android.content.ContentValues.TAG
import android.net.Uri
import android.util.Log
import com.example.project_manager.models.Role
import com.example.project_manager.models.User
import com.example.project_manager.utils.FileRepository
import com.example.project_manager.utils.UserRepository

class UserService {
    val userRepository = UserRepository()
    val projectService= ProjectService()
    val fileRepository = FileRepository()
    val taskService = TaskService()

    fun getCurrentUserId(): String? {
        return userRepository.getCurrentUserId()
    }

    suspend fun getUserNameById(userId: String): String? {
        return userRepository.getUserNameById(userId)
    }

    suspend fun getCurrentUser(): User? {
        return try {
            userRepository.getCurrentUser()
        } catch (e: Exception) {
            Log.e("UserService", "Error retrieving user", e)
            null
        }
    }

    suspend fun getCurrentUserRole(): Role? {
        return getCurrentUser()?.role
    }

    suspend fun getLeadersOfDeveloper(developerId: String): List<User>{
        return try{
            userRepository.getMyLeaders(developerId)
        }catch (e: Exception){
            Log.e("UserService", "Error retrieving user", e)
            throw e
        }
    }

    public fun signInProcedure(
        email: String,
        pw: String,
        name: String,
        surname: String,
        role: Role,
        imageUri: Uri,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        userRepository.sign_in(email, pw, onSuccess = { signInSuccess ->
            if (signInSuccess) {
                Log.d("Auth", "User signed in successfully")

                fileRepository.uploadProfileImage(imageUri, onSuccess = { imageUrl ->
                    Log.d("Upload", "Profile image uploaded: $imageUrl")

                    userRepository.createUser(name, surname, role, email, imageUri, imageUrl, onSuccess = {
                        Log.d("Firestore", "User data stored successfully")

                        // Usa la funzione login esistente invece di Firebase direttamente
                        userRepository.login(email, pw, onSuccess = {
                            onSuccess()
                        }, onFailure = { e ->
                            Log.e("Login", "Errore durante il login", e)
                            onFailure(e)
                        })

                    }, onFailure = { e ->
                        Log.e("Firestore", "Failed to create user in Firestore", e)
                        onFailure(e)
                    })
                }, onFailure = { e ->
                    Log.e("Upload", "Failed to upload profile image", e)
                    onFailure(e)
                })
            } else {
                onFailure(Exception("User sign-in failed"))
            }
        }, onFailure = { e ->
            Log.e("Auth", "User sign-in failed", e)
            onFailure(e)
        })
    }

    public suspend fun getLeaderOfManager(managerId: String): List<User> {
        val leaders = mutableListOf<User>()
        val uniqueLeaderIds = mutableSetOf<String>()

        try {
            val managerProjects = projectService.loadProjectForUser(managerId)

            // For each project, get its leader
            for (project in managerProjects) {
                val leaderId = project.assignedTo

                // Only process if it's a new leader and not empty
                if (leaderId.isNotEmpty() && uniqueLeaderIds.add(leaderId)) {
                    try {
                        // Get leader user details
                        getUserById(leaderId)?.let { leader ->
                            leaders.add(leader)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error getting leader details for ID: $leaderId", e)
                        continue // Continue with next leader if one fails
                    }
                }
            }

            Log.d(TAG, "Found ${leaders.size} leaders for manager $managerId")

        } catch (e: Exception) {
            Log.e(TAG, "Error getting leaders for manager $managerId", e)
        }

        return leaders
    }

    public suspend fun getMyManagers(userId: String): List<User> {
        val managers = mutableListOf<User>()
        val uniqueManagerIds = mutableSetOf<String>()

        try {
            // Get all projects where user is leader
            val leaderProjects = projectService.loadProjectByLeader(userId)

            // For each project, get its creator (manager)
            for (project in leaderProjects) {
                val managerId = project.creator

                // Only process if it's a new manager and not the user themselves
                if (managerId.isNotEmpty() &&
                    managerId != userId &&
                    uniqueManagerIds.add(managerId)) {

                    try {
                        // Get manager user details
                        getUserById(managerId)?.let { manager ->
                            managers.add(manager)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error getting manager details for ID: $managerId", e)
                        continue // Continue with next manager if one fails
                    }
                }
            }

            Log.d(TAG, "Found ${managers.size} managers for leader $userId")

        } catch (e: Exception) {
            Log.e(TAG, "Error getting managers for leader $userId", e)
        }

        return managers
    }


    public suspend fun getMyDeveloperCollegue(userId: String): List<User> {
        val developers = mutableListOf<User>()

        try {
            // Get all developer IDs from colleague tasks
            val developerIds = taskService.getDeveloperCollegue(userId)

            // Convert each ID to a User object
            for (developerId in developerIds) {
                try {
                    getUserById(developerId)?.let { user ->
                        developers.add(user)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error getting user details for ID: $developerId", e)
                    continue // Continue with next developer if one fails
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting developer colleagues for user $userId", e)
        }
        return developers
    }

    public suspend fun getUsersByRole(role:Role): ArrayList<User>{
        return userRepository.getUsersByRole(role)
    }


    public suspend fun getUserById(userId: String): User? {
        return userRepository.getUserById(userId)
    }

    suspend fun getDevelopersFromLeaderProjects(leaderId: String): List<User> {
        val developerUsers = mutableListOf<User>()
        val uniqueDeveloperIds = mutableSetOf<String>()

        try {
            // Get all projects where user is leader
            val leaderProjects = projectService.loadProjectByLeader(leaderId)

            // For each project, get all tasks
            for (project in leaderProjects) {
                val projectTasks = taskService.getAllTaskByProjectId(project.projectId)

                // For each task, get the assigned developer's ID
                for (task in projectTasks) {
                    val developerId = task.assignedTo
                    // Only process if it's a new developer and not the leader themselves
                    if (developerId.isNotEmpty() &&
                        developerId != leaderId &&
                        uniqueDeveloperIds.add(developerId)) {

                        // Get developer user details
                        try {
                            getUserById(developerId)?.let { developer ->
                                developerUsers.add(developer)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error getting developer details for ID: $developerId", e)
                            continue // Continue with next developer if one fails
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting developers from leader projects", e)
        }

        return developerUsers
    }
}