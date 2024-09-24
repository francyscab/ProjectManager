package com.example.project_manager

import android.content.ContentValues
import android.content.ContentValues.TAG
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.log

class LoggedActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logged)
        loadProjectData()
    }
    private fun loadProjectData(){
        lateinit var role:String
        val db = FirebaseFirestore.getInstance()

        // ArrayList of class ItemsViewModel
        var data = ArrayList<ItemsViewModel>()
        val dataLeader= arrayListOf<ItemsViewModel>()
        val newProject=findViewById<ImageButton>(R.id.newProject)


        var userName: String = ""
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            userName = user.email.toString()
            Log.d(TAG, "user: $userName")
        } else {
            Log.w(ContentValues.TAG, "Error current user")
        }

        //recycleview
        db.collection("progetti")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val title = document.getString("titolo") ?: "" // Ottieni il titolo
                    val leader =document.getString("leader")?:""//ottieni il nome del leader
                    val assegnato =document.getString("assegnato")?:"" //ottieni il booleano assegnato

                    data.add(ItemsViewModel(title, leader, assegnato.toBoolean() )) // Aggiungi il titolo all'array data
                }
                Log.d(TAG, "data array: $data")
                val recyclerview = findViewById<RecyclerView>(R.id.recyclerview)
                recyclerview.layoutManager = LinearLayoutManager(this)

                //distinguo diversi tipi di utenti
                db.collection("utenti")
                    .get()
                    .addOnSuccessListener{ result->
                        for(document in result){
                            val nome=document.getString("name")
                            val email=document.getString("email")
                            role= document.getString("role").toString()
                            if(email==userName){
                                if(role=="Manager"){
                                    Log.d(TAG, "MANAGER:")
                                    newProject.setOnClickListener {
                                        val intent = Intent(this, NewProjectActivity::class.java)
                                        intent.putExtra("tipo_form", "project")
                                        startActivity(intent)
                                    }
                                }
                                else if(role=="Leader"){
                                    Log.d(TAG, "Role FIND: $role")
                                    Log.d(TAG, "LEADER:")
                                    //togliere bottone per creare nuovo progetto
                                    newProject.visibility= View.INVISIBLE
                                    //MOSTRARE SOLO I PROGETTI DI CUI SI È LEADER--modifico array data
                                    data=data.filter{it.leader==nome}as ArrayList<ItemsViewModel>
                                    Log.d(TAG,"data with nome= $nome  now: $data")
                                }
                                else{
                                    Log.d(TAG, "DEVELOPER:")
                                    //togliere bottone per creare nuovo progetto
                                    newProject.visibility= View.INVISIBLE
                                    //MOSTRARE SOLO I TASK DI CUI SI È developer--modifico array data
                                }
                                break//esci dal ciclo quando trovi lutente desiderato in modo che role non venga modificato
                            }
                        }

                        Log.d(TAG, "Role OUT: $role")
                        //passo arraylist all'adapter
                        Log.d(TAG,"DATA PRIMA ADAPTER =$data")
                        val adapter = CustomAdapter(data)

                        recyclerview.adapter = adapter

                        adapter.setOnItemClickListener(object : CustomAdapter.onItemClickListener{
                            override fun onItemClick(position: Int) {
                                val clickedItemTitle=data[position].text
                                Toast.makeText(this@LoggedActivity,"You clicked on item,   $clickedItemTitle ,YOU'RE ROLE IS $role",
                                    Toast.LENGTH_LONG).show()

                                val intent = Intent(this@LoggedActivity,ProjectActivity::class.java)
                                intent.putExtra("projectId", clickedItemTitle)
                                Log.d(TAG, "Role SEND: $role")
                                intent.putExtra("role",role)// "projectId" è il nome dell'extra, projectId è l'ID del progetto
                                startActivity(intent)
                            }
                        })
                    }

            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting documents.", exception)
            }

        Log.d(TAG, "username: $userName")
    }


    override fun onResume() {
        super.onResume()
        loadProjectData()
    }
}