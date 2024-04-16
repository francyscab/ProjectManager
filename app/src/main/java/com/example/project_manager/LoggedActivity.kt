package com.example.project_manager

import android.content.ContentValues
import android.content.ContentValues.TAG
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
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