package com.example.project_manager.utils

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
import com.google.firebase.firestore.*

class NotificationHelper(private val context: Context, private val db: FirebaseFirestore) {

    private val sharedPreferences = context.getSharedPreferences("NotificationPrefs", Context.MODE_PRIVATE)
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel("it.sollecito", "Sollecito Notification", "Notifiche per le richieste di soluzione")
            createNotificationChannel("it.newprogress", "Progress Notification", "Notifiche per il completamento dei progetti")
            createNotificationChannel("it.newmessage", "Chat Notification", "Notifiche per nuove chat e messaggi")
        }
    }

    fun handleNotification(role: Role, userUid: String, type: String, data: List<Chat>? = null) {
        when (type) {
            "sollecito" -> handleSollecitoNotification(userUid)
            "chat" -> data?.let { handleChatNotifications(role, userUid, it) }
            "progresso" -> handleProgressNotifications(role, userUid)
        }
    }

    private fun handleSollecitoNotification(recipientId: String) {
        sendNotification(
            type = "sollecito",
            title = "Sollecito",
            text = "Ricordati di procedere con i compiti da svolgere.",
            recipientId = recipientId,
            projectId = null,
            taskId = null
        )
    }

    private fun handleChatNotifications(role: Role, userId: String, chatIds: List<Chat>) {
        chatIds.forEach { chatId ->
            db.collection("chat").document(chatId.chatId).addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("ChatListener", "Errore durante l'ascolto della chat ${chatId.chatId}.", e)
                    return@addSnapshotListener
                }

                snapshot?.get("lastMessage")?.let { message ->
                    val timestamp = snapshot.get("timestamp").toString()
                    if(snapshot.get("senderId").toString() !== userId.toString()) {
                        sendNotification(
                            type = "chat",
                            title = "Nuovo messaggio",
                            text = "Hai ricevuto un nuovo messaggio: $message",
                            recipientId = userId,
                            projectId = chatId.chatId,
                            taskId = timestamp
                        )
                    }
                }
            }
        }
    }

    private fun handleProgressNotifications(role: Role, userId: String) {
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
                        sendNotification(
                            type = "progresso",
                            title = "Progetto completato",
                            text = "Il progetto ${document.get("titolo")} è stato completato.",
                            recipientId = userId,
                            projectId = projectId,
                            taskId = null
                        )
                        setNotificationShown("progresso_$projectId")
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
                                        sendNotification(
                                            type = "progresso",
                                            title = "Task completato",
                                            text = "Il task ${document.get("titolo")} è stato completato.",
                                            recipientId = userId,
                                            projectId = projectId,
                                            taskId = change.document.id
                                        )
                                        setNotificationShown("progresso_${change.document.id}")
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

    private fun sendNotification(
        type: String,
        title: String,
        text: String,
        recipientId: String,
        projectId: String?,
        taskId: String?
    ) {
        var intent: Intent? = null
        var canale: String? = null

        if (type == "sollecito" || type == "progresso") {
            canale = if (type == "sollecito") "it.sollecito" else "it.newprogress"
            intent = Intent(context, ItemActivity::class.java).apply {
                putExtra("projectId", projectId)
                putExtra("taskId", taskId)
            }
        } else if (type == "chat") {
            canale = "it.newmessage"
            intent = Intent(context, ChatActivity::class.java).apply {
                putExtra("chatID", projectId)
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