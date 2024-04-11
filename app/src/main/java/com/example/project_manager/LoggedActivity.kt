package com.example.project_manager

import android.content.ContentValues
import android.content.ContentValues.TAG
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class LoggedActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logged)

        val db = FirebaseFirestore.getInstance()

        // ArrayList of class ItemsViewModel
        val data = ArrayList<ItemsViewModel>()

        val newProject=findViewById<Button>(R.id.newProject)
        newProject.setOnClickListener {
            startActivity(Intent(this, NewProjectActivity::class.java))
            //aprire nuova schermata per fare aggiungere nuovo taskchiedere subito a chi assegnarlo!!!!

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
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting documents.", exception)
            }
    }
}