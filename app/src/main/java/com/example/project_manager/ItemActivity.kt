package com.example.project_manager

import android.app.DatePickerDialog
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.CheckBox
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
import com.example.project_manager.repository.FileRepository
import com.example.project_manager.services.FileService
import com.example.project_manager.services.ProjectService
import com.example.project_manager.services.SubTaskService
import com.example.project_manager.services.TaskService
import com.example.project_manager.services.UserService
import com.example.project_manager.repository.NotificationHelper
import com.google.firebase.firestore.FirebaseFirestore
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
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
    val fileRepository= FileRepository()
    private val PICK_FILE_REQUEST_CODE = 2002
    private val fileService = FileService()

    private lateinit var progressSeekBar: SeekBar
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
    private lateinit var feedbackScore: TextView
    private lateinit var feedbackComment: TextView
    private lateinit var assignedCont: LinearLayout
    private lateinit var tipo: String
    private lateinit var buttonFile: ImageButton
    private lateinit var fileLayout: LinearLayout
    private lateinit var filesRecyclerView: RecyclerView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var imageCreator: CircleImageView
    private lateinit var imageAssignedTo: CircleImageView

    private var startDate: Long = -1L
    private var endDate: Long = -1L

    private lateinit var filteredDataByStatus: ArrayList<ItemsViewModel>
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())



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
            loadUsersFilter(role)
            loadDetails(tipo, notificationHelper)
            menu(tipo)
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
        imageCreator = findViewById(R.id.profileImageCreator)
        imageAssignedTo= findViewById(R.id.profileImageAssignedTo)
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

        sollecitaButton.setOnClickListener {
            lifecycleScope.launch {
                projectService.sollecita(projectId)
            }
        }


        sollecitaCont.visibility = View.VISIBLE
        progLeaderTask.visibility = View.GONE
        seekbarLayout.visibility = View.GONE
        fileLayout.visibility= View.GONE



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
            lifecycleScope.launch {
                taskService.sollecita(projectId,taskId)

            }
        }
        seekbarLayout.visibility = View.GONE
        progLeaderTask.visibility = View.GONE
        fileLayout.visibility= View.VISIBLE
        buttonFile.visibility= View.GONE
        filesRecyclerView.visibility= View.VISIBLE

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
        setProfileImage(item)
    }

    private suspend fun setProfileImage(item: ItemsViewModel) {
        val creatorPathImage= userService.getUserById(item.creator)?.profile_image_url
        val assignedToPathImage= userService.getUserById(item.assignedTo)?.profile_image_url
        fileRepository.loadProfileImage(this, imageCreator, creatorPathImage!!)
        fileRepository.loadProfileImage(this, imageAssignedTo, assignedToPathImage!!)
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



    private fun menu(tipo: String) {
        val menuButton: ImageButton = findViewById(R.id.menuButton)
        //il leader non può modificare un progetto
        if (role == Role.Leader && tipo == "progetto") {
            menuButton.visibility = View.GONE
            return
        }
        //il developer non puo modificare un task
        else if (role == Role.Developer && tipo == "task") {
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
                            true
                        }

                        R.id.menu_delete -> {
                            // Mostra l'AlertDialog qui
                            AlertDialog.Builder(this)
                                .setTitle("Conferma eliminazione")
                                .setMessage("Sei sicuro di voler eliminare questo $tipo?")
                                .setPositiveButton("Elimina") { _, _ ->
                                    lifecycleScope.launch {
                                        try {
                                            val success = when (tipo) {
                                                "progetto" -> projectService.deleteProject(projectId)
                                                "task" -> taskService.deleteTask(projectId, taskId)
                                                "subtask" -> subtaskService.deleteSubTask(projectId, taskId, subtaskId)
                                                else -> {
                                                    Log.e(TAG, "Tipo non valido: $tipo")
                                                    false
                                                }
                                            }

                                            if (success) {
                                                Toast.makeText(
                                                    this@ItemActivity,
                                                    "$tipo eliminato con successo",
                                                    Toast.LENGTH_SHORT
                                                ).show()

                                                // Usa navigateToLoggedActivity per gestire la navigazione
                                                navigateToLoggedActivity()
                                            } else {
                                                Toast.makeText(
                                                    this@ItemActivity,
                                                    "Errore durante l'eliminazione del $tipo",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        } catch (e: Exception) {
                                            Log.e(TAG, "Error during deletion", e)
                                            Toast.makeText(
                                                this@ItemActivity,
                                                "Errore durante l'eliminazione del $tipo: ${e.message}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                                .setNegativeButton("Annulla", null)
                                .show()

                            true
                        }

                        else -> false
                    }
                }

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

    private fun deleteItem(tipo: String) {
        lifecycleScope.launch {
            try {
                val success = when (tipo) {
                    "progetto" -> projectService.deleteProject(projectId)
                    "task" -> taskService.deleteTask(projectId, taskId)
                    "subtask" -> subtaskService.deleteSubTask(projectId, taskId, subtaskId)
                    else -> {
                        Log.e(TAG, "Tipo non valido: $tipo")
                        false
                    }
                }

                if (success) {
                    Toast.makeText(
                        this@ItemActivity,
                        "$tipo eliminato con successo",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                } else {
                    Toast.makeText(
                        this@ItemActivity,
                        "Errore durante l'eliminazione del $tipo",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during deletion", e)
                Toast.makeText(
                    this@ItemActivity,
                    "Errore durante l'eliminazione del $tipo: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


    //funzione che carica i task o sottotask nella recycler view
    private fun loadTask() {
        var data = ArrayList<ItemsViewModel>()

        // Avvia una Coroutine nel contesto del Main Thread
        CoroutineScope(Dispatchers.Main).launch {
            try {
                if (role == Role.Leader) {
                    data = taskService.getAllTaskByProjectId(projectId)
                } else if (role == Role.Developer) {
                    data = subtaskService.getAllSubTaskByTaskId(projectId, taskId)
                }
                updateUI(data)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading tasks: ", e)
                updateUI(data) // Anche in caso di errore, aggiorna la UI
            }
            setupFilterHandlers(data)
        }

    }

    private fun updateUI(
        data: ArrayList<ItemsViewModel>,
    ) {
        val recyclerviewTask = findViewById<RecyclerView>(R.id.recyclerviewTask)
        val noTasksTextView = findViewById<TextView>(R.id.noTasksTextView)

        val hasData = data.isNotEmpty()
        recyclerviewTask.visibility = if (hasData) View.VISIBLE else View.GONE
        noTasksTextView.visibility = if (hasData) View.GONE else View.VISIBLE

        if (hasData) {
            // Configure RecyclerView
            recyclerviewTask.apply {
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
                val currentProgress = subtaskService.getSubTaskProgress(projectId, taskId, subtaskId)
                seekBar.progress = currentProgress
                progressLabel.text = "$currentProgress%"

                // Add SeekBar change listener for real-time updates
                seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                        // Update the progress label in real-time
                        progressLabel.text = "$progress%"
                        // Update also the progress info TextView
                        findViewById<TextView>(R.id.progressiTextView).text = "$progress%"
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar?) {
                        // Not needed, but required by interface
                    }

                    override fun onStopTrackingTouch(seekBar: SeekBar?) {
                        // Not needed, but required by interface
                    }
                })

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

    private fun setupFilterHandlers(data: ArrayList<ItemsViewModel>) {

        val buttonApplyFilters = findViewById<Button>(R.id.apply_filters)
        buttonApplyFilters.setOnClickListener {
            applyFilters(role,data)
            drawerLayout.closeDrawer(GravityCompat.END)
        }

        deadlineFilterHandler()
    }

    private fun deadlineFilterHandler() {
        val startDateText: TextView = findViewById(R.id.text_start_date)
        val endDateText: TextView = findViewById(R.id.text_end_date)
        val buttonSelectStartDate: Button = findViewById(R.id.button_select_start_date)
        val buttonSelectEndDate: Button = findViewById(R.id.button_select_end_date)
        val buttonClearStartDate: Button = findViewById(R.id.button_clear_start_date)
        val buttonClearEndDate: Button = findViewById(R.id.button_clear_end_date)

        buttonClearStartDate.setOnClickListener {
            startDateText.text = "Nessuna data selezionata"
            startDate = -1L
        }
        buttonClearEndDate.setOnClickListener {
            endDateText.text = "Nessuna data selezionata"
            endDate = -1L
        }

        buttonSelectStartDate.setOnClickListener { showDatePickerDialog(true) }
        buttonSelectEndDate.setOnClickListener { showDatePickerDialog(false) }
    }

    private fun showDatePickerDialog(isStartDate: Boolean) {
        val calendar = Calendar.getInstance()
        val datePicker = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedDate = Calendar.getInstance()
                selectedDate.set(year, month, dayOfMonth)
                val formattedDate = dateFormat.format(selectedDate.time)

                if (isStartDate) {
                    startDate = selectedDate.timeInMillis
                    findViewById<TextView>(R.id.text_start_date).text = formattedDate
                } else {
                    endDate = selectedDate.timeInMillis
                    findViewById<TextView>(R.id.text_end_date).text = formattedDate
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }

    private suspend fun loadUsersFilter(role: Role) {
        val leaderContainer: LinearLayout = findViewById(R.id.leader_container)
        val leaderFilterTitle: TextView = findViewById(R.id.leader_filter_title) // Aggiungi questo ID nel layout XML

        when (role) {
            Role.Leader -> {
                // Per il Leader, carica i Developer
                val developerNames = userService.getUsersByRole(Role.Developer)

                leaderContainer.removeAllViews()
                leaderFilterTitle.text = "Developer" // Cambia il titolo

                for (user in developerNames) {
                    val checkBox = CheckBox(this)
                    checkBox.text = "${user.name} ${user.surname}"
                    checkBox.tag = user.uid
                    checkBox.layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    leaderContainer.addView(checkBox)
                }
            }
            Role.Developer -> {
                // Per il Developer, nascondi l'intera sezione dei filtri per leader
                val leaderFilterSection: LinearLayout = findViewById(R.id.leader_container)
                leaderFilterSection.visibility = View.GONE
            }
            else -> {
                // Per altri ruoli (es. Manager), lascia invariato
                val developerNames = userService.getUsersByRole(Role.Leader)

                leaderContainer.removeAllViews()
                leaderFilterTitle.text = "Leader"

                for (user in developerNames) {
                    val checkBox = CheckBox(this)
                    checkBox.text = "${user.name} ${user.surname}"
                    checkBox.tag = user.uid
                    checkBox.layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    leaderContainer.addView(checkBox)
                }
            }
        }
    }

    private fun applyFilters(role: Role,data: ArrayList<ItemsViewModel>) {
        val completedCheckBox: CheckBox = findViewById(R.id.filter_completati)
        val inProgressCheckBox: CheckBox = findViewById(R.id.filter_in_corso)
        val highPriorityCheckBox: CheckBox = findViewById(R.id.filter_alta)
        val mediumPriorityCheckBox: CheckBox = findViewById(R.id.filter_media)
        val lowPriorityCheckBox: CheckBox = findViewById(R.id.filter_bassa)

        lifecycleScope.launch {
            filteredDataByStatus = data

            // Filtra per stato
            if (completedCheckBox.isChecked && !inProgressCheckBox.isChecked) {
                filteredDataByStatus = filterDataByProgress(role, data, "completed")
            } else if (inProgressCheckBox.isChecked && !completedCheckBox.isChecked) {
                filteredDataByStatus = filterDataByProgress(role, data, "incompleted")
            }

            // Filtra per scadenza
            var filteredDataByDeadline = filteredDataByStatus
            if (startDate != -1L || endDate != -1L) {
                filteredDataByDeadline = filterByDeadline(filteredDataByStatus, startDate, endDate, dateFormat)
            }

            // Filtra per leader
            val leaderContainer: LinearLayout = findViewById(R.id.leader_container)
            val selectedLeaders = mutableListOf<String>()
            for (i in 0 until leaderContainer.childCount) {
                val checkBox = leaderContainer.getChildAt(i) as? CheckBox
                if (checkBox?.isChecked == true) {
                    selectedLeaders.add(checkBox.tag.toString())
                }
            }

            val filteredDataByLeader = if (selectedLeaders.isNotEmpty()) {
                filteredDataByDeadline.filter { item ->
                    selectedLeaders.contains(item.assignedTo)
                }.toCollection(ArrayList())
            } else {
                filteredDataByDeadline
            }

            // Filtra per priorità
            val filteredDataByPriority = when {
                !highPriorityCheckBox.isChecked &&
                        !mediumPriorityCheckBox.isChecked &&
                        !lowPriorityCheckBox.isChecked -> filteredDataByLeader
                else -> {
                    filteredDataByLeader.filter { item ->
                        when {
                            highPriorityCheckBox.isChecked && item.priority == "High" -> true
                            mediumPriorityCheckBox.isChecked && item.priority == "Medium" -> true
                            lowPriorityCheckBox.isChecked && item.priority == "Low" -> true
                            else -> false
                        }
                    }.toCollection(ArrayList())
                }
            }

            // Aggiorna la RecyclerView con i dati filtrati
            updateUI(filteredDataByPriority)
        }
    }

    private suspend fun filterDataByProgress(
        role: Role,
        data: ArrayList<ItemsViewModel>,
        progressType: String
    ): ArrayList<ItemsViewModel> {
        return when (role) {
            Role.Developer-> subtaskService.filterSubTasksByProgress(data, progressType)
            Role.Leader -> taskService.filterTasksByProgress(data, progressType)
            else -> data
        }
    }

    private fun filterByDeadline(
        items: ArrayList<ItemsViewModel>,
        startDate: Long,
        endDate: Long,
        dateFormat: SimpleDateFormat
    ): ArrayList<ItemsViewModel> {
        return items.filter { item ->
            val taskDate: Long = try {
                val deadlineDate = dateFormat.parse(item.deadline)
                deadlineDate?.time ?: -1L
            } catch (e: Exception) {
                -1L
            }

            if (taskDate == -1L) return@filter false

            when {
                startDate != -1L && endDate != -1L -> taskDate in startDate..endDate
                startDate != -1L -> taskDate >= startDate
                endDate != -1L -> taskDate <= endDate
                else -> true
            }
        } as ArrayList<ItemsViewModel>
    }

    override fun onBackPressed() {
        when {
            // Case 1: Viewing a subtask - go back to task view
            !subtaskId.isNullOrEmpty() -> {
                Log.d(TAG, "Navigating back from subtask to task view")
                navigateToItem(projectId, taskId, null)
            }

            // Case 2: Viewing a task
            !taskId.isNullOrEmpty() -> {
                when (role) {
                    Role.Leader -> {
                        // Leader viewing task - go back to project view
                        Log.d(TAG, "Leader navigating back from task to project view")
                        navigateToItem(projectId, null, null)
                    }
                    Role.Developer -> {
                        // Developer viewing task - go back to task list
                        Log.d(TAG, "Developer navigating back from task to task list")
                        navigateToLoggedActivity()
                    }
                    else -> {
                        Log.w(TAG, "Unexpected role $role for task view")
                        super.onBackPressed()
                    }
                }
            }

            // Case 3: Viewing a project
            !projectId.isNullOrEmpty() -> {
                when (role) {
                    Role.Manager, Role.Leader -> {
                        // Go back to project list
                        Log.d(TAG, "Navigating back to project list")
                        navigateToLoggedActivity()
                    }
                    else -> {
                        Log.w(TAG, "Unexpected role $role for project view")
                        super.onBackPressed()
                    }
                }
            }

            // Default case
            else -> {
                Log.d(TAG, "No specific navigation path, using default back behavior")
                super.onBackPressed()
            }
        }
    }

    private fun navigateToItem(projectId: String?, taskId: String?, subtaskId: String?) {
        Intent(this, ItemActivity::class.java).apply {
            projectId?.let { putExtra("projectId", it) }
            taskId?.let { putExtra("taskId", it) }
            subtaskId?.let { putExtra("subtaskId", it) }
            putExtra("role", role.toString())
            startActivity(this)
        }
        finish()
    }

    private fun navigateToLoggedActivity() {
        startActivity(Intent(this, LoggedActivity::class.java))
        finish()
    }
}
