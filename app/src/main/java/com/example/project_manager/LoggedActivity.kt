package com.example.project_manager

import android.content.ContentValues
import android.content.ContentValues.TAG
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoggedActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logged)

        val db = FirebaseFirestore.getInstance()

        // ArrayList of class ItemsViewModel
        val data = ArrayList<ItemsViewModel>()
        val newProject=findViewById<Button>(R.id.newProject)

        //NON TROVA L'UTENTE LOGGATO!!!!!!
        var userName: String?=null;
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            userName = user.displayName
        } else {
            Log.w(ContentValues.TAG, "Error current user")
        }

        Log.d(TAG, "username: $user")
        db.collection("utenti")
            .get()
            .addOnSuccessListener{ result->
                for(document in result){
                    val name=document.getString("name")
                    val role=document.getString("role")
                    Log.d(TAG, "doppooooooo:")
                    Log.d(TAG, "nome utente: $name")
                    Log.d(TAG, "nome mio: $userName")
                    if(name==userName){
                        if(role=="Manager"){
                            Log.d(TAG, "MANAGER:")
                            newProject.setOnClickListener {
                                startActivity(Intent(this, NewProjectActivity::class.java))

                            }
                        }
                        else if(role=="Leader"){
                            Log.d(TAG, "LEADER:")
                            newProject.visibility= View.INVISIBLE
                            //togliere bottone per creare nuovo progetto
                        }
                        else{
                            //role=Developer
                        }
                    }
                }
            }




        db.collection("progetti")
            .get()
            .addOnSuccessListener { result ->

                for (document in result) {
                    val title = document.getString("titolo") ?: "" // Ottieni il titolo
                    data.add(ItemsViewModel(title)) // Aggiungi il titolo all'array data
                    Log.d(TAG, "${document.id} => ${document.data}")
                }
                Log.d(TAG, "data array: $data")

                // getting the recyclerview by its id
                val recyclerview = findViewById<RecyclerView>(R.id.recyclerview)

                // this creates a vertical layout Manager
                recyclerview.layoutManager = LinearLayoutManager(this)

                // This will pass the ArrayList to our Adapter
                val adapter = CustomAdapter(data)

                // Setting the Adapter with the recyclerview
                recyclerview.adapter = adapter

                adapter.setOnItemClickListener(object : CustomAdapter.onItemClickListener{
                    override fun onItemClick(position: Int) {
                        val clickedItemTitle=data[position].text
                        Toast.makeText(this@LoggedActivity,"You clicked on item  $clickedItemTitle",
                            Toast.LENGTH_LONG).show()

                        val intent = Intent(this@LoggedActivity,ProjectActivity::class.java)
                        intent.putExtra("projectId", clickedItemTitle) // "projectId" è il nome dell'extra, projectId è l'ID del progetto
                        startActivity(intent)
                    }
                })

            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting documents.", exception)
            }
    }
}