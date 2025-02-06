package com.example.project_manager

import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.project_manager.models.ItemsViewModel
import com.example.project_manager.models.Role
import com.example.project_manager.services.FileService
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ItemActivity : AppCompatActivity() {


    private lateinit var role: Role
    private lateinit var projectId: String
    private lateinit var taskId: String
    private lateinit var subtaskId: String

    val projectService = ProjectService()
    val userService = UserService()
    val taskService = TaskService()
    val subtaskService = SubTaskService()
    private val PICK_FILE_REQUEST_CODE = 2002
    private val fileService = FileService()

    private lateinit var projectNameTextView: TextView
    private lateinit var projectDescriptionTextView: TextView
    private lateinit var projectDeadlineTextView: TextView
    private lateinit var projectCreatorTextView: TextView
    private lateinit var projectAssignedTextView: TextView
    private lateinit var progressSeekBar: SeekBar
    private lateinit var progressInfo: TextView
    private lateinit var progLeaderTask: LinearLayout
    private lateinit var tipoElenco: TextView
    private lateinit var seekbarLayout: LinearLayout
    private lateinit var seekbutton: Button
    private lateinit var progressLabel: TextView
    private lateinit var sollecitaCont: LinearLayout
    private lateinit var sollecitaButton: Button
    private lateinit var feedbackLayout: LinearLayout
    private lateinit var feedback: LinearLayout
    private lateinit var valuta: Button
    private var isFeedbackGiven: Boolean = false
    private lateinit var feedbackScore: TextView
    private lateinit var feedbackComment: TextView
    private lateinit var assignedCont: LinearLayout
    private lateinit var tipo: String
    private lateinit var buttonFile: ImageButton
    private lateinit var fileLayout: LinearLayout
    private lateinit var filesRecyclerView: RecyclerView
    private lateinit var drawerLayout: DrawerLayout


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_project)

        val notificationHelper = NotificationHelper(this, FirebaseFirestore.getInstance())
        inizialiseView()
        getIntentData()

        tipo = getItemType(subtaskId, taskId, projectId)

        drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout_item)
        val iconButton = findViewById<ImageView>(R.id.filterButton)

        iconButton.setOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                drawerLayout.closeDrawer(GravityCompat.END)
            } else {
                drawerLayout.openDrawer(GravityCompat.END)
            }
        }

        lifecycleScope.launch {
            role = userService.getCurrentUserRole()!!
            loadDetails(tipo, notificationHelper)
            menu()
        }
    }



    private fun getIntentData() {
        projectId = intent.getStringExtra("projectId") ?: ""
        taskId = intent.getStringExtra("taskId") ?: ""
        subtaskId = intent.getStringExtra("subtaskId") ?: ""
    }

    private fun inizialiseView() {
        assignedCont = findViewById(R.id.assignedCont)
        sollecitaCont = findViewById(R.id.sollecitaCont)
        sollecitaButton = findViewById(R.id.sollecitaButton)
        feedbackLayout = findViewById(R.id.feedbackLayout)
        feedback = findViewById(R.id.feedback)//layout del bottone feedback
        valuta = findViewById(R.id.feedbackButton)
        feedbackScore = findViewById(R.id.feedbackScore)
        feedbackComment = findViewById(R.id.feedbackComment)
        progLeaderTask = findViewById<LinearLayout>(R.id.progLeaderTask)
        tipoElenco = findViewById(R.id.typeElenco)
        seekbarLayout = findViewById(R.id.seekbarLayout)
        progressSeekBar = findViewById(R.id.seekBar)
        seekbutton = findViewById(R.id.saveButton)
        progressLabel = findViewById(R.id.progressLabel)
        buttonFile = findViewById(R.id.aggiungiFileButton)
        fileLayout= findViewById(R.id.file)
        filesRecyclerView = findViewById(R.id.filesRecyclerView)
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
            "progetto" -> {
                lifecycleScope.launch {
                    handleProjectDetails(notificationHelper)
                    handleFeedback()
                }

            }
            "task" -> {
                lifecycleScope.launch{
                    handleTaskDetails(notificationHelper)
                    handleFeedback()
                }

            }
            "subtask" -> {
                lifecycleScope.launch {
                    handleSubtaskDetails()
                    handleFeedback()
                }

            }
            else -> Log.e(TAG, "Tipo non riconosciuto: $tipo")
        }
    }


    private suspend fun handleProjectDetails(notificationHelper: NotificationHelper) {
        if (role == Role.Leader) {
            setupLeaderView(notificationHelper)
        } else if (role == Role.Manager) {
            setupManagerView(notificationHelper)
        } else {
            throw error("Ruolo non valido")
        }
    }

    private suspend fun handleTaskDetails(notificationHelper: NotificationHelper) {
        try {
            if (role == Role.Leader) {
                setupLeaderTaskView(notificationHelper)
            } else if (role == Role.Developer) {
                setupDeveloperTaskView()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Errore durante il caricamento dei dettagli del task", e)
        }
    }

    private suspend fun handleSubtaskDetails() {
        try {
            val subTask = subtaskService.getSubTaskById(projectId, taskId, subtaskId)
            setupDeveloperSubTaskView()
            setupProgressManagement(projectId, taskId, subtaskId, progressSeekBar, progressLabel)
        } catch (e: Exception) {
            Log.e(TAG, "Errore durante il caricamento dei dettagli del sottotask", e)
        }
    }

    private suspend fun setupLeaderView(notificationHelper: NotificationHelper) {
        setData(tipo, taskId, projectId, subtaskId)
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
        fileLayout.visibility= View.GONE


        loadTask()
    }

    private suspend fun setupManagerView(notificationHelper: NotificationHelper) {
        setData(tipo, taskId, projectId, subtaskId)

        sollecitaCont.visibility = View.VISIBLE
        progLeaderTask.visibility = View.GONE
        seekbarLayout.visibility = View.GONE
        fileLayout.visibility= View.GONE

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

        setData(tipo, taskId, projectId, subtaskId)
    }


    private suspend fun setupDeveloperSubTaskView() {
        sollecitaCont.visibility = View.GONE
        progLeaderTask.visibility = View.GONE
        seekbarLayout.visibility = View.VISIBLE
        assignedCont.visibility = View.GONE
        fileLayout.visibility= View.GONE

        setData(tipo, taskId, projectId, subtaskId)

    }

    private suspend fun setupLeaderTaskView(notificationHelper: NotificationHelper) {
        setData(tipo, taskId, projectId, subtaskId)
        sollecitaCont.visibility = View.VISIBLE
        sollecitaButton.setOnClickListener {
            //notificationHelper.handleNotification(role, name, "sollecito")
        }
        seekbarLayout.visibility = View.GONE
        progLeaderTask.visibility = View.GONE
        fileLayout.visibility= View.VISIBLE
        setupFileUpload()

        val filesRecyclerView = findViewById<RecyclerView>(R.id.filesRecyclerView)
        filesRecyclerView.layoutManager = LinearLayoutManager(this)
        loadFiles() // Load existing files

    }

    private suspend fun setupDeveloperTaskView() {
        setData(tipo, taskId, projectId, subtaskId)
        sollecitaCont.visibility = View.GONE
        progLeaderTask.visibility = View.VISIBLE
        seekbarLayout.visibility = View.GONE
        fileLayout.visibility= View.VISIBLE

        setupFileUpload()
        val filesRecyclerView = findViewById<RecyclerView>(R.id.filesRecyclerView)
        filesRecyclerView.layoutManager = LinearLayoutManager(this)
        loadFiles()

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

    private suspend fun setData(
        tipo: String,
        taskId: String,
        projectId: String,
        subtaskId: String
    ) {
        var item: ItemsViewModel
        if (tipo == "progetto") {
            Log.d(TAG, "progetto")
            item = projectService.getProjectById(projectId)!!
        } else if (tipo == "task")
            item = taskService.getTaskById(projectId, taskId)!!
        else if (tipo == "subtask")
            item = subtaskService.getSubTaskById(projectId, taskId, subtaskId)!!
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


    private suspend fun setName(item: ItemsViewModel) {
        val projectNameTextView = findViewById<TextView>(R.id.projectNameTextView)
        projectNameTextView.text = item.title.uppercase()
    }

    private suspend fun setDescription(item: ItemsViewModel) {
        val projectDescriptionTextView = findViewById<TextView>(R.id.descrizioneProgetto)
        projectDescriptionTextView.text = item.description
    }

    private fun setDeadline(item: ItemsViewModel) {
        val projectDeadlineTextView = findViewById<TextView>(R.id.projectDeadlineTextView)
        projectDeadlineTextView.text = item.deadline
    }

    private suspend fun setAssignedTo(item: ItemsViewModel) {
        val projectAssignedTextView = findViewById<TextView>(R.id.projectAssignedTextView)
        val name = userService.getUserNameById(item.assignedTo)
        projectAssignedTextView.text = name
    }

    private suspend fun setCreator(item: ItemsViewModel) {
        val projectCreatorTextView = findViewById<TextView>(R.id.projectCreatorTextView)
        val name = userService.getUserNameById(item.creator)
        projectCreatorTextView.text = name

    }

    private fun setProgressInfo(item: ItemsViewModel) {
        val progressInfo = findViewById<TextView>(R.id.progressiTextView)
        progressInfo.text = "${item.progress}%"
    }


    private suspend fun handleFeedback() {
        val currentItem = when (tipo) {
            "progetto" -> projectService.getProjectById(projectId)
            "task" -> taskService.getTaskById(projectId, taskId)
            "subtask" -> subtaskService.getSubTaskById(projectId, taskId, subtaskId)
            else -> null
        }

        when {
            // Project opened by leader
            tipo == "progetto" && role == Role.Leader -> {
                feedback.visibility = View.GONE
                feedbackLayout.visibility = View.GONE
            }

            // Project opened by manager
            tipo == "progetto" && role == Role.Manager -> {
                currentItem?.let { item ->
                    when {
                        item.progress == 100 && !item.valutato -> {
                            // Progetto completato ma non ancora valutato
                            feedback.visibility = View.VISIBLE
                            feedbackLayout.visibility = View.GONE
                            setupFeedbackForm("progetto")
                        }
                        item.valutato -> {
                            // Progetto già valutato, mostra il feedback esistente
                            feedback.visibility = View.GONE
                            feedbackLayout.visibility = View.VISIBLE
                            displayFeedback(item.rating, item.comment)
                        }
                        else -> {
                            // Progetto non completato
                            feedback.visibility = View.GONE
                            feedbackLayout.visibility = View.GONE
                        }
                    }
                }
            }

            // Task opened by leader
            tipo == "task" && role == Role.Leader -> {
                currentItem?.let { item ->
                    when {
                        item.progress == 100 && !item.valutato -> {
                            // Task completato ma non ancora valutato
                            feedback.visibility = View.VISIBLE
                            feedbackLayout.visibility = View.GONE
                            setupFeedbackForm("task")
                        }
                        item.valutato -> {
                            // Task già valutato, mostra il feedback esistente
                            feedback.visibility = View.GONE
                            feedbackLayout.visibility = View.VISIBLE
                            displayFeedback(item.rating, item.comment)
                        }
                        else -> {
                            // Task non completato
                            feedback.visibility = View.GONE
                            feedbackLayout.visibility = View.GONE
                        }
                    }
                }
            }

            // Altri utenti possono solo vedere il feedback se esiste
            else -> {
                currentItem?.let { item ->
                    if (item.valutato) {
                        feedback.visibility = View.GONE
                        feedbackLayout.visibility = View.VISIBLE
                        displayFeedback(item.rating, item.comment)
                    } else {
                        feedback.visibility = View.GONE
                        feedbackLayout.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun setupFeedbackForm(type: String) {
        valuta.setOnClickListener {
            val dialogView = layoutInflater.inflate(R.layout.feedback_form, null)
            val radioGroup = dialogView.findViewById<RadioGroup>(R.id.ratingRadioGroup)
            val commentEditText = dialogView.findViewById<EditText>(R.id.commentEditText)

            AlertDialog.Builder(this)
                .setTitle("Dai un feedback")
                .setView(dialogView)
                .setPositiveButton("Salva") { _, _ ->
                    val selectedRatingId = radioGroup.checkedRadioButtonId
                    val rating = dialogView.findViewById<RadioButton>(selectedRatingId)?.text?.toString()?.toInt() ?: 0
                    val comment = commentEditText.text.toString()

                    lifecycleScope.launch {
                        saveFeedback(type, rating, comment)
                    }
                }
                .setNegativeButton("Annulla", null)
                .show()
        }
    }

    private suspend fun saveFeedback(type: String, rating: Int, comment: String) {
        val success = when (type) {
            "progetto" -> projectService.saveFeedback(projectId, rating, comment)
            "task" -> taskService.saveFeedback(projectId, taskId, rating, comment)
            else -> false
        }

        if (success) {
            handleFeedbackSuccess(rating, comment)
        } else {
            handleFeedbackError()
        }
    }

    private fun displayFeedback(rating: Int, comment: String) {
        feedbackScore.text = rating.toString()
        feedbackComment.text = comment
    }

    private fun handleFeedbackSuccess(rating: Int, comment: String) {
        valuta.visibility = View.GONE
        feedbackLayout.visibility = View.VISIBLE
        displayFeedback(rating, comment)
    }

    private fun handleFeedbackError() {
        Toast.makeText(this, "Errore durante il salvataggio del feedback", Toast.LENGTH_SHORT).show()
    }



    private fun menu() {
        val menuButton: ImageButton = findViewById(R.id.menuButton)
        //il leader non può modificare un progetto
        if (role == Role.Leader && tipo == "Progetto") {
            menuButton.visibility = View.GONE
            return
        }
        //il developer non puo modificare un task
        else if (role == Role.Developer && tipo == "subtask") {
            menuButton.visibility = View.GONE
            return
        } else {
            menuButton.visibility = View.VISIBLE
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
                    data = taskService.getAllTaskByProjectId(projectId)
                } else if (role == Role.Developer) {
                    data = subtaskService.getAllSubTaskByTaskId(projectId, taskId)
                }
                updateUI(data, recyclerviewTask, noTasksTextView)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading tasks: ", e)
                updateUI(
                    data,
                    recyclerviewTask,
                    noTasksTextView
                ) // Anche in caso di errore, aggiorna la UI
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

    private fun setupProgressManagement(
        projectId: String,
        taskId: String,
        subtaskId: String,
        seekBar: SeekBar,
        progressLabel: TextView
    ) {
        if (role != Role.Developer) return

        lifecycleScope.launch {
            try {
                val currentProgress =
                    subtaskService.getSubTaskProgress(projectId, taskId, subtaskId)
                seekBar.progress = currentProgress
                progressLabel.text = "$currentProgress%"

                val saveButton = findViewById<Button>(R.id.saveButton)
                saveButton.setOnClickListener {
                    saveProgress(projectId, taskId, subtaskId, seekBar, progressLabel)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error setting up progress management", e)
                Toast.makeText(this@ItemActivity, "Error loading progress", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun saveProgress(
        projectId: String,
        taskId: String,
        subtaskId: String,
        seekBar: SeekBar,
        progressLabel: TextView
    ) {
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

                    Toast.makeText(
                        this@ItemActivity,
                        "Progress updated successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this@ItemActivity,
                        "Failed to update progress",
                        Toast.LENGTH_SHORT
                    ).show()
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

    private fun loadFiles() {
        lifecycleScope.launch {
            try {
                val files = fileService.getTaskFiles(projectId, taskId)
                // Update your RecyclerView adapter with the files
                filesRecyclerView.adapter = FilesAdapter(files)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading files", e)
                Toast.makeText(this@ItemActivity, "Error loading files", Toast.LENGTH_SHORT).show()
            }
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
        val progressDialog = ProgressDialog(this).apply {
            setTitle("Uploading File")
            setMessage("Please wait...")
            setCancelable(false)
            show()
        }

        val fileName = generateFileName(fileUri) // New helper function to generate unique file names

        fileService.uploadFile(
            path = "projects/${projectId}/tasks/${taskId}/files",
            fileUri = fileUri,
            fileName = fileName,
            onSuccess = { downloadUrl ->
                progressDialog.dismiss()
                Toast.makeText(this, "File uploaded successfully", Toast.LENGTH_SHORT).show()
                // Immediately reload the files list
                lifecycleScope.launch {
                    loadFiles()
                }
            },
            onFailure = { exception ->
                progressDialog.dismiss()
                Toast.makeText(this, "Error uploading file: ${exception.message}", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Error uploading file", exception)
            }
        )
    }

    // Helper function to generate unique file names
    private fun generateFileName(fileUri: Uri): String {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val originalFileName = fileUri.lastPathSegment ?: "file"
        return "${timeStamp}_${originalFileName}"
    }
}
