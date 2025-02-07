/*package com.example.project_manager // Cambia il package in base al tuo progetto

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.firestore.*

class NotificationHelper(private val context: Context, private val db: FirebaseFirestore) {

    private var notificationManager: NotificationManager? = null
    private val resultIntent: Intent = Intent(context, ItemActivity::class.java)

    fun notification(role: String, name: String, type: String, data: List<String>? = null) {
        if (type == "sollecito") {
            // Se il tipo è "sollecito", invia subito la notifica senza aggiungere listener
            sendNotificationProgress(type,name,role)
            return
        }
        if(type=="chat"){
            Log.d("Notification", "sto mettendo listener sulle chat di $name")
            if (data != null) {
                Log.d("Notification", "dati ricevuti: $data")
                addChatListener(type,name,role,data)
            }
            return
        }

        val query = when (role) {
            "Manager" -> db.collection("progetti").whereEqualTo("creator", name)
            "Leader" -> db.collection("progetti").whereEqualTo("leader", name)
            else -> null
        }

        query?.get()?.addOnSuccessListener { documents ->
            if (documents.isEmpty) {
                Log.d("FirestoreQuery", "Nessun documento trovato per il ruolo $role.")
            } else {
                for (document in documents) {
                    val projectId = document.id
                    Log.d("FirestoreQuery", "Documento trovato: $projectId, dati: ${document.data}")

                    if (role == "Manager") {
                        addProjectListener(projectId, type,name,role)
                    } else if (role == "Leader") {
                        addTaskListener(projectId, type,name,role)
                    }
                }
            }
        }?.addOnFailureListener { e ->
            Log.e("FirestoreQuery", "Errore durante l'esecuzione della query per il ruolo $role.", e)
        }
    }


    private fun addChatListener( type: String, name: String, role: String,chatIds: List<String>) {
        // Itera su ogni chatId nell'elenco
        for (chatId in chatIds) {
            val chatQuery = db.collection("chat").document(chatId)

            chatQuery.addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("ChatListener", "Errore durante l'ascolto della chat $chatId.", e)
                    return@addSnapshotListener
                }

                snapshot?.let {
                    val newMessage = it.get("lastMessage")
                    val newTimestamp = it.get("timestamp")

                    if (newMessage != null) {
                        Log.d("Firestore", "Nuovo messaggio nella chat $chatId: $newMessage")

                        // Invia una notifica in base al tipo e ai dettagli
                        sendNotificationChat(type, name, role, chatId, newMessage.toString(), newTimestamp.toString())
                    } else {
                        Log.d("Firestore", "Nessun nuovo messaggio nella chat $chatId.")
                    }
                }
            }
        }
    }

    private fun addProjectListener(projectId: String, type: String,name: String,role: String) {
        val projectQuery = db.collection("progetti").document(projectId)

        projectQuery.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e("ProjectListener", "Errore durante l'ascolto del progetto $projectId.", e)
                return@addSnapshotListener
            }

            snapshot?.let {
                val newProgress = it.get("progress")
                if (type == "progresso" && newProgress.toString() == "100") {
                    Log.d("Firestore", "Il progetto $projectId è completo.")
                    sendNotificationProgress(type,name,role,projectId)
                } else {
                    Log.d("Firestore", "Progress del progetto aggiornato: $newProgress")
                }
            }
        }
    }

    private fun addTaskListener(projectId: String, type: String, name: String, role: String) {
        val taskQuery = db.collection("progetti").document(projectId).collection("task")

        taskQuery.addSnapshotListener { taskSnapshots, e ->
            if (e != null) {
                Log.e("TaskListener", "Errore durante l'ascolto dei task per il progetto $projectId.", e)
                return@addSnapshotListener
            }

            taskSnapshots?.let {
                for (taskChange in it.documentChanges) {
                    if (taskChange.type == DocumentChange.Type.MODIFIED) {
                        val task = taskChange.document
                        val newProgress = task.get("progress")
                        val taskId = task.id  // Ottieni l'ID del task

                        if (type == "progresso" && newProgress.toString() == "100") {
                            Log.d("Firestore", "Il task $taskId del progetto $projectId è completo.")
                            sendNotificationProgress(type, name, role, projectId, taskId)  // Passa anche taskId
                        } else if (type == "sollecito") {
                            Log.d("Firestore", "Sollecito per il task $taskId del progetto $projectId.")
                            sendNotificationProgress(type, name, role, projectId, taskId)  // Passa anche taskId
                        } else {
                            Log.d("Firestore", "Progress del task aggiornato: $newProgress")
                        }
                    }
                }
            }
        }
    }

    private fun sendNotificationChat(type: String, name: String, role: String, chatId: String, message: String, timestamp: String) {
        val channelID = "it.newchat"
        val channelName = "Chat Notification"
        val channelDescription = "Notifiche per nuove chat e messaggi"

        Log.w("NotificationHelper", "Sending notification: type=$type, name=$name, role=$role, chatId=$chatId, message=$message, timestamp=$timestamp")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(channelID, channelName, channelDescription)
        }

        val resultIntent = Intent(context, ChatActivity::class.java).apply {
            // Aggiungi qui i dati relativi alla chat, al ruolo e al nome
            putExtra("chatId", chatId)
            putExtra("role", role)
            putExtra("name", name)
        }

        // Crea un PendingIntent che si attiverà quando l'utente clicca sulla notifica
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            resultIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationTitle: String
        val notificationText: String

        when (type) {
            "new_message" -> {
                notificationTitle = "Nuovo messaggio"
                notificationText = "Hai ricevuto un nuovo messaggio nella chat: $message"
            }
            else -> {
                notificationTitle = "Notifica Chat"
                notificationText = "Hai ricevuto un aggiornamento nella chat."
            }
        }

        // Crea la notifica
        val notification = NotificationCompat.Builder(context, channelID)
            .setSmallIcon(R.drawable.username) // Sostituisci con l'icona della tua app
            .setContentTitle(notificationTitle)
            .setContentText(notificationText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent) // Imposta l'intent da eseguire al clic
            .setAutoCancel(true) // La notifica scompare quando viene cliccata
            .build()

        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager?.notify(1, notification) // Mostra la notifica
    }


    private fun sendNotificationProgress(type: String, name: String, role: String, projectID: String? = null, taskId:String?=null) {
        val channelID = "it.newprogress"
        val channelName = "Progress Notification"
        val channelDescription = "Notifiche per il completamento dei progetti"

        Log.w("NotificationHelper", "Sending notification: type=$type, name=$name, role=$role, projectID=$projectID")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(channelID, channelName, channelDescription)
        }

        if(role=="Manager"){resultIntent.apply {
            // Aggiungi qui i dati del progetto, del ruolo e del nome
            putExtra("projectId", projectID)
            putExtra("role", role)
            putExtra("name", name)
        }}
        else if(role=="Leader"){resultIntent.apply {
            // Aggiungi qui i dati del progetto, del ruolo e del nome
            putExtra("projectId", projectID)
            putExtra("taskId",taskId)
            putExtra("role", role)
            putExtra("name", name)
        }}
        // Creiamo l'Intent per aprire ProjectActivity


        // Crea un PendingIntent che si attiverà quando l'utente clicca sulla notifica
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            resultIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationTitle: String
        val notificationText: String

        when (type) {
            "progresso" -> {
                notificationTitle = "Progetto completato"
                notificationText = "Uno dei tuoi progetti ha raggiunto il 100% di completamento."
            }
            "sollecito" -> {
                notificationTitle = "Sollecito"
                notificationText = "Ricordati di procedere con i compiti da svolgere per il progetto in corso."
            }
            else -> {
                notificationTitle = "Notifica"
                notificationText = "Hai ricevuto un aggiornamento sul progetto."
            }
        }

        // Crea la notifica
        val notification = NotificationCompat.Builder(context, channelID)
            .setSmallIcon(R.drawable.username) // Sostituisci con l'icona della tua app
            .setContentTitle(notificationTitle)
            .setContentText(notificationText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent) // Imposta l'intent da eseguire al clic
            .setAutoCancel(true) // La notifica scompare quando viene cliccata
            .build()

        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager?.notify(1, notification) // Mostra la notifica
    }

    private fun createNotificationChannel(id: String, name: String, description: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(id, name, importance).apply {
                this.description = description
            }
            notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }
}
*/