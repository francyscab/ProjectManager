package com.example.project_manager


import android.content.ContentValues
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class ProjectActivity : AppCompatActivity() {

    private lateinit var projectId: String
    private lateinit var projectNameTextView: TextView
    private lateinit var projectDeadlineTextView: TextView
    private lateinit var projectLeaderTextView: TextView
    private lateinit var subTaskListLayout: LinearLayout
    private lateinit var progressSeekBar: SeekBar
    private lateinit var assegnaSottotask: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_project)

        // Inizializza le views
        projectNameTextView = findViewById(R.id.projectNameTextView)
        projectDeadlineTextView = findViewById(R.id.projectDeadlineTextView)
        projectLeaderTextView = findViewById(R.id.projectLeaderTextView)
        subTaskListLayout = findViewById(R.id.subTaskListLayout)
        progressSeekBar = findViewById(R.id.progressSeekBar)
        assegnaSottotask = findViewById(R.id.buttonAssegnaSottottask)

        // Ottieni l'ID del progetto dall'intent
        projectId = intent.getStringExtra("projectId") ?: ""

        // Carica i dettagli del progetto
        loadProjectDetails()

        assegnaSottotask.setOnClickListener{
            for( i in 0 until subTaskListLayout.childCount)
            {
                val child=subTaskListLayout.getChildAt(i)
                if(child is LinearLayout){
                    //trovo lo spinner
                    val spinner=child.getChildAt(1)as? Spinner
                    if(spinner!=null){
                        spinner.visibility=View.VISIBLE
                    }
                }
            }
        }
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
                    val projectLeader = document.getString("leader")

                    // Aggiorna le views con i dettagli del progetto
                    projectNameTextView.text = projectName
                    projectDeadlineTextView.text = "Deadline: $projectDeadline"
                    projectLeaderTextView.text = "Leader: $projectLeader"


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

        //trovo i nomi dei developer per inserirli nello spinner
        val developerNames=ArrayList<String>()
        db.collection("utenti")
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val role = document.getString("role")
                    if (role == "Developer") {
                        val name = document.getString("name").toString()
                        developerNames.add(name)
                    }
                }
                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, developerNames)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)


                // Ottieni il riferimento alla collezione dei sottotask del progetto
                val subTasksRef = db.collection("progetti").document(projectId).collection("sottotask")

                // Ottieni tutti i sottotask del progetto
                subTasksRef.get()
                    .addOnSuccessListener { documents ->
                        for (document in documents) {
                            val subTaskName = document.getString("nome")

                            //CREA IL LINEAR LAYOUT DELLA TEXT VIEW E SPINNER
                            val linearLayout=LinearLayout(this)
                            linearLayout.orientation=LinearLayout.HORIZONTAL

                            // Aggiungi una TextView per ogni sottotask alla LinearLayout
                            val subTaskTextView = TextView(this)
                            subTaskTextView.text = subTaskName
                            subTaskListLayout.addView(subTaskTextView)

                            //CREO LO SPINNER PER OGNI SOTTOTASK PER SELEZIONARE IL DEVELOPER
                            val subTaskSpinner= Spinner(this)
                            subTaskSpinner.visibility= View.INVISIBLE
                            subTaskSpinner.adapter=adapter

                            linearLayout.addView(subTaskSpinner)
                            subTaskListLayout.addView(linearLayout)


                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.d(TAG, "get failed with ", exception)
                    }

            }
            .addOnFailureListener { exception ->
                Log.w(ContentValues.TAG, "Error getting documents: ", exception)
            }


    }

    companion object {
        private const val TAG = "ProjectDetailsActivity"
    }
}