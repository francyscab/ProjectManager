package com.example.project_manager

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.project_manager.ItemListFragment.Companion
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
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit


private const val ARG_USER_ID = "userId"
private const val ARG_USER_ROLE = "userRole"

class StatisticheFragment : Fragment() {

    private var userId: String? = null
    private var userRole: Role? = null

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
        arguments?.let {
            userId = it.getString(ARG_USER_ID)
            userRole = it.getString(ARG_USER_ROLE)?.let { role -> Role.valueOf(role) }
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.statistic_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews(view)
        lifecycleScope.launch {
            observeDataChanges()
        }
    }

    private suspend fun observeDataChanges() {
        loadStatistics()
        val db = FirebaseFirestore.getInstance()
        db.collection("progetti")  // Cambia il nome della collezione se necessario
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e(StatisticheFragment.TAG, "Errore durante l'ascolto delle modifiche", error)
                    return@addSnapshotListener
                }

                if (snapshots != null && !snapshots.isEmpty) {
                    lifecycleScope.launch {
                        loadStatistics()  // Ricarica i dati ogni volta che ci sono modifiche
                    }
                }
            }
    }

    private fun initializeViews(view: View) {
        completedCountText = view.findViewById(R.id.completedCountText)
        activeCountText = view.findViewById(R.id.activeCountText)
        notStartedCountText = view.findViewById(R.id.notStartedCountText)
        avgCompletionTimeText = view.findViewById(R.id.avgCompletionTimeText)
        barChart = view.findViewById(R.id.barChart)
        setupBarChart()
    }

    private fun loadStatistics() {
        viewLifecycleOwner.lifecycleScope.launch {
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

    private fun setupBarChart() {
        barChart.apply {
            description.isEnabled = false
            setDrawGridBackground(false)
            setDrawBarShadow(false)
            setScaleEnabled(false)
            setPinchZoom(false)

            xAxis.apply {
                textColor = Color.WHITE
                granularity = 1f
                setCenterAxisLabels(true)
                setDrawGridLines(false)
            }

            axisLeft.apply {
                textColor = Color.WHITE
                setDrawGridLines(true)
                spaceTop = 35f
                axisMinimum = 0f
            }

            axisRight.isEnabled = false
            legend.apply {
                isEnabled = true
                textColor = Color.WHITE
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

    private fun updateBarChartData(completed: Int, active: Int, notStarted: Int) {
        val entries = listOf(
            BarEntry(0f, completed.toFloat()),
            BarEntry(1f, active.toFloat()),
            BarEntry(2f, notStarted.toFloat())
        )

        val dataSet = BarDataSet(entries, "Projects/Tasks").apply {
            colors = listOf(
                ContextCompat.getColor(requireContext(), R.color.progress_foreground),
                ContextCompat.getColor(requireContext(), R.color.progress_foreground_darker),
                ContextCompat.getColor(requireContext(), R.color.progress_foreground_darkest)
            )
        }

        dataSet.apply {
            setValueTextColor(Color.WHITE)
            setValueTextSize(12f)
        }

        val labels = listOf("Completed", "Active", "Not Started")
        barChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)

        val barData = BarData(dataSet).apply {
            barWidth = 0.7f
        }

        barChart.data = barData
        barChart.invalidate()
    }

    companion object {
        private const val TAG = "StatisticheFragment"

        @JvmStatic
        fun newInstance(userId: String? = null, userRole: String? = null) =
            StatisticheFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_USER_ID, userId)
                    putString(ARG_USER_ROLE, userRole)
                }
            }
    }
}