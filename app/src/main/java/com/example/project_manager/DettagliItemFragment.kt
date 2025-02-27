package com.example.project_manager

import android.content.ContentValues.TAG
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.project_manager.models.ItemsViewModel
import com.example.project_manager.models.Role
import com.example.project_manager.repository.FileRepository
import com.example.project_manager.repository.NotificationHelper
import com.example.project_manager.services.*
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import com.google.firebase.firestore.FirebaseFirestore
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

private const val ARG_PROJECT_ID = "projectId"
private const val ARG_TASK_ID = "taskId"
private const val ARG_SUBTASK_ID = "subtaskId"

class DettagliItemFragment : Fragment() {

    private lateinit var role: Role
    private var projectId: String = ""
    private var taskId: String = ""
    private var subtaskId: String = ""
    private lateinit var rootView: View

    private val projectService = ProjectService()
    private val userService = UserService()
    private val taskService = TaskService()
    private val subtaskService = SubTaskService()
    private val fileRepository = FileRepository()

    private lateinit var progressSeekBar: Slider
    private lateinit var seekbarLayout: MaterialCardView
    private lateinit var progressLabel: TextView
    private lateinit var sollecitaButton: Button
    private lateinit var feedbackLayout: MaterialCardView
    private lateinit var valuta: MaterialButton
    private lateinit var feedbackScore: TextView
    private lateinit var feedbackComment: TextView
    private lateinit var assignedCont: MaterialCardView
    private lateinit var imageCreator: CircleImageView
    private lateinit var imageAssignedTo: CircleImageView
    private lateinit var menuButton: ImageButton
    private lateinit var toolbar: androidx.appcompat.widget.Toolbar
    private lateinit var projectDescription: TextView
    private lateinit var deadlineDate: TextView
    private lateinit var assigneeName: TextView
    private lateinit var creatorName: TextView
    private lateinit var progressText: TextView
    private lateinit var progressIndicator: com.google.android.material.progressindicator.CircularProgressIndicator
    private lateinit var itemType: String
    private lateinit var ratingText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            projectId = it.getString(ARG_PROJECT_ID, "")
            taskId = it.getString(ARG_TASK_ID, "")
            subtaskId = it.getString(ARG_SUBTASK_ID, "")
        }
        itemType = getItemType()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_item, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated CALLED - savedInstanceState: $savedInstanceState")
        Log.d(TAG, "Current projectId: $projectId, taskId: $taskId, subtaskId: $subtaskId")

        val notificationHelper = NotificationHelper(requireContext(), FirebaseFirestore.getInstance())
        initializeViews(view)

        hideAllOptionalElements()

        getArgumentsData()

        Log.d(TAG, "AFTER getArgumentsData - projectId: $projectId, taskId: $taskId, subtaskId: $subtaskId")

        itemType = getItemType(subtaskId, taskId, projectId)

        Log.d(TAG, "Determined tipo: $itemType")
        viewLifecycleOwner.lifecycleScope.launch {
            role = userService.getCurrentUserRole()!!
            loadDetails(itemType, notificationHelper)
            menu(itemType)
        }
    }

    private fun hideAllOptionalElements() {
        seekbarLayout.visibility = View.GONE
        sollecitaButton.visibility = View.GONE
        feedbackLayout.visibility = View.GONE
        valuta.visibility = View.GONE
        assignedCont.visibility = View.GONE
        menuButton.visibility = View.GONE
    }

    private fun getItemType(): String {
        return when {
            subtaskId.isNotEmpty() -> "subtask"
            taskId.isNotEmpty() -> "task"
            projectId.isNotEmpty() -> "progetto"
            else -> throw IllegalArgumentException("No valid ID provided")
        }
    }
    private fun getArgumentsData() {
        arguments?.let { args ->
            projectId = args.getString("projectId", "")
            taskId = args.getString("taskId", "")
            subtaskId = args.getString("subtaskId", "")
        }
        Log.d(TAG, "getArgumentsData - projectId: $projectId, taskId: $taskId, subtaskId: $subtaskId")
    }

    private fun initializeViews(view: View) {
        ratingText = view.findViewById(R.id.ratingText)
        progressSeekBar = view.findViewById(R.id.progressSlider)
        seekbarLayout = view.findViewById(R.id.progressCard)
        progressLabel = view.findViewById(R.id.progressPercentage)
        sollecitaButton = view.findViewById(R.id.buttonSollecita)
        feedbackLayout = view.findViewById(R.id.feedbackCard)
        valuta = view.findViewById(R.id.buttonVota)
        feedbackScore = view.findViewById(R.id.ratingNumber)
        feedbackComment = view.findViewById(R.id.ratingComment)
        assignedCont = view.findViewById(R.id.assignedToCard)
        imageCreator = view.findViewById(R.id.creatorImage)
        imageAssignedTo = view.findViewById(R.id.assigneeImage)
        menuButton = view.findViewById(R.id.menuButton)
        toolbar = view.findViewById(R.id.toolbar)
        projectDescription = view.findViewById(R.id.projectDescription)
        deadlineDate = view.findViewById(R.id.deadlineDate)
        assigneeName = view.findViewById(R.id.assigneeName)
        creatorName = view.findViewById(R.id.creatorName)
        progressText = view.findViewById(R.id.progressText)
        progressIndicator = view.findViewById(R.id.progressIndicator)
    }


    private fun getRatingText(rating: Int): String {
        return when (rating) {
            1 -> getString(R.string.pessimo)
            2 -> getString(R.string.molto_scarso)
            3 -> getString(R.string.scarso)
            4 -> getString(R.string.mediocre)
            5 -> getString(R.string.sufficiente)
            6 -> getString(R.string.discreto)
            7 -> getString(R.string.buono)
            8 -> getString(R.string.molto_buono)
            9 -> getString(R.string.ottimo)
            10 -> getString(R.string.eccellente)
            else -> getString(R.string.non_valutato)
        }
    }

    private fun getItemType(subtaskId: String, taskId: String, projectId: String): String {
        if (subtaskId.isNotEmpty()) {
            return "subtask"
        } else if (taskId.isNotEmpty()) {
            return "task"
        } else if (projectId.isNotEmpty()) {
            return "progetto"
        }
        else{
            Log.e(TAG, "Nessun ID del progetto o del task fornito.")

            throw IllegalArgumentException("Nessun ID del progetto o del task fornito.")
        }
    }

    private fun loadDetails(tipo: String, notificationHelper: NotificationHelper) {
        when (tipo) {
            "progetto" -> {
                viewLifecycleOwner.lifecycleScope.launch {
                    handleProjectDetails(notificationHelper)
                    handleFeedback()
                }

            }
            "task" -> {
                viewLifecycleOwner.lifecycleScope.launch{
                    handleTaskDetails()
                    handleFeedback()
                }

            }
            "subtask" -> {
                viewLifecycleOwner.lifecycleScope.launch {
                    handleSubtaskDetails()
                    handleFeedback()
                }

            }

            else -> Log.e(TAG, "Tipo non riconosciuto: $tipo")
        }
    }


    private suspend fun handleProjectDetails(notificationHelper: NotificationHelper) {
        if (role == Role.Leader) {
            setupLeaderView()
        } else if (role == Role.Manager) {
            setupManagerView()
        } else {
            throw error("Ruolo non valido")
        }
    }

    private suspend fun handleTaskDetails() {
        try {
            if (role == Role.Leader) {
                setupLeaderTaskView()
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

    //leader che vede progetto
    private suspend fun setupLeaderView() {
        setData(itemType, taskId, projectId, subtaskId)
        seekbarLayout.visibility = View.GONE
        sollecitaButton.visibility= View.GONE
        assignedCont.visibility = View.VISIBLE
        menuButton.visibility = View.GONE
    }


    private suspend fun setupManagerView() {
        setData(itemType, taskId, projectId, subtaskId)

        sollecitaButton.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                projectService.sollecita(projectId)
            }
        }


        assignedCont.visibility = View.VISIBLE
        menuButton.visibility = View.VISIBLE
        sollecitaButton.visibility=View.VISIBLE
        seekbarLayout.visibility = View.GONE



    }

    private suspend fun setupDeveloperSubTaskView() {
        setData(itemType, taskId, projectId, subtaskId)
        sollecitaButton.visibility=View.GONE
        seekbarLayout.visibility = View.VISIBLE
        assignedCont.visibility = View.GONE
        menuButton.visibility = View.VISIBLE


    }

    private suspend fun setupLeaderTaskView() {

        setData(itemType, taskId, projectId, subtaskId)

        sollecitaButton.visibility=View.VISIBLE
        sollecitaButton.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                taskService.sollecita(projectId,taskId)

            }
        }
        seekbarLayout.visibility = View.GONE
        assignedCont.visibility = View.VISIBLE
        menuButton.visibility = View.VISIBLE
    }

    private suspend fun setupDeveloperTaskView() {
        setData(itemType, taskId, projectId, subtaskId)
        sollecitaButton.visibility=View.GONE
        seekbarLayout.visibility = View.GONE
        assignedCont.visibility = View.VISIBLE
        menuButton.visibility = View.GONE
    }

    private suspend fun setData(
        tipo: String,
        taskId: String,
        projectId: String,
        subtaskId: String
    ) {
        var item: ItemsViewModel
        if (tipo == "progetto") {
            Log.d(TAG, "Fetching project data...")
            item = projectService.getProjectById(projectId)!!
            Log.d(TAG, "Project data fetched: ${item != null}")
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
        val creatorPathImage = userService.getUserById(item.creator)?.profile_image_url
        val assignedToPathImage = userService.getUserById(item.assignedTo)?.profile_image_url
        fileRepository.loadProfileImage(requireContext(), imageCreator, creatorPathImage!!)
        fileRepository.loadProfileImage(requireContext(), imageAssignedTo, assignedToPathImage!!)
    }

    private suspend fun setName(item: ItemsViewModel) {
        val projectNameTextView = requireView().findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        projectNameTextView.title = item.title.uppercase()
    }

    private suspend fun setDescription(item: ItemsViewModel) {
        val projectDescriptionTextView = requireView().findViewById<TextView>(R.id.projectDescription)
        projectDescriptionTextView.text = item.description
    }

    private fun setDeadline(item: ItemsViewModel) {
        val projectDeadlineTextView = requireView().findViewById<TextView>(R.id.deadlineDate)
        projectDeadlineTextView.text = item.deadline
    }

    private suspend fun setAssignedTo(item: ItemsViewModel) {
        val projectAssignedTextView = requireView().findViewById<TextView>(R.id.assigneeName)
        val name = userService.getUserNameById(item.assignedTo)
        projectAssignedTextView.text = name
    }

    private suspend fun setCreator(item: ItemsViewModel) {
        val projectCreatorTextView = requireView().findViewById<TextView>(R.id.creatorName)
        val name = userService.getUserNameById(item.creator)
        projectCreatorTextView.text = name

    }

    private fun setProgressInfo(item: ItemsViewModel) {
        val progressInfo = requireView().findViewById<TextView>(R.id.progressText)
        progressInfo.text = "${item.progress}%"
        val progressIndicator = requireView().findViewById<com.google.android.material.progressindicator.CircularProgressIndicator>(R.id.progressIndicator)
        progressIndicator.progress = item.progress
    }


    private suspend fun handleFeedback() {
        val currentItem = when (itemType) {
            "progetto" -> projectService.getProjectById(projectId)
            "task" -> taskService.getTaskById(projectId, taskId)
            "subtask" -> subtaskService.getSubTaskById(projectId, taskId, subtaskId)
            else -> null
        }

        when {
            //progetto aperto da leader
            itemType == "progetto" && role == Role.Leader -> {
                valuta.visibility = View.GONE
                feedbackLayout.visibility = View.GONE
                currentItem?.let { item ->
                    if (item.valutato) {
                    // Progetto già valutato, mostra il feedback esistente
                    valuta.visibility = View.GONE
                    feedbackLayout.visibility = View.VISIBLE
                    displayFeedback(item.rating, item.comment)
                    }
                    else {
                    // Progetto non completato
                    valuta.visibility = View.GONE
                    feedbackLayout.visibility = View.GONE
                }
                }

            }

            // progetto aperto da manager
            itemType == "progetto" && role == Role.Manager -> {
                currentItem?.let { item ->
                    when {
                        item.progress == 100 && !item.valutato -> {
                            // Progetto completato ma non ancora valutato
                            valuta.visibility = View.VISIBLE
                            feedbackLayout.visibility = View.GONE
                            setupFeedbackForm("progetto")
                        }
                        item.valutato -> {
                            // Progetto già valutato, mostra il feedback esistente
                            valuta.visibility = View.GONE
                            feedbackLayout.visibility = View.VISIBLE
                            displayFeedback(item.rating, item.comment)
                        }
                        else -> {
                            // Progetto non completato
                            valuta.visibility = View.GONE
                            feedbackLayout.visibility = View.GONE
                        }
                    }
                }
            }

            // task aperto da leader
            itemType == "task" && role == Role.Leader -> {
                currentItem?.let { item ->
                    when {
                        item.progress == 100 && !item.valutato -> {
                            // Task completato ma non ancora valutato
                            valuta.visibility = View.VISIBLE
                            feedbackLayout.visibility = View.GONE
                            setupFeedbackForm("task")
                        }
                        item.valutato -> {
                            // Task già valutato, mostra il feedback esistente
                            valuta.visibility = View.GONE
                            feedbackLayout.visibility = View.VISIBLE
                            displayFeedback(item.rating, item.comment)
                        }
                        else -> {
                            // Task non completato
                            valuta.visibility = View.GONE
                            feedbackLayout.visibility = View.GONE
                        }
                    }
                }
            }

            // Altri utenti possono solo vedere il feedback se esiste
            else -> {
                currentItem?.let { item ->
                    if (item.valutato) {
                        valuta.visibility = View.GONE
                        feedbackLayout.visibility = View.VISIBLE
                        displayFeedback(item.rating, item.comment)
                    } else {
                        valuta.visibility = View.GONE
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

            AlertDialog.Builder(requireContext())
                .setTitle("Dai un feedback")
                .setView(dialogView)
                .setPositiveButton("Salva") { _, _ ->
                    val selectedRatingId = radioGroup.checkedRadioButtonId
                    val rating = dialogView.findViewById<RadioButton>(selectedRatingId)?.text?.toString()?.toInt() ?: 0
                    val comment = commentEditText.text.toString()

                    viewLifecycleOwner.lifecycleScope.launch {
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
        val text = getRatingText(rating)
        ratingText.text = text
        feedbackScore.text = "$rating"
        feedbackComment.text = comment
    }

    private fun handleFeedbackSuccess(rating: Int, comment: String) {
        valuta.visibility = View.GONE
        feedbackLayout.visibility = View.VISIBLE
        displayFeedback(rating, comment)
    }

    private fun handleFeedbackError() {
        Toast.makeText(requireContext(), "Errore durante il salvataggio del feedback", Toast.LENGTH_SHORT).show()
    }



    private fun menu(tipo: String) {
        val menuButton: ImageButton = requireView().findViewById(R.id.menuButton)
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
                // Crea il PopupMenu con il tema personalizzato
                val wrapper = ContextThemeWrapper(requireContext(), R.style.CustomPopupMenuStyle)
                val popupMenu = PopupMenu(wrapper, view)

                // Inflating the menu
                val inflater = popupMenu.menuInflater
                inflater.inflate(R.menu.menu_task_option, popupMenu.menu)

                // Applica il colore bianco a tutte le voci del menu
                for (i in 0 until popupMenu.menu.size()) {
                    val item = popupMenu.menu.getItem(i)
                    val spanString = SpannableString(item.title)
                    spanString.setSpan(ForegroundColorSpan(Color.WHITE), 0, spanString.length, 0)
                    item.title = spanString
                }

                // Imposta il colore rosso per l'opzione elimina
                val deleteItem = popupMenu.menu.findItem(R.id.menu_delete)
                val deleteText = SpannableString(deleteItem.title)
                deleteText.setSpan(ForegroundColorSpan(Color.RED), 0, deleteText.length, 0)
                deleteItem.title = deleteText

                // Imposta listener per gli item del menu
                popupMenu.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.menu_edit -> {
                            updateProject()
                            true
                        }

                        R.id.menu_delete -> {
                            // Crea l'AlertDialog con tema personalizzato
                            MaterialAlertDialogBuilder(requireContext(), R.style.CustomAlertDialogStyle)
                                .setTitle("Conferma eliminazione")
                                .setMessage("Sei sicuro di voler eliminare questo $tipo?")
                                .setPositiveButton("Elimina") { _, _ ->
                                    viewLifecycleOwner.lifecycleScope.launch {
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
                                                    requireContext(),
                                                    "$tipo eliminato con successo",
                                                    Toast.LENGTH_SHORT
                                                ).show()

                                                navigateBack()
                                            } else {
                                                Toast.makeText(
                                                    requireContext(),
                                                    "Errore durante l'eliminazione del $tipo",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        } catch (e: Exception) {
                                            Log.e(TAG, "Error during deletion", e)
                                            Toast.makeText(
                                                requireContext(),
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

    private fun updateProject() {
        Log.d(TAG, "STO CHIAMANDO UPDATE PROJECT")
        val intent = Intent(requireContext(), UpdateProjectActivity::class.java).apply {
            putExtra("projectId", projectId)
            putExtra("taskId", taskId)
            putExtra("subtaskId", subtaskId)
        }

        startActivity(intent)
    }

    private fun setupProgressManagement(
        projectId: String,
        taskId: String,
        subtaskId: String,
        slider: Slider,
        progressLabel: TextView
    ) {
        if (role != Role.Developer) return

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                var currentProgress = subtaskService.getSubTaskProgress(projectId, taskId, subtaskId)
                slider.value= currentProgress.toFloat()
                progressLabel.text = "$currentProgress%"
                if (currentProgress == 100) {
                    slider.isEnabled = false
                    // Opzionalmente, puoi cambiare anche l'aspetto visivo per indicare che è disabilitato
                    slider.alpha = 0.6f
                } else {
                    slider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
                        override fun onStartTrackingTouch(slider: Slider) {
                            currentProgress = slider.value.toInt()
                            progressLabel.text = "$currentProgress%"
                        }

                        override fun onStopTrackingTouch(slider: Slider) {
                            saveProgress(projectId, taskId, subtaskId, slider, progressLabel)
                        }
                    })
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error setting up progress management", e)
                Toast.makeText(requireContext(), "Error loading progress", Toast.LENGTH_SHORT)
                    .show()
            }
        }

    }

    private fun saveProgress(
        projectId: String,
        taskId: String,
        subtaskId: String,
        seekBar: Slider,
        progressLabel: TextView
    ) {
        val currentProgress = seekBar.value.toInt()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val success = subtaskService.updateSubTaskProgress(
                    projectId,
                    taskId,
                    subtaskId,
                    currentProgress
                )

                if (success) {
                    progressIndicator.progress = currentProgress
                    progressLabel.text = "$currentProgress%"
                    progressText.text = "${currentProgress}%"

                    Toast.makeText(
                        requireContext(),
                        "Progress updated successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        requireContext(),
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

    private fun navigateBack() {
        val subItem = when (role) {
            Role.Leader -> projectId.isNotEmpty() && taskId.isNotEmpty() && subtaskId.isEmpty() // Leader con project e task
            Role.Developer -> projectId.isNotEmpty() && taskId.isNotEmpty() && subtaskId.isNotEmpty()  // Developer con project, task e subtask
            else -> false
        }

        if (subItem) {
            val intent = Intent(requireContext(), HomeItemActivity::class.java).apply {
                when (role) {
                    Role.Leader -> {
                        // Leader torna indietro di un livello
                        putExtra("projectId", projectId)
                        putExtra("taskId", "")
                        putExtra("subtaskId", "")
                        putExtra("startingTab", 1)
                    }
                    Role.Developer -> {
                        // Developer torna indietro di un livello
                        putExtra("projectId", projectId)
                        putExtra("taskId", taskId)
                        putExtra("subtaskId", "")
                        putExtra("startingTab", 1)
                    }
                    else -> {
                    }
                }
            }
            startActivity(intent)
        } else {
            // vai alla HomeActivity
            val intent = Intent(requireContext(), HomeActivity::class.java)
            startActivity(intent)
        }
        requireActivity().finish()
    }



    companion object {
        private const val TAG = "DettagliItemFragment"

        @JvmStatic
        fun newInstance(
            projectId: String,
            taskId: String = "",
            subtaskId: String = ""
        ) = DettagliItemFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_PROJECT_ID, projectId)
                putString(ARG_TASK_ID, taskId)
                putString(ARG_SUBTASK_ID, subtaskId)
            }
        }
    }
}

