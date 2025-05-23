package com.example.project_manager

import android.content.Intent
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
    private var isSubitem: String =""

    private val userService = UserService()
    private var startingTab: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home_item_activity)

        getIntentData()
        initializeViews()

        lifecycleScope.launch {
            setupViewPagerWithRole()
            viewPager.currentItem = startingTab
        }
    }

    private fun getIntentData() {
        projectId = intent.getStringExtra("projectId") ?: ""
        taskId = intent.getStringExtra("taskId") ?: ""
        subtaskId = intent.getStringExtra("subtaskId") ?: ""
        isSubitem = intent.getStringExtra("subitem")?:"false"
        startingTab = intent.getIntExtra("startingTab", 0)

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
                showFiles,
                isSubitem
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
            role == Role.Developer && taskId.isNotEmpty() &&subtaskId.isEmpty() -> true
            // Leader can see files for tasks
            role == Role.Leader && taskId.isNotEmpty() -> true
            else -> false
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        if (isSubitem=="true") {
            handleSubitemBackNavigation()
        } else {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun handleSubitemBackNavigation() {
        val intent = Intent(this, HomeItemActivity::class.java).apply {
            when {
                // If we have all three IDs, go back to task view
                subtaskId.isNotEmpty() -> {
                    putExtra("projectId", projectId)
                    putExtra("taskId", taskId)
                    putExtra("subtaskId", "")
                    putExtra("startingTab", 1)
                }
                // If we have project and task IDs, go back to project view
                taskId.isNotEmpty() -> {
                    putExtra("projectId", projectId)
                    putExtra("taskId", "")
                    putExtra("subtaskId", "")
                    putExtra("startingTab", 1)

                }
                // If we only have project ID, just finish()
                else -> {
                    finish()
                    return
                }
            }
        }
        startActivity(intent)
        finish()
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