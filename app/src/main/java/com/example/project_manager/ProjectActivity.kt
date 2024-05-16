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
import com.google.api.Distribution.BucketOptions.Linear
import com.google.firebase.firestore.FirebaseFirestore

class ProjectActivity : AppCompatActivity() {

    private lateinit var projectId: String
    private lateinit var projectNameTextView: TextView
    private lateinit var projectDeadlineTextView: TextView
    private lateinit var projectLeaderTextView: TextView
    private lateinit var subTaskListLayout: LinearLayout
    private lateinit var progressSeekBar: SeekBar
    private lateinit var assegnaSottotask: Button
    private lateinit var salvaSottotask: Button

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
        salvaSottotask = findViewById(R.id.buttonSalvaAsseganmenti)

        // Ottieni l'ID del progetto dall'intent
        projectId = intent.getStringExtra("projectId") ?: ""
        Log.d(TAG,"nome progetto selezionato: $projectId")

        // Carica i dettagli del progetto
        loadProjectDetails()


    }

    //assegna con spinner i vari sottotask ai developer(azione che posso fare solo se leader)
    private fun assegna() {
        //mostra gli spinner con la scelta del developer per ogni sottotask
        assegnaSottotask.setOnClickListener{
            assegnaSottotask.visibility=View.INVISIBLE
            for( i in 0 until subTaskListLayout.childCount)
            {
                val child=subTaskListLayout.getChildAt(i)
                Log.d(TAG,"child $child")
                if(child is LinearLayout){
                    //trovo lo spinner
                    val spinner=child.getChildAt(1)as? Spinner
                    Log.d(TAG,"spinner  $spinner")
                    if(spinner!=null){
                        spinner.visibility=View.VISIBLE
                    }
                }
            }
            //mostro il bottone di salvataggio
            salvaSottotask.visibility=View.VISIBLE
            salvaSottotask.setOnClickListener {

                val db=FirebaseFirestore.getInstance()
                //scorro i singoli sottotask
                for(i in 0 until subTaskListLayout.childCount){
                    val child=subTaskListLayout.getChildAt(i)

                    if(child is LinearLayout){
                        //trovo lo spinner del sottotask e il titolo del sottotask
                        val spinner=child.getChildAt(1) as? Spinner
                        val  subTaskTextView=child.getChildAt(0) as? TextView

                        //se entrambi esistono
                        if(spinner!=null && subTaskTextView!=null){
                            //estrapolo il developer selezionato dallo spinner
                            val selectedDeveloper= spinner.selectedItem as? String
                            if(selectedDeveloper!=null){
                                //creo  nuova textview dove inserire il nome una volta salvate le modifiche
                                val developerTextView=TextView(this)
                                developerTextView.text=selectedDeveloper
                                child.removeViewAt(1)
                                child.addView(developerTextView,1)

                                developerTextView.layoutParams=spinner.layoutParams

                                //ottengo nome del sottotask
                                val subTaskName=subTaskTextView.text.toString()

                                Log.d(TAG,"$selectedDeveloper Developer assegnato al sottotask: $subTaskName")
                                //aggiorno doc firestore con nome developer
                                val subTaskRef=db.collection("progetti").document(projectId)
                                    .collection("sottotask").document(subTaskName)
                                subTaskRef.update("developer", selectedDeveloper)
                                    .addOnSuccessListener {
                                        Log.d(TAG,"Developer assegnato con successo al sottotask: $subTaskName")
                                    }
                                    .addOnFailureListener{ exception->
                                        Log.e(TAG,"Errore nell'aggiornamento del sottotask: $subTaskName",exception)
                                    }
                            }
                            spinner.visibility=View.INVISIBLE
                        }
                    }
                    //setto assegnato del 6ask a true cosi non risulta piu con bordo rosso
                    val TaskRef=db.collection("progetti").document(projectId)
                    TaskRef.update("assegnato", true.toString())
                    salvaSottotask.visibility=View.INVISIBLE
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
                            linearLayout.addView(subTaskTextView)

                            //CREO LO SPINNER PER OGNI SOTTOTASK PER SELEZIONARE IL DEVELOPER
                            val subTaskSpinner= Spinner(this)
                            subTaskSpinner.visibility= View.INVISIBLE
                            subTaskSpinner.adapter=adapter

                            linearLayout.addView(subTaskSpinner)
                            subTaskListLayout.addView(linearLayout)


                        }
                        assegna();
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