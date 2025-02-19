package com.example.project_manager

import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.project_manager.models.Role
import com.example.project_manager.repository.NotificationHelper
import com.example.project_manager.services.ChatService
import com.example.project_manager.services.UserService
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch


class HomeItemActivity: AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var fragmentContainer: FrameLayout
    private lateinit var projectId:String
    private lateinit var taskId:String
    private lateinit var subtaskId:String

    val userService = UserService()
    val chatService = ChatService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home_item_activity)

        bottomNavigationView = findViewById(R.id.bottomBarItem)
        fragmentContainer = findViewById(R.id.fragment_container_item)

        getIntentData()
        //setupNotifications()
        lifecycleScope.launch {
            setupMenuVisibility()
        }


        val initialFragment = DettagliItemFragment().apply {
            arguments = Bundle().apply {
                putString("projectId", projectId)
                putString("taskId", taskId)
                putString("subtaskId", subtaskId)
            }
        }

        // Mostra il fragment iniziale con i dati



        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.dettagli -> {
                    val dettagliFragment = DettagliItemFragment().apply {
                        arguments = Bundle().apply {
                            putString("projectId", projectId)
                            putString("taskId", taskId)
                            putString("subtaskId", subtaskId)
                        }
                    }
                    Log.d("HomeItemActivity", "Before replaceFragment()")
                    replaceFragment(DettagliItemFragment())
                    Log.d("HomeItemActivity", "After replaceFragment()")
                    true
                }
                R.id.subitem -> {
                    Log.d(TAG, "subitem sta ricevendo - projectId: $projectId, taskId: $taskId")

                    val itemListFragment = ItemListFragment().apply {
                        arguments = Bundle().apply {
                            putString("projectId", projectId)
                            putString("taskId", taskId)
                            putString("subtaskId", subtaskId)
                        }
                    }
                    replaceFragment(itemListFragment)
                    true
                }
                R.id.files -> {
                    val fileListFragment = ItemListFragment().apply {
                        arguments = Bundle().apply {
                            putString("projectId", projectId)
                            putString("taskId", taskId)
                            putString("subtaskId", subtaskId)
                            putBoolean("isFileMode", true)
                        }
                    }
                    replaceFragment(fileListFragment)
                    true
                }
                else -> false
            }
        }
        Log.d("HomeItemActivity", "Before replaceFragment()")
        replaceFragment(initialFragment)
        Log.d("HomeItemActivity", "After replaceFragment()")
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container_item, fragment)
            .commit()
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


    private suspend fun setupMenuVisibility() {
        val role = userService.getCurrentUserRole()
        val type = getItemType(subtaskId, taskId, projectId)

        val dettagliItem = bottomNavigationView.menu.findItem(R.id.dettagli)
        val subitemItem = bottomNavigationView.menu.findItem(R.id.subitem)
        val filesItem = bottomNavigationView.menu.findItem(R.id.files)

        when {
            // Manager visualizza progetto
            role == Role.Manager && type == "progetto" -> {
                dettagliItem.isVisible = true
                subitemItem.isVisible = false
                filesItem.isVisible = false
            }

            // Leader visualizza progetto
            role == Role.Leader && type == "progetto" -> {
                dettagliItem.isVisible = true
                subitemItem.isVisible = true
                filesItem.isVisible = false
            }

            // Leader visualizza task
            role == Role.Leader && type == "task" -> {
                dettagliItem.isVisible = true
                subitemItem.isVisible = true
                filesItem.isVisible = false
            }

            // Developer visualizza task
            role == Role.Developer && type == "task" -> {
                dettagliItem.isVisible = true
                subitemItem.isVisible = true
                filesItem.isVisible = true
            }

            // Developer visualizza subtask
            role == Role.Developer && type == "subtask" -> {
                dettagliItem.isVisible = true
                subitemItem.isVisible = false
                filesItem.isVisible = true
            }

            // Caso di default
            else -> {
                dettagliItem.isVisible = true
                subitemItem.isVisible = false
                filesItem.isVisible = false
            }
        }

        // Seleziona il primo item visibile come default
        if (dettagliItem.isVisible) {
            bottomNavigationView.selectedItemId = R.id.dettagli
        }
    }

    private fun getIntentData() {
        projectId = intent.getStringExtra("projectId") ?: ""
        taskId = intent.getStringExtra("taskId") ?: ""
        subtaskId = intent.getStringExtra("subtaskId") ?: ""
    }

    /*private fun setupNotifications() {
        val notificationHelper = NotificationHelper(this, FirebaseFirestore.getInstance())

        lifecycleScope.launch {
            try {
                val role = userService.getCurrentUserRole()
                val userId = userService.getCurrentUserId()

                if (role != null && userId != null) {
                    val chat = chatService.getCurrentUserChats()

                    // Setup chat notifications for all roles
                    notificationHelper.handleNotification(role, userId, "chat", lifecycleScope, chat)

                    when (role) {
                        Role.Manager -> {
                            notificationHelper.handleNotification(role, userId, "chat", lifecycleScope, chat)
                        }
                        Role.Leader -> {
                            notificationHelper.handleNotification(role, userId, "progresso", lifecycleScope)
                            notificationHelper.handleNotification(role, userId, "chat", lifecycleScope, chat)
                            notificationHelper.handleNotification(role, userId, "sollecito", lifecycleScope)
                        }
                        Role.Developer -> {
                            notificationHelper.handleNotification(role, userId, "chat", lifecycleScope, chat)
                            notificationHelper.handleNotification(role, userId, "progresso", lifecycleScope)
                            notificationHelper.handleNotification(role, userId, "sollecito", lifecycleScope)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeActivity", "Error setting up notifications", e)
            }
        }
    }*/
}