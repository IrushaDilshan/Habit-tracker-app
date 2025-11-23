package com.example.newmad

import android.content.Context
import android.content.SharedPreferences

/**
 * Centralized SharedPreferences access and keys.
 * Move all prefs names and common keys here so other classes import this single helper.
 */
object Prefs {
    // Pref file names
    private const val PREFS_SETTINGS = "settings_prefs"
    private const val PREFS_HABITS = "habits_prefs"
    private const val PREFS_MOOD = "mood_entries"
    private const val PREFS_HABIT_HISTORY = "habit_history_prefs"

    // Settings keys
    const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
    const val KEY_HYDRATION_ENABLED = "hydration_enabled"
    const val KEY_HYDRATION_INTERVAL = "hydration_interval"
    const val KEY_DARK_MODE = "dark_mode"
    // Added hydration tracking keys
    const val KEY_HYDRATION_DAILY_TARGET = "hydration_daily_target" // int (ml)
    const val KEY_HYDRATION_INTAKE = "hydration_intake" // int (ml consumed today)
    const val KEY_HYDRATION_LAST_DATE = "hydration_last_date" // ISO yyyy-MM-dd

    // Habits keys
    const val KEY_HABITS = "habits_json"
    const val KEY_LAST_DATE = "habits_last_date"

    // Expose SharedPreferences objects for each logical area
    fun settings(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE)

    fun habits(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_HABITS, Context.MODE_PRIVATE)

    fun mood(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_MOOD, Context.MODE_PRIVATE)

    fun habitHistory(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_HABIT_HISTORY, Context.MODE_PRIVATE)
}
