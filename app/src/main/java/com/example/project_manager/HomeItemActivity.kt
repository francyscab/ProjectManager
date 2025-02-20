package com.example.project_manager

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager.widget.ViewPager
import com.example.project_manager.models.Role
import com.example.project_manager.services.UserService
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch

class HomeItemActivity : AppCompatActivity() {
    private lateinit var viewPager: ViewPager
    private lateinit var tabLayout: TabLayout

    private var projectId: String = ""
    private var taskId: String = ""
    private var subtaskId: String = ""
    private lateinit var role: Role

    private val userService = UserService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home_item_activity)

        getIntentData()
        initializeViews()

        lifecycleScope.launch {
            setupViewPagerWithRole()
        }
    }

    private fun getIntentData() {
        projectId = intent.getStringExtra("projectId") ?: ""
        taskId = intent.getStringExtra("taskId") ?: ""
        subtaskId = intent.getStringExtra("subtaskId") ?: ""

        Log.d(TAG, "Received - projectId: $projectId, taskId: $taskId, subtaskId: $subtaskId")
    }

    private fun initializeViews() {
        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)
    }

    private suspend fun setupViewPagerWithRole() {
        try {
            role = userService.getCurrentUserRole() ?: throw Exception("Role not found")

            val showSubitems = shouldShowSubitems(role)
            val showFiles = shouldShowFiles(role)

            val adapter = HomeItemAdapter(
                supportFragmentManager,
                projectId,
                taskId,
                subtaskId,
                showSubitems,
                showFiles
            )

            viewPager.adapter = adapter
            tabLayout.setupWithViewPager(viewPager)

            // Setup tab icons
            setupTabIcons()

        } catch (e: Exception) {
            Log.e(TAG, "Error setting up ViewPager", e)
        }
    }

    private fun shouldShowSubitems(role: Role): Boolean {
        return when {
            // Leader viewing project - can see tasks
            role == Role.Leader && taskId.isEmpty() -> true
            // Developer viewing task - can see subtasks
            role == Role.Developer && taskId.isNotEmpty() && subtaskId.isEmpty() -> true
            else -> false
        }
    }

    private fun shouldShowFiles(role: Role): Boolean {
        return when {
            // Developer can see files for tasks
            role == Role.Developer && taskId.isNotEmpty() -> true
            // Leader can see files for tasks
            role == Role.Leader && taskId.isNotEmpty() -> true
            else -> false
        }
    }

    private fun setupTabIcons() {
        for (i in 0 until tabLayout.tabCount) {
            val tab = tabLayout.getTabAt(i)
            tab?.setIcon(getTabIcon(i))
        }
    }

    private fun getTabIcon(position: Int): Int {
        return when(position) {
            0 -> R.drawable.dettagli
            1 -> R.drawable.task
            2 -> R.drawable.files
            else -> R.drawable.dettagli
        }
    }

    companion object {
        private const val TAG = "HomeItemActivity"
    }
}