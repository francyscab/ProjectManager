package com.example.project_manager


import android.content.ContentValues
import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
                    val progLeaderTask= findViewById<LinearLayout>(R.id.progLeaderTask)
                    //val testoTask=findViewById<TextView>(R.id.testoTask)

                    // nascondere il TextView
                    if (role=="Leader") {
                        //aggiungere lisener su bottone + per task
                        val bottoneTask=findViewById<ImageButton>(R.id.aggiungiTaskButton).setOnClickListener {
                            //aprire schermata per aggiungere task
                            val intent = Intent(this, NewProjectActivity::class.java)
                            // Aggiungere il parametro "task" all'Intent
                            intent.putExtra("tipo_form", "task")
                            intent.putExtra("project-id",projectId)
                            startActivity(intent)
                        }

                        //RENDI VISIBILE SEZIONE PER TASK
                        progLeaderCont.visibility = View.GONE
                        progLeaderTask.visibility = View.VISIBLE
                        projectNameTextView.text = projectName
                        projectDeadlineTextView.text = "$projectDeadline"
                        projectDescriptionTextView.text="$projectdescr"

                        loadTask()

                    } else {
                        // Se la condizione è falsa mostrare il TextView CON NOME LEADER PROGETTO
                        progLeaderTask.visibility = View.GONE
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

                    data.add(ItemsViewModel(title, developer, assegnato))
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