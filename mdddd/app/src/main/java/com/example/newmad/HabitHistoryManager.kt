package com.example.newmad

import android.content.Context
import org.json.JSONObject
import java.time.LocalDate

/**
 * Stores daily habit completion snapshots in SharedPreferences.
 * Key format: habit_hist_YYYY-MM-DD -> JSON { total:Int, completed:Int }
 */
class HabitHistoryManager(context: Context) {
    private val prefs = Prefs.habitHistory(context)

    fun recordToday(total: Int, completed: Int) {
        val today = LocalDate.now()
        recordDay(today, total, completed)
    }

    fun recordDay(day: LocalDate, total: Int, completed: Int) {
        val key = keyFor(day)
        val obj = JSONObject()
        obj.put("total", total)
        obj.put("completed", completed)
        prefs.edit().putString(key, obj.toString()).apply()
    }

    fun getCompletionPercent(day: LocalDate): Int {
        val key = keyFor(day)
        val str = prefs.getString(key, null) ?: return 0
        return try {
            val obj = JSONObject(str)
            val total = obj.optInt("total", 0)
            val completed = obj.optInt("completed", 0)
            if (total <= 0) 0 else (completed * 100 / total)
        } catch (_: Exception) { 0 }
    }

    fun getLastNDaysPercents(n: Int): List<Pair<LocalDate, Int>> {
        val list = mutableListOf<Pair<LocalDate, Int>>()
        val today = LocalDate.now()
        for (i in n - 1 downTo 0) {
            val day = today.minusDays(i.toLong())
            list.add(day to getCompletionPercent(day))
        }
        return list
    }

    private fun keyFor(day: LocalDate) = "habit_hist_${day}" // Correct interpolation so each day is unique
}
