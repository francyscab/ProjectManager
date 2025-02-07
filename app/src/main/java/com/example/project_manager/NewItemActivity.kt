package com.example.project_manager

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.project_manager.models.Role
import com.example.project_manager.services.ProjectService
import com.example.project_manager.services.SubTaskService
import com.example.project_manager.services.TaskService
import com.example.project_manager.services.UserService
import kotlinx.coroutines.launch

class NewItemActivity : AppCompatActivity() {
    private lateinit var role: Role
    private lateinit var creatorId: String
    private lateinit var projectId: String
    private lateinit var taskId: String
    private lateinit var formType: String

    private val projectService = ProjectService()
    private val taskService = TaskService()
    private val subtaskService = SubTaskService()
    private val userService = UserService()

    // UI Elements
    private lateinit var titleEditText: EditText
    private lateinit var descriptionEditText: EditText
    private lateinit var deadlineButton: Button
    private lateinit var priorityRadioGroup: RadioGroup
    private lateinit var assigneeSpinner: Spinner
    private lateinit var errorTitle: TextView
    private lateinit var errorDate: TextView
    private lateinit var errorDescription: TextView
    private lateinit var errorPriority: TextView
    private lateinit var errorSpinner: TextView
    private lateinit var saveButton: Button
    private lateinit var priorityTextLayout: LinearLayout
    private lateinit var priorityLayout: LinearLayout

