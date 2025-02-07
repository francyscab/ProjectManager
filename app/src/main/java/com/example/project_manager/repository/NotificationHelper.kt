package com.example.project_manager.repository

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.project_manager.ChatActivity
import com.example.project_manager.ItemActivity
//ProjectActivity
import com.example.project_manager.R
import com.example.project_manager.models.Chat
import com.example.project_manager.models.Role
import com.example.project_manager.services.ProjectService
import com.example.project_manager.services.TaskService
import com.example.project_manager.services.UserService
import com.google.firebase.firestore.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class NotificationHelper(private val context: Context, private val db: FirebaseFirestore,
    ) {

    private val projectService = ProjectService()
    private val taskService= TaskService()
    private val userService= UserService()
    private val sharedPreferences = context.getSharedPreferences("NotificationPrefs", Context.MODE_PRIVATE)
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel("it.sollecito", "Sollecito Notification", "Notifiche per le richieste di soluzione")
            createNotificationChannel("it.newprogress", "Progress Notification", "Notifiche per il completamento dei progetti")
            createNotificationChannel("it.newmessage", "Chat Notification", "Notifiche per nuove chat e messaggi")
        }
    }

    suspend fun handleNotification(role: Role, userUid: String, type: String, coroutineScope: CoroutineScope, data: List<Chat>? = null) {
        when (type) {
            "sollecito" -> when (role) {
                Role.Leader -> setupLeaderSollecitoListeners(userUid,coroutineScope)
                Role.Developer -> setupDeveloperSollecitoListeners(userUid,coroutineScope)
                else -> {} // Manager doesn't need sollecito notifications
            }
            "chat" -> data?.let { handleChatNotifications(role, userUid,coroutineScope, it) }
            "progresso" -> handleProgressNotifications(role, userUid,coroutineScope)
        }
    }

    private suspend fun setupLeaderSollecitoListeners(leaderId: String,coroutineScope: CoroutineScope) {
        val projects = projectService.loadProjectByLeader(leaderId)

        projects.forEach { project ->
            db.collection("progetti")
                .document(project.projectId)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e("SollecitoListener", "Error listening to project ${project.projectId}", e)
                        return@addSnapshotListener
                    }

                    if (snapshot != null && snapshot.exists()) {
                        val sollecitato = snapshot.getBoolean("sollecitato") ?: false
                        if (sollecitato) {
                            coroutineScope.launch {
                                sendNotification(
                                    type = "sollecito",
                                    title = "Sollecito Progetto",
                                    text = "Il manager ${snapshot.getString("creator")
                                        ?.let { userService.getUserById(it)?.name }} richiede aggiornamenti sul progetto ${snapshot.getString("title")}",
                                    role = Role.Manager,
                                    recipientId = leaderId,
                                    projectId = project.projectId,
                                    taskId = null
                                )
                            }

                        }
                    }
                }
        }
    }

    private suspend fun setupDeveloperSollecitoListeners(developerId: String,coroutineScope: CoroutineScope) {
        val tasks=taskService.filterTaskByDeveloper(developerId)
        tasks.forEach { task ->
            db.collection("progetti")
                .document(task.projectId)
                .collection("task")
                .document(task.taskId!!)
                .addSnapshotListener { snapshot, e ->
                        if (e != null) {
                            Log.e("SollecitoListener", "Error listening to developer tasks", e)
                            return@addSnapshotListener
                        }

                    if (snapshot != null && snapshot.exists()) {
                        val sollecitato = snapshot.getBoolean("sollecitato") ?: false
                        if (sollecitato) {
                            coroutineScope.launch {
                                sendNotification(
                                    type = "sollecito",
                                    title = "Sollecito Progetto",
                                    text = "Il leader ${snapshot.getString("creator")
                                        ?.let { userService.getUserById(it)?.name }} richiede aggiornamenti sul progetto ${snapshot.getString("title")}",
                                    role = Role.Developer,
                                    recipientId = developerId,
                                    projectId = task.projectId,
                                    taskId = task.taskId
                                )
                            }

                        }
                    }
            }
        }
    }

    private fun handleChatNotifications(role: Role, userId: String,coroutineScope: CoroutineScope, chatIds: List<Chat>) {
        chatIds.forEach { chatId ->
            db.collection("chat").document(chatId.chatId).addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("ChatListener", "Errore durante l'ascolto della chat ${chatId.chatId}.", e)
                    return@addSnapshotListener
                }

                snapshot?.get("lastMessage")?.let { message ->
                    val timestamp = snapshot.get("timestamp").toString()
                    if(snapshot.get("senderId").toString() !== userId.toString()) {
                        coroutineScope.launch {
                            sendNotification(
                                type = "chat",
                                title = "Nuovo messaggio",
                                text = "Hai ricevuto un nuovo messaggio: $message da ${
                                    userService.getUserById(
                                        snapshot.get("senderId").toString())?.name
                                }",
                                role = role,
                                recipientId = userId,
                                projectId = chatId.chatId,
                                taskId = timestamp
                            )
                        }
                    }
                }
            }
        }
    }

    private fun handleProgressNotifications(role: Role, userId: String,coroutineScope: CoroutineScope) {
        Log.d("ProgressNotifications", "Entrato nella funzione handleProgressNotifications con ruolo: $role, nome: $userId")

        if (role == Role.Manager) {
            val query = db.collection("progetti").whereEqualTo("creator", userId)
            query.get().addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Log.d("ProgressNotifications", "Nessun progetto trovato per il Manager: $userId")
                } else {
                    Log.d("ProgressNotifications", "Trovati ${documents.size()} progetti per il Manager: $userId")
                }

                documents.forEach { document ->
                    val projectId = document.id
                    val progress = document.getLong("progress")
                    Log.d("ProgressNotifications", "Progresso del progetto $projectId: $progress")
                    if (progress?.toInt() == 100 && !isNotificationShown("progresso_$projectId")) {
                        Log.d("ProgressNotifications", "Progetto $projectId completato al 100%")
                        coroutineScope.launch {
                            sendNotification(
                                type = "progresso",
                                title = "Progetto completato",
                                text = "Il progetto ${document.get("title")} è stato completato.",
                                role = role,
                                recipientId = userId,
                                projectId = projectId,
                                taskId = null
                            )
                        }
                        //setNotificationShown("progresso_$projectId")
                    }
                }
            }.addOnFailureListener { exception ->
                Log.e("ProgressNotifications", "Errore durante il recupero dei progetti per il Manager: $userId", exception)
            }
        } else if (role == Role.Leader) {
            val query = db.collection("progetti").whereEqualTo("assignedTo", userId)
            query.get().addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Log.d("ProgressNotifications", "Nessun progetto trovato per il Leader: $userId")
                } else {
                    Log.d("ProgressNotifications", "Trovati ${documents.size()} progetti per il Leader: $userId")
                }

                documents.forEach { document ->
                    val projectId = document.id
                    Log.d("ProgressNotifications", "Trovato progetto con ID: $projectId")

                    db.collection("progetti").document(projectId).collection("task")
                        .addSnapshotListener { snapshots, e ->
                            if (e != null) {
                                Log.e("TaskListener", "Errore durante l'ascolto dei task per il progetto $projectId.", e)
                                return@addSnapshotListener
                            }

                            snapshots?.documentChanges?.forEach { change ->
                                if (change.type == DocumentChange.Type.MODIFIED) {
                                    val progress = change.document.getLong("progress")
                                    if (progress?.toInt() == 100 && !isNotificationShown("progresso_${change.document.id}")) {
                                        Log.d("ProgressNotifications", "Task ${change.document.id} completato al 100% nel progetto $projectId")
                                        coroutineScope.launch {
                                            sendNotification(
                                                type = "progresso",
                                                title = "Task completato",
                                                text = "Il task ${document.get("titolo")} è stato completato.",
                                                role= role,
                                                recipientId = userId,
                                                projectId = projectId,
                                                taskId = change.document.id
                                            )
                                        }

                                        //setNotificationShown("progresso_${change.document.id}")
                                    }
                                }
                            }
                        }
                }
            }.addOnFailureListener { exception ->
                Log.e("ProgressNotifications", "Errore durante il recupero dei progetti per il Leader: $userId", exception)
            }
        }
    }

    private suspend fun sendNotification(
        type: String,
        title: String,
        text: String,
        role: Role,
        recipientId: String,
        projectId: String?,
        taskId: String?
    ) {
        var intent: Intent? = null
        var canale: String? = null

        if (type == "sollecito" || type == "progresso") {
            canale = if (type == "sollecito") "it.sollecito" else "it.newprogress"
            if (type == "sollecito" && role == Role.Developer){
                taskService.elimina_sollecita(projectId!!,taskId!!)
            }else if (type == "progresso" && role == Role.Leader){
                projectService.elimina_sollecita(projectId!!)
            }
            intent = Intent(context, ItemActivity::class.java).apply {
                putExtra("projectId", projectId)
                putExtra("taskId", taskId)
            }
        } else if (type == "chat") {
            canale = "it.newmessage"
            intent = Intent(context, ChatActivity::class.java).apply {
                putExtra("chatId", projectId)
                putExtra("timestamp", taskId)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = canale?.let {
            NotificationCompat.Builder(context, it)
                .setSmallIcon(R.drawable.username)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()
        }

        notificationManager.notify("${type}_${recipientId}".hashCode(), notification)
    }

    private fun createNotificationChannel(id: String, userId: String, description: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(id, userId, NotificationManager.IMPORTANCE_HIGH).apply {
                this.description = description
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun isNotificationShown(key: String): Boolean {
        return sharedPreferences.getBoolean(key, false)
    }

    private fun setNotificationShown(key: String) {
        sharedPreferences.edit().putBoolean(key, true).apply()
    }
}