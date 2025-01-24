package com.example.project_manager

import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class UpdateProjectActivity : AppCompatActivity() {

    // Variabili per i componenti dell'interfaccia
    private lateinit var titleNewProject: EditText
    private lateinit var descrizioneNewProject: EditText
    private lateinit var pickDate: Button
    private lateinit var projectElementSpinner: Spinner
    private lateinit var buttonSave: Button
    private lateinit var erroreDescrizione: TextView
    private lateinit var erroreTitolo: TextView
    private lateinit var db: FirebaseFirestore

    // Dati ricevuti dall'Intent
    private var taskId: String? = null
    private var projectId: String? = null
    private var subtaskId: String? = null
    private var role: String? = null
    private var titolo: String? = null
    private var descrizione: String? = null
    private var scadenza: String? = null
    private var assignedTo: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_project_form)

        db = FirebaseFirestore.getInstance()

        titleNewProject = findViewById(R.id.titleNewProject)
        descrizioneNewProject = findViewById(R.id.descrizioneNewProject)
        pickDate = findViewById(R.id.pickDate)
        projectElementSpinner = findViewById(R.id.projectElementSpinner)
        buttonSave = findViewById(R.id.buttonSave)
        erroreDescrizione = findViewById(R.id.errore_descrizione)
        erroreTitolo = findViewById(R.id.errore_titolo)

        taskId = intent.getStringExtra("taskId")
        projectId = intent.getStringExtra("projectId")
        subtaskId = intent.getStringExtra("subtaskId")
        role = intent.getStringExtra("role")
        titolo = intent.getStringExtra("titolo")
        descrizione = intent.getStringExtra("descrizione")
        scadenza = intent.getStringExtra("scadenza")
        assignedTo = intent.getStringExtra("assignedTo")

        Log.d("UpdateProjectActivity", "dati ricevuti: $taskId, $projectId, $subtaskId, $role, $titolo, $descrizione, $scadenza, $assignedTo")

        title = "Update Project"

        //popola i campi con i dati ricevuti
        titleNewProject.setText(titolo)
        descrizioneNewProject.setText(descrizione)

        // Impedisce la modifica dei campi non editabili
        titleNewProject.isEnabled = true
        descrizioneNewProject.isEnabled = true



        // Mostra i dati non modificabili
        if (role=="Manager") {
            Log.d("UpdateProjectActivity", "leader ricevuto: $assignedTo")
            loadSpinnerData(db,"Leader") { names ->
                showDataInSpinner(projectElementSpinner,names,assignedTo)
            }


        } else if (role=="Leader") {
            Log.d("UpdateProjectActivity", "developer ricevuto: $assignedTo")
            loadSpinnerData(db,"Developer") { names ->
                showDataInSpinner(projectElementSpinner,names, assignedTo)
            }
        }else if(role=="Developer"){
            findViewById<LinearLayout>(R.id.spinnerLinearLayout).visibility = View.GONE
        }

        pickDate.text = scadenza
        pickDate.isEnabled = false

        // Aggiungi l'azione per il bottone di salvataggio
        buttonSave.setOnClickListener {
            // Verifica se i campi sono validi
            val titoloText = titleNewProject.text.toString()
            val descrizioneText = descrizioneNewProject.text.toString()

            if (titoloText.isBlank()) {
                erroreTitolo.text = "Il titolo non può essere vuoto"
            } else {
                erroreTitolo.text = ""
            }

            if (descrizioneText.isBlank()) {
                erroreDescrizione.text = "La descrizione non può essere vuota"
            } else {
                erroreDescrizione.text = ""
            }

            // Se non ci sono errori, salva le modifiche
            if (erroreTitolo.text.isEmpty() && erroreDescrizione.text.isEmpty()) {
                // Salva il progetto aggiornato nel database
                updateProject(titoloText, descrizioneText)

                // Torna a LoggedActivity
                val intent = Intent(this, LoggedActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }

    private fun showDataInSpinner(spinner: Spinner, names: List<String>, selectedValue: String?) {
        // Imposta l'adattatore per lo spinner con i nomi
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, names)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = spinnerAdapter

        // Se il valore selezionato non è null, imposta la selezione
        selectedValue?.let {
            val position = names.indexOf(it)
            if (position >= 0) {
                spinner.setSelection(position)
            }
        }

        val isEditable = if (role == "Leader") true else false
        spinner.isEnabled = isEditable
    }

    private fun loadSpinnerData(db: FirebaseFirestore, spinnerRole: String, callback: (List<String>) -> Unit) {
        val spinnerNames = mutableListOf<String>()

        // Recupera i dati dalla raccolta "utenti"
        db.collection("utenti")
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val role = document.getString("role")
                    if (role == spinnerRole) {
                        val name = document.getString("name").toString()
                        spinnerNames.add(name)
                    }
                }
                // Passa la lista dei nomi alla funzione callback
                callback(spinnerNames)
            }
            .addOnFailureListener { exception ->
                Log.w(ContentValues.TAG, "Error getting documents: ", exception)
            }
    }

    // Funzione per aggiornare il progetto nel database
    private fun updateProject(titolo: String, descrizione: String) {
        Log.d("UpdateProjectActivity", "$projectId, $taskId, $subtaskId")
        if (projectId == null) {
            Log.e("UpdateProjectActivity", "Impossibile aggiornare: ID del progetto non fornito")
            return
        }

        if (taskId.isNullOrEmpty() && subtaskId.isNullOrEmpty()) {
            // Caso progetto
            Log.d("UpdateProjectActivity", "Aggiornamento diretto del progetto con ID $projectId")
            val projectRef = db.collection("progetti").document(projectId!!)

            projectRef.update("titolo", titolo, "descrizione", descrizione)
                .addOnSuccessListener {
                    Log.d("UpdateProjectActivity", "Progetto aggiornato con successo")
                }
                .addOnFailureListener { exception ->
                    Log.e("UpdateProjectActivity", "Errore durante l'aggiornamento del progetto", exception)
                }
        } else if(taskId!!.isNotEmpty() && subtaskId.isNullOrEmpty()){
            // Caso task
            Log.d("UpdateProjectActivity", "Aggiornamento del task con ID $taskId nel progetto con ID $projectId")
            val taskRef = db.collection("progetti").document(projectId!!)
                .collection("task").document(taskId!!)

            val selectedDeveloper = projectElementSpinner.selectedItem?.toString()

            val taskUpdates = hashMapOf(
                "titolo" to titolo,
                "descrizione" to descrizione
            )
            if (selectedDeveloper != null) {
                taskUpdates["developer"] = selectedDeveloper
            }

            taskRef.update(taskUpdates as Map<String, Any>)
                .addOnSuccessListener {
                    Log.d("UpdateProjectActivity", "Task aggiornato con successo")
                }
                .addOnFailureListener { exception ->
                    Log.e("UpdateProjectActivity", "Errore durante l'aggiornamento del task", exception)
                }
        } else if (taskId != null && subtaskId != null) {
            // Caso  subtask
            Log.d("UpdateProjectActivity", "Aggiornamento del subtask con ID $subtaskId nel task con ID $taskId appartenente al progetto con ID $projectId")
            val subtaskRef = db.collection("progetti").document(projectId!!)
                .collection("task").document(taskId!!)
                .collection("subtask").document(subtaskId!!)

            subtaskRef.update("titolo", titolo, "descrizione", descrizione)
                .addOnSuccessListener {
                    Log.d("UpdateProjectActivity", "Subtask aggiornato con successo")
                }
                .addOnFailureListener { exception ->
                    Log.e("UpdateProjectActivity", "Errore durante l'aggiornamento del subtask", exception)
                }
        } else {
            Log.e("UpdateProjectActivity", "Condizione non gestita")
        }
    }


}