    private val userMap = mutableMapOf<String, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_project_form)

        lifecycleScope.launch {
            initializeViews()
            setupIntentData()
            setupDatePicker()
            setupSaveButton()
        }

    }

    private fun initializeViews() {
        titleEditText = findViewById(R.id.titleNewProject)
        descriptionEditText = findViewById(R.id.descrizioneNewProject)
        deadlineButton = findViewById(R.id.pickDate)
        priorityRadioGroup = findViewById(R.id.radioGroupPriority)
        assigneeSpinner = findViewById(R.id.projectElementSpinner)
        errorTitle = findViewById(R.id.errore_titolo)
        errorDate = findViewById(R.id.errore_date)
        errorDescription = findViewById(R.id.errore_descrizione)
        errorPriority = findViewById(R.id.errore_priorita)
        errorSpinner = findViewById(R.id.errore_spinner)
        saveButton = findViewById(R.id.buttonSave)
        priorityTextLayout = findViewById(R.id.priorityTextLayout)
        priorityLayout = findViewById(R.id.priorityLayout)
    }

    private fun setupIntentData() {
        projectId = intent.getStringExtra("projectId") ?: ""
        taskId = intent.getStringExtra("taskId") ?: ""
        formType = intent.getStringExtra("tipoForm") ?: ""

        lifecycleScope.launch {
            creatorId = userService.getCurrentUserId() ?: ""
            role = userService.getCurrentUserRole() ?: Role.Developer

            // Update UI based on form type
            updateFormType(role)
        }
    }

    private fun updateFormType(role:Role) {
        priorityLayout.visibility= View.VISIBLE
        priorityTextLayout.visibility= View.GONE
        val typeNewTextView = findViewById<TextView>(R.id.typeNew)
        val spinnerLayout = findViewById<LinearLayout>(R.id.spinnerLinearLayout)


        when (formType) {
            "progetto" -> {
                typeNewTextView.text = "NEW PROJECT"
                when (role) {
                    Role.Manager -> {
                        spinnerLayout.visibility = View.VISIBLE
                        lifecycleScope.launch {
                            setupSpinner(role)
                        }
                    }
                    else -> {
                        spinnerLayout.visibility = View.GONE
                    }
                }
            }
            "task" -> {
                typeNewTextView.text = "NEW TASK"
                when (role) {
                    Role.Leader -> {
                        spinnerLayout.visibility = View.VISIBLE
                        lifecycleScope.launch {
                            setupSpinner(role)
                        }
                    }
                    else -> {
                        spinnerLayout.visibility = View.GONE
                    }
                }
            }
            "subtask" -> {
                typeNewTextView.text = "NEW SUBTASK"
                spinnerLayout.visibility = View.GONE // Subtask non ha spinner
            }
        }
    }

    private suspend fun setupSpinner(role:Role) {
        val spinnerRole = when (formType) {
            "progetto" -> when (role) {
                Role.Manager -> Role.Leader
                else -> null
            }
            "task" -> when (role) {
                Role.Leader -> Role.Developer
                else -> null
            }
            else -> null
        }

        if (spinnerRole != null) {
            try {
                val users = userService.getUsersByRole(spinnerRole)

                // Pulisci la mappa esistente
                userMap.clear()
                val displayNames = users.map { user ->
                    val displayName = "${user.name} ${user.surname}"
                    userMap[displayName] = user.uid
                    displayName
                }

                showDataInSpinner(assigneeSpinner, displayNames)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading users for spinner", e)
                Toast.makeText(this, "Error loading users", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun setupDatePicker() {
        deadlineButton.setOnClickListener {
            DatePickerFragment().newInstance("pickDate")
                .show(supportFragmentManager, "datePicker")
        }
    }

    private fun showDataInSpinner(spinner: Spinner, names: List<String>) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, names)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
    }

    private fun setupSaveButton() {
        saveButton.setOnClickListener {
            if (validateForm()) {
                lifecycleScope.launch {
                    saveItem()
                }
            }
        }
    }

    private fun validateForm(): Boolean {
        var isValid = true

        // Clear previous errors
        errorTitle.text = ""
        errorDate.text = ""
        errorDescription.text = ""
        errorPriority.text = ""
        errorSpinner.text = ""

        // Validate fields
        if (titleEditText.text.isEmpty()) {
            errorTitle.text = "Missing title"
            isValid = false
        }

        if (deadlineButton.text.isEmpty()) {
            errorDate.text = "Missing deadline"
            isValid = false
        }

        if (descriptionEditText.text.isEmpty()) {
            errorDescription.text = "Missing description"
            isValid = false
        }

        val priority = getSelectedPriority()
        if (priority.isEmpty()) {
            errorPriority.text = "Please select a priority"
            isValid = false
        }

        if (shouldValidateSpinner() && assigneeSpinner.selectedItemPosition == AdapterView.INVALID_POSITION) {
            errorSpinner.text = "Please select an assignee"
            isValid = false
        }

        return isValid
    }

    private suspend fun saveItem() {
        try {
            val title = titleEditText.text.toString()
            val description = descriptionEditText.text.toString()
            val deadline = deadlineButton.text.toString()
            val priority = getSelectedPriority()

            // Ottieni l'ID dell'utente selezionato invece del nome visualizzato
            val selectedDisplayName = assigneeSpinner.selectedItem?.toString() ?: ""
            val assigneeId = userMap[selectedDisplayName] ?: ""

            when (formType) {
                "progetto" -> {
                    val projectId = projectService.uploadNewProject(
                        title, description, deadline, priority, creatorId, assigneeId
                    )
                    navigateToProject(projectId)
                }
                "task" -> {
                    val taskId = taskService.uploadNewTask(
                        projectId, title, description, deadline, priority, creatorId, assigneeId
                    )
                    navigateToTask(projectId, taskId)
                }
                "subtask" -> {
                    val subtaskId = subtaskService.uploadNewSubTask(
                        projectId, taskId, title, description, deadline, priority, creatorId, assigneeId
                    )
                    navigateToSubtask(projectId, taskId, subtaskId)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving item", e)
            Toast.makeText(this, "Error saving item", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getSelectedPriority(): String {
        return when (priorityRadioGroup.checkedRadioButtonId) {
            R.id.radioHigh -> "High"
            R.id.radioMedium -> "Medium"
            R.id.radioLow -> "Low"
            else -> ""
        }
    }

    private fun shouldValidateSpinner(): Boolean {
        return formType in listOf("progetto", "task")
    }

    private fun navigateToProject(projectId: String) {
        val intent = Intent(this, ItemActivity::class.java).apply {
            putExtra("projectId", projectId)
            putExtra("role", role.toString())
        }
        startActivity(intent)
        finish()
    }

    private fun navigateToTask(projectId: String, taskId: String) {
        val intent = Intent(this, ItemActivity::class.java).apply {
            putExtra("projectId", projectId)
            putExtra("taskId", taskId)
            putExtra("role", role.toString())
        }
        startActivity(intent)
        finish()
    }

    private fun navigateToSubtask(projectId: String, taskId: String, subtaskId: String) {
        val intent = Intent(this, ItemActivity::class.java).apply {
            putExtra("projectId", projectId)
            putExtra("taskId", taskId)
            putExtra("subtaskId", subtaskId)
            putExtra("role", role.toString())
        }
        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        AlertDialog.Builder(this)
            .setTitle("Confirm Navigation")
            .setMessage("You have unsaved changes. Are you sure you want to go back?")
            .setPositiveButton("Yes") { _, _ -> handleBackNavigation() }
            .setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun handleBackNavigation() {
        val intent = Intent(this, ItemActivity::class.java).apply {
            putExtra("projectId", projectId)
            putExtra("taskId", taskId)
            putExtra("role", role.toString())
        }
        startActivity(intent)
        finish()
    }
}