package com.example.project_manager

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SearchView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.project_manager.models.ItemsViewModel
import com.example.project_manager.models.Role
import com.example.project_manager.models.User
import com.example.project_manager.services.ChatService
import com.example.project_manager.services.ProjectService
import com.example.project_manager.services.TaskService
import com.example.project_manager.services.UserService
import com.example.project_manager.repository.NotificationHelper
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


class LoggedActivity : AppCompatActivity() {

    val projectService= ProjectService()
    val userService= UserService()
    val taskService= TaskService()
    val chatService= ChatService()

    private lateinit var data: ArrayList<ItemsViewModel>
    private lateinit var role: Role
    private lateinit var filteredDataByStatus: ArrayList<ItemsViewModel>
    private lateinit var buttonApplyFilters: Button


    private lateinit var drawerLayout: DrawerLayout
    private var startDate: Long = -1L
    private var endDate: Long = -1L

    private lateinit var newProject: ImageButton


    private lateinit var startDateText: TextView
    private lateinit var endDateText: TextView
    private val calendar = Calendar.getInstance()

    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logged)

        loadData()
        deadlineFilterHandler()

        chatButtonHandler()
        profileButtonHandler()
        statistticButtonHandler()

        //barra laterale per filtri
        drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout_logged)
        val iconButton = findViewById<ImageView>(R.id.icon_logged)

        iconButton.setOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                drawerLayout.closeDrawer(GravityCompat.END)
            } else {
                drawerLayout.openDrawer(GravityCompat.END)
            }
        }

        //barra di ricerca
        val searchView = findViewById<SearchView>(R.id.searchView)
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
        visualizza(findViewById(R.id.recyclerview), filteredData)
    }



    private fun applyFilters(role:Role) {
        val completedCheckBox = findViewById<CheckBox>(R.id.filter_completati)
        val inProgressCheckBox = findViewById<CheckBox>(R.id.filter_in_corso)
        val highPriorityCheckBox = findViewById<CheckBox>(R.id.filter_alta)
        val mediumPriorityCheckBox = findViewById<CheckBox>(R.id.filter_media)
        val lowPriorityCheckBox = findViewById<CheckBox>(R.id.filter_bassa)

        Log.d(TAG, "applyFilters started")
        // Filtraggio basato sullo stato (completati/incompleti)
        lifecycleScope.launch {
            loadUsersFilter()

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

            val leaderContainer = findViewById<LinearLayout>(R.id.leader_container)
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
            visualizza(findViewById(R.id.recyclerview), filteredDataByPriority)
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
        startDateText = findViewById(R.id.text_start_date)
        endDateText = findViewById(R.id.text_end_date)
        val buttonSelectStartDate = findViewById<Button>(R.id.button_select_start_date)
        val buttonSelectEndDate = findViewById<Button>(R.id.button_select_end_date)
        val buttonClearStartDate = findViewById<Button>(R.id.button_clear_start_date)
        val buttonClearEndDate = findViewById<Button>(R.id.button_clear_end_date)

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
            this,
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


    private suspend fun loadUsersFilter() {
        val leaderContainer = findViewById<LinearLayout>(R.id.leader_container)

        val leaderNames = loadFilterName(Role.Leader) // Ora è una chiamata diretta e sospesa

        leaderContainer.removeAllViews() // Pulisce la lista prima di riempirla

        for (user in leaderNames) {
            val checkBox = CheckBox(leaderContainer.context) // Usa il contesto del container
            checkBox.text = user.name + "" + user.surname
            checkBox.tag=user.uid
            checkBox.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            leaderContainer.addView(checkBox) // Aggiunge il CheckBox alla sidebar
        }
    }


    private suspend fun loadFilterName(itemRole: Role): ArrayList<User> {
        var filteredNames = ArrayList<User>()
        filteredNames = userService.getUsersByRole(itemRole)
        return filteredNames
    }

    private fun loadData() {
        val recyclerview = findViewById<RecyclerView>(R.id.recyclerview)
        recyclerview.layoutManager = LinearLayoutManager(this)
        newProject = findViewById(R.id.newProject)
        val notificationHelper = NotificationHelper(this, FirebaseFirestore.getInstance())
        lifecycleScope.launch {
            try {
                role= userService.getCurrentUserRole()!!
                val userId=userService.getCurrentUserId()
                val chat=chatService.getCurrentUserChats()

            when (role) {
                Role.Manager -> {
                    Log.d(TAG, "sono in manager")
                    data = projectService.loadProjectForUser(userId.toString())
                    Log.d(TAG, "Data: $data")
                    notificationHelper.handleNotification(role,userId!!, "chat",chat)
                    managerView()
                    newProjectButtonHandler()
                    visualizza(recyclerview, data)
                }

                Role.Leader -> {
                    data = projectService.loadProjectByLeader(userId.toString())
                    notificationHelper.handleNotification( role, userId!! ,"progresso")
                    notificationHelper.handleNotification(role,userId!!, "chat", chat)
                    leaderView()
                    visualizza(recyclerview, data)
                }

                Role.Developer -> {
                    data = taskService.filterTaskByDeveloper(userId.toString())
                    notificationHelper.handleNotification(role,userId!!, "chat", chat)
                    notificationHelper.handleNotification( role, userId!! ,"progresso")
                    developerView()
                    visualizza(recyclerview, data)
                }

                else -> throw Exception("Role or user not found")
            }


            buttonApplyFilters = findViewById(R.id.apply_filters)
            buttonApplyFilters.setOnClickListener {
                applyFilters(role)
                drawerLayout.closeDrawer(GravityCompat.END)
            }

            }catch (e:Exception){
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

                val intent = Intent(this@LoggedActivity, ItemActivity::class.java)
                intent.putExtra("projectId", clickedItem.projectId)
                intent.putExtra("taskId", clickedItem.taskId)
                intent.putExtra("subtaskId", clickedItem.subtaskId)
                startActivity(intent)
            }
        })
    }

    private fun newProjectButtonHandler(){
        findViewById<ImageButton>(R.id.newProject).setOnClickListener {
            val intent = Intent(this@LoggedActivity, NewItemActivity::class.java)
            intent.putExtra("tipoForm", "progetto")
            startActivity(intent)
        }
    }

    private fun chatButtonHandler(){
        findViewById<ImageButton>(R.id.button_chat).setOnClickListener {
            val intent = Intent(this, ChatListActivity::class.java)
            startActivity(intent)
        }
    }

    private fun profileButtonHandler(){
        findViewById<ImageButton>(R.id.button_person).setOnClickListener {
            val intent = Intent(this, UserProfileActivity::class.java)
            startActivity(intent)
        }
    }

    private fun statistticButtonHandler(){
        findViewById<ImageButton>(R.id.button_statistiche).setOnClickListener {
            val intent = Intent(this, StatisticheActivity::class.java)
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

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        AlertDialog.Builder(this)
            .setTitle("Confirm Exit")
            .setMessage("Are you sure you want to exit the application?")
            .setPositiveButton("Yes") { _, _ ->
                // User confirmed, move the app to the background
                moveTaskToBack(true) // Mette l'app in background senza chiuderla
            }
            .setNegativeButton("No") { dialog, _ ->
                // User canceled, dismiss the dialog
                dialog.dismiss()
            }
            .show()
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
        }
    }
}