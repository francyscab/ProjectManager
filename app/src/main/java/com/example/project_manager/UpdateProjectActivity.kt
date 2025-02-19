package com.example.project_manager

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.project_manager.models.ItemsViewModel
import com.example.project_manager.models.Role
import com.example.project_manager.models.User
import com.example.project_manager.services.ProjectService
import com.example.project_manager.services.SubTaskService
import com.example.project_manager.services.TaskService
import com.example.project_manager.services.UserService
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class UpdateProjectActivity : AppCompatActivity() {
    private lateinit var titleNewProject: TextInputEditText
    private lateinit var descrizioneNewProject: TextInputEditText
    private lateinit var pickDate: MaterialButton
    private lateinit var projectElementSpinner: AutoCompleteTextView
    private lateinit var buttonSave: Button
    private lateinit var erroreDescrizione: TextView
    private lateinit var erroreTitolo: TextView

    private var selectedUserId: String? = null

    private var taskId: String? = null
    private var projectId: String? = null
    private var subtaskId: String? = null
    private lateinit var role: Role

    private val projectService = ProjectService()
    private val taskService = TaskService()
    private val subtaskService = SubTaskService()
    private val userService = UserService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_project_form)

        initializeViews()
        loadIntentData()

        lifecycleScope.launch {
            role = userService.getCurrentUserRole() ?: throw IllegalStateException("Role not found")
            val type = getItemType()
            loadAndDisplayItem(type)
            setupSaveButton(type)
        }


    }

    private fun initializeViews() {
        titleNewProject = findViewById(R.id.titleNewProject)
        descrizioneNewProject = findViewById(R.id.descrizioneNewProject)
        pickDate = findViewById(R.id.pickDate)
        projectElementSpinner = findViewById(R.id.assignedTo_field)
        buttonSave = findViewById(R.id.buttonSave)
        erroreDescrizione = findViewById(R.id.errore_descrizione)
        erroreTitolo = findViewById(R.id.errore_titolo)
    }

    private fun loadIntentData() {
        taskId = intent.getStringExtra("taskId")
        projectId = intent.getStringExtra("projectId")
        subtaskId = intent.getStringExtra("subtaskId")
    }

    private fun getItemType(): String {
        return when {
            !subtaskId.isNullOrEmpty() -> "subtask"
            !taskId.isNullOrEmpty() -> "task"
            !projectId.isNullOrEmpty() -> "progetto"
            else -> throw IllegalArgumentException("No valid ID provided")
        }
    }

    private suspend fun loadAndDisplayItem(type: String) {
        if (projectId == null) throw IllegalStateException("Project ID is required")

        val item = when(type) {
            "progetto" -> projectService.getProjectById(projectId!!)
            "task" -> {
                if (taskId == null) throw IllegalStateException("Task ID is required")
                taskService.getTaskById(projectId!!, taskId!!)
            }
            "subtask" -> {
                if (taskId == null) throw IllegalStateException("Task ID is required")
                if (subtaskId == null) throw IllegalStateException("Subtask ID is required")
                subtaskService.getSubTaskById(projectId!!, taskId!!, subtaskId!!)
            }
            else -> throw IllegalStateException("Invalid type")
        } ?: throw IllegalStateException("Failed to load item")

        setData(item, role)
    }

    private suspend fun setData(item: ItemsViewModel, role: Role) {
        titleNewProject.setText(item.title)
        descrizioneNewProject.setText(item.description)
        pickDate.text = item.deadline
        pickDate.isEnabled = false

        // Trova il layout contenente i RadioButton e nascondilo
        val priorityLayout: LinearLayout = findViewById(R.id.radioGroupPriority)
        val priorityTitle: TextView = findViewById(R.id.radioGroupTitle)
        priorityLayout.visibility = View.GONE
        priorityTitle.visibility = View.GONE


        titleNewProject.setText(item.title)
        descrizioneNewProject.setText(item.description)
        pickDate.text = item.deadline
        pickDate.isEnabled = false

        when (role) {
            Role.Manager -> {
                val users = loadSpinnerData(Role.Leader)
                showDataInSpinner(projectElementSpinner, users, item.assignedTo, role)
            }
            Role.Leader -> {
                val users = loadSpinnerData(Role.Developer)
                showDataInSpinner(projectElementSpinner, users, item.assignedTo, role)
            }
            Role.Developer -> {
                findViewById<LinearLayout>(R.id.assignedTo_field).visibility = View.GONE
            }
        }
    }

    private fun showDataInSpinner(
        autoComplete: AutoCompleteTextView,
        users: ArrayList<User>,
        selectedValue: String?,
        role: Role
    ) {
        val userMap = mutableMapOf<String, String>()
        val displayNames = users.map { user ->
            val displayName = "${user.name} ${user.surname}"
            userMap[displayName] = user.uid
            displayName
        }.toTypedArray()

        val adapter = ArrayAdapter(
            this,
            R.layout.custom_dropdown_item,
            displayNames
        )
        autoComplete.setAdapter(adapter)

        autoComplete.setOnItemClickListener { parent, _, position, _ ->
            val selectedName = parent.getItemAtPosition(position).toString()
            selectedUserId = userMap[selectedName]
        }

        selectedValue?.let { value ->
            val selectedUser = users.find { it.uid == value }
            selectedUser?.let {
                val displayName = "${it.name} ${it.surname}"
                autoComplete.setText(displayName, false)
                selectedUserId = it.uid
            }
        }

        autoComplete.isEnabled = role == Role.Manager
    }

    private suspend fun loadSpinnerData(role: Role): ArrayList<User> {
        return try {
            userService.getUsersByRole(role)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading spinner data", e)
            ArrayList()
        }
    }

    private fun setupSaveButton(tipo:String) {
        buttonSave.setOnClickListener {
            val title = titleNewProject.text.toString()
            val description = descrizioneNewProject.text.toString()

            if (validateInputs(title, description)) {
                lifecycleScope.launch {
                    updateItem(title, description,tipo)
                    //navigateBack()
                }
            }
        }
    }

    private fun validateInputs(title: String, description: String): Boolean {
        var isValid = true

        if (title.isBlank()) {
            erroreTitolo.visibility= View.VISIBLE
            isValid = false
        } else {
            erroreTitolo.visibility= View.INVISIBLE
        }

        if (description.isBlank()) {
            erroreDescrizione.visibility= View.VISIBLE
            isValid = false
        } else {
            erroreDescrizione.visibility= View.INVISIBLE
        }

        return isValid
    }

    private suspend fun updateItem(title: String, description: String,tipo:String) {
        try {
            when (tipo) {
                "progetto" -> {
                    projectService.updateProject(projectId!!, title, description, selectedUserId!!)
                }
                "task" -> {
                    taskService.updateTask(projectId!!, taskId!!, title, description)
                }
                "subtask" -> {
                    subtaskService.updateSubTask(projectId!!, taskId!!, subtaskId!!, title,description)
                }
            }
            Toast.makeText(this, "Update successful", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Update failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /*private fun navigateBack() {
        val intent = Intent(this, ItemActivity::class.java).apply {
            putExtra("projectId", projectId)
            putExtra("taskId", taskId)
            putExtra("subtaskId", subtaskId)
        }
        startActivity(intent)
        finish()
    }*/
}