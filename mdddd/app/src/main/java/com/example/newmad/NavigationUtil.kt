package com.example.newmad

import android.app.Activity
import android.content.Intent
import com.google.android.material.bottomnavigation.BottomNavigationView

object NavigationUtil {
    fun setupBottomNav(activity: Activity, selectedId: Int) {
        val bottomNav = activity.findViewById<BottomNavigationView>(R.id.home_bottom_nav) ?: return
        // Ensure the selected menu item is checked
        try {
            bottomNav.menu.findItem(selectedId).isChecked = true
        } catch (e: Exception) {
            // ignore
        }

        // New API (Material 1.4+)
        try {
            bottomNav.setOnItemSelectedListener { item ->
                handleNavSelection(activity, selectedId, item.itemId)
            }
        } catch (e: NoSuchMethodError) {
            // fall back to older API below
        }

        // Older Design library API
        try {
            bottomNav.setOnNavigationItemSelectedListener { item ->
                handleNavSelection(activity, selectedId, item.itemId)
            }
        } catch (e: NoSuchMethodError) {
            // ignore if not available
        }
    }

    private fun handleNavSelection(activity: Activity, selectedId: Int, itemId: Int): Boolean {
        when (itemId) {
            R.id.menu_habits -> {
                if (selectedId != R.id.menu_habits) {
                    activity.startActivity(Intent(activity, HomeActivity::class.java))
                    activity.finish()
                }
                return true
            }
            R.id.menu_mood -> {
                if (selectedId != R.id.menu_mood) {
                    activity.startActivity(Intent(activity, MoodJournalActivity::class.java))
                    activity.finish()
                }
                return true
            }
            R.id.menu_calendar -> {
                if (selectedId != R.id.menu_calendar) {
                    activity.startActivity(Intent(activity, CalendarActivity::class.java))
                    activity.finish()
                }
                return true
            }
            R.id.menu_charts -> {
                if (selectedId != R.id.menu_charts) {
                    activity.startActivity(Intent(activity, ChartsActivity::class.java))
                    activity.finish()
                }
                return true
            }
            R.id.menu_settings -> {
                if (selectedId != R.id.menu_settings) {
                    activity.startActivity(Intent(activity, SettingsActivity::class.java))
                    activity.finish()
                }
                return true
            }
            else -> return false
        }
    }
}
