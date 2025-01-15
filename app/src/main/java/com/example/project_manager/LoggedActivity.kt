package com.example.project_manager

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View

import android.widget.ImageButton

import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationCompat

import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


class LoggedActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private var notificationManager: NotificationManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logged)


        db = FirebaseFirestore.getInstance()
        loadRecycleView()
    }


    private fun loadRecycleView() {
        Log.d(TAG, "LOADRECYCLEVIEW")
        //val db = FirebaseFirestore.getInstance()

        var data= ArrayList<ItemsViewModel>()
        var userName=""
        var role=""
        var name=""
        //val dataLeader= arrayListOf<ItemsViewModel>()
        val newProject = findViewById<ImageButton>(R.id.newProject)

        //recycleview
        val recyclerview = findViewById<RecyclerView>(R.id.recyclerview)
        recyclerview.layoutManager = LinearLayoutManager(this)


        lifecycleScope.launch {
            userName = getUser()
            role = getRole(userName)
            data = loadProjectData()
            name=getName(userName)
            Log.d(TAG,"SONO IN LOGGED ACTIVITY E SONO username=$userName ruolo=$role e mi chiamo=$name")

            if (role == "Manager") {
                Log.d(TAG, "SICCOME SONO IL MANAGER: $userName")
                //ASCOLTO DB SE CI SONO MODIFICHE SU COMPLETAMENTO DI QUALCHE PROGETTO DI CUI SONO IL MANAGER.
                val query = db.collection("progetti")
                    .whereEqualTo("creator", name)

                query.get().addOnSuccessListener { documents ->
                    if (documents.isEmpty) {
                        Log.d("FirestoreQuery", "Nessun documento trovato per la query.")
                    } else {
                        for (document in documents) {
                            Log.d("FirestoreQuery", "Documento trovato: ${document.id}, dati: ${document.data}")
                        }
                    }
                }.addOnFailureListener { e ->
                    Log.e("FirestoreQuery", "Errore durante l'esecuzione della query.", e)
                }



                query.addSnapshotListener { snapshots, e ->
                    if (e != null) {
                        Log.w("Firestore", "Errore nel listener: ", e)
                        return@addSnapshotListener
                    }

                    if (snapshots != null) {
                        for (change in snapshots.documentChanges) {
                            if (change.type == DocumentChange.Type.MODIFIED) {
                                val document = change.document
                                val newProgress = document.get("progress")
                                val oldProgress = change.oldIndex // In Firestore client SDK, può essere simulato.

                                if (newProgress != oldProgress) {
                                    Log.d("Firestore", "Progress aggiornato: $newProgress")
                                    // Aggiungi logica per inviare una notifica
                                }

                                if (newProgress == 100) {
                                    val projectId = document.id
                                    sendNotification(
                                    )
                                }
                            }
                        }
                    }
                }

                /*myref.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        // This method is called once with the initial value and again
                        // whenever data at this location is updated.
                        val value = dataSnapshot.getValue<String>()
                        Log.d("REALTIME_DB", "Value is: $value")
                    }
                    override fun onCancelled(error: DatabaseError) {
                        // Failed to read value
                        Log.w("REALTIME_DB", "Failed to read value.", error.toException())
                    }
                })*/


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

    fun sendNotification(view: View) {

        val notificationId = 1
        val resultIntent = Intent(this, MainActivity::class.java)

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            resultIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val channelID="it.manager.newprogress"

        val notification= Notification.Builder(this,channelID)
            .setContentTitle("exemple notification")
            .setContentText("this is an exemple text")
            .setChannelId(channelID)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

    }

    private fun createNotificationChannel(id: String, nome: String, descrizione: String) {
        val importance= NotificationManager.IMPORTANCE_LOW
        val channel=NotificationChannel(id,nome,importance)

        channel.description=descrizione
        notificationManager?.createNotificationChannel(channel)
    }
}