package com.example.project_manager

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager.widget.ViewPager
import com.example.project_manager.models.Role
import com.example.project_manager.repository.NotificationHelper
import com.example.project_manager.services.ChatService
import com.example.project_manager.services.UserService
import com.google.android.material.tabs.TabLayout
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {
    private lateinit var viewPager: ViewPager
    private lateinit var tabLayout: TabLayout

    private val userService = UserService()
    private val chatService = ChatService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.logged_activity_2)

        initializeViews()
        setupViewPager()
        setupNotifications()
    }

    private fun initializeViews() {
        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)
    }

    private fun setupViewPager() {
        viewPager.adapter = HomePageAdapter(this,supportFragmentManager)
        tabLayout.setupWithViewPager(viewPager)

        // Setup tab icons
        for (i in 0 until tabLayout.tabCount) {
            val tab = tabLayout.getTabAt(i)
            tab?.setIcon(getTabIcon(i))
        }
    }

    private fun getTabIcon(position: Int): Int {
        return when(position) {
            0 -> R.drawable.icona_progetto_ios
            1 -> R.drawable.icon_chat
            2 -> R.drawable.statistiche
            3 -> R.drawable.icona_user_ios
            else -> R.drawable.icona_progetto_ios
        }
    }

    private fun setupNotifications() {
        val notificationHelper = NotificationHelper(this, FirebaseFirestore.getInstance())

        lifecycleScope.launch {
            try {
                val role = userService.getCurrentUserRole()
                val userId = userService.getCurrentUserId()

                if (role != null && userId != null) {
                    val chat = chatService.getCurrentUserChats()
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