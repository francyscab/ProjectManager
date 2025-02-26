package com.example.project_manager

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager.widget.ViewPager
import com.example.project_manager.models.Role
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


}