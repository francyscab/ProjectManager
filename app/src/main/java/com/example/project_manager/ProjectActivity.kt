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
import android.widget.PopupMenu
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
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
    private lateinit var role:String
    private lateinit var projectId: String
    private lateinit var taskId: String
    private lateinit var subtaskId: String
    private var titolo: String? = null
    private var descrizione: String? = null
    private var scadenza: String? = null
    private var creator: String? = null
    private var assignedTo: String? = null
    private var leader: String? = null
    private var developer: String? = null
    private var progress: Int? = null

    private lateinit var projectNameTextView: TextView
    private lateinit var projectDescriptionTextView: TextView
    private lateinit var projectDeadlineTextView: TextView
    private lateinit var projectCreatorTextView: TextView
    private lateinit var projectAssignedTextView: TextView
    private lateinit var progressSeekBar: SeekBar
    private lateinit var progressInfo: TextView
    private lateinit var progLeaderTask:LinearLayout
    private lateinit var tipoElenco:TextView
    private lateinit var seekbarLayout:LinearLayout
    private lateinit var seekbutton:Button
    private lateinit var progressLabel:TextView
    private lateinit var sollecitaCont:LinearLayout
    private lateinit var sollecitaButton:Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_project)

        // Inizializza le views
        projectNameTextView = findViewById(R.id.projectNameTextView)
        projectDeadlineTextView = findViewById(R.id.projectDeadlineTextView)
        projectCreatorTextView = findViewById(R.id.projectCreatorTextView)
        projectDescriptionTextView=findViewById(R.id.descrizioneProgetto)
        projectAssignedTextView = findViewById(R.id.projectAssignedTextView)
        progressInfo=findViewById(R.id.projectProgressiTextView)
        sollecitaCont=findViewById(R.id.sollecitaCont)
        sollecitaButton=findViewById(R.id.sollecitaButton)

        // Ottieni INFORMAZIONI dall'intent

        projectId = intent.getStringExtra("projectId") ?: ""
        taskId=intent.getStringExtra("taskId")?:""
        subtaskId=intent.getStringExtra("subtaskId")?: ""
        role=intent.getStringExtra("role")?:""
        name=intent.getStringExtra("name")?:""
        Log.d(TAG, "PROJECT ACTIVITY, SONO STATA CHIAMATA DA $name CON PROJECTID $projectId TASKID $taskId SUBTASKID $subtaskId E ROLE $role")

        progLeaderTask= findViewById<LinearLayout>(R.id.progLeaderTask)
        tipoElenco=findViewById(R.id.typeElenco)
        seekbarLayout=findViewById(R.id.seekbarLayout)
        progressSeekBar=findViewById(R.id.seekBar)
        seekbutton=findViewById(R.id.saveButton)
        progressLabel=findViewById(R.id.progressLabel)

        val notificationHelper = NotificationHelper(this, FirebaseFirestore.getInstance())


        if (subtaskId.isNotEmpty()) {
            // Esegui la logica per il SOTTOTASK
            Log.d(TAG, " E' UN SUBTASK")
            loadDetails("subtask",notificationHelper)
        } else if (taskId.isNotEmpty()) {
            Log.d(TAG, "E' UNTASK")
            // Esegui la logica per il task
            loadDetails("task",notificationHelper)
        } else if (projectId.isNotEmpty()) {
            Log.d(TAG, "E' UN PROJECT")
            // Esegui la logica per il PROGETTO
            loadDetails("progetto",notificationHelper)
        }else {
            // Gestisci il caso in cui nessun ID sia passato
            Log.w(TAG, "Nessun ID del progetto o del task fornito.")
        }
        Log.d(TAG, "$assignedTo")
        menu()
    }

    private fun loadDetails(tipo: String, notificationHelper: NotificationHelper) {
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
                        creator=document.getString("creator")
                        titolo= document.getString("titolo")?.uppercase()
                        scadenza = document.getString("scadenza")
                        descrizione=document.getString("descrizione")
                        progress= document.getLong("progress")?.toInt() ?:0
                        assignedTo = document.getString("leader")?.split(" ")?.joinToString(" ") {
                            it.replaceFirstChar {
                                if (it.isLowerCase()) it.titlecase(
                                    Locale.getDefault()
                                ) else it.toString()
                            }
                        }
                        Log.d(TAG, "HO OTTENUTO LE SEGUENTI INFORMAZIONI: CREATOR$creator  PROJECTNAME $titolo PROJECTDEADLINE $scadenza PROJECTDESCRIZIONE $descrizione PROJECTLEADER $assignedTo ")
                        progressInfo.text="$progress%"

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

                            projectCreatorTextView.text = creator
                            projectAssignedTextView.text=assignedTo
                            progLeaderTask.visibility = View.VISIBLE
                            seekbarLayout.visibility= View.GONE
                            tipoElenco.text="Task"
                            projectNameTextView.text = titolo
                            projectDeadlineTextView.text = "$scadenza"
                            projectDescriptionTextView.text="$descrizione"
                            sollecitaCont.visibility=View.GONE


                            //carica i task
                            loadTask()

                        } else if(role=="Manager") {
                            //visualizza sollecita button che invia sollecito a leader progetto
                            sollecitaCont.visibility=View.VISIBLE
                            sollecitaButton.setOnClickListener {
                                Log.d(TAG,"STO cliccando su SOLLECITO")
                                notificationHelper.notification(role, name, "sollecito")
                            }

                            //TOLGO VISUALIZZAZIONE RECYCLER VIEW
                            progLeaderTask.visibility = View.GONE
                            seekbarLayout.visibility= View.GONE
                            projectNameTextView.text = titolo
                            projectDeadlineTextView.text = "$scadenza"
                            projectCreatorTextView.text = "$creator"
                            projectAssignedTextView.text = "$assignedTo"
                            projectDescriptionTextView.text="$descrizione"

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
                        titolo = taskDocument.getString("titolo")?.uppercase()
                        scadenza = taskDocument.getString("scadenza")
                        descrizione = taskDocument.getString("descrizione")
                        developer = taskDocument.getString("developer")
                        progress= taskDocument.getLong("progress")?.toInt() ?:0
                        taskDocument.getString("developer")?.split(" ")?.joinToString(" ") {
                            it.replaceFirstChar {
                                if (it.isLowerCase()) it.titlecase(
                                    Locale.getDefault()
                                ) else it.toString()
                            }
                        }

                        Log.d(TAG, "HO OTTENUTO LE SEGUENTI INFORMAZIONI: TASKNAME $titolo TASKDEADLINE $scadenza TASKDESCRIZIONE $descrizione TASKDEV $developer ")
                        projectAssignedTextView.text = developer
                        tipoElenco.text="Task"
                        projectNameTextView.text = titolo
                        projectDeadlineTextView.text = "$scadenza"
                        projectDescriptionTextView.text = "$descrizione"
                        progressInfo.text="$progress%"

                        if(role=="Leader"){
                            sollecitaCont.visibility=View.VISIBLE
                            sollecitaButton.setOnClickListener {
                                Log.d(TAG,"STO cliccando su SOLLECITO")
                                notificationHelper.notification(role, name, "sollecito")
                            }
                            Log.w(TAG,"Sono un leader e ho la recycler view gone")
                            seekbarLayout.visibility= View.GONE
                            //sto visualizzando un task di un leader percio non devo visualizzare i sottotask
                            progLeaderTask.visibility = View.GONE
                        }else if(role=="Developer"){
                            sollecitaCont.visibility=View.GONE
                            Log.w(TAG,"Sono un developer e ho la recycler view visibile")
                            //sto visualizzando un task di un developer percio devo visualizzare i sottotask
                            progLeaderTask.visibility = View.VISIBLE
                            tipoElenco.text="Sottotask"

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
                        titolo = subtaskDocument.getString("titolo")?.uppercase()
                        scadenza = subtaskDocument.getString("scadenza")
                        descrizione = subtaskDocument.getString("descrizione")
                        creator = subtaskDocument.getString("developer")
                        assignedTo=subtaskDocument.getString("developer")
                        progress= subtaskDocument.getLong("progress")?.toInt() ?:0
                        subtaskDocument.getString("developer")?.split(" ")?.joinToString(" ") {
                            it.replaceFirstChar {
                                if (it.isLowerCase()) it.titlecase(
                                    Locale.getDefault()
                                ) else it.toString()
                            }
                        }

                        Log.d(
                            TAG,
                            "HO OTTENUTO LE SEGUENTI INFORMAZIONI: SUBTASKNAME $titolo SUBTASKDEADLINE $scadenza TASKDESCRIZIONE $descrizione TASKDEV $developer "
                        )
                        findViewById<LinearLayout>(R.id.assignedCont).visibility = GONE
                        //visualizzo solo info sottotask da developer quindi tolgo recicler view e tolgo "assegnato a"
                        progLeaderTask.visibility = GONE
                        seekbarLayout.visibility= View.VISIBLE

                        manageSubtaskProgress(projectId,taskId,subtaskId,role,progressSeekBar,seekbutton,progressLabel,
                            { calculateAndUpdateTaskProgress(db,projectId,taskId,
                                { calculateAndUpdateProjectProgress(projectId,db) }) })

                        projectNameTextView.text = titolo
                        projectDeadlineTextView.text = "$scadenza"
                        projectDescriptionTextView.text = "$descrizione"
                        progressInfo.text="$progress%"
                        sollecitaCont.visibility=View.GONE

                    }
                } catch (e: Exception) {
                    Log.d(TAG, "get failed with ", e)
                }
            }


        }else{
            //errore
        }
    }

    private fun menu(){
        val menuButton: ImageButton = findViewById(R.id.menuButton)
        //il leader non può modificare un progetto
        if(role=="Leader" && (subtaskId.isEmpty()&& taskId.isEmpty())){
            menuButton.visibility=View.GONE
            return
        }
        //il developer non puo modificare un task
        else if(role=="Developer" && (subtaskId.isEmpty())){
            menuButton.visibility=View.GONE
            return
        }else{
            menuButton.visibility=View.VISIBLE
            menuButton.setOnClickListener { view ->
                // Crea il PopupMenu
                val popupMenu = PopupMenu(this, view)

                // Inflating the menu
                val inflater = popupMenu.menuInflater
                inflater.inflate(R.menu.menu_task_option, popupMenu.menu)

                // Imposta listener per gli item del menu
                popupMenu.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.menu_edit -> {
                            updateProject()
                            // Azione per modificare il task
                            true
                        }
                        R.id.menu_delete -> {
                            deleteItem()
                            //torna alla chermata precedente e ricarica
                            val intent = Intent(this, LoggedActivity::class.java)
                            startActivity(intent)
                            true
                        }
                        else -> false
                    }
                }

                // Mostra il menu
                popupMenu.show()
            }
        }

    }

    fun updateProject() {
        Log.d(TAG, "STO CHIAMANDO UPDATE PROJECT")
        val intent = Intent(this, UpdateProjectActivity::class.java)
        intent.putExtra("role", role)
        intent.putExtra("projectId", projectId)
        intent.putExtra("taskId", taskId)
        intent.putExtra("subtaskId", subtaskId)
        intent.putExtra("titolo", titolo)
        intent.putExtra("descrizione", descrizione)
        intent.putExtra("scadenza", scadenza)
        intent.putExtra("assignedTo", assignedTo)
        intent.putExtra("progress", progress)

        startActivity(intent)
    }

    fun deleteItem() {
        val db = FirebaseFirestore.getInstance()

        // Se c'è solo projectId
        if (!projectId.isNullOrEmpty() && taskId.isNullOrEmpty() && subtaskId.isNullOrEmpty()) {
            // Elimina il progetto dalla raccolta di progetti
            db.collection("progetti")
                .document(projectId)
                .delete()
                .addOnSuccessListener {
                    // Conferma eliminazione del progetto
                    Toast.makeText(this, "Progetto eliminato con successo", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    // Gestisci eventuali errori
                    Toast.makeText(this, "Errore durante l'eliminazione del progetto: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        // Se c'è anche taskId
        else if (!projectId.isNullOrEmpty() && !taskId.isNullOrEmpty() && subtaskId.isNullOrEmpty()) {
            // Elimina il task nel progetto specificato
            db.collection("progetti")
                .document(projectId)
                .collection("tasks")
                .document(taskId)
                .delete()
                .addOnSuccessListener {
                    // Conferma eliminazione del task
                    Toast.makeText(this, "Task eliminato con successo", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    // Gestisci eventuali errori
                    Toast.makeText(this, "Errore durante l'eliminazione del task: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        // Se c'è anche subTaskId
        else if (!projectId.isNullOrEmpty() && !taskId.isNullOrEmpty() && !subtaskId.isNullOrEmpty()) {
            // Elimina il sottotask nel task specificato del progetto
            db.collection("progetti")
                .document(projectId)
                .collection("tasks")
                .document(taskId)
                .collection("subtasks")
                .document(subtaskId)
                .delete()
                .addOnSuccessListener {
                    // Conferma eliminazione del sottotask
                    Toast.makeText(this, "Sottotask eliminato con successo", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    // Gestisci eventuali errori
                    Toast.makeText(this, "Errore durante l'eliminazione del sottotask: ${e.message}", Toast.LENGTH_SHORT).show()
                }
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
                        val taskId = document.id
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
                        val subtaskId = document.id
                        //il secondo title  rappresenta il subtask id
                        data.add(ItemsViewModel(title, developer, assegnato, projectId, taskId,subtaskId))
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
                    // Ottieni l'elemento cliccato
                    val selectedItem = data[position]
                    Log.w("l'elemento cliccato è", selectedItem.toString())

                    // Crea un intent per avviare ProjectActivity
                    val intent = Intent(this@ProjectActivity, ProjectActivity::class.java)

                    // Aggiungi informazioni necessarie per il passaggio dei dati
                    intent.putExtra("projectId", selectedItem.projectId)
                    intent.putExtra("taskId", selectedItem.taskId)
                    intent.putExtra("subtaskId", selectedItem.subtaskId) // Se è un subtask
                    intent.putExtra("role", role) // Mantieni il ruolo
                    intent.putExtra("name", name) // Mantieni il nome utente




                    // Avvia la nuova attività
                    startActivity(intent)
                }
            })
            recyclerviewTask.adapter = adapter
        }
    }

    fun manageSubtaskProgress(
        projectId: String,
        taskId: String,
        subtaskId: String,
        role: String,
        seekBar: SeekBar,
        saveButton: Button,
        progressLabel: TextView,
        onSuccess: () -> Unit
    ): Boolean {
        // Check if the role is valid
        if (role != "Developer") {
            Log.e("manageSubtaskProgress", "Invalid role: $role. This function is only for Developers.")
            return false
        }

        val db = FirebaseFirestore.getInstance()
        val collectionPath = "progetti/$projectId/task/$taskId/subtask"

        // Load the current progress from Firestore
        db.collection(collectionPath)
            .document(subtaskId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val currentProgress = document.getLong("progress")?.toInt() ?: 0
                    seekBar.progress = currentProgress
                    progressLabel.text = "$currentProgress%"
                } else {
                    // Document doesn't exist, set progress to 0
                    seekBar.progress = 0
                    progressLabel.text = "0%"
                }
            }
            .addOnFailureListener { exception ->
                Log.e("manageSubtaskProgress", "Error loading progress: ${exception.message}")
            }

        // Set up the Save button click listener
        saveButton.setOnClickListener {
            val currentProgress = seekBar.progress

            // Update the progress in Firestore
            db.collection(collectionPath)
                .document(subtaskId)
                .update("progress", currentProgress)
                .addOnSuccessListener {
                    onSuccess()
                    Log.d("manageSubtaskProgress", "Progress saved successfully: $currentProgress")
                }
                .addOnFailureListener { exception ->
                    Log.e("manageSubtaskProgress", "Error saving progress: ${exception.message}")
                }
        }
        return true
    }

    fun calculateAndUpdateProjectProgress(
        projectId: String,
        db: FirebaseFirestore,
    ) {
        val projectRef = db.collection("progetti").document(projectId)

        // Get all tasks for the project
        projectRef.collection("task")
            .get()
            .addOnSuccessListener { taskDocuments ->
                if (taskDocuments.isEmpty) {
                    // No tasks found, set project progress to 0
                    projectRef.update("progress", 0)
                        .addOnSuccessListener {
                            Log.d("calculateAndUpdateProjectProgress", "Project progress updated to 0 (no tasks).")

                        }
                        .addOnFailureListener { exception ->
                            Log.e("calculateAndUpdateProjectProgress", "Error updating project progress to 0: ${exception.message}")
                        }
                    return@addOnSuccessListener
                }

                var totalProgress = 0
                var taskCount = 0

                for (taskDocument in taskDocuments) {
                    val progress = taskDocument.getLong("progress")?.toInt() ?: 0
                    totalProgress += progress
                    taskCount++
                }

                // Calculate average progress
                val averageProgress = if (taskCount > 0) {
                    totalProgress / taskCount
                } else {
                    0
                }

                // Update project's "avanzamento" field
                projectRef.update("progress", averageProgress)
                    .addOnSuccessListener {
                        Log.d("calculateAndUpdateProjectProgress", "Project progress updated to: $averageProgress")

                    }
                    .addOnFailureListener { exception ->
                        Log.e("calculateAndUpdateProjectProgress", "Error updating project progress: ${exception.message}")
                    }
            }
            .addOnFailureListener { exception ->
                Log.e("calculateAndUpdateProjectProgress", "Error getting tasks: ${exception.message}")
            }
    }

    fun calculateAndUpdateTaskProgress(
        db: FirebaseFirestore,
        projectId: String,
        taskId: String,
        onSuccess: () -> Unit,

    ) {
        val taskRef = db.collection("progetti").document(projectId).collection("task").document(taskId)

        // Get all subtasks for the task
        taskRef.collection("subtask")
            .get()
            .addOnSuccessListener { subtaskDocuments ->
                if (subtaskDocuments.isEmpty) {
                    // No subtasks found, set task progress to 0
                    taskRef.update("progress", 0)
                        .addOnSuccessListener {
                            Log.d("calculateAndUpdateTaskProgress", "Task progress updated to 0 (no subtasks).")
                            onSuccess()
                        }
                        .addOnFailureListener { exception ->
                            Log.e("calculateAndUpdateTaskProgress", "Error updating task progress to 0: ${exception.message}")
                        }
                    return@addOnSuccessListener
                }

                var totalProgress = 0
                var subtaskCount = 0

                for (subtaskDocument in subtaskDocuments) {
                    val progress = subtaskDocument.getLong("progress")?.toInt() ?: 0
                    totalProgress += progress
                    subtaskCount++
                }

                // Calculate average progress
                val averageProgress = if (subtaskCount > 0) {
                    totalProgress / subtaskCount
                } else {
                    0
                }

                // Update task's "progress" field
                taskRef.update("progress", averageProgress)
                    .addOnSuccessListener {
                        Log.d("calculateAndUpdateTaskProgress", "Task progress updated to: $averageProgress")
                        onSuccess()
                    }
                    .addOnFailureListener { exception ->
                        Log.e("calculateAndUpdateTaskProgress", "Error updating task progress: ${exception.message}")

                    }
            }
            .addOnFailureListener { exception ->
                Log.e("calculateAndUpdateTaskProgress", "Error getting subtasks: ${exception.message}")

            }
    }

    companion object {
        private const val TAG = "ProjectDetailsActivity"
    }

    override fun onBackPressed() {
        // If we're viewing a subtask, go back to the task view
        if (subtaskId.isNotEmpty()) {
            Log.d(TAG, "Back from subtask to task")
            val intent = Intent(this, ProjectActivity::class.java)
            intent.putExtra("projectId", projectId)
            intent.putExtra("taskId", taskId)
            intent.putExtra("role", role)
            intent.putExtra("name", name)
            startActivity(intent)
            finish() // Finish the current activity (the subtask view)
        } else if (taskId.isNotEmpty() && role == "Leader") {
            // If it's a task, go to the project view
            Log.d(TAG, "Back from task to project")
            val intent = Intent(this, ProjectActivity::class.java)
            intent.putExtra("projectId", projectId)
            intent.putExtra("role", role)
            intent.putExtra("name", name)
            startActivity(intent)
            finish() // Finish the current activity (the task view)
        }else if (taskId.isNotEmpty() && role == "Developer") {
            // If it's a task and the role is Developer, go to the LoggedActivity
            Log.d(TAG, "Back from task (Developer) to LoggedActivity")
            val intent = Intent(this, LoggedActivity::class.java)
            intent.putExtra("name", name)
            startActivity(intent)
            finish() // Finish the current activity (the task view) else if (projectId.isNotEmpty()) {
            // If it's a project, go to the previous activity
            Log.d(TAG, "Back from project to previous activity")
            finish() // Finish the current activity (the project view)
        } else {
            // Default behavior (shouldn't happen, but just in case)
            Log.d(TAG, "Back default")
            super.onBackPressed()
        }
    }
}

