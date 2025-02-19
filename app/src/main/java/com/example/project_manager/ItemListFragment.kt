package com.example.project_manager

import android.app.DatePickerDialog
import android.content.ContentValues.TAG
import android.content.Intent
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
import androidx.appcompat.widget.SearchView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.project_manager.models.ItemsViewModel
import com.example.project_manager.models.Role
import com.example.project_manager.services.ChatService
import com.example.project_manager.services.FileService
import com.example.project_manager.services.ProjectService
import com.example.project_manager.services.SubTaskService
import com.example.project_manager.services.TaskService
import com.example.project_manager.services.UserService
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ItemListFragment : Fragment() {

    private val projectService = ProjectService()
    private val userService = UserService()
    private val taskService = TaskService()
    private val subtaskService = SubTaskService()
    private val chatService = ChatService()
    private val fileService = FileService()

    private lateinit var data: ArrayList<ItemsViewModel>
    private lateinit var role: Role
    private lateinit var filteredDataByStatus: ArrayList<ItemsViewModel>
    private lateinit var buttonApplyFilters: Button
    private var projectId: String = ""
    private var taskId: String = ""

    private lateinit var drawerLayout: DrawerLayout
    private var startDate: Long = -1L
    private var endDate: Long = -1L
    private val PICK_FILE_REQUEST_CODE = 2002



    private lateinit var newProject: MaterialCardView

    private lateinit var startDateText: TextView
    private lateinit var endDateText: TextView
    private val calendar = Calendar.getInstance()

    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.itemlist_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getArgumentsData()

        if(projectId.isNotEmpty() || taskId.isNotEmpty()) {
            loadSpecificData()
        } else {
            lifecycleScope.launch {
                loadData()
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

    //per quando visualizzo i dati
    private fun setupFileView() {
        // Nascondi elementi non necessari per la modalità file
        requireView().findViewById<MaterialCardView>(R.id.icon_logged).visibility = View.GONE
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)

        // Modifica il comportamento del pulsante +
        newProject.visibility = View.VISIBLE
        requireView().findViewById<MaterialCardView>(R.id.newProjectButton).setOnClickListener {
            setupFileUpload()
        }

        // Carica i file
        loadFiles()
    }

    private fun setupFileUpload() {
        requireView().findViewById<MaterialCardView>(R.id.newProjectButton).setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            startActivityForResult(intent, PICK_FILE_REQUEST_CODE)
        }
    }

    private fun loadFiles() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val files = fileService.getTaskFiles(projectId, taskId)
                val recyclerview = requireView().findViewById<RecyclerView>(R.id.recyclerview)
                recyclerview.layoutManager = LinearLayoutManager(requireContext())
                recyclerview.adapter = FilesAdapter(files)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading files", e)
                Toast.makeText(requireContext(), "Error loading files", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getArgumentsData() {
        arguments?.let { args ->
            projectId = args.getString("projectId", "")
            taskId = args.getString("taskId", "")
        }
        Log.d(TAG, "itemlist ha ricevuto: - projectId: $projectId, taskId: $taskId")
    }




    private fun loadSpecificData() {
        val recyclerview = requireView().findViewById<RecyclerView>(R.id.recyclerview)
        recyclerview.layoutManager = LinearLayoutManager(requireContext())

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                role = userService.getCurrentUserRole()!!
                data = when {
                    taskId.isNotEmpty() -> {
                        // Se abbiamo taskId, carica i subtask
                        subtaskService.getAllSubTaskByTaskId(projectId, taskId)
                    }
                    projectId.isNotEmpty() -> {
                        // Se abbiamo solo projectId, carica i task
                        taskService.getAllTaskByProjectId(projectId)
                    }
                    else -> ArrayList()
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
                return false
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                // Filtro la RecyclerView in base al testo inserito
                filterProjects(newText,data)
                return true
            }
        })
    }
    //barra di ricerca
    private fun filterProjects(query: String?,data: ArrayList<ItemsViewModel>) {
        val filteredData= projectService.filterProjects(query,data)
        visualizza(requireView().findViewById(R.id.recyclerview), filteredData)
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

    private suspend fun loadData() {
        val view = view ?: return
        role = userService.getCurrentUserRole()!!

        val recyclerview: RecyclerView = view.findViewById(R.id.recyclerview) ?: return
        recyclerview.layoutManager = LinearLayoutManager(requireActivity())

        newProject = view.findViewById(R.id.newProjectButton) ?: return

        lifecycleScope.launch {
            try {
                role = userService.getCurrentUserRole() ?: return@launch
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
                startActivity(intent)
            }
        })
    }

    private fun newProjectButtonHandler(){
        requireView().findViewById<MaterialCardView>(R.id.newProjectButton).setOnClickListener {
            val intent = Intent(requireActivity(), NewItemActivity::class.java)
            intent.putExtra("tipoForm", "progetto")
            startActivity(intent)
        }
    }

    private fun managerView(){
        Log.d(TAG,"managerView")
        newProject.visibility = View.VISIBLE
    }
    private fun leaderView(){
        newProject.visibility = View.INVISIBLE
    }

    private fun developerView(){
        newProject.visibility = View.INVISIBLE
    }
}