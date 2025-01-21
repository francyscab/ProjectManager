package com.example.project_manager

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ChatListActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var chatListAdapter: ChatListAdapter

    private val chats = mutableListOf<Chat>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_list)

        db = FirebaseFirestore.getInstance()
        recyclerView = findViewById(R.id.recyclerViewChatList)
        val role=intent.getStringExtra("role")?:""
        Log.d("ChatListActivity", "Role: $role")

        val startChatButton: Button = findViewById(R.id.buttonStartNewChat)
        startChatButton.setOnClickListener {
            showSelectUserDialog(role)
        }

        chatListAdapter = ChatListAdapter(chats) { chat ->
            Log.d("ChatListActivity", "Chat selected with ID: ${chat.chatId}")
            openChat(chat)
        }

        recyclerView.adapter = chatListAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        loadChats()
    }

    private suspend fun getDeveloperUsers(currentUserName: String): List<User> {
        val users = mutableListOf<User>()
        val userSet = mutableSetOf<String>() // Per evitare duplicati

        try {
            val projects = db.collection("progetti").get().await()

            for (project in projects) {
                val projectId = project.id
                val projectLeaderName = project.getString("leader")
                Log.d("ChatListActivity", "Project found: $projectId")

                val tasks = db.collection("progetti")
                    .document(projectId)
                    .collection("task")
                    .get()
                    .await()

                var isDeveloperInProject = false
                for (task in tasks) {
                    val developerName = task.getString("developer")
                    if (developerName == currentUserName) {
                        isDeveloperInProject = true
                    }
                }

                if (isDeveloperInProject) {
                    // Aggiungi il leader del progetto
                    projectLeaderName?.let {
                        val leaderEmail = getUserEmailByName(it)
                        if (leaderEmail.isNotEmpty() && !userSet.contains(it)) {
                            User(it, leaderEmail).let { user ->
                                users.add(user)
                                userSet.add(it)
                            }
                        }
                    }

                    // Recupera gli altri developer dei task
                    val tasksForProject = db.collection("progetti")
                        .document(projectId)
                        .collection("task")
                        .get()
                        .await()

                    for (task in tasksForProject) {
                        val developerName = task.getString("developer")
                        if (developerName != null && developerName != currentUserName && !userSet.contains(developerName)) {
                            val email = getUserEmailByName(developerName)
                            if (email.isNotEmpty()) {
                                User(developerName, email).let { user ->
                                    users.add(user)
                                    userSet.add(developerName)
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("ChatListActivity", "Error getting projects or tasks.", e)
        }
        return users
    }

    private fun showUserSelectionDialog(users: List<User>) {
        val userNames = users.map { it.name } // Estrai solo i nomi

        val builder = AlertDialog.Builder(this@ChatListActivity)
        builder.setTitle("Select User")

        builder.setItems(userNames.toTypedArray()) { _, which ->
            val selectedUser = users[which]
            startChatWithUser(selectedUser)
        }

        builder.show()
    }

    private fun showSelectUserDialogForDeveloper(role: String) {
        lifecycleScope.launch {
            val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email ?: ""
            val currentUserName = getUserNameByEmail(currentUserEmail)
            var users = listOf<User>()

            if(role=="developer"){
                // Recupera gli utenti che sono developer nei miei stessi progetti e dei leader dei miei progetti
                users = getDeveloperUsers(currentUserName)
            }else if(role=="Leader") {
                // Recupera gli utenti che sono developer
                users = getLeaderUsers(currentUserName)
            }
            

            // Mostra il dialog con gli utenti
            showUserSelectionDialog(users)
        }
    }

    private fun getLeaderUsers(currentUserName: String): List<User> {

    }
    /*private fun showSelectUserDialog(role: String) {
        val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email ?: ""
        var currentUserName = "" // Nome dell'utente loggato

        // Recupera il nome dell'utente loggato usando coroutines
        lifecycleScope.launch {
            currentUserName = getUserNameByEmail(currentUserEmail)

            // Inizializza le liste degli utenti
            val users = mutableListOf<User>()
            val userNames = mutableListOf<String>()
            val userSet = mutableSetOf<String>() // Per evitare duplicati
            val projectIdsWithDeveloper = mutableListOf<String>()

            Log.d("ChatListActivity", "Role: $role")

            if (role == "Developer") {
                Log.d("ChatListActivity", "Sono un developer")

                // Recupera i progetti
                try {
                    val projects = db.collection("progetti").get().await()

                    for (project in projects) {
                        val projectId = project.id
                        val projectLeaderName = project.getString("leader")
                        Log.d("ChatListActivity", "Project found: $projectId")

                        // Per ogni progetto, controlla i task e verifica se sei uno dei developer
                        val tasks = db.collection("progetti")
                            .document(projectId)
                            .collection("task")
                            .get()
                            .await()

                        var isDeveloperInProject = false
                        for (task in tasks) {
                            val developerName = task.getString("developer") // Recupera il nome del developer

                            // Confronta il nome del developer con il nome dell'utente loggato
                            if (developerName == currentUserName) {
                                isDeveloperInProject = true
                            }
                        }

                        // Se sei un developer in questo progetto, aggiungi l'ID del progetto alla lista
                        if (isDeveloperInProject) {
                            projectIdsWithDeveloper.add(projectId)

                            // Aggiungi il leader del progetto
                            projectLeaderName?.let {
                                val leaderEmail = getUserEmailByName(it)
                                if (leaderEmail.isNotEmpty() && !userSet.contains(it)) {
                                    User(it, leaderEmail).let { user ->
                                        users.add(user)
                                        userNames.add(it)
                                        userSet.add(it)
                                    }
                                }
                            }

                            // Recupera gli altri developer dei task
                            val tasksForProject = db.collection("progetti")
                                .document(projectId)
                                .collection("task")
                                .get()
                                .await()

                            for (task in tasksForProject) {
                                val developerName = task.getString("developer")

                                if (developerName != null && developerName != currentUserName && !userSet.contains(developerName)) {
                                    // Recupera l'email dell'utente dal nome
                                    val email = getUserEmailByName(developerName)
                                    if (email.isNotEmpty()) {
                                        User(developerName, email).let { user ->
                                            users.add(user)
                                            userNames.add(developerName)
                                            userSet.add(developerName)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Mostra il dialog per selezionare l'utente
                    val builder = AlertDialog.Builder(this@ChatListActivity)
                    builder.setTitle("Select User")

                    builder.setItems(userNames.toTypedArray()) { _, which ->
                        val selectedUser = users[which]
                        startChatWithUser(selectedUser)
                    }

                    builder.show()

                } catch (e: Exception) {
                    Log.w("ChatListActivity", "Error getting projects or tasks.", e)
                }
            }
        }
    }*/


    // Funzione sospesa per recuperare il nome dell'utente
    private suspend fun getUserNameByEmail(email: String): String {
        return try {
            val querySnapshot = db.collection("utenti")
                .whereEqualTo("email", email)
                .get()
                .await()

            if (!querySnapshot.isEmpty) { // Corretto isNotEmpty con !isEmpty
                querySnapshot.documents.first().getString("name") ?: ""
            } else {
                ""
            }
        } catch (e: Exception) {
            Log.w("ChatListActivity", "Error getting user name.", e)
            ""
        }
    }

    // Funzione per recuperare l'email dato un nome
    private suspend fun getUserEmailByName(name: String): String {
        return try {
            val querySnapshot = db.collection("utenti")
                .whereEqualTo("name", name)
                .get()
                .await()

            if (!querySnapshot.isEmpty) { // Corretto isNotEmpty con !isEmpty
                querySnapshot.documents.first().getString("email") ?: ""
            } else {
                ""
            }
        } catch (e: Exception) {
            Log.w("ChatListActivity", "Error getting user email by name.", e)
            ""
        }
    }

    private fun startChatWithUser(user: User) {
        // Recupera l'email dell'utente loggato
        val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email ?: ""
        Log.d("ChatListActivity", "Current User Email: $currentUserEmail")

        // Genera l'ID della chat utilizzando le email degli utenti
        val chatId = generateChatId(currentUserEmail, user.email)

        // Crea il documento della chat
        val chatData = hashMapOf(
            "chatID" to chatId,
            "lastMessage" to "",
            "timestamp" to System.currentTimeMillis(),
            "unreadCount" to 0,
            "user1" to currentUserEmail, // Salva l'email dell'utente loggato
            "user2" to user.email       // Salva l'email dell'altro utente
        )

        // Aggiungi il documento della chat al database
        db.collection("chat")
            .document(chatId)
            .set(chatData)
            .addOnSuccessListener {
                Log.d("ChatListActivity", "Chat created with ID: $chatId")
                openChatWithUser(chatId)
            }
            .addOnFailureListener { e ->
                Log.w("ChatListActivity", "Error creating chat.", e)
            }
    }


    private fun generateChatId(currentUserEmail: String, otherUserEmail: String): String {
        // Genera un chatID unico per la coppia di utenti usando le loro email
        val sortedEmails = listOf(currentUserEmail, otherUserEmail).sorted()
        return sortedEmails.joinToString("_")
    }

    private fun openChatWithUser(chatId: String) {
        val intent = Intent(this, ChatActivity::class.java)
        intent.putExtra("chatId", chatId)
        startActivity(intent)
    }

    private fun loadChats() {
        // Recupera l'email dell'utente loggato
        val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email ?: ""
        Log.d("ChatListActivity", "Current User Email: $currentUserEmail")

        // Modifica la query per filtrare solo le chat in cui l'utente loggato è user1 o user2
        db.collection("chat")
            .whereEqualTo("user1", currentUserEmail)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("ChatListActivity", "Listen failed.", e)
                    return@addSnapshotListener
                }

                chats.clear()
                if (snapshots != null) {
                    for (doc in snapshots) {
                        // Stampa il contenuto del documento
                        Log.d("ChatListActivity", "Document ID: ${doc.id}, Data: ${doc.data}")

                        // Estrai i dati manualmente dal documento Firestore
                        val chatId = doc.getString("chatID") ?: ""
                        val lastMessage = doc.getString("lastMessage") ?: ""
                        val timestamp = doc.getLong("timestamp") ?: 0L
                        val unreadCount = doc.getLong("unreadCount")?.toInt() ?: 0
                        val user1 = doc.getString("user1") ?: ""
                        val user2 = doc.getString("user2") ?: ""

                        // Crea l'oggetto Chat
                        val chat = Chat(
                            chatId = chatId,
                            lastMessage = lastMessage,
                            timestamp = timestamp,
                            unreadCount = unreadCount,
                            user1 = user1,
                            user2 = user2
                        )

                        // Aggiungi l'oggetto Chat alla lista
                        chats.add(chat)

                        // Debug: stampa l'oggetto Chat aggiunto
                        Log.d("ChatListActivity", "Chat added with ID: ${chat.chatId}")
                    }
                    // Debug: stampa il numero di chat trovate
                    Log.d("ChatListActivity", "Chats found: ${chats.size}")
                } else {
                    Log.d("ChatListActivity", "No documents found.")
                }

                // Aggiorna l'adapter (decommenta quando serve)
                chatListAdapter.notifyDataSetChanged()
            }

        // Inoltre, puoi aggiungere un'altra query per controllare se l'utente è `user2`
        db.collection("chat")
            .whereEqualTo("user2", currentUserEmail)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("ChatListActivity", "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    for (doc in snapshots) {
                        // Stampa il contenuto del documento
                        Log.d("ChatListActivity", "Document ID: ${doc.id}, Data: ${doc.data}")

                        // Estrai i dati manualmente dal documento Firestore
                        val chatId = doc.getString("chatID") ?: ""
                        val lastMessage = doc.getString("lastMessage") ?: ""
                        val timestamp = doc.getLong("timestamp") ?: 0L
                        val unreadCount = doc.getLong("unreadCount")?.toInt() ?: 0
                        val user1 = doc.getString("user1") ?: ""
                        val user2 = doc.getString("user2") ?: ""

                        // Crea l'oggetto Chat
                        val chat = Chat(
                            chatId = chatId,
                            lastMessage = lastMessage,
                            timestamp = timestamp,
                            unreadCount = unreadCount,
                            user1 = user1,
                            user2 = user2
                        )

                        // Aggiungi l'oggetto Chat alla lista
                        chats.add(chat)

                        // Debug: stampa l'oggetto Chat aggiunto
                        Log.d("ChatListActivity", "Chat added with ID: ${chat.chatId}")
                    }
                    // Debug: stampa il numero di chat trovate
                    Log.d("ChatListActivity", "Chats found: ${chats.size}")
                } else {
                    Log.d("ChatListActivity", "No documents found.")
                }

                // Aggiorna l'adapter (decommenta quando serve)
                chatListAdapter.notifyDataSetChanged()
            }
    }


    private fun openChat(chat: Chat) {
        val intent = Intent(this, ChatActivity::class.java)
        intent.putExtra("chatId", chat.chatId)
        startActivity(intent)
    }
}
