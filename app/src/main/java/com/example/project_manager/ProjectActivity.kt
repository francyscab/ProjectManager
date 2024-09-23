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
import java.util.Locale

class ProjectActivity : AppCompatActivity() {

    private lateinit var projectId: String
    private lateinit var projectNameTextView: TextView
    private lateinit var projectDescriptionTextView: TextView
    private lateinit var projectDeadlineTextView: TextView
    private lateinit var projectLeaderTextView: TextView
    private lateinit var TaskListLayout: LinearLayout
    private lateinit var progressSeekBar: SeekBar
    private lateinit var assegnaTask: Button
    private lateinit var salvatask: Button
    private lateinit var role:String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_project)

        // Inizializza le views
        projectNameTextView = findViewById(R.id.projectNameTextView)
        projectDeadlineTextView = findViewById(R.id.projectDeadlineTextView)
        projectLeaderTextView = findViewById(R.id.projectLeaderTextView)
        projectDescriptionTextView=findViewById(R.id.descrizioneProgetto)
        //TaskListLayout = findViewById(R.id.TaskListLayout)
        //progressSeekBar = findViewById(R.id.progressSeekBar)
        //assegnaTask = findViewById(R.id.buttonAssegnaSottottask)
        //salvatask = findViewById(R.id.buttonSalvaAsseganmenti)

        // Ottieni l'ID del progetto dall'intent
        projectId = intent.getStringExtra("projectId") ?: ""
        role=intent.getStringExtra("role")?:""

        Log.d(TAG,"nome progetto selezionato: $projectId")

        // Carica i dettagli del progetto
        loadProjectDetails()
    }

    //assegna con spinner i vari task ai developer(azione che posso fare solo se leader)
    /*private fun assegna() {
        Log.d(TAG, "Role received: $role")
        //mostra gli spinner con la scelta del developer per ogni task
        if(role=="Leader"){
            assegnaTask.visibility=View.VISIBLE
            assegnaTask.setOnClickListener{
                assegnaTask.visibility=View.INVISIBLE
                for( i in 0 until TaskListLayout.childCount)
                {
                    val child=TaskListLayout.getChildAt(i)
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
                salvatask.visibility=View.VISIBLE
                salvatask.setOnClickListener {

                    val db=FirebaseFirestore.getInstance()
                    //scorro i singoli task
                    for(i in 0 until TaskListLayout.childCount){
                        val child=TaskListLayout.getChildAt(i)

                        if(child is LinearLayout){
                            //trovo lo spinner del task e il titolo del task
                            val spinner=child.getChildAt(1) as? Spinner
                            val  TaskTextView=child.getChildAt(0) as? TextView

                            //se entrambi esistono
                            if(spinner!=null && TaskTextView!=null){
                                //estrapolo il developer selezionato dallo spinner
                                val selectedDeveloper= spinner.selectedItem as? String
                                if(selectedDeveloper!=null){
                                    //creo  nuova textview dove inserire il nome una volta salvate le modifiche
                                    val developerTextView=TextView(this)
                                    developerTextView.text=selectedDeveloper
                                    child.removeViewAt(1)
                                    child.addView(developerTextView,1)

                                    developerTextView.layoutParams=spinner.layoutParams

                                    //ottengo nome del task
                                    val TaskName=TaskTextView.text.toString()

                                    Log.d(TAG,"$selectedDeveloper Developer assegnato al task: $TaskName")
                                    //aggiorno doc firestore con nome developer
                                    val TaskRef=db.collection("progetti").document(projectId)
                                        .collection("task").document(TaskName)
                                    TaskRef.update("developer", selectedDeveloper)
                                        .addOnSuccessListener {
                                            Log.d(TAG,"Developer assegnato con successo al sottotask: $TaskName")
                                        }
                                        .addOnFailureListener{ exception->
                                            Log.e(TAG,"Errore nell'aggiornamento del sottotask: $TaskName",exception)
                                        }
                                }
                                spinner.visibility=View.INVISIBLE
                            }
                        }
                        //setto assegnato del task a true cosi non risulta piu con bordo rosso
                        val TaskRef=db.collection("progetti").document(projectId)
                        TaskRef.update("assegnato", true.toString())
                        salvatask.visibility=View.INVISIBLE
                    }

                }
            }
        }
        else{
            assegnaTask.visibility=View.INVISIBLE
        }

    }*/

    private fun loadProjectDetails() {
        val db = FirebaseFirestore.getInstance()

        // Ottieni il riferimento al documento del progetto
        val projectRef = db.collection("progetti").document(projectId)

        // Ottieni i dettagli del progetto dal documento
        projectRef.get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val projectName = document.getString("titolo")?.uppercase()
                    val projectDeadline = document.getString("scadenza")
                    val projectdescr=document.getString("descrizione")
                    val projectLeader = document.getString("leader")?.split(" ")?.joinToString(" ") {
                        it.replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase(
                                Locale.getDefault()
                            ) else it.toString()
                        }
                    }

                    Log.d(TAG, "Role received: $role")
                    val progLeaderCont = findViewById<LinearLayout>(R.id.progLeaderCont)

                    // nascondere il TextView
                    if (role=="Leader") {
                        progLeaderCont.visibility = View.GONE
                        projectNameTextView.text = projectName
                        projectDeadlineTextView.text = "$projectDeadline"
                        projectDescriptionTextView.text="$projectdescr"
                    } else {
                        // Se la condizione è falsa mostrare il TextView
                        progLeaderCont.visibility = View.VISIBLE
                        projectNameTextView.text = projectName
                        projectDeadlineTextView.text = "$projectDeadline"
                        projectLeaderTextView.text = "$projectLeader"
                        projectDescriptionTextView.text="$projectdescr"
                    }


                    // Aggiorna le views con i dettagli del progetto



                    // Carica i sottotask del progetto
                    //loadTasks()

                } else {
                    Log.d(TAG, "No such document")
                }
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "get failed with ", exception)
            }
    }

    private fun loadTasks() {
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
                val TasksRef = db.collection("progetti").document(projectId).collection("task")

                // Ottieni tutti i task del progetto
                TasksRef.get()
                    .addOnSuccessListener { documents ->
                        for (document in documents) {
                            val TaskName = document.getString("nome")

                            //CREA IL LINEAR LAYOUT DELLA TEXT VIEW E SPINNER
                            val linearLayout=LinearLayout(this)
                            linearLayout.orientation=LinearLayout.HORIZONTAL

                            // Aggiungi una TextView per ogni sottotask alla LinearLayout
                            val TaskTextView = TextView(this)
                            TaskTextView.text = TaskName
                            linearLayout.addView(TaskTextView)

                            //CREO LO SPINNER PER OGNI TASK PER SELEZIONARE IL DEVELOPER
                            val TaskSpinner= Spinner(this)
                            TaskSpinner.visibility= View.INVISIBLE
                            TaskSpinner.adapter=adapter

                            linearLayout.addView(TaskSpinner)
                            TaskListLayout.addView(linearLayout)


                        }
                        //assegna();
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