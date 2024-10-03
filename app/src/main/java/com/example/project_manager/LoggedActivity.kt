package com.example.project_manager

import android.content.ContentValues
import android.content.ContentValues.TAG
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoggedActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logged)
        loadRecycleView()
    }


    private fun loadRecycleView() {
        Log.d(TAG, "LOADRECYCLEVIEW")
        //val db = FirebaseFirestore.getInstance()

        var data= ArrayList<ItemsViewModel>()
        var userName=""
        var role=""
        //val dataLeader= arrayListOf<ItemsViewModel>()
        val newProject = findViewById<ImageButton>(R.id.newProject)

        //recycleview
        val recyclerview = findViewById<RecyclerView>(R.id.recyclerview)
        recyclerview.layoutManager = LinearLayoutManager(this)


        lifecycleScope.launch {
            userName = getUser()
            Log.d(TAG, "username: $userName")

            role = getRole(userName)
            Log.d(TAG, "role: $role")

            data = loadProjectData()
            Log.d(TAG, "data: $data")

            if (role == "Manager") {
                Log.d(TAG, "MANAGER: $userName")

                //comportamento bottone nuovo progetto
                /*newProject.setOnClickListener {
                    val intent = Intent(this, NewProjectActivity::class.java)
                    intent.putExtra("tipo_form", "progetto")
                    intent.putExtra("role", role)
                    startActivity(intent)
                }*/
                visualizza(recyclerview, data, role)
            } else if (role == "Leader") {
                Log.d(TAG, "LEADER: $userName")

                //togliere bottone per creare nuovo progetto
                newProject.visibility = View.INVISIBLE

                //MOSTRARE SOLO I PROGETTI DI CUI SI È LEADER--modifico array data
                data = data.filter { it.leader == userName } as ArrayList<ItemsViewModel>
                visualizza(recyclerview, data, role)
            } else if (role == "Developer") {
                Log.d(TAG, "DEVELOPER: $userName")
                //togliere bottone per creare nuovo progetto
                newProject.visibility = View.INVISIBLE

                //salvo array di task che hanno come developer quello attualmente loggato
                var tasksForDeveloper = ArrayList<ItemsViewModel>()
                tasksForDeveloper=loadTask(data, userName)
                visualizza(recyclerview,data,role)
            }
            else{
                //errore
            }


        }
    }


    private fun visualizza(recyclerView: RecyclerView, data: List<ItemsViewModel>, role: String) {
        Log.d(TAG, "VISUALIZZA RICEVE ARRAY : $data")
        val adapter = CustomAdapter(data)

        recyclerView.adapter = adapter

        adapter.setOnItemClickListener(object : CustomAdapter.onItemClickListener {
            override fun onItemClick(position: Int) {
                val clickedItemTitle = data[position].text
                Toast.makeText(this@LoggedActivity, "You clicked on item: $clickedItemTitle, YOU'RE ROLE IS $role",
                    Toast.LENGTH_LONG).show()

                val intent = Intent(this@LoggedActivity, ProjectActivity::class.java)
                intent.putExtra("projectId", clickedItemTitle)
                Log.d(TAG, "Role SEND: $role")
                intent.putExtra("role", role) // Passa il ruolo
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

    //carica tutti i progetti
    private suspend fun loadProjectData(): ArrayList<ItemsViewModel> {
        Log.d(TAG, "loadProjectData:")
        val db = Firebase.firestore
        val data = ArrayList<ItemsViewModel>()

        try {
            val result = db.collection("progetti").get().await()
            for (document in result) {
                val title = document.getString("titolo") ?: "" // Ottieni il titolo
                val leader = document.getString("leader") ?: "" // Ottieni il nome del leader
                val assegnato = document.getString("assegnato") ?: "" // Ottieni il booleano assegnato
                data.add(ItemsViewModel(title, leader, assegnato.toBoolean()))
            }
        } catch (exception: Exception) {
            Log.w(TAG, "Error getting project.", exception)
        }

        return data
    }

    private suspend fun loadTask(data: ArrayList<ItemsViewModel>, userName: String): ArrayList<ItemsViewModel> {
        val userTasks = ArrayList<ItemsViewModel>()
        val db = FirebaseFirestore.getInstance()

        for (project in data) {
            val result = db.collection("progetti")
                .document(project.text)
                .collection("task")
                .get()
                .await()

            for (document in result) {
                val title = document.getString("titolo") ?: ""
                val developer = document.getString("developer") ?: ""
                val assegnato = false
                if (developer == userName) {
                    userTasks.add(ItemsViewModel(title, developer, assegnato))
                }
            }
        }
        return userTasks
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            val data = loadProjectData()
        }
    }
}