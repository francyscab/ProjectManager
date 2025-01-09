package com.example.project_manager

import kotlinx.coroutines.*
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.View.GONE
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Locale

class ProjectActivity : AppCompatActivity() {

    private lateinit var name: String //nome utente chiamante
    private lateinit var creator : String
    private lateinit var projectId: String
    private lateinit var taskId: String
    private lateinit var subtaskId: String
    private lateinit var projectNameTextView: TextView
    private lateinit var projectDescriptionTextView: TextView
    private lateinit var projectDeadlineTextView: TextView
    private lateinit var projectCreatorTextView: TextView
    private lateinit var projectAssignedTextView: TextView
    private lateinit var TaskListLayout: LinearLayout
    private lateinit var progressSeekBar: SeekBar
    private lateinit var assegnaTask: Button
    private lateinit var salvatask: Button
    private lateinit var role:String
    private lateinit var progLeaderCont:LinearLayout
    private lateinit var progLeaderTask:LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_project)

        // Inizializza le views
        projectNameTextView = findViewById(R.id.projectNameTextView)
        projectDeadlineTextView = findViewById(R.id.projectDeadlineTextView)
        projectCreatorTextView = findViewById(R.id.projectCreatorTextView)
        projectDescriptionTextView=findViewById(R.id.descrizioneProgetto)
        projectAssignedTextView = findViewById(R.id.projectAssignedTextView)
        //TaskListLayout = findViewById(R.id.TaskListLayout)
        //progressSeekBar = findViewById(R.id.progressSeekBar)
        //assegnaTask = findViewById(R.id.buttonAssegnaSottottask)
        //salvatask = findViewById(R.id.buttonSalvaAsseganmenti)

        // Ottieni INFORMAZIONI dall'intent

        projectId = intent.getStringExtra("projectId") ?: ""
        taskId=intent.getStringExtra("taskId")?:""
        subtaskId=intent.getStringExtra("subtaskId")?: ""
        role=intent.getStringExtra("role")?:""
        name=intent.getStringExtra("name")?:""
        Log.d(TAG, "PROJECT ACTIVITY, SONO STATA CHIAMATA DA $name CON PROJECTID $projectId TASKID $taskId SUBTASKID $subtaskId E ROLE $role")

        progLeaderTask= findViewById<LinearLayout>(R.id.progLeaderTask)

        if (subtaskId.isNotEmpty()) {
            // Esegui la logica per il SOTTOTASK
            Log.d(TAG, " E' UN SUBTASK")
            loadDetails("subtask")
        } else if (taskId.isNotEmpty()) {
            Log.d(TAG, "E' UNTASK")
            // Esegui la logica per il task
            loadDetails("task")
        } else if (projectId.isNotEmpty()) {
            Log.d(TAG, "E' UN PROJECT")
            // Esegui la logica per il PROGETTO
            loadDetails("progetto")
        }else {
            // Gestisci il caso in cui nessun ID sia passato
            Log.w(TAG, "Nessun ID del progetto o del task fornito.")
        }
    }

    private fun loadDetails(tipo : String) {
        val db = FirebaseFirestore.getInstance()

        //devo capire se si tratta di un task, un progetto o di un sottotask
        if (tipo=="progetto"){
            Log.d(TAG, "LOAD DETAILS PROGETTO")
            val projectRef = db.collection("progetti").document(projectId)

            // Ottieni i dettagli del progetto dal documento
            // -se sei un leader carica anche  ì task
            // -se sei manager togli sezione per visulaizzare task
            projectRef.get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        val projectCreator=document.getString("creator")
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
                        Log.d(TAG, "HO OTTENUTO LE SEGUENTI INFORMAZIONI: CREATOR$projectCreator  PROJECTNAME $projectName PROJECTDEADLINE $projectDeadline PROJECTDESCRIZIONE $projectdescr PROJECTLEADER $projectLeader ")

                        if (role=="Leader") {
                            //aggiungere lisener su bottone + per task
                            findViewById<ImageButton>(R.id.aggiungiTaskButton).setOnClickListener {
                                //aprire schermata per aggiungere task
                                val intent = Intent(this, NewProjectActivity::class.java)
                                // Aggiungere il parametro "task" all'Intent per capire che stiamo aggiungendo un task (con stasso codice posso aggiungere task, sottotask e progetti)
                                intent.putExtra("tipo_form", "task")
                                intent.putExtra("project-id",projectId)
                                intent.putExtra("role",role)
                                Log.d(TAG,"STO CHIAMANDO NEWPROJECTACTIVITY con TIPO FORM= task e PROJECTID= $projectId")
                                startActivity(intent)
                                //CHIUDO E TORNO ALLA SCHERMATA DEL PROGETTO E AGGIORNO con nuovo progetto
                                loadTask()
                            }

                            projectCreatorTextView.text = projectCreator
                            projectAssignedTextView.text=projectLeader
                            progLeaderTask.visibility = View.VISIBLE
                            projectNameTextView.text = projectName
                            projectDeadlineTextView.text = "$projectDeadline"
                            projectDescriptionTextView.text="$projectdescr"

                            //carica i task
                            loadTask()

                        } else if(role=="Manager") {
                            //TOLGO VISUALIZZAZIONE RECYCLER VIEW
                            progLeaderTask.visibility = View.GONE
                            projectNameTextView.text = projectName
                            projectDeadlineTextView.text = "$projectDeadline"
                            projectCreatorTextView.text = "$projectCreator"
                            projectAssignedTextView.text = "$projectLeader"
                            projectDescriptionTextView.text="$projectdescr"
                        }else{
                            //generare errore perche ruolo è errato
                        }
                    } else {
                        Log.d(TAG, "No such document")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.d(TAG, "get failed with ", exception)
                }
        }else if (tipo == "task") {
            Log.d(TAG, "LOAD DETAILS TASK")
            lifecycleScope.launch {
                try {
                    val projectDocument = db.collection("progetti").document(projectId).get().await()
                    val projectLeader = projectDocument.getString("leader")
                    projectCreatorTextView.text = projectLeader

                    val taskDocument = db.collection("progetti")
                        .document(projectId)
                        .collection("task")
                        .document(taskId)
                        .get()
                        .await()

                    if (taskDocument != null) {
                        val taskName = taskDocument.getString("titolo")?.uppercase()
                        val taskDeadline = taskDocument.getString("scadenza")
                        val taskdescr = taskDocument.getString("descrizione")
                        val taskDev = taskDocument.getString("developer")
                        taskDocument.getString("developer")?.split(" ")?.joinToString(" ") {
                            it.replaceFirstChar {
                                if (it.isLowerCase()) it.titlecase(
                                    Locale.getDefault()
                                ) else it.toString()
                            }
                        }

                        Log.d(TAG, "HO OTTENUTO LE SEGUENTI INFORMAZIONI: TASKNAME $taskName TASKDEADLINE $taskDeadline TASKDESCRIZIONE $taskdescr TASKDEV $taskDev ")
                        projectAssignedTextView.text = taskDev
                        progLeaderTask.visibility = View.VISIBLE
                        projectNameTextView.text = taskName
                        projectDeadlineTextView.text = "$taskDeadline"
                        projectDescriptionTextView.text = "$taskdescr"

                        if(role=="Leader"){
                            Log.w(TAG,"Sono un leader e ho la recycler view gone")
                            //sto visualizzando un task di un leader percio non devo visualizzare i sottotask
                            progLeaderTask.visibility = View.GONE
                        }else if(role=="Developer"){
                            Log.w(TAG,"Sono un developer e ho la recycler view visibile")
                            //sto visualizzando un task di un developer percio devo visualizzare i sottotask
                            progLeaderTask.visibility = View.VISIBLE
                            findViewById<ImageButton>(R.id.aggiungiTaskButton).setOnClickListener {
                                //aprire schermata per aggiungere task
                                val intent =
                                    Intent(this@ProjectActivity, NewProjectActivity::class.java)
                                // Aggiungere il parametro "task" all'Intent per capire che stiamo aggiungendo un task (con stasso codice posso aggiungere task, sottotask e progetti)
                                intent.putExtra("tipo_form", "subtask")
                                intent.putExtra("project-id", projectId)
                                intent.putExtra("task-id", taskId)
                                intent.putExtra("role",role)
                                Log.d(
                                    TAG,
                                    "STO CHIAMANDO NEWPROJECTACTIVITY con TIPO FORM= sottotask e PROJECTID= $projectId e TASKID= $taskId e ROLE= $role"
                                )
                                startActivity(intent)
                                //CHIUDO E TORNO ALLA SCHERMATA DEL PROGETTO E AGGIORNO con nuovo progetto
                                loadTask()
                            }
                            loadTask()
                        }
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "get failed with ", e)
                }
            }
        }
        else if(tipo=="subtask" ){
            Log.d(TAG, "LOAD DETAILS SOTTOTASK")
            Log.d(TAG, "role è $role")

            lifecycleScope.launch {
                try {
                    val projectDocument =
                        db.collection("progetti").document(projectId).collection("task")
                            .document(taskId).get().await()
                    val projectLeader = projectDocument.getString("developer")
                    projectCreatorTextView.text = projectLeader


                    val subtaskDocument = db.collection("progetti")
                        .document(projectId)
                        .collection("task")
                        .document(taskId)
                        .collection("subtask")
                        .document(subtaskId)
                        .get()
                        .await()

                    if (subtaskDocument != null) {
                        val subtaskName = subtaskDocument.getString("titolo")?.uppercase()
                        val subtaskDeadline = subtaskDocument.getString("scadenza")
                        val subtaskdescr = subtaskDocument.getString("descrizione")
                        val subtaskDev = subtaskDocument.getString("developer")
                        subtaskDocument.getString("developer")?.split(" ")?.joinToString(" ") {
                            it.replaceFirstChar {
                                if (it.isLowerCase()) it.titlecase(
                                    Locale.getDefault()
                                ) else it.toString()
                            }
                        }

                        Log.d(
                            TAG,
                            "HO OTTENUTO LE SEGUENTI INFORMAZIONI: SUBTASKNAME $subtaskName SUBTASKDEADLINE $subtaskDeadline TASKDESCRIZIONE $subtaskdescr TASKDEV $subtaskDev "
                        )
                        findViewById<LinearLayout>(R.id.assignedCont).visibility = GONE
                        //visualizzo solo info sottotask da developer quindi tolgo recicler view e tolgo "assegnato a"
                        progLeaderTask.visibility = GONE
                        projectNameTextView.text = subtaskName
                        projectDeadlineTextView.text = "$subtaskDeadline"
                        projectDescriptionTextView.text = "$subtaskdescr"
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "get failed with ", e)
                }
            }


        }else{
            //errore
        }
    }

    //funzione che carica i task o sottotask nella recycler view
    private fun loadTask() {
        val data = ArrayList<ItemsViewModel>()
        val db = FirebaseFirestore.getInstance()
        val recyclerviewTask = findViewById<RecyclerView>(R.id.recyclerviewTask)
        val noTasksTextView = findViewById<TextView>(R.id.noTasksTextView)

        // Avvia una Coroutine nel contesto del Main Thread
        CoroutineScope(Dispatchers.Main).launch {
            try {
                if (role == "Leader") {
                    val taskDocuments = db.collection("progetti")
                        .document(projectId)
                        .collection("task")
                        .get()
                        .await() // Attendi il risultato della query

                    for (document in taskDocuments) {
                        val title = document.getString("titolo") ?: ""
                        val developer = document.getString("developer") ?: ""
                        val assegnato = false
                        data.add(ItemsViewModel(title, developer, assegnato, projectId, taskId))
                    }
                } else if (role == "Developer") {
                    val subtaskDocuments = db.collection("progetti")
                        .document(projectId)
                        .collection("task")
                        .document(taskId)
                        .collection("subtask")
                        .get()
                        .await() // Attendi il risultato della query

                    for (document in subtaskDocuments) {
                        val title = document.getString("titolo") ?: ""
                        val developer = document.getString("developer") ?: ""
                        val assegnato = false
                        data.add(ItemsViewModel(title, developer, assegnato, projectId, taskId))
                    }
                }

                // Aggiorna la UI una volta caricati i dati
                updateUI(data, recyclerviewTask, noTasksTextView)

            } catch (e: Exception) {
                Log.e(TAG, "Error loading tasks: ", e)
                updateUI(data, recyclerviewTask, noTasksTextView) // Anche in caso di errore, aggiorna la UI
            }
        }
    }

    //carica recycle view con i dati
    private fun updateUI(
        data: ArrayList<ItemsViewModel>,
        recyclerviewTask: RecyclerView,
        noTasksTextView: TextView
    ) {
        if (data.isEmpty()) {
            recyclerviewTask.visibility = View.GONE
            noTasksTextView.visibility = View.VISIBLE
        } else {
            recyclerviewTask.visibility = View.VISIBLE
            noTasksTextView.visibility = View.GONE

            recyclerviewTask.layoutManager = LinearLayoutManager(this)
            val adapter = CustomAdapter(data)
            adapter.setOnItemClickListener(object : CustomAdapter.onItemClickListener {
                override fun onItemClick(position: Int) {
                    // Logica quando un elemento viene cliccato
                }
            })
            recyclerviewTask.adapter = adapter
        }
    }


    companion object {
        private const val TAG = "ProjectDetailsActivity"
    }
}