package com.example.project_manager

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.project_manager.models.ItemsViewModel
import com.example.project_manager.models.Role
import com.example.project_manager.services.ProjectService
import com.example.project_manager.services.TaskService
import com.example.project_manager.services.UserService
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class StatisticheActivity : AppCompatActivity() {
    private val projectService = ProjectService()
    private val taskService = TaskService()
    private val userService = UserService()

    private lateinit var completedCountText: TextView
    private lateinit var activeCountText: TextView
    private lateinit var notStartedCountText: TextView
    private lateinit var avgCompletionTimeText: TextView
    private lateinit var barChart: BarChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistiche)

        initializeViews()
        loadStatistics()
    }

    private fun initializeViews() {
        completedCountText = findViewById(R.id.completedCountText)
        activeCountText = findViewById(R.id.activeCountText)
        notStartedCountText = findViewById(R.id.notStartedCountText)
        avgCompletionTimeText = findViewById(R.id.avgCompletionTimeText)
        barChart = findViewById(R.id.barChart)
        setupBarChart()
    }

    private fun loadStatistics() {
        lifecycleScope.launch {
            try {
                val role = userService.getCurrentUserRole()
                val userId = userService.getCurrentUserId()

                if (role == null || userId == null) {
                    Log.e("Statistics", "User role or ID not found")
                    return@launch
                }

                val items = when (role) {
                    Role.Manager -> projectService.loadProjectForUser(userId)
                    Role.Leader -> projectService.loadProjectByLeader(userId)
                    Role.Developer -> taskService.getMyTask(userId)
                }

                updateStatistics(items)
            } catch (e: Exception) {
                Log.e("Statistics", "Error loading statistics", e)
            }
        }
    }

    private fun updateStatistics(items: List<ItemsViewModel>) {
        // Count items by status
        val completed = items.count { it.progress == 100 }
        val active = items.count { it.progress in 1..99 }
        val notStarted = items.count { it.progress == 0 }

        // Calculate average completion time
        val avgCompletionTime = calculateAverageCompletionTime(items)

        // Update UI
        completedCountText.text = completed.toString()
        activeCountText.text = active.toString()
        notStartedCountText.text = notStarted.toString()
        avgCompletionTimeText.text = formatCompletionTime(avgCompletionTime)

        // Update chart data
        updateBarChartData(completed, active, notStarted)
    }

    private fun calculateAverageCompletionTime(items: List<ItemsViewModel>): Long {
        val completedItems = items.filter {
            it.progress == 100 && it.completedAt != null && it.createdAt > 0
        }

        if (completedItems.isEmpty()) return 0

        val totalTime = completedItems.sumOf { item ->
            item.completedAt!! - item.createdAt
        }

        return totalTime / completedItems.size
    }

    private fun formatCompletionTime(averageTimeMillis: Long): String {
        if (averageTimeMillis == 0L) return "N/A"

        val days = TimeUnit.MILLISECONDS.toDays(averageTimeMillis)
        val hours = TimeUnit.MILLISECONDS.toHours(averageTimeMillis) % 24

        return when {
            days > 0 -> "$days days ${hours}h"
            hours > 0 -> "$hours hours"
            else -> "< 1 hour"
        }
    }

    private fun setupBarChart() {
        barChart.apply {
            description.isEnabled = false
            setDrawGridBackground(false)
            setDrawBarShadow(false)
            setScaleEnabled(false)
            setPinchZoom(false)

            xAxis.apply {
                granularity = 1f
                setCenterAxisLabels(true)
                setDrawGridLines(false)
            }

            axisLeft.apply {
                setDrawGridLines(true)
                spaceTop = 35f
                axisMinimum = 0f
            }

            axisRight.isEnabled = false
            legend.isEnabled = true
        }
    }

    private fun updateBarChartData(completed: Int, active: Int, notStarted: Int) {
        val entries = listOf(
            BarEntry(0f, completed.toFloat()),
            BarEntry(1f, active.toFloat()),
            BarEntry(2f, notStarted.toFloat())
        )

        val dataSet = BarDataSet(entries, "Projects/Tasks").apply {
            colors = listOf(
                resources.getColor(R.color.green, theme),
                resources.getColor(R.color.black, theme),
                resources.getColor(R.color.gray, theme)
            )
        }

        val labels = listOf("Completed", "Active", "Not Started")
        barChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)

        val barData = BarData(dataSet).apply {
            barWidth = 0.7f
        }

        barChart.data = barData
        barChart.invalidate()
    }
}