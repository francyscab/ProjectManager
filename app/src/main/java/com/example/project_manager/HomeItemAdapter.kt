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
    private val showFiles: Boolean,
    private val isSubitem: String
) : FragmentPagerAdapter(fm) {


    override fun getItem(position: Int): Fragment {
        return when {
            position == 0 -> DettagliItemFragment.newInstance(projectId, taskId, subtaskId)

            showSubitems && position == 1 -> ItemListFragment.newInstance(projectId, taskId, subtaskId) // Subitems

            showSubitems && showFiles && position == 2 -> ItemListFragment.newInstance(projectId, taskId, subtaskId, true) // Files

            !showSubitems && showFiles && position == 1 -> ItemListFragment.newInstance(projectId, taskId, subtaskId, true) // Files quando non ci sono Subitems

            else -> DettagliItemFragment.newInstance(projectId, taskId, subtaskId)
        }
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return when {
            position == 0 -> "Details"

            showSubitems && position == 1 -> "Subitems"

            showSubitems && showFiles && position == 2 -> "Files"

            !showSubitems && showFiles && position == 1 -> "Files"

            else -> null
        }
    }

    override fun getCount(): Int {
        return when {
            showSubitems && showFiles -> 3  // Dettagli + Subitems + Files
            showSubitems -> 2              // Dettagli + Subitems
            showFiles -> 2                 // Dettagli + Files
            else -> 1                      // Solo Dettagli
        }
    }
}