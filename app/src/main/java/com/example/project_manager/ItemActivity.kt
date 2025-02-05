package com.example.project_manager

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
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
import com.example.project_manager.models.ItemsViewModel
import com.example.project_manager.models.Role
import com.example.project_manager.services.ProjectService
import com.example.project_manager.services.SubTaskService
import com.example.project_manager.services.TaskService
import com.example.project_manager.services.UserService
import com.example.project_manager.utils.FileRepository
import com.example.project_manager.utils.NotificationHelper
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ItemActivity : AppCompatActivity() {


    private lateinit var role:Role
    private lateinit var projectId: String
    private lateinit var taskId: String
    private lateinit var subtaskId: String

    val projectService= ProjectService()
    val userService= UserService()
    val taskService= TaskService()
    val subtaskService= SubTaskService()
    private val PICK_FILE_REQUEST_CODE = 2002
    private val fileRepository = FileRepository()

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
    private lateinit var feedbackLayout:LinearLayout
    private lateinit var feedback:LinearLayout
    private lateinit var valuta:Button
    private var isFeedbackGiven: Boolean = false
    private lateinit var feedbackScore: TextView
    private lateinit var feedbackComment: TextView
    private lateinit var assignedCont:LinearLayout
    private lateinit var tipo:String
    private lateinit var buttonFile :ImageButton



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_project)

        val notificationHelper = NotificationHelper(this, FirebaseFirestore.getInstance())
        inizialiseView()
        getIntentData()

        tipo = getItemType(subtaskId, taskId, projectId)

        lifecycleScope.launch{
            role=userService.getCurrentUserRole()!!
            loadDetails(tipo,notificationHelper)
            menu()
        }
    }
    private fun getIntentData(){
        projectId = intent.getStringExtra("projectId") ?: ""
        taskId=intent.getStringExtra("taskId")?:""
        subtaskId=intent.getStringExtra("subtaskId")?: ""
    }

    private fun inizialiseView(){
        assignedCont=findViewById(R.id.assignedCont)
        sollecitaCont=findViewById(R.id.sollecitaCont)
        sollecitaButton=findViewById(R.id.sollecitaButton)
        feedbackLayout=findViewById(R.id.feedbackLayout)
        feedback=findViewById(R.id.feedback)//layout del bottone feedback
        valuta=findViewById(R.id.feedbackButton)
        feedbackScore=findViewById(R.id.feedbackScore)
        feedbackComment=findViewById(R.id.feedbackComment)
        progLeaderTask= findViewById<LinearLayout>(R.id.progLeaderTask)
        tipoElenco=findViewById(R.id.typeElenco)
        seekbarLayout=findViewById(R.id.seekbarLayout)
        progressSeekBar=findViewById(R.id.seekBar)
        seekbutton=findViewById(R.id.saveButton)
        progressLabel=findViewById(R.id.progressLabel)
        buttonFile=findViewById(R.id.aggiungiFileButton)
    }

    private fun getItemType(subtaskId: String, taskId: String, projectId: String): String {
        if (subtaskId.isNotEmpty()) {
            return "subtask"
        } else if (taskId.isNotEmpty()) {
            return "task"
        } else if (projectId.isNotEmpty()) {
            return "progetto"
        }
        Log.e(TAG, "Nessun ID del progetto o del task fornito.")

        throw IllegalArgumentException("Nessun ID del progetto o del task fornito.")
    }

    private suspend fun loadDetails(tipo: String, notificationHelper: NotificationHelper) {
        when (tipo) {
            "progetto" -> handleProjectDetails(notificationHelper) { feedback() }
            "task" -> handleTaskDetails( notificationHelper) { feedback() }
            "subtask" -> handleSubtaskDetails() { feedback() }
            else -> Log.e(TAG, "Tipo non riconosciuto: $tipo")
        }
    }

    private fun feedback(){

    }
    private suspend fun handleProjectDetails(notificationHelper: NotificationHelper, onComplete: (() -> Unit)? = null) {
        if (role == Role.Leader) {
            setupLeaderView(notificationHelper)
        } else if (role == Role.Manager) {
            setupManagerView(notificationHelper)

        } else {
            throw error("Ruolo non valido")
        }
        onComplete?.invoke()
    }

    private suspend fun handleTaskDetails(notificationHelper: NotificationHelper, onComplete: (() -> Unit)? = null) {
        try {
            if (role == Role.Leader) {
                setupLeaderTaskView(notificationHelper)
            } else if (role == Role.Developer) {
                setupDeveloperTaskView()
            }

            onComplete?.invoke()
        } catch (e: Exception) {
            Log.e(TAG, "Errore durante il caricamento dei dettagli del task", e)
        }
    }

    private suspend fun handleSubtaskDetails(onComplete: (() -> Unit)? = null) {

        try {
            val subTask=subtaskService.getSubTaskById(projectId,taskId,subtaskId)
            setupDeveloperSubTaskView()

            setupProgressManagement(projectId, taskId, subtaskId, progressSeekBar, progressLabel)
        } catch (e: Exception) {
            Log.e(TAG, "Errore durante il caricamento dei dettagli del sottotask", e)
        }
    }

    private suspend fun setupLeaderView(notificationHelper: NotificationHelper) {
        setData(tipo,taskId,projectId,subtaskId)
        findViewById<ImageButton>(R.id.aggiungiTaskButton).setOnClickListener {
            val intent = Intent(this, NewItemActivity::class.java).apply {
                putExtra("tipoForm", "task")
                putExtra("projectId", projectId)
                putExtra("role", role)
            }
            startActivity(intent)
            //loadTask()
        }
        progLeaderTask.visibility = View.VISIBLE
        seekbarLayout.visibility = View.GONE
        sollecitaCont.visibility = View.GONE


        loadTask()
    }

    private suspend fun setupManagerView(notificationHelper: NotificationHelper) {
        setData(tipo,taskId,projectId,subtaskId)

        sollecitaCont.visibility = View.VISIBLE
        progLeaderTask.visibility = View.GONE
        seekbarLayout.visibility = View.GONE

        sollecitaButton.setOnClickListener {
            lifecycleScope.launch {
                val item = projectService.getProjectById(projectId)!!
                val assignedTo = item.assignedTo
                notificationHelper.handleNotification(Role.Developer, assignedTo, "sollecito")
            }
        }

    }

    private suspend fun setupDeveloperView() {
        sollecitaCont.visibility = View.GONE
        progLeaderTask.visibility = View.GONE
        seekbarLayout.visibility = View.GONE

        setData(tipo,taskId,projectId,subtaskId)
    }


    private suspend fun setupDeveloperSubTaskView(){
        sollecitaCont.visibility = View.GONE
        progLeaderTask.visibility = View.GONE
        seekbarLayout.visibility = View.VISIBLE
        assignedCont.visibility=View.GONE

        setData(tipo,taskId,projectId,subtaskId)

    }
    private suspend fun setupLeaderTaskView( notificationHelper: NotificationHelper) {
        setData(tipo,taskId,projectId,subtaskId)
        sollecitaCont.visibility = View.VISIBLE
        sollecitaButton.setOnClickListener {
            //notificationHelper.handleNotification(role, name, "sollecito")
        }
        seekbarLayout.visibility = View.GONE
        progLeaderTask.visibility = View.GONE

    }

    private suspend fun setupDeveloperTaskView() {
        setData(tipo,taskId,projectId,subtaskId)
        sollecitaCont.visibility = View.GONE
        progLeaderTask.visibility = View.VISIBLE
        seekbarLayout.visibility = View.GONE

        tipoElenco.text = "Sottotask"

        findViewById<ImageButton>(R.id.aggiungiTaskButton).setOnClickListener {
            val intent = Intent(this, NewItemActivity::class.java).apply {
                putExtra("tipoForm", "subtask")
                putExtra("projectId", projectId)
                putExtra("taskId", taskId)
                putExtra("role", role)
            }
            startActivity(intent)
            //loadTask()
        }
        loadTask()
    }

    private suspend fun setData(tipo:String,taskId: String, projectId: String, subtaskId: String){
        var item:ItemsViewModel
        if(tipo=="progetto"){
            Log.d(TAG, "progetto")
            item= projectService.getProjectById(projectId)!!}
        else if(tipo=="task")
            item=taskService.getTaskById(projectId,taskId)!!
        else if(tipo=="subtask")
            item=subtaskService.getSubTaskById(projectId,taskId,subtaskId)!!
        else
            return

        Log.e(TAG, "Nessun ID del progetto o del task fornito.")

        setName(item)
        setDescription(item)
        setDeadline(item)
        setCreator(item)
        setAssignedTo(item)
        setProgressInfo(item)
    }


    private suspend fun setName(item:ItemsViewModel){
        val projectNameTextView = findViewById<TextView>(R.id.projectNameTextView)
        projectNameTextView.text = item.title.uppercase()
    }

    private suspend fun setDescription(item:ItemsViewModel){
        val projectDescriptionTextView=findViewById<TextView>(R.id.descrizioneProgetto)
        projectDescriptionTextView.text =item.description
    }

    private fun setDeadline(item:ItemsViewModel){
        val projectDeadlineTextView = findViewById<TextView>(R.id.projectDeadlineTextView)
        projectDeadlineTextView.text = item.deadline
    }

    private suspend fun setAssignedTo(item:ItemsViewModel){
        val projectAssignedTextView = findViewById<TextView>(R.id.projectAssignedTextView)
        val name=userService.getUserNameById(item.assignedTo)
        projectAssignedTextView.text = name
    }
    private suspend fun setCreator(item:ItemsViewModel){
        val projectCreatorTextView = findViewById<TextView>(R.id.projectCreatorTextView)
        val name=userService.getUserNameById(item.creator)
        projectCreatorTextView.text = name

    }
    private fun setProgressInfo(item:ItemsViewModel){
        val progressInfo = findViewById<TextView>(R.id.progressiTextView)
        progressInfo.text = "${item.progress}%"
    }



    /*private fun feedback() {
        //progetto aperto da leader
        if (projectId.isNotEmpty() && taskId.isEmpty() && role == "Leader") {
            feedback.visibility = View.GONE
            feedbackLayout.visibility = View.GONE
        }
        // progetto aperto da manager
        else if (projectId.isNotEmpty() && taskId.isEmpty() && role == "Manager") {
            if (progress == 100 && !isFeedbackGiven) {
                Log.d(TAG, "non ho mai dato un feedback")
                feedback.visibility = View.VISIBLE
                feedbackLayout.visibility = View.GONE
                openFeedbackForm("project")
            } else if (isFeedbackGiven) {
                Log.d(TAG, "ho gia dato un feedback lo devo solo visualizzare")
                feedback.visibility = View.GONE
                feedbackLayout.visibility = View.VISIBLE
            } else if (progress != 100) {
                Log.d(TAG, "non ho ancora terminato il progetto")
                feedback.visibility = View.GONE
                feedbackLayout.visibility = View.GONE
            }
        }
        // task aperto da leader
        else if (projectId.isNotEmpty() && taskId.isNotEmpty() && role == "Leader") {
            Log.d(TAG, "sono qui")
            if (progress == 100 && !isFeedbackGiven) {
                Log.d(TAG, "non ho mai dato un feedback")
                feedback.visibility = View.VISIBLE
                feedbackLayout.visibility = View.GONE
                openFeedbackForm("task")
            } else if (isFeedbackGiven) {
                Log.d(TAG, "ho gia dato un feedback lo devo solo visualizzare")
                feedback.visibility = View.GONE
                feedbackLayout.visibility = View.VISIBLE
            } else if (progress != 100) {
                Log.d(TAG, "non ho ancora terminato il task")
                feedback.visibility = View.GONE
                feedbackLayout.visibility = View.GONE
            }
        }
        //per il momento tutti altri casi non lo vedono
        else if(isFeedbackGiven) {
            feedback.visibility=View.GONE
            feedbackLayout.visibility=View.VISIBLE
        }
        else{
            feedback.visibility=View.GONE
            feedbackLayout.visibility=View.GONE

        }
    }

    fun openFeedbackForm(tipo: String) {
        valuta.setOnClickListener {
            val dialogView = layoutInflater.inflate(R.layout.feedback_form, null)
            val radioGroup = dialogView.findViewById<RadioGroup>(R.id.ratingRadioGroup)
            val commentEditText = dialogView.findViewById<EditText>(R.id.commentEditText)

            AlertDialog.Builder(this)
                .setTitle("Dai un feedback")
                .setView(dialogView)
                .setPositiveButton("Salva") { _, _ ->
                    val selectedRatingId = radioGroup.checkedRadioButtonId
                    val rating =
                        dialogView.findViewById<RadioButton>(selectedRatingId)?.text?.toString()
                            ?.toInt() ?: 0
                    val comment = commentEditText.text.toString()

                    saveFeedbackToFirestore(tipo,rating, comment)
                }
                .setNegativeButton("Annulla", null)
                .show()
        }
    }

    private fun saveFeedbackToFirestore(tipo: String, rating: Int, comment: String) {
        // Crea un oggetto feedback
        val feedbackData = hashMapOf(
            "rating" to rating,
            "comment" to comment,
            "valutato" to true
        )

        when (tipo) {
            "project" -> {
                // Aggiungi il feedback al documento "feedback" del progetto
                db!!.collection("progetti").document(projectId)
                    .update(feedbackData as Map<String, Any>)  // Usa update() per aggiungere i campi senza sovrascrivere
                    .addOnSuccessListener {
                        // Gestisci il successo
                        handleFeedbackSuccess(rating, comment)
                    }
                    .addOnFailureListener { exception ->
                        // Gestisci gli errori
                        Log.e(TAG, "Errore durante l'aggiornamento del feedback del progetto", exception)
                        handleFeedbackError()
                    }
            }
            "task" -> {
                Log.d(TAG, "salvo task")
                // Aggiungi il feedback al documento "feedback" del task
                db!!.collection("progetti").document(projectId)
                    .collection("task").document(taskId)
                    .update(feedbackData as Map<String, Any>)
                    .addOnSuccessListener {
                        // Gestisci il successo
                        handleFeedbackSuccess(rating, comment)
                    }
                    .addOnFailureListener { exception ->
                        // Gestisci gli errori
                        Log.e(TAG, "Errore durante l'aggiornamento del feedback del task", exception)
                        handleFeedbackError()
                    }
            }
            else -> {
                // Gestisci l'errore in caso di tipo non valido
                Toast.makeText(this, "Tipo non valido: $tipo", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun handleFeedbackSuccess(rating: Int, comment: String) {
        valuta.visibility = View.GONE
        feedbackLayout.visibility = View.VISIBLE
        feedbackScore.text = "$rating"
        feedbackComment.text = "$comment"
        isFeedbackGiven = true
    }

    private fun handleFeedbackError() {
        Toast.makeText(this, "Errore durante il salvataggio del feedback", Toast.LENGTH_SHORT).show()
    }
*/







    private fun menu(){
        val menuButton: ImageButton = findViewById(R.id.menuButton)
        //il leader non può modificare un progetto
        if(role==Role.Leader && tipo=="Progetto"){
            menuButton.visibility=View.GONE
            return
        }
        //il developer non puo modificare un task
        else if(role==Role.Developer && tipo=="subtask"){
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
                            //deleteItem()
                            //torna alla chermata precedente e ricarica
                            //val intent = Intent(this, LoggedActivity::class.java)
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
        intent.putExtra("projectId", projectId)
        intent.putExtra("taskId", taskId)
        intent.putExtra("subtaskId", subtaskId)

        startActivity(intent)
    }

    /*fun deleteItem() {
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
*/

    //funzione che carica i task o sottotask nella recycler view
   private fun loadTask() {
        var data = ArrayList<ItemsViewModel>()
        val recyclerviewTask = findViewById<RecyclerView>(R.id.recyclerviewTask)
        val noTasksTextView = findViewById<TextView>(R.id.noTasksTextView)

        // Avvia una Coroutine nel contesto del Main Thread
        CoroutineScope(Dispatchers.Main).launch {
            try {
                if (role == Role.Leader) {
                    data=taskService.getAllTaskByProjectId(projectId)
                    }
                else if (role == Role.Developer) {
                    data=subtaskService.getAllSubTaskByTaskId(projectId,taskId)
                }
                updateUI(data, recyclerviewTask, noTasksTextView)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading tasks: ", e)
                updateUI(data, recyclerviewTask, noTasksTextView) // Anche in caso di errore, aggiorna la UI
            }
        }
    }

    private fun updateUI(
        data: ArrayList<ItemsViewModel>,
        recyclerViewTask: RecyclerView,
        noTasksTextView: TextView
    ) {
        val hasData = data.isNotEmpty()
        recyclerViewTask.visibility = if (hasData) View.VISIBLE else View.GONE
        noTasksTextView.visibility = if (hasData) View.GONE else View.VISIBLE

        if (hasData) {
            // Configure RecyclerView
            recyclerViewTask.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = CustomAdapter(data).apply {
                    setOnItemClickListener(object : CustomAdapter.onItemClickListener {
                        override fun onItemClick(position: Int) {
                            navigateToSelectedItem(data[position])
                        }
                    })
                }
            }
        }
    }

    private fun navigateToSelectedItem(selectedItem: ItemsViewModel) {
        Log.d(TAG, "Selected item: $selectedItem")

        Intent(this@ItemActivity, ItemActivity::class.java).apply {
            putExtra("projectId", selectedItem.projectId)
            putExtra("taskId", selectedItem.taskId)
            putExtra("subtaskId", selectedItem.subtaskId)
            putExtra("role", role.toString())
            startActivity(this)
        }
    }

    private fun setupProgressManagement(projectId: String, taskId: String, subtaskId: String,seekBar: SeekBar, progressLabel: TextView) {
        if (role != Role.Developer) return

        lifecycleScope.launch {
            try {
                val currentProgress = subtaskService.getSubTaskProgress(projectId, taskId, subtaskId)
                seekBar.progress = currentProgress
                progressLabel.text = "$currentProgress%"

                val saveButton = findViewById<Button>(R.id.saveButton)
                saveButton.setOnClickListener {
                    saveProgress(projectId, taskId, subtaskId, seekBar, progressLabel)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error setting up progress management", e)
                Toast.makeText(this@ItemActivity, "Error loading progress", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveProgress(projectId: String, taskId: String, subtaskId: String,seekBar: SeekBar, progressLabel: TextView) {
        val currentProgress = seekBar.progress

        lifecycleScope.launch {
            try {
                val success = subtaskService.updateSubTaskProgress(
                    projectId,
                    taskId,
                    subtaskId,
                    currentProgress
                )

                if (success) {
                    progressLabel.text = "$currentProgress%"

                    val progressInfo = findViewById<TextView>(R.id.progressiTextView)
                    progressInfo.text = "${currentProgress}%"

                    Toast.makeText(this@ItemActivity, "Progress updated successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@ItemActivity, "Failed to update progress", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving progress", e)
                throw e
            }
        }
    }

    companion object {
        private const val TAG = "ProjectDetailsActivity"
    }

    /*override fun onBackPressed() {
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
            //val intent = Intent(this, LoggedActivity::class.java)
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
    }*/

    private fun setupFileUpload() {
        buttonFile.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"  // Allow all file types
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            startActivityForResult(intent, PICK_FILE_REQUEST_CODE)
        }
    }

    // Add/Update onActivityResult method
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_FILE_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            val fileUri = data.data
            if (fileUri != null) {
                uploadFile(fileUri)
            } else {
                Toast.makeText(this, "Error selecting file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Add this method to handle file upload
    private fun uploadFile(fileUri: Uri) {
        // Show progress dialog or indicator
        val progressDialog = ProgressDialog(this).apply {
            setTitle("Uploading File")
            setMessage("Please wait...")
            setCancelable(false)
            show()
        }

        fileRepository.uploadProjectFile(
            projectId = projectId,
            taskId = if (tipo == "task" || tipo == "subtask") taskId else null,
            imageUri = fileUri,
            onSuccess = { downloadUrl ->
                progressDialog.dismiss()
                Toast.makeText(this, "File uploaded successfully", Toast.LENGTH_SHORT).show()
                // Here you could also update Firestore with the file reference if needed
            },
            onFailure = { exception ->
                progressDialog.dismiss()
                Toast.makeText(this, "Error uploading file: ${exception.message}", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Error uploading file", exception)
            }
        )
    }

    // Update your onCreate method to add
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_project)

    }
