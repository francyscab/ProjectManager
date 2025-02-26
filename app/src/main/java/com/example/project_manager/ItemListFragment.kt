package com.example.project_manager

import android.app.DatePickerDialog
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SearchView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.project_manager.models.FileModel
import com.example.project_manager.models.ItemsViewModel
import com.example.project_manager.models.Role
import com.example.project_manager.services.ChatService
import com.example.project_manager.services.FileService
import com.example.project_manager.services.ProjectService
import com.example.project_manager.services.SubTaskService
import com.example.project_manager.services.TaskService
import com.example.project_manager.services.UserService
import com.google.android.material.card.MaterialCardView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val ARG_PROJECT_ID = "projectId"
private const val ARG_TASK_ID = "taskId"
private const val ARG_SUBTASK_ID = "subtaskId"
private const val ARG_IS_FILE_MODE = "isFileMode"


class ItemListFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var newItemButton: MaterialCardView
    private lateinit var filterButton: MaterialCardView

    private var dataListener: ListenerRegistration? = null
    private var fileListener: ListenerRegistration? = null


    private val projectService = ProjectService()
    private val userService = UserService()
    private val taskService = TaskService()
    private val subtaskService = SubTaskService()
    private val chatService = ChatService()
    private val fileService = FileService()

    private lateinit var data: ArrayList<ItemsViewModel>
    private lateinit var files: ArrayList<FileModel>
    private lateinit var role: Role
    private lateinit var filteredDataByStatus: ArrayList<ItemsViewModel>
    private lateinit var buttonApplyFilters: Button
    private var projectId: String = ""
    private var taskId: String = ""
    private var subtaskId: String = ""

    private var isFileMode: Boolean = false

    private lateinit var drawerLayout: DrawerLayout
    private var startDate: Long = -1L
    private var endDate: Long = -1L
    private val PICK_FILE_REQUEST_CODE = 2002



    private lateinit var newProject: MaterialCardView

    private lateinit var startDateText: TextView
    private lateinit var endDateText: TextView
    private val calendar = Calendar.getInstance()

    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            projectId = it.getString(ARG_PROJECT_ID, "")
            taskId = it.getString(ARG_TASK_ID, "")
            subtaskId = it.getString(ARG_SUBTASK_ID, "")
            isFileMode = it.getBoolean(ARG_IS_FILE_MODE, false)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.itemlist_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews(view)
        lifecycleScope.launch {
            if(isFileMode){
                files=ArrayList<FileModel>()
                observeFileChanges { loadFiles() }
            }
            else if(projectId.isNotEmpty() || taskId.isNotEmpty()) {
                observeDataChanges{loadSpecificData()}
            } else {
                    observeDataChanges{loadData()}
                    deadlineFilterHandler()
                }
        }


        //barra laterale per filtri
        drawerLayout = view.findViewById(R.id.drawer_layout_logged)

        // Find the filter icon
        val iconButton = view.findViewById<MaterialCardView>(R.id.icon_logged)

        // Set click listener to toggle drawer
        iconButton.setOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                drawerLayout.closeDrawer(GravityCompat.END)
            } else {
                drawerLayout.openDrawer(GravityCompat.END)
            }
        }


    }

    override fun onResume() {
        super.onResume()
        setupListeners()
    }

    override fun onPause() {
        super.onPause()
        removeListeners()
    }

    private fun setupListeners() {
        lifecycleScope.launch {
            if(isFileMode){
                files = ArrayList()
                observeFileChanges { loadFiles() }
            }
            else if(projectId.isNotEmpty() || taskId.isNotEmpty()) {
                observeDataChanges { loadSpecificData() }
            } else {
                observeDataChanges { loadData() }
                deadlineFilterHandler()
            }
        }
    }

    private fun removeListeners() {
        dataListener?.remove()
        fileListener?.remove()
        dataListener = null
        fileListener = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        removeListeners()
    }


    private fun initializeViews(view: View) {
        recyclerView = view.findViewById(R.id.recyclerview)
        drawerLayout = view.findViewById(R.id.drawer_layout_logged)
        searchView = view.findViewById(R.id.searchView)
        newItemButton = view.findViewById(R.id.newProjectButton)
        filterButton = view.findViewById(R.id.icon_logged)
        startDateText = view.findViewById(R.id.text_start_date)
        endDateText = view.findViewById(R.id.text_end_date)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun loadFiles() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                role=userService.getCurrentUserRole()!!
                files = fileService.getTaskFiles(projectId, taskId)
                recyclerView.layoutManager = LinearLayoutManager(requireContext())
                recyclerView.adapter = FilesAdapter(files)
                setupFileView(role)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading files", e)
                Toast.makeText(requireContext(), "Error loading files", Toast.LENGTH_SHORT).show()
            }
        }
    }

    //per quando visualizzo i dati
    private fun setupFileView(role: Role) {
        //Nascondi elementi non necessari per la modalità file
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)

        if (role == Role.Developer) {
            // Nascondi il pulsante per aggiungere nuovi file per i Developer
            newItemButton.visibility = View.GONE
        } else {
            newItemButton.visibility = View.VISIBLE
            setupFileUpload()
        }

        handleFileSearchBar(files)
    }

    private fun setupFileUpload() {
        newItemButton.setOnClickListener {
            newItemButton.setOnClickListener {
                filePickerLauncher.launch("*/*")
            }
        }
    }

    private fun uploadFile(fileUri: Uri, customFileName: String) {
        val progressDialog = ProgressDialog(requireContext()).apply {
            setTitle("Uploading File")
            setMessage("Please wait...")
            setCancelable(false)
            show()
        }

        // Aggiungi timestamp al nome del file per evitare conflitti
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "${customFileName}_${timeStamp}"

        fileService.uploadTaskFile(
            projectId,
            taskId,
            fileUri = fileUri,
            fileName = fileName,
            onSuccess = { downloadUrl ->
                progressDialog.dismiss()
                Toast.makeText(requireContext(), "File caricato con successo", Toast.LENGTH_SHORT)
                    .show()
                viewLifecycleOwner.lifecycleScope.launch {
                    loadFiles() // Ricarica la lista dei file
                }
            },
            onFailure = { exception ->
                progressDialog.dismiss()
                Log.e(TAG, "Error uploading file", exception)
                Toast.makeText(
                    requireContext(),
                    "Errore durante il caricamento del file",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

   private fun getItemType(): String {
        return when {
            subtaskId.isNotEmpty() -> "subtask"
            taskId.isNotEmpty() -> "task"
            projectId.isNotEmpty() -> "progetto"
            else -> throw IllegalArgumentException("No valid ID provided")
        }
    }


    private fun loadSpecificData() {
        val recyclerview = requireView().findViewById<RecyclerView>(R.id.recyclerview)
        recyclerview.layoutManager = LinearLayoutManager(requireContext())

        newItemButton.setOnClickListener {
            val tipoForm = when (getItemType()) {
                "progetto" -> "task"
                "task" -> "subtask"
                else -> null
            }
            tipoForm?.let {
                val intent = Intent(requireActivity(), NewItemActivity::class.java)
                intent.putExtra("tipoForm", it)
                intent.putExtra("projectId", projectId)
                intent.putExtra("taskId", taskId)
                intent.putExtra("subtaskId", subtaskId)
                intent.putExtra("subitem","true")
                startActivity(intent)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                role = userService.getCurrentUserRole()!!
                data = when (getItemType()) {
                    "task" -> subtaskService.getAllSubTaskByTaskId(projectId, taskId)
                    "progetto" -> taskService.getAllTaskByProjectId(projectId)
                    else -> throw IllegalArgumentException("Tipo non valido")
                }

                // Aggiorna la UI
                recyclerview.visibility = View.VISIBLE
                visualizza(recyclerview, data)

                handleSeaarchBar(data)

                buttonApplyFilters = requireView().findViewById(R.id.apply_filters)
                buttonApplyFilters.setOnClickListener {
                    applyFilters(role)
                    drawerLayout.closeDrawer(GravityCompat.END)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error loading specific data", e)
                recyclerview.visibility = View.GONE
            }
        }
    }

    private fun handleSeaarchBar(data: ArrayList<ItemsViewModel>){
        //barra di ricerca
        val searchView = requireView().findViewById<SearchView>(R.id.searchView)

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                //cosa fare quando l'utente preme invio.
                filterProjects(query,data)
                return false
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                // Filtro la RecyclerView in base al testo inserito
                filterProjects(newText,data)
                return true
            }
        })
    }

    private fun handleFileSearchBar(files: ArrayList<FileModel>){
        val searchView = requireView().findViewById<SearchView>(R.id.searchView)

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterFiles(query,files)
                //cosa fare quando l'utente preme invio.
                return false
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                // Filtro la RecyclerView in base al testo inserito
                filterFiles(newText,files)



                return true
            }
        })
    }

    //barra di ricerca
    private fun filterProjects(query: String?,data: ArrayList<ItemsViewModel>) {
        val filteredData= projectService.filterProjects(query,data)
        visualizza(requireView().findViewById(R.id.recyclerview), filteredData)
    }


    private fun filterFiles(query: String?,data: ArrayList<FileModel>) {
        val filteredData= fileService.filterFiles(query,files)
        visualizzaFile(requireView().findViewById(R.id.recyclerview), filteredData)
    }



    private fun applyFilters(role:Role) {
        val completedCheckBox = requireView().findViewById<CheckBox>(R.id.filter_completati)
        val inProgressCheckBox = requireView().findViewById<CheckBox>(R.id.filter_in_corso)
        val highPriorityCheckBox = requireView().findViewById<CheckBox>(R.id.filter_alta)
        val mediumPriorityCheckBox = requireView().findViewById<CheckBox>(R.id.filter_media)
        val lowPriorityCheckBox = requireView().findViewById<CheckBox>(R.id.filter_bassa)

        Log.d(TAG, "applyFilters started")
        // Filtraggio basato sullo stato (completati/incompleti)
        lifecycleScope.launch {


            filteredDataByStatus = data

            if (completedCheckBox.isChecked && !inProgressCheckBox.isChecked) {
                filteredDataByStatus=filterDataByProgress(role, data, "completed")
            } else if(inProgressCheckBox.isChecked && !completedCheckBox.isChecked) {
                filteredDataByStatus=filterDataByProgress(role, data, "incompleted")
            } else{
                filteredDataByStatus=data
            }

            //filtro per scadenza
            var filteredDataByDeadline: ArrayList<ItemsViewModel>
            if (startDate == -1L && endDate == -1L) {
                filteredDataByDeadline=filteredDataByStatus
            } else {
                filteredDataByDeadline = filterByDeadline(filteredDataByStatus, startDate, endDate, dateFormat)
            }

            val leaderContainer = requireView().findViewById<LinearLayout>(R.id.leader_container_logged)
            val selectedLeaders = mutableListOf<String>()
            for (i in 0 until leaderContainer.childCount) {
                val checkBox = leaderContainer.getChildAt(i) as? CheckBox
                if (checkBox?.isChecked == true) {
                    selectedLeaders.add(checkBox.text.toString())
                }
            }
            Log.d(TAG, "Selected leaders: $selectedLeaders")

            //filtra sulla base dei leader
            val filteredDataByLeader = if (selectedLeaders.isNotEmpty()) {
                filteredDataByDeadline.filter { item -> selectedLeaders.contains(item.assignedTo) }
                    .toCollection(ArrayList()) // Conversione in ArrayList
            } else {
                ArrayList(filteredDataByDeadline) // Assicura che il risultato sia un ArrayList
            }
            Log.d(TAG, "filteredDataByLeader: $filteredDataByLeader")

// Filtraggio basato sulla priorità
            val filteredDataByPriority = when {
                !highPriorityCheckBox.isChecked && !mediumPriorityCheckBox.isChecked && !lowPriorityCheckBox.isChecked -> {
                    filteredDataByLeader // Nessuna priorità selezionata, mostra i dati senza filtro
                }
                else -> {
                    filteredDataByLeader.filter { item ->
                        when {
                            highPriorityCheckBox.isChecked && item.priority.contains("High") -> true
                            mediumPriorityCheckBox.isChecked && item.priority.contains("Medium") -> true
                            lowPriorityCheckBox.isChecked && item.priority.contains("Low") -> true
                            else -> false
                        }
                    }.toCollection(ArrayList()) // Conversione in ArrayList
                }
            }

            Log.d(TAG, "filteredDataByPriority: $filteredDataByPriority")

            Log.d(TAG, "sto chiamado visualizza con data= $filteredDataByPriority")
            visualizza(requireView().findViewById(R.id.recyclerview), filteredDataByPriority)
        }
    }

    private suspend fun filterDataByProgress(role: Role, data: ArrayList<ItemsViewModel>, s: String): ArrayList<ItemsViewModel> {
        return when (role) {
            Role.Manager -> projectService.filterProjectByProgress(data, s)
            Role.Leader -> projectService.filterProjectByProgress(data, s)
            Role.Developer -> taskService.filterTasksByProgress(data, s)
            else -> throw IllegalArgumentException("Ruolo non supportato per il filtraggio dei progressi")
        }
    }

    fun filterByDeadline(
        item: ArrayList<ItemsViewModel>,
        startDate: Long,
        endDate: Long,
        dateFormat: SimpleDateFormat
    ): ArrayList<ItemsViewModel> {
        return item.filter { item ->
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

    private fun deadlineFilterHandler(){
        startDateText = requireView().findViewById(R.id.text_start_date)
        endDateText = requireView().findViewById(R.id.text_end_date)
        val buttonSelectStartDate = requireView().findViewById<Button>(R.id.button_select_start_date)
        val buttonSelectEndDate = requireView().findViewById<Button>(R.id.button_select_end_date)
        val buttonClearStartDate = requireView().findViewById<Button>(R.id.button_clear_start_date)
        val buttonClearEndDate = requireView().findViewById<Button>(R.id.button_clear_end_date)

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
        val datePicker = DatePickerDialog(
            requireActivity(),
            { _, year, month, dayOfMonth ->
                val selectedDate = Calendar.getInstance()
                selectedDate.set(year, month, dayOfMonth)
                val formattedDate = dateFormat.format(selectedDate.time)

                if (isStartDate) {
                    startDate = selectedDate.timeInMillis // Salva la data di inizio in millisecondi
                    startDateText.text = formattedDate
                } else {
                    endDate = selectedDate.timeInMillis // Salva la data di fine in millisecondi
                    endDateText.text = formattedDate
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }


    private suspend fun loadUsersFilter(role:Role) {
        val leaderContainer: LinearLayout = requireView().findViewById(R.id.leader_container_logged)
        val leaderFilterTitle: TextView = requireView().findViewById(R.id.leader_filter_title_logged) // Aggiungi questo ID nel layout XML

        when (role) {
            Role.Leader -> {
                // Per il Leader, carica i Developer
                val developerNames = userService.getUsersByRole(Role.Developer)

                leaderContainer.removeAllViews()
                leaderFilterTitle.text = getString(R.string.developer) // Cambia il titolo

                for (user in developerNames) {
                    val checkBox = CheckBox(requireActivity())
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
                val leaderFilterSection: LinearLayout = requireView().findViewById(R.id.leader_container_logged)
                val leaderText: TextView = requireView().findViewById(R.id.leader_filter_title_logged)
                leaderFilterSection.visibility = View.GONE
                leaderText.visibility = View.GONE
            }
            else -> {
                // Per altri ruoli (es. Manager), lascia invariato
                val developerNames = userService.getUsersByRole(Role.Leader)

                leaderContainer.removeAllViews()
                leaderFilterTitle.text = getString(R.string.leader)

                for (user in developerNames) {
                    val checkBox = CheckBox(requireActivity())
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

    private suspend fun observeDataChanges(onDataChanged: suspend () -> Unit) {
        onDataChanged()  // Initial load

        val db = FirebaseFirestore.getInstance()
        dataListener=db.collection("progetti")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e(TAG, "Errore durante l'ascolto delle modifiche", error)
                    return@addSnapshotListener
                }

                if (snapshots != null && !snapshots.isEmpty) {
                    lifecycleScope.launch {
                        onDataChanged()  // Reload data when changes occur
                    }
                }
            }
    }

    private suspend fun observeFileChanges(onDataChanged: suspend () -> Unit) {
        onDataChanged()  // Initial load

        // Set up a listener on the Firestore collection that tracks file metadata
        val db = FirebaseFirestore.getInstance()
        fileListener=db.collection("progetti")
            .document(projectId)
            .collection("task")
            .document(taskId)
            .collection("files")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening for file changes", error)
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        onDataChanged()  // Reload files when the collection changes
                    }
                }
            }
    }

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { fileUri ->
            showFileNameDialog(fileUri)
        }
    }

    private fun showFileNameDialog(fileUri: Uri) {
        val originalFileName = fileUri.lastPathSegment ?: "file"

        val builder = android.app.AlertDialog.Builder(requireContext())
        val input = android.widget.EditText(requireContext())
        input.setText(originalFileName)

        builder.setTitle("Nome File")
            .setMessage("Inserisci il nome del file:")
            .setView(input)
            .setPositiveButton("Upload") { dialog, _ ->
                val fileName = input.text.toString()
                if (fileName.isNotEmpty()) {
                    uploadFile(fileUri, fileName)
                } else {
                    Toast.makeText(requireContext(), "Il nome del file non può essere vuoto", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Annulla") { dialog, _ ->
                dialog.cancel()
            }

        builder.show()
    }

    private fun loadData() {
        val view = view ?: return


        val recyclerview: RecyclerView = view.findViewById(R.id.recyclerview) ?: return
        recyclerview.layoutManager = LinearLayoutManager(requireActivity())

        newProject = view.findViewById(R.id.newProjectButton) ?: return

        lifecycleScope.launch {
            try {
                role = userService.getCurrentUserRole()!!
                loadUsersFilter(role)

                val userId = userService.getCurrentUserId() ?: return@launch
                val chat = chatService.getCurrentUserChats()

                when (role) {
                    Role.Manager -> {
                        Log.d(TAG, "sono in manager")
                        data = projectService.loadProjectForUser(userId.toString())
                        Log.d(TAG, "Data: $data")
                        managerView()
                        newProjectButtonHandler()
                        visualizza(recyclerview, data)
                    }

                    Role.Leader -> {
                        data = projectService.loadProjectByLeader(userId.toString())

                        leaderView()
                        visualizza(recyclerview, data)
                    }

                    Role.Developer -> {
                        data = taskService.filterTaskByDeveloper(userId.toString())

                        developerView()
                        visualizza(recyclerview, data)
                    }

                    else -> throw Exception("Role or user not found")
                }

                val applyFiltersButton: Button = view.findViewById(R.id.apply_filters) ?: return@launch
                applyFiltersButton.setOnClickListener {
                    applyFilters(role)
                    drawerLayout.closeDrawer(GravityCompat.END)
                }
                handleSeaarchBar(data)

            } catch (e: Exception) {
                Log.e("Auth", "Errore nel recuperare il ruolo", e)
                return@launch
            }
        }
    }

    //visualizza l'array nella reciclerView
    private fun visualizza(recyclerView: RecyclerView, data: ArrayList<ItemsViewModel>) {
        Log.d(TAG, "visualizza con data= $data")
        val adapter = CustomAdapter(data)
        recyclerView.adapter = adapter

        adapter.setOnItemClickListener(object : CustomAdapter.onItemClickListener {
            override fun onItemClick(position: Int) {
                val clickedItem = data[position]
                Log.d(TAG, "hai cliccato su $clickedItem")
                Log.d(TAG,"projectid=${clickedItem.projectId} taskid=${clickedItem.taskId} subtaskid=${clickedItem.subtaskId}")

                val intent = Intent(requireActivity(), HomeItemActivity::class.java)
                intent.putExtra("projectId", clickedItem.projectId)
                intent.putExtra("taskId", clickedItem.taskId)
                intent.putExtra("subtaskId", clickedItem.subtaskId)
                if (!clickedItem.subtaskId.isNullOrEmpty()) {
                    intent.putExtra("subitem","true")
                }
                startActivity(intent)
            }
        })
    }

    private fun visualizzaFile(recyclerView: RecyclerView, data: ArrayList<FileModel>) {
        Log.d(TAG, "visualizza con data= $data")
        val adapter = FilesAdapter(data)
        recyclerView.adapter = adapter
    }

    private fun newProjectButtonHandler(){
        newItemButton.setOnClickListener {
            val intent = Intent(requireActivity(), NewItemActivity::class.java)
            intent.putExtra("tipoForm", "progetto")
            intent.putExtra("projectId", projectId)
            intent.putExtra("taskId", taskId)
            intent.putExtra("subtaskId", subtaskId)
            startActivity(intent)
        }
    }

    private fun managerView(){
        Log.d(TAG,"managerView")
        newProject.visibility = View.VISIBLE
    }
    private fun leaderView(){
        newProject.visibility = View.GONE
    }

    private fun developerView(){
        newProject.visibility = View.GONE
    }



    companion object {
        private const val TAG = "ItemListFragment"

        @JvmStatic
        fun newInstance(
            projectId: String = "",
            taskId: String = "",
            subtaskId: String = "",
            isFileMode: Boolean = false
        ) = ItemListFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_PROJECT_ID, projectId)
                putString(ARG_TASK_ID, taskId)
                putString(ARG_SUBTASK_ID, subtaskId)
                putBoolean(ARG_IS_FILE_MODE, isFileMode)
            }
        }
    }
}