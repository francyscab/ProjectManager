package com.example.project_manager


import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

class ProjectActivity : AppCompatActivity() {

    private lateinit var projectId: String
    private lateinit var taskId: String
    private lateinit var subtaskId: String
    private lateinit var projectNameTextView: TextView
    private lateinit var projectDescriptionTextView: TextView
    private lateinit var projectDeadlineTextView: TextView
    private lateinit var projectLeaderTextView: TextView
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
        projectLeaderTextView = findViewById(R.id.projectLeaderTextView)
        projectDescriptionTextView=findViewById(R.id.descrizioneProgetto)
        //TaskListLayout = findViewById(R.id.TaskListLayout)
        //progressSeekBar = findViewById(R.id.progressSeekBar)
        //assegnaTask = findViewById(R.id.buttonAssegnaSottottask)
        //salvatask = findViewById(R.id.buttonSalvaAsseganmenti)

        // Ottieni l'ID del progetto dall'intent

        Log.d(TAG, "paramentri ricevuti su projectactivity")
        projectId = intent.getStringExtra("projectId") ?: ""
        Log.d(TAG, "projectid  $projectId")
        taskId=intent.getStringExtra("taskId")?:""
        Log.d(TAG, "taskid  $taskId")
        subtaskId=intent.getStringExtra("subtaskId")?: ""
        Log.d(TAG, "subtaskid  $subtaskId")
        role=intent.getStringExtra("role")?:""
        Log.d(TAG, "role  $role")

        progLeaderCont = findViewById<LinearLayout>(R.id.progLeaderCont)
        progLeaderTask= findViewById<LinearLayout>(R.id.progLeaderTask)

        if (subtaskId.isNotEmpty()) {
            // Esegui la logica per il progetto
            Log.d(TAG, "SUBTASK")
            loadDetails("subtask")
        } else if (taskId.isNotEmpty()) {
            Log.d(TAG, "TASK")
            // Esegui la logica per il task
            loadDetails("task")
        } else if (projectId.isNotEmpty()) {
            Log.d(TAG, "PROJECT")
            // Esegui la logica per il task
            loadDetails("progetto")
        }else {
            // Gestisci il caso in cui nessun ID sia passato
            Log.w(TAG, "Nessun ID del progetto o del task fornito.")
        }
    }

    private fun loadDetails(tipo : String) {
        val db = FirebaseFirestore.getInstance()

        //devo capire se si tratta di un task, un progetto o in un successivo momento di un sottotask
        if (tipo=="progetto"){
            Log.d(TAG, "LOADDETAILS PROGETTO")
            val projectRef = db.collection("progetti").document(projectId)

            // Ottieni i dettagli del progetto dal documento
            // -se sei un leader carica anche  ì task e togli sezione per visualizzare leader
            // -se sei manager togli sezione per visulaizzare task e visualizza sezione per nome leader
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
                        //val progLeaderCont = findViewById<LinearLayout>(R.id.progLeaderCont)
                        //val progLeaderTask= findViewById<LinearLayout>(R.id.progLeaderTask)

                        // nascondere sezione con leader
                        if (role=="Leader") {
                            //aggiungere lisener su bottone + per task
                            findViewById<ImageButton>(R.id.aggiungiTaskButton).setOnClickListener {
                                //aprire schermata per aggiungere task
                                val intent = Intent(this, NewProjectActivity::class.java)
                                // Aggiungere il parametro "task" all'Intent per capire che stiamo aggiungendo un task (con stasso codice posso aggiungere task, sottotask e progetti)
                                intent.putExtra("tipo_form", "task")
                                intent.putExtra("project-id",projectId)
                                startActivity(intent)
                                //CHIUDO E TORNO ALLA SCHERMATA DEL PROGETTO E AGGIORNO con nuovo progetto
                                loadTask()
                            }

                            //RENDI VISIBILE SEZIONE recycleview PER TASK
                            progLeaderCont.visibility = View.GONE
                            progLeaderTask.visibility = View.VISIBLE
                            projectNameTextView.text = projectName
                            projectDeadlineTextView.text = "$projectDeadline"
                            projectDescriptionTextView.text="$projectdescr"

                            //carica i task
                            loadTask()

                        } else if(role=="Manager") {
                            // Se la condizione è falsa mostrare il TextView CON NOME LEADER PROGETTO
                            progLeaderTask.visibility = View.GONE
                            progLeaderCont.visibility = View.VISIBLE
                            projectNameTextView.text = projectName
                            projectDeadlineTextView.text = "$projectDeadline"
                            projectLeaderTextView.text = "$projectLeader"
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
        }else if(tipo=="task"){
            val taskRef = db.collection("progetti").document(projectId).collection("task").document(taskId)
            taskRef.get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        val taskName = document.getString("titolo")?.uppercase()
                        val taskDeadline = document.getString("scadenza")
                        val taskdescr = document.getString("descrizione")
                        val taskDev = document.getString("developer")
                        document.getString("developer")?.split(" ")?.joinToString(" ") {
                            it.replaceFirstChar {
                                if (it.isLowerCase()) it.titlecase(
                                    Locale.getDefault()
                                ) else it.toString()
                            }
                        }

                        progLeaderTask.visibility = View.GONE
                        progLeaderCont.visibility = View.VISIBLE
                        projectNameTextView.text = taskName
                        projectDeadlineTextView.text = "$taskDeadline"
                        findViewById<TextView>(R.id.projectLeader).text="DEVELOPER"
                        projectLeaderTextView.text = "$taskDev"
                        projectDescriptionTextView.text="$taskdescr"
                    }
                }.addOnFailureListener { exception ->
                    Log.d(TAG, "get failed with ", exception)
                }
        }else if(tipo=="subtask"){
            val typeElencoTextView = findViewById<TextView>(R.id.typeElenco)
            typeElencoTextView.text = "SOTTOTASK" //

        }else{
            //errore
        }

        // Ottieni il riferimento al documento del progetto

    }

    private fun loadTask() {
        val data = ArrayList<ItemsViewModel>()
        val db = FirebaseFirestore.getInstance()

        db.collection("progetti")
            .document(projectId)
            .collection("task")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val title = document.getString("titolo") ?: ""
                    val developer = document.getString("developer") ?: ""
                    val assegnato = false

                    data.add(ItemsViewModel(title, developer, assegnato, projectId, taskId))
                }

                Log.d(TAG, "data array: $data")


                // Ottieni riferimenti a RecyclerView e TextView
                val recyclerviewTask = findViewById<RecyclerView>(R.id.recyclerviewTask)
                val noTasksTextView = findViewById<TextView>(R.id.noTasksTextView)

                if (data.isEmpty()) {
                    // Se la lista è vuota, nascondi il RecyclerView e mostra il TextView
                    recyclerviewTask.visibility = View.GONE
                    noTasksTextView.visibility = View.VISIBLE
                } else {
                    // Altrimenti, imposta l'adapter e mostra il RecyclerView
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
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error getting tasks: ", exception)
                // Gestisci gli errori, se necessario
            }
    }


    companion object {
        private const val TAG = "ProjectDetailsActivity"
    }
}