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

    private suspend fun getUsersWithRoleDeveloper(currentUserName: String): List<User> {
        val users = mutableListOf<User>()
        val userSet = mutableSetOf<String>() // Per tracciare i nomi univoci

        try {
            // Recupera gli utenti con ruolo "Developer"
            val userDocuments = db.collection("utenti")
                .whereEqualTo("role", "Developer")
                .get()
                .await()

            for (userDocument in userDocuments) {
                val name = userDocument.getString("name")
                val email = userDocument.getString("email")

                // Controlla che nome ed email siano validi e che il nome non sia duplicato
                if (!name.isNullOrEmpty() && !email.isNullOrEmpty() && userSet.add(name)) {
                    users.add(User(name, email))
                }
            }

            // Recupera i progetti di cui l'utente è il leader
            val projects = db.collection("progetti")
                .whereEqualTo("leader", currentUserName)
                .get()
                .await()

            // Aggiungi i manager dei progetti in cui l'utente è leader
            for (project in projects) {
                val managerName = project.getString("manager") // Recupera il nome del manager
                val managerEmail = project.getString("managerEmail") // Recupera l'email del manager

                // Aggiungi il manager se non è già presente nella lista
                if (!managerName.isNullOrEmpty() && !managerEmail.isNullOrEmpty() && userSet.add(managerName)) {
                    users.add(User(managerName, managerEmail))
                }
            }
        } catch (e: Exception) {
            Log.w("ChatListActivity", "Error fetching users with role Developer and project managers.", e)
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

    private fun showSelectUserDialog(role: String) {
        lifecycleScope.launch {
            val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email ?: ""
            val currentUserName = getUserNameByEmail(currentUserEmail)
            var users = listOf<User>()

            if(role=="Developer"){
                Log.d("ChatListActivity", "Sono un developer")
                // Recupera gli utenti che sono developer nei miei stessi progetti e dei leader dei miei progetti
                users = getDeveloperUsers(currentUserName)
                Log.d("ChatListActivity", "Users: $users")
            }else if(role=="Leader") {
                    Log.d("ChatListActivity", "Sono un leader")
                // Recupera gli utenti che sono developer e i manager dei miei progetti
                users = getUsersWithRoleDeveloper(currentUserName)
                Log.d("ChatListActivity", "Users: $users")
            }else if(role=="Manager"){
                Log.d("ChatListActivity", "Sono un manager")
                //recupera i leader
                users = getUsersForManager()
                Log.d("ChatListActivity", "Users: $users")
            }
            

            // Mostra il dialog con gli utenti
            showUserSelectionDialog(users)
        }
    }

    private suspend fun getUsersForManager(): List<User> {
        val users = mutableListOf<User>()
        val userSet = mutableSetOf<String>() // Per tracciare i nomi univoci

        try {
            // Recupera tutti gli utenti con il ruolo di "leader"
            val userDocuments = db.collection("utenti")
                .whereEqualTo("role", "Leader")
                .get()
                .await()

            // Aggiungi ogni leader alla lista, evitando duplicati
            for (userDocument in userDocuments) {
                val name = userDocument.getString("name")
                val email = userDocument.getString("email")

                // Controlla che il nome e l'email siano validi e che il nome non sia duplicato
                if (!name.isNullOrEmpty() && !email.isNullOrEmpty() && userSet.add(name)) {
                    users.add(User(name, email))
                }
            }
        } catch (e: Exception) {
            Log.w("ChatListActivity", "Error fetching leaders.", e)
        }

        return users
    }


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
