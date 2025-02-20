package com.example.project_manager


import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter

class HomeItemAdapter(
    fm: FragmentManager,
    private val projectId: String,
    private val taskId: String,
    private val subtaskId: String,
    private val showSubitems: Boolean,
    private val showFiles: Boolean
) : FragmentPagerAdapter(fm) {

    override fun getCount(): Int {
        var count = 1 // Always show details
        if (showSubitems) count++
        if (showFiles) count++
        return count
    }

    override fun getItem(position: Int): Fragment {
        return when(position) {
            0 -> DettagliItemFragment.newInstance(projectId, taskId, subtaskId)
            1 -> ItemListFragment.newInstance(projectId, taskId,subtaskId)
            2 -> ItemListFragment.newInstance(projectId, taskId,subtaskId, isFileMode = true)
            else -> DettagliItemFragment.newInstance(projectId, taskId, subtaskId)
        }
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return when(position) {
            0 -> "Details"
            1 -> if (showSubitems) "Subitems" else "Files"
            2 -> "Files"
            else -> null
        }
    }
}