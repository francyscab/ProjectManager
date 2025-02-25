package com.example.project_manager

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
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
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class NewItemActivity : AppCompatActivity() {
    private lateinit var role: Role
    private lateinit var creatorId: String
    private lateinit var projectId: String
    private lateinit var taskId: String
    private lateinit var subtaskId: String
    private lateinit var subitem: String
    private lateinit var formType: String

    private val projectService = ProjectService()
    private val taskService = TaskService()
    private val subtaskService = SubTaskService()
    private val userService = UserService()

    // UI Elements
    private lateinit var titleEditText: TextInputEditText
    private lateinit var descriptionEditText: TextInputEditText
    private lateinit var deadlineButton: MaterialButton
    private lateinit var priorityRadioGroup: RadioGroup
    private lateinit var assigneeSpinner: AutoCompleteTextView
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
        assigneeSpinner = findViewById(R.id.assignedTo_field)
        errorTitle = findViewById(R.id.errore_titolo)
        errorDate = findViewById(R.id.errore_date)
        errorDescription = findViewById(R.id.errore_descrizione)
        errorPriority = findViewById(R.id.errore_priorita)
        errorSpinner = findViewById(R.id.errore_spinner)
        saveButton = findViewById(R.id.buttonSave)

    }

    private fun setupIntentData() {
        projectId = intent.getStringExtra("projectId") ?: ""
        taskId = intent.getStringExtra("taskId") ?: ""
        subtaskId = intent.getStringExtra("subtaskId") ?: ""
        subitem = intent.getStringExtra("subitem") ?: ""
        formType = intent.getStringExtra("tipoForm") ?: ""


        lifecycleScope.launch {
            creatorId = userService.getCurrentUserId() ?: ""
            role = userService.getCurrentUserRole() ?: Role.Developer

            // Update UI based on form type
            updateFormType(role)
        }
    }

    private fun updateFormType(role:Role) {
        priorityRadioGroup.visibility= View.VISIBLE
        val typeNewTextView = findViewById<TextView>(R.id.typeNew)
        val assigneeLayout = findViewById<TextInputLayout>(R.id.assignedToLayout)

        when (formType) {
            "progetto" -> {
                typeNewTextView.text = getString(R.string.nuovo_progetto)
                when (role) {
                    Role.Manager -> {
                        assigneeSpinner.visibility= View.VISIBLE
                        errorSpinner.visibility= View.INVISIBLE
                        assigneeLayout.hint="Leader"
                        lifecycleScope.launch {
                            setupSpinner(role)
                        }
                    }
                    else -> {
                        assigneeSpinner.visibility= View.GONE
                        errorSpinner.visibility= View.GONE
                    }
                }
            }
            "task" -> {
                typeNewTextView.text = getString(R.string.new_task)
                when (role) {
                    Role.Leader -> {
                        assigneeSpinner.visibility= View.VISIBLE
                        assigneeLayout.hint="Developer"
                        errorSpinner.visibility= View.INVISIBLE
                        lifecycleScope.launch {
                            setupSpinner(role)
                        }
                    }
                    else -> {
                        assigneeSpinner.visibility= View.GONE
                        errorSpinner.visibility= View.GONE
                    }
                }
            }
            "subtask" -> {
                typeNewTextView.text = getString(R.string.new_subtask)
                assigneeSpinner.visibility= View.GONE
                errorSpinner.visibility= View.GONE // Subtask non ha spinner
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
                }.toTypedArray()

                val adapter = ArrayAdapter(
                    this,
                    R.layout.custom_dropdown_item,
                    displayNames
                )
                assigneeSpinner.setAdapter(adapter)
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
        errorTitle.visibility= View.INVISIBLE
        errorDate.visibility= View.INVISIBLE
        errorDescription.visibility= View.INVISIBLE
        errorPriority.visibility= View.INVISIBLE
        errorSpinner.visibility= View.INVISIBLE

        // Validate fields
        if (titleEditText.text?.isEmpty() == true) {
            errorTitle.visibility= View.VISIBLE
            isValid = false
        }

        if (deadlineButton.text.isEmpty()) {
            errorDate.visibility= View.VISIBLE
            isValid = false
        }

        if (descriptionEditText.text?.isEmpty() == true) {
            errorDescription.visibility= View.VISIBLE
            isValid = false
        }

        val priority = getSelectedPriority()
        if (priority.isEmpty()) {
            errorPriority.visibility= View.VISIBLE
            isValid = false
        }

        if (shouldValidateSpinner() && assigneeSpinner.toString().isEmpty()) {
            errorSpinner.visibility=View.VISIBLE
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



            when (formType) {
                "progetto" -> {
                    // Ottieni l'ID dell'utente selezionato invece del nome visualizzato
                    val selectedDisplayName = assigneeSpinner.text.toString()
                    val assigneeId = userMap[selectedDisplayName]!!
                    projectId = projectService.uploadNewProject(
                        title, description, deadline, priority, creatorId, assigneeId
                    )
                }
                "task" -> {
                    // Ottieni l'ID dell'utente selezionato invece del nome visualizzato
                    val selectedDisplayName = assigneeSpinner.text.toString()
                    val assigneeId = userMap[selectedDisplayName]!!
                    taskId = taskService.uploadNewTask(
                        projectId, title, description, deadline, priority, creatorId, assigneeId
                    )
                }
                "subtask" -> {
                    subtaskId = subtaskService.uploadNewSubTask(
                        projectId, taskId, title, description, deadline, priority, creatorId,creatorId
                    )
                }
            }
            navigateToHome()
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

    private fun navigateToHome() {
        if(subitem=="true"){
            val intent = Intent(this, HomeItemActivity::class.java).apply {
                putExtra("projectId", projectId)
                putExtra("taskId", taskId)
                putExtra("subtaskId", subtaskId)
                putExtra("subitem","true")

            }

            startActivity(intent)
            finish()
        }
        else {
            val intent = Intent(this, HomeItemActivity::class.java).apply {
                putExtra("projectId", projectId)
            }
            startActivity(intent)
            finish()
        }
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
        val intent = Intent(this, HomeActivity::class.java).apply {
            putExtra("projectId", projectId)
            putExtra("taskId", taskId)
            putExtra("role", role.toString())
        }
        startActivity(intent)
        finish()
    }


}