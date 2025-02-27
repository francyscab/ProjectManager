package com.example.project_manager.repository

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.project_manager.ChatActivity
import com.example.project_manager.HomeActivity
import com.example.project_manager.HomeItemActivity
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

    private val sollecitoListeners = mutableMapOf<String, ListenerRegistration>()

    private var userChatCollectionListener: ListenerRegistration? = null

    private val activeListeners = mutableListOf<ListenerRegistration>()
    private val activeChatListeners = mutableMapOf<String, ListenerRegistration>()

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



    fun getActiveListeners(): List<ListenerRegistration> {
        return activeListeners.toList()
    }

    private fun addListener(listener: ListenerRegistration) {
        activeListeners.add(listener)
    }

    suspend fun handleNotification(role: Role, userUid: String, type: String, coroutineScope: CoroutineScope, data: List<Chat>? = null) {
        when (type) {
            "sollecito" -> when (role) {
                Role.Leader -> setupNewProjectSollecitoListener(userUid,coroutineScope)
                Role.Developer -> setupDeveloperSollecitoListeners(userUid,coroutineScope)
                else -> {} // Manager doesn't need sollecito notifications
            }
            "chat" -> data?.let { handleChatNotifications(role, userUid,coroutineScope) }
            "progresso" -> handleProgressNotifications(role, userUid,coroutineScope)
        }
    }

    fun setupNewProjectSollecitoListener(leaderId: String, coroutineScope: CoroutineScope) {
        val newProjectListener = db.collection("progetti")
            .whereEqualTo("assignedTo", leaderId)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e("NewProjectSollecitoListener", "Error listening to new projects", e)
                    return@addSnapshotListener
                }

                snapshots?.documentChanges?.forEach { documentChange ->
                    if (documentChange.type == DocumentChange.Type.ADDED) {
                        val newProjectId = documentChange.document.id
                        val sollecitato = documentChange.document.getBoolean("sollecitato") ?: false

                        // If the new project is already sollecitato, send notification
                        if (sollecitato) {
                            coroutineScope.launch {
                                val creatorId = documentChange.document.getString("creator") ?: ""
                                val creatorName = userService.getUserById(creatorId)?.name ?: "Manager"
                                val projectTitle = documentChange.document.getString("title") ?: "progetto"

                                sendNotification(
                                    type = "sollecito",
                                    title = "Sollecito Progetto",
                                    text = "Il manager $creatorName richiede aggiornamenti sul progetto $projectTitle",
                                    role = Role.Leader,
                                    recipientId = leaderId,
                                    projectId = newProjectId,
                                    taskId = null
                                )
                            }
                        }

                        // Attach a continuous listener to this project's sollecito field
                        val projectSollecitoListener = db.collection("progetti")
                            .document(newProjectId)
                            .addSnapshotListener { snapshot, listenerError ->
                                if (listenerError != null) {
                                    Log.e("ProjectSollecitoListener", "Error listening to project $newProjectId", listenerError)
                                    return@addSnapshotListener
                                }

                                if (snapshot != null && snapshot.exists()) {
                                    val currentSollecitato = snapshot.getBoolean("sollecitato") ?: false
                                    if (currentSollecitato) {
                                        coroutineScope.launch {
                                            val creatorId = snapshot.getString("creator") ?: ""
                                            val creatorName = userService.getUserById(creatorId)?.name ?: "Manager"
                                            val projectTitle = snapshot.getString("title") ?: "progetto"

                                            sendNotification(
                                                type = "sollecito",
                                                title = "Sollecito Progetto",
                                                text = "Il manager $creatorName richiede aggiornamenti sul progetto $projectTitle",
                                                role = Role.Leader,
                                                recipientId = leaderId,
                                                projectId = newProjectId,
                                                taskId = null
                                            )
                                        }
                                    }
                                }
                            }

                        // Store and track this listener
                        sollecitoListeners["project_$newProjectId"] = projectSollecitoListener
                        addListener(projectSollecitoListener)
                    }
                }
            }

        // Add the new project listener to active listeners
        addListener(newProjectListener)
    }


    fun setupDeveloperSollecitoListeners(developerId: String, coroutineScope: CoroutineScope) {


        // Listener for tracking new tasks assigned to the developer
        val newTaskListener = db.collection("progetti")
            .whereArrayContains("developersIds", developerId)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e("DeveloperNewTaskListener", "Error listening to new tasks", e)
                    return@addSnapshotListener
                }

                snapshots?.documentChanges?.forEach { projectDocumentChange ->
                    if (projectDocumentChange.type == DocumentChange.Type.ADDED ||
                        projectDocumentChange.type == DocumentChange.Type.MODIFIED) {

                        val projectId = projectDocumentChange.document.id

                        // Listener for tasks in this project
                        val taskListener = db.collection("progetti")
                            .document(projectId)
                            .collection("task")
                            .whereEqualTo("assignedTo", developerId)
                            .addSnapshotListener { taskSnapshots, taskError ->
                                if (taskError != null) {
                                    Log.e("DeveloperTaskListener", "Error listening to tasks", taskError)
                                    return@addSnapshotListener
                                }

                                taskSnapshots?.documentChanges?.forEach { taskDocumentChange ->
                                    if (taskDocumentChange.type == DocumentChange.Type.ADDED ||
                                        taskDocumentChange.type == DocumentChange.Type.MODIFIED) {

                                        val taskDoc = taskDocumentChange.document
                                        val taskId = taskDoc.id
                                        val sollecitato = taskDoc.getBoolean("sollecitato") ?: false

                                        if (sollecitato) {
                                            coroutineScope.launch {
                                                sendNotification(
                                                    type = "sollecito",
                                                    title = "Sollecito Progetto",
                                                    text = "Il leader ${taskDoc.getString("creator")?.let { userService.getUserById(it)?.name }} richiede aggiornamenti sul progetto ${taskDoc.getString("title")}",
                                                    role = Role.Developer,
                                                    recipientId = developerId,
                                                    projectId = projectId,
                                                    taskId = taskId
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                        // Store and track this listener
                        sollecitoListeners["task_$projectId"] = taskListener
                        addListener(taskListener)
                    }
                }
            }

        // Add the new task listener to active listeners
        addListener(newTaskListener)
    }

    private fun handleChatNotifications(role: Role, userId: String, coroutineScope: CoroutineScope) {

        userChatCollectionListener = db.collection("utenti")
            .document(userId)
            .collection("chat")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e("UserChatListener", "Error listening for user chat changes", e)
                    return@addSnapshotListener
                }

                if (snapshots == null) return@addSnapshotListener

                for (dc in snapshots.documentChanges) {

                    val chatId = dc.document.id
                    Log.d("UserChatListener modifica", "Chat ID: $chatId")
                    when (dc.type) {
                        DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                            if (!activeChatListeners.containsKey(chatId)) {
                                setupChatListener(chatId, userId, role, coroutineScope)
                            }
                        }
                        DocumentChange.Type.REMOVED -> {
                            // Remove listener for this chat
                            activeChatListeners[chatId]?.remove()
                            activeChatListeners.remove(chatId)
                        }
                    }
                }
            }

         addListener(userChatCollectionListener!!)
    }

    private fun setupChatListener(chatId: String, userId: String, role: Role, coroutineScope: CoroutineScope) {
      //inseridci ultimo timestamp per evitare duplicati
        var lastSeenTimestamp: Long = 0

        val chatListener = db.collection("chat")
            .document(chatId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("ChatListener", "Error listening to chat $chatId", e)
                    return@addSnapshotListener
                }

                if (snapshot == null || !snapshot.exists()) return@addSnapshotListener


                val lastMessage = snapshot.getString("lastMessage") ?: return@addSnapshotListener
                val currentTimestamp = snapshot.getLong("timestamp") ?: return@addSnapshotListener
                val senderId = snapshot.getString("senderId") ?: return@addSnapshotListener

                // solo se:
                // 1. è un nuovo messaggio (timestamp maggiore dell'ultimo)
                // 2. il sender non è l'utente
                // 3. il messaggio non è vuoto
                if (currentTimestamp > lastSeenTimestamp &&
                    senderId != userId &&
                    lastMessage.isNotEmpty()) {

                    coroutineScope.launch {
                        sendNotification(
                            type = "chat",
                            title = "Nuovo messaggio",
                            text = "Hai ricevuto un nuovo messaggio: $lastMessage da ${
                                userService.getUserById(senderId)?.name
                            }",
                            role = role,
                            recipientId = userId,
                            projectId = chatId,
                            taskId = currentTimestamp.toString()
                        )
                    }

                    // aggiorna l'ultimo timestamp
                    lastSeenTimestamp = currentTimestamp
                }
            }


        activeChatListeners[chatId] = chatListener
        addListener(chatListener)
    }


    private suspend fun handleProgressNotifications(role: Role, userId: String, coroutineScope: CoroutineScope) {
        Log.d("ProgressNotifications", "Entrato nella funzione handleProgressNotifications con ruolo: $role, nome: $userId")

        if (role == Role.Manager) {
            // Sostituzione della query una tantum con un listener persistente
            val listener = db.collection("progetti")
                .whereEqualTo("creator", userId)
                .addSnapshotListener { snapshots, e ->
                    if (e != null) {
                        Log.e("ProgressNotifications", "Error listening to manager projects", e)
                        return@addSnapshotListener
                    }

                    if (snapshots == null) return@addSnapshotListener

                    for (document in snapshots.documents) {
                        val projectId = document.id
                        val progress = document.getLong("progress")
                        Log.d("ProgressNotifications", "Progresso del progetto $projectId: $progress")
                        if (progress?.toInt() == 100 && !isNotificationShown("progresso_$projectId")) {
                            Log.d("ProgressNotifications", "Progetto $projectId completato al 100%")
                            coroutineScope.launch {
                                sendNotification(
                                    type = "progresso",
                                    title = "Progetto completato",
                                    text = "Il progetto ${document.getString("title")} è stato completato.",
                                    role = role,
                                    recipientId = userId,
                                    projectId = projectId,
                                    taskId = null
                                )
                            }
                            setNotificationShown("progresso_$projectId")
                        }
                    }
                }

            addListener(listener)
        } else if (role == Role.Leader) {
            // Per i Leader, dobbiamo monitorare i progetti a cui sono assegnati
            val listenerForLeaderProjects = db.collection("progetti")
                .whereEqualTo("assignedTo", userId)
                .addSnapshotListener { snapshots, e ->
                    if (e != null) {
                        Log.e("ProgressNotifications", "Error listening to leader's projects", e)
                        return@addSnapshotListener
                    }

                    if (snapshots == null) return@addSnapshotListener

                    for (projectDoc in snapshots.documents) {
                        val projectId = projectDoc.id

                        // Per ogni progetto, monitorare i suoi task
                        val taskListener = db.collection("progetti")
                            .document(projectId)
                            .collection("task")
                            .addSnapshotListener { taskSnapshots, taskError ->
                                if (taskError != null) {
                                    Log.e("ProgressNotifications", "Error listening to tasks for project $projectId", taskError)
                                    return@addSnapshotListener
                                }

                                if (taskSnapshots == null) return@addSnapshotListener

                                for (taskChange in taskSnapshots.documentChanges) {
                                    // Interessati solo alle modifiche dei task esistenti
                                    if (taskChange.type == DocumentChange.Type.MODIFIED) {
                                        val taskDoc = taskChange.document
                                        val taskId = taskDoc.id
                                        val taskProgress = taskDoc.getLong("progress")

                                        // Se il task è completato (100%) e non abbiamo già mostrato la notifica
                                        if (taskProgress?.toInt() == 100 && !isNotificationShown("progresso_task_${taskId}")) {
                                            Log.d("ProgressNotifications", "Task $taskId completato al 100% nel progetto $projectId")

                                            coroutineScope.launch {
                                                sendNotification(
                                                    type = "progresso",
                                                    title = "Task completato",
                                                    text = "Il task ${taskDoc.getString("title")} è stato completato.",
                                                    role = role,
                                                    recipientId = userId,
                                                    projectId = projectId,
                                                    taskId = taskId
                                                )
                                            }

                                            setNotificationShown("progresso_task_${taskId}")
                                        }
                                    }
                                }
                            }

                        // Aggiungi anche il listener dei task alla lista dei listener attivi
                        addListener(taskListener)
                    }
                }

            // Aggiungi il listener dei progetti del leader alla lista dei listener attivi
            addListener(listenerForLeaderProjects)
        } else if (role == Role.Developer) {
            // Per i Developer, dobbiamo monitorare i subtask a cui sono assegnati
            val tasks = taskService.filterTaskByDeveloper(userId)

            tasks.forEach { task ->
                if (task.taskId != null && task.projectId.isNotEmpty()) {
                    // Monitorare i subtask di ogni task assegnato al developer
                    val subtaskListener = db.collection("progetti")
                        .document(task.projectId)
                        .collection("task")
                        .document(task.taskId)
                        .collection("subtask")
                        .whereEqualTo("assignedTo", userId)
                        .addSnapshotListener { subtaskSnapshots, subtaskError ->
                            if (subtaskError != null) {
                                Log.e("ProgressNotifications", "Error listening to subtasks for task ${task.taskId}", subtaskError)
                                return@addSnapshotListener
                            }

                            if (subtaskSnapshots == null) return@addSnapshotListener

                            for (subtaskChange in subtaskSnapshots.documentChanges) {
                                // Interessati solo alle modifiche dei subtask esistenti
                                if (subtaskChange.type == DocumentChange.Type.MODIFIED) {
                                    val subtaskDoc = subtaskChange.document
                                    val subtaskId = subtaskDoc.id
                                    val subtaskProgress = subtaskDoc.getLong("progress")

                                    // Se il subtask è completato (100%) e non abbiamo già mostrato la notifica
                                    if (subtaskProgress?.toInt() == 100 && !isNotificationShown("progresso_subtask_${subtaskId}")) {
                                        Log.d("ProgressNotifications", "Subtask $subtaskId completato al 100%")

                                        coroutineScope.launch {
                                            sendNotification(
                                                type = "progresso",
                                                title = "Subtask completato",
                                                text = "Il subtask ${subtaskDoc.getString("title")} è stato completato.",
                                                role = role,
                                                recipientId = userId,
                                                projectId = task.projectId,
                                                taskId = task.taskId
                                            )
                                        }

                                        setNotificationShown("progresso_subtask_${subtaskId}")
                                    }
                                }
                            }
                        }

                    // Aggiungi il listener dei subtask alla lista dei listener attivi
                    addListener(subtaskListener)
                }
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
        // Create a default intent that points to HomeActivity
        val intent = Intent(context, HomeItemActivity::class.java).apply {
            putExtra("projectId", projectId)
            putExtra("taskId", taskId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        var channelId = when (type) {
            "sollecito" -> {
                if (role == Role.Developer) {
                    taskService.elimina_sollecita(projectId!!, taskId!!)
                } else if (role == Role.Leader) {
                    projectService.elimina_sollecita(projectId!!)
                }
                "it.sollecito"
            }

            "progresso" -> {
                if (role == Role.Leader) {
                        intent.putExtra("isSubitem", true)

                    }
                    "it.newprogress"
            }

            "chat" -> {
                // Override default intent for chat notifications
                intent.setClass(context, ChatActivity::class.java)
                intent.putExtra("chatId", projectId)
                intent.putExtra("timestamp", taskId)
                "it.newmessage"
            }
            else -> "it.default"
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.username)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

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
