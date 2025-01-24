package com.example.project_manager

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.ContentValues.TAG
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button

import android.widget.ImageButton

import androidx.appcompat.app.AlertDialog

import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


class LoggedActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private var notificationManager: NotificationManager? = null
    private lateinit var data: ArrayList<ItemsViewModel>
    private lateinit var userName: String
    private lateinit var role: String
    private lateinit var name: String
    private lateinit var chat: List<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logged)


        db = FirebaseFirestore.getInstance()

        loadRecycleView()

        val chatButton = findViewById<ImageButton>(R.id.button_chat)
        chatButton.setOnClickListener {
            val intent = Intent(this, ChatListActivity::class.java)
            Log.d("LoggedActivity", "Role: $role")
            intent.putExtra("role", role)
            startActivity(intent)
        }

        val buttonPerson = findViewById<ImageButton>(R.id.button_person)
        buttonPerson.setOnClickListener {
            val intent = Intent(this, UserProfileActivity::class.java)
            startActivity(intent)
        }
    }

    suspend fun filterTasksByProgress(
        tasks: List<ItemsViewModel>,
        filter: String // "completed" o "incompleted"
    ): List<ItemsViewModel> {
        val db = FirebaseFirestore.getInstance()
        val filteredTasks = mutableListOf<ItemsViewModel>()

        for (task in tasks) {
            try {
                val progress = if (task.taskId.isNullOrEmpty()) {
                    // Caso: Solo projectId
                    val projectDoc = db.collection("progetti")
                        .document(task.projectId)
                        .get()
                        .await()
                    projectDoc.getLong("progress")?.toInt()
                } else {
                    // Caso: projectId e taskId
                    val taskDoc = db.collection("progetti")
                        .document(task.projectId)
                        .collection("task")
                        .document(task.taskId)
                        .get()
                        .await()
                    taskDoc.getLong("progress")?.toInt()
                }

                // Applica il filtro basato sulla stringa "completed" o "incompleted"
                when (filter) {
                    "completed" -> if (progress == 100) filteredTasks.add(task)
                    "incompleted" -> if ((progress ?: 0) < 100) filteredTasks.add(task)
                    else -> throw IllegalArgumentException("Filtro non valido: $filter. Usa 'completed' o 'incompleted'.")
                }
            } catch (e: Exception) {
                // Gestione degli errori (ad esempio documento non trovato)
                e.printStackTrace()
            }
        }

        return filteredTasks
    }


    private fun loadRecycleView() {
        Log.d(TAG, "LOADRECYCLEVIEW")
        //val db = FirebaseFirestore.getInstance()


        //val dataLeader= arrayListOf<ItemsViewModel>()
        val newProject = findViewById<ImageButton>(R.id.newProject)

        //recycleview
        val recyclerview = findViewById<RecyclerView>(R.id.recyclerview)
        recyclerview.layoutManager = LinearLayoutManager(this)
        val notificationHelper = NotificationHelper(this, FirebaseFirestore.getInstance())



        lifecycleScope.launch {
            userName = getUser()
            role = getRole(userName)
            data = loadProjectData()
            name=getName(userName)
            chat=getMyChat(userName)
            Log.d(TAG,"SONO IN LOGGED ACTIVITY E SONO username=$userName ruolo=$role e mi chiamo=$name")


            notificationHelper.notification( role, name, "progresso")

            //listener su bottone per filtrare i progetti per progresso(completati)
            setupFilterButtonCompleted(data, role, name,recyclerview)
            //listener su bottone per filtrare i progetti per progresso(non completati)
            setupFilterButtonIncompleted(data, role, name,recyclerview)
            //listener su bottone per filtrare i progetti per progresso(tutti)
            setupFilterButtonAll(data, role, name,recyclerview)

            if (role == "Manager") {
                Log.d(TAG, "SICCOME SONO IL MANAGER: $userName")
                notificationHelper.notification(role, name, "chat",chat)
                //comportamento bottone nuovo progetto
                newProject.setOnClickListener {
                    val intent = Intent(this@LoggedActivity, NewProjectActivity::class.java)
                    intent.putExtra("tipo_form", "progetto")
                    intent.putExtra("role", role)
                    intent.putExtra("creator", name)
                    Log.d(TAG,"STO CHIAMANDO NEWPROJECTACTIVITY con role= $role e creator= $name E TIPO FORM= progetto")
                    startActivity(intent)
                }
                Log.d(TAG,"STO CHIAMANDO VISULAIZZA CON DATA= $data E ROLE= $role E NAME= $name")
                visualizza(recyclerview, data, role,name)
            } else if (role == "Leader") {
                Log.d(TAG, "SICCOME SONO IL LEADER: $userName")

                //togliere bottone per creare nuovo progetto
                newProject.visibility = View.INVISIBLE

                //MOSTRARE SOLO I PROGETTI DI CUI SI È LEADER--modifico array data
                data = data.filter { it.leader == name } as ArrayList<ItemsViewModel>
                Log.d(TAG,"STO CHIAMANDO VISULAIZZA CON DATA= $data E ROLE= $role E NAME= $name")
                visualizza(recyclerview, data, role,name)
            } else if (role == "Developer") {
                Log.d(TAG, "SICCOME SONO IL DEVELOPER: $userName")

                //togliere bottone per creare nuovo progetto
                newProject.visibility = View.INVISIBLE

                //salvo array di task che hanno come developer quello attualmente loggato
                var tasksForDeveloper = ArrayList<ItemsViewModel>()
                tasksForDeveloper=loadTask(data, name)
                Log.d(TAG,"STO CHIAMANDO VISULAIZZA CON DATA= $data E ROLE= $role E NAME= $name")
                visualizza(recyclerview,tasksForDeveloper,role,name)
            }
            else{
                //errore
            }


        }
    }

    private fun setupFilterButtonCompleted(data: List<ItemsViewModel>, role: String, name: String, recyclerview: RecyclerView) {
        val filterConclusiButton = findViewById<Button>(R.id.buttonConclusi)
        filterConclusiButton.setOnClickListener {
            lifecycleScope.launch {
                try {
                    val filteredTasks = filterTasksByProgress(data, "completed")
                    visualizza(recyclerview, filteredTasks, role, name)
                } catch (e: Exception) {
                    Log.e(TAG, "Errore durante il filtraggio: ${e.message}", e)
                }
            }
        }

    }

    private fun setupFilterButtonIncompleted(data: List<ItemsViewModel>, role: String, name: String, recyclerview: RecyclerView) {
        val filterNonConclusiButton = findViewById<Button>(R.id.buttonInCorso)
        filterNonConclusiButton.setOnClickListener {
            lifecycleScope.launch {
                try {
                    val filteredTasks = filterTasksByProgress(data, "incompleted")
                    visualizza(recyclerview, filteredTasks, role, name)
                } catch (e: Exception) {
                }

            }
        }
    }

    private fun setupFilterButtonAll(
        data: List<ItemsViewModel>,
        role: String,
        name: String,
        recyclerview: RecyclerView
    ) {
        val allButton = findViewById<Button>(R.id.buttonTutti)
        allButton.setOnClickListener {
            lifecycleScope.launch {
                try {
                    Log.d(TAG, "Bottone 'Tutti' cliccato. Ripristino la vista originale.")
                    visualizza(recyclerview, data, role, name)
                } catch (e: Exception) {
                    Log.e(TAG, "Errore durante il reset dei task: ${e.message}", e)
                }
            }
        }
    }
    private suspend fun getMyChat(currentUserEmail: String): List<String> {
        val chatIds = mutableListOf<String>()

        Log.d("ChatListActivity", "Current User Email: $currentUserEmail")

        // Recupera le chat dove l'utente è `user1`
        val queryUser1 = db.collection("chat")
            .whereEqualTo("user1", currentUserEmail)
            .get() // Usa `get()` per una query sincrona

        // Recupera le chat dove l'utente è `user2`
        val queryUser2 = db.collection("chat")
            .whereEqualTo("user2", currentUserEmail)
            .get() // Usa `get()` per una query sincrona

        try {
            // Esegui entrambe le query in parallelo usando coroutines
            val user1Snapshot = queryUser1.await()
            val user2Snapshot = queryUser2.await()

            // Debug: stampa il risultato di queryUser1
            Log.d("ChatListActivity", "Results for user1 query:")
            user1Snapshot.forEach { doc ->
                Log.d("ChatListActivity", "User1 Chat ID: ${doc.getString("chatID")}, Last Message: ${doc.getString("lastMessage")}")
            }

            // Debug: stampa il risultato di queryUser2
            Log.d("ChatListActivity", "Results for user2 query:")
            user2Snapshot.forEach { doc ->
                Log.d("ChatListActivity", "User2 Chat ID: ${doc.getString("chatID")}, Last Message: ${doc.getString("lastMessage")}")
            }

            // Estrai gli ID delle chat per `user1`
            for (doc in user1Snapshot) {
                val chatId = doc.getString("chatID") ?: ""
                if (chatId.isNotEmpty() && !chatIds.contains(chatId)) {
                    chatIds.add(chatId)
                }
            }

            // Estrai gli ID delle chat per `user2`
            for (doc in user2Snapshot) {
                val chatId = doc.getString("chatID") ?: ""
                if (chatId.isNotEmpty() && !chatIds.contains(chatId)) {
                    chatIds.add(chatId)
                }
            }

            // Debug: stampa l'array degli ID delle chat trovate
            Log.d("ChatListActivity", "Chat IDs found: $chatIds")
        } catch (e: Exception) {
            Log.w("ChatListActivity", "Error fetching chat data", e)
        }

        return chatIds
    }



    private fun visualizza(recyclerView: RecyclerView, data: List<ItemsViewModel>, role: String,name:String) {
        Log.d(TAG, "SONO IN VISUALIZZA CON DATA= $data E ROLE= $role E NAME= $name")
        val adapter = CustomAdapter(data)
        recyclerView.adapter = adapter

        adapter.setOnItemClickListener(object : CustomAdapter.onItemClickListener {
            override fun onItemClick(position: Int) {
                val clickedItem = data[position]
                Log.d(TAG, "hai cliccato su $clickedItem")
                Log.d(TAG,"projectid=${clickedItem.projectId} taskid=${clickedItem.taskId} subtaskid=${clickedItem.subtaskId}")

                val intent = Intent(this@LoggedActivity, ProjectActivity::class.java)
                intent.putExtra("projectId", clickedItem.projectId)
                intent.putExtra("taskId", clickedItem.taskId)
                intent.putExtra("subtaskId", clickedItem.subtaskId)
                intent.putExtra("role", role) // Passa il ruolo
                intent.putExtra("name", name)
                Log.d(TAG,"STO CHIAMANDO PROJECT ACTIVITY CON ROLE= $role E NAME= $name PROJECTID= ${clickedItem.projectId} TASKID= ${clickedItem.taskId} SUBTASKID= ${clickedItem.subtaskId}")
                startActivity(intent)
            }
        })
    }

    //ricavo user e in particolare username
    private suspend fun getUser(): String {
        val user = FirebaseAuth.getInstance().currentUser
        return if (user != null) {
            val userName = user.email.toString()
            Log.d(TAG, "user: $userName")
            userName
        } else {
            Log.w(TAG, "Error current user")
            "" // Restituisci una stringa vuota se non c'è un utente corrente
        }
    }

    //ricavo ruolo dell'utente
    private suspend fun getRole(userName: String): String {
        val db = FirebaseFirestore.getInstance()
        var role = ""

        try {
            val result = db.collection("utenti").get().await() // Usa await() per attendere il completamento
            for (document in result) {
                val email = document.getString("email")
                if (email == userName) {
                    role = document.getString("role") ?: "" // Ottieni il ruolo e gestisci i null
                    break
                }
            }
        } catch (exception: Exception) {
            Log.w(TAG, "Error getting role.", exception)
        }

        return role
    }

    //ricavo nome dell'utente
    private suspend fun getName(userName: String): String {
        val db = FirebaseFirestore.getInstance()
        var name = ""

        try {
            val result = db.collection("utenti").get().await() // Usa await() per attendere il completamento
            for (document in result) {
                val email = document.getString("email")
                if (email == userName) {
                    name = document.getString("name") ?: "" // Ottieni il nome e gestisci i null
                    break
                }
            }
        } catch (exception: Exception) {
            Log.w(TAG, "Error getting name.", exception)
        }

        return name
    }
    //carica tutti i progetti
    private suspend fun loadProjectData(): ArrayList<ItemsViewModel> {
        val db = Firebase.firestore
        val data = ArrayList<ItemsViewModel>()

        try {
            val result = db.collection("progetti").get().await()
            for (document in result) {
                val title = document.getString("titolo") ?: "" // Ottieni il titolo
                val leader = document.getString("leader") ?: "" // Ottieni il nome del leader
                val assegnato = document.getString("assegnato") ?: "" // Ottieni il booleano assegnato
                data.add(ItemsViewModel(title, leader, assegnato.toBoolean(), document.id))
            }
        } catch (exception: Exception) {
            Log.w(TAG, "Error getting project.", exception)
        }

        return data
    }

    private suspend fun loadTask(data: ArrayList<ItemsViewModel>, name: String): ArrayList<ItemsViewModel> {
        val userTasks = ArrayList<ItemsViewModel>()
        val db = FirebaseFirestore.getInstance()

        for (project in data) {
            // Get the project document to retrieve the leader information
            val projectDocument = db.collection("progetti").document(project.text).get().await()
            val leader = projectDocument.getString("leader") ?: "" // Get leader from project document

            val result = db.collection("progetti")
                .document(project.text)
                .collection("task")
                .whereEqualTo("developer", name) // Filter tasks assigned to the developer
                .get()
                .await()

            for (document in result) {
                val title = document.getString("titolo") ?: ""
                userTasks.add(
                    ItemsViewModel(
                        text = title, // Task title
                        leader = leader, // Set leader information from project document
                        assegnato = false, // You might need to fetch the assigned status
                        projectId = project.projectId, // Project ID
                        taskId = document.id // Task ID
                    )
                )
            }
        }
        return userTasks
    }

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        AlertDialog.Builder(this)
            .setTitle("Confirm Exit")
            .setMessage("Are you sure you want to exit the application?")
            .setPositiveButton("Yes") { _, _ ->
                // User confirmed, exit the application
                finishAffinity() // Close all activities in the task
            }
            .setNegativeButton("No") { dialog, _ ->
                // User canceled, dismiss the dialog
                dialog.dismiss()
            }
            .show()
    }


    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            val data = loadProjectData()
        }
    }
}