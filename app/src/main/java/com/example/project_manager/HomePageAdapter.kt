package com.example.project_manager

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter

class HomePageAdapter(private val context: Context, fm: FragmentManager) : FragmentPagerAdapter(fm) {
    override fun getCount(): Int = 4  // Numero di tab: Projects, Chat, Statistics, Profile

    override fun getItem(position: Int): Fragment {
        return when(position) {
            0 -> ItemListFragment.newInstance()  // Projects tab
            1 -> ChatListFragment.newInstance()  // Chat tab
            2 -> StatisticheFragment.newInstance()  // Statistics tab
            3 -> ProfileFragment.newInstance()  // Profile tab
            else -> ItemListFragment.newInstance()
        }
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return when (position) {
            0 -> context.getString(R.string.progetti)
            1 -> context.getString(R.string.chat)
            2 -> context.getString(R.string.statistics)
            3 -> context.getString(R.string.profilo)
            else -> null
        }
    }
}