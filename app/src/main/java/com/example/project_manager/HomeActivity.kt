package com.example.project_manager

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


class HomeActivity: AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var fragmentContainer: FrameLayout

    val userService = UserService()
    val chatService = ChatService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.logged_activity_2)

        bottomNavigationView = findViewById(R.id.bottomBar)
        fragmentContainer = findViewById(R.id.fragment_container)

        setupNotifications()

        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_projects -> {
                    replaceFragment(ItemListFragment())
                    true
                }
                R.id.navigation_chat -> {
                    replaceFragment(ChatListFragment())
                    true
                }
                R.id.navigation_statistics -> {
                    replaceFragment(StatisticheFragment())
                    true
                }
                R.id.navigation_profile -> {
                    replaceFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }

        // Mostra il fragment del profilo per impostazione predefinita
        replaceFragment(ItemListFragment())
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun setupNotifications() {
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
                            notificationHelper.handleNotification(role, userId, "progresso", lifecycleScope)

                        }
                        Role.Leader -> {
                            notificationHelper.handleNotification(role, userId, "progresso", lifecycleScope)
                            notificationHelper.handleNotification(role, userId, "sollecito", lifecycleScope)
                        }
                        Role.Developer -> {
                            notificationHelper.handleNotification(role, userId, "progresso", lifecycleScope)
                            notificationHelper.handleNotification(role, userId, "sollecito", lifecycleScope)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeActivity", "Error setting up notifications", e)
            }
        }
    }
}