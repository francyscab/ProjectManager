package com.example.project_manager


import android.content.ContentValues
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class ProjectActivity : AppCompatActivity() {

    private lateinit var projectId: String
    private lateinit var projectNameTextView: TextView
    private lateinit var projectDeadlineTextView: TextView
    private lateinit var subTaskListLayout: LinearLayout
    private lateinit var progressSeekBar: SeekBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_project)

        // Inizializza le views
        projectNameTextView = findViewById(R.id.projectNameTextView)
        projectDeadlineTextView = findViewById(R.id.projectDeadlineTextView)
        subTaskListLayout = findViewById(R.id.subTaskListLayout)
        progressSeekBar = findViewById(R.id.progressSeekBar)

        // Ottieni l'ID del progetto dall'intent
        projectId = intent.getStringExtra("projectId") ?: ""

        // Carica i dettagli del progetto
        loadProjectDetails()
    }

    private fun loadProjectDetails() {
        val db = FirebaseFirestore.getInstance()

        // Ottieni il riferimento al documento del progetto
        val projectRef = db.collection("progetti").document(projectId)

        // Ottieni i dettagli del progetto dal documento
        projectRef.get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val projectName = document.getString("titolo")
                    val projectDeadline = document.getString("scadenza")

                    // Aggiorna le views con i dettagli del progetto
                    projectNameTextView.text = projectName
                    projectDeadlineTextView.text = "Deadline: $projectDeadline"

                    // Carica i sottotask del progetto
                    loadSubTasks()
                } else {
                    Log.d(TAG, "No such document")
                }
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "get failed with ", exception)
            }
    }

    private fun loadSubTasks() {
        val db = FirebaseFirestore.getInstance()

        // Ottieni il riferimento alla collezione dei sottotask del progetto
        val subTasksRef = db.collection("progetti").document(projectId).collection("sottotask")

        // Ottieni tutti i sottotask del progetto
        subTasksRef.get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val subTaskName = document.getString("nome")

                    // Aggiungi una TextView per ogni sottotask alla LinearLayout
                    val subTaskTextView = TextView(this)
                    subTaskTextView.text = subTaskName
                    subTaskListLayout.addView(subTaskTextView)
                }
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "get failed with ", exception)
            }
    }

    companion object {
        private const val TAG = "ProjectDetailsActivity"
    }
}