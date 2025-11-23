package com.example.newmad

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.progressindicator.CircularProgressIndicator
import org.json.JSONArray

class HomeActivity : AppCompatActivity(), HabitActionListener {

    private lateinit var habitAdapter: HabitAdapter
    private lateinit var prefs: SharedPreferences
    private val TAG = "HomeActivity"

    // Summary views
    private var totalView: TextView? = null
    private var completedView: TextView? = null
    private var remainingView: TextView? = null
    private var progressCircle: CircularProgressIndicator? = null
    private var progressText: TextView? = null
    private var hydrationSwitch: MaterialSwitch? = null
    private var hydrationProgressView: TextView? = null

    private lateinit var habitActivityLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home)
        try {
            NavigationUtil.setupBottomNav(this, R.id.menu_habits)
            prefs = Prefs.habits(this) // separate prefs for habits

            totalView = findViewById(R.id.home_total)
            completedView = findViewById(R.id.home_completed)
            remainingView = findViewById(R.id.home_remaining)
            progressCircle = findViewById(R.id.home_progress_circle)
            progressText = findViewById(R.id.home_progress_percent)
            hydrationSwitch = findViewById(R.id.hydration_switch)
            hydrationProgressView = findViewById(R.id.hydration_progress_home)
            val hydrationCard: android.view.View? = findViewById(R.id.hydration_card)
            val habitListView = findViewById<RecyclerView>(R.id.home_habit_list)

            habitListView.layoutManager = LinearLayoutManager(this)
            habitAdapter = HabitAdapter(loadHabits(), this)
            habitListView.adapter = habitAdapter

            // Hydration switch reflects settings (read-only state here; tap opens Settings)
            val settingsPrefs = Prefs.settings(this)
            val hydrationEnabled = settingsPrefs.getBoolean(Prefs.KEY_HYDRATION_ENABLED, false)
            hydrationSwitch?.isChecked = hydrationEnabled
            updateHydrationProgress(settingsPrefs)
            hydrationSwitch?.setOnCheckedChangeListener { _, _ ->
                // Direct changes not allowed here; navigate to Settings
                startActivity(Intent(this, SettingsActivity::class.java))
            }
            hydrationCard?.setOnClickListener {
                // Open hydration details screen (fragment host)
                startActivity(Intent(this, HydrationActivity::class.java))
            }

            // Register result launcher for add/edit
            habitActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK && result.data != null) {
                    handleHabitActivityResult(result.data!!)
                }
            }

            findViewById<ExtendedFloatingActionButton>(R.id.home_add_habit).setOnClickListener {
                launchAddHabit()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing HomeActivity", e)
            Toast.makeText(this, "Error loading home screen", Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::habitAdapter.isInitialized) {
            ensureTodayState()
            ensureHydrationTodayState()
            val saved = loadHabits()
            habitAdapter.setAll(saved)
            updateSummary(saved)
            val settingsPrefs = Prefs.settings(this)
            hydrationSwitch?.isChecked = settingsPrefs.getBoolean(Prefs.KEY_HYDRATION_ENABLED, false)
            updateHydrationProgress(settingsPrefs)
        }
    }

    private fun ensureTodayState() {
        val today = java.time.LocalDate.now().toString()
        val last = prefs.getString(Prefs.KEY_LAST_DATE, null)
        if (last != today) {
            // New day -> reset completion flags
            val habits = loadHabits()
            var changed = false
            for (h in habits) if (h.completed) { h.completed = false; changed = true }
            if (changed) saveHabits(habits)
            prefs.edit().putString(Prefs.KEY_LAST_DATE, today).apply()
        }
    }

    private fun ensureHydrationTodayState() {
        val settingsPrefs = Prefs.settings(this)
        val today = java.time.LocalDate.now().toString()
        val last = settingsPrefs.getString(Prefs.KEY_HYDRATION_LAST_DATE, null)
        if (last != today) {
            settingsPrefs.edit()
                .putInt(Prefs.KEY_HYDRATION_INTAKE, 0)
                .putString(Prefs.KEY_HYDRATION_LAST_DATE, today)
                .apply()
        }
    }

    private fun updateHydrationProgress(settingsPrefs: SharedPreferences) {
        val intake = settingsPrefs.getInt(Prefs.KEY_HYDRATION_INTAKE, 0)
        val target = settingsPrefs.getInt(Prefs.KEY_HYDRATION_DAILY_TARGET, 1600)
        hydrationProgressView?.text = getString(R.string.hydration_progress_compact, intake, target)
    }

    // HabitActionListener
    override fun onEdit(habit: Habit, position: Int) {
        launchEditHabit(habit)
    }

    override fun onDataChanged(habits: List<Habit>) {
        updateSummary(habits)
        saveHabits(habits)
    }

    private fun launchAddHabit() {
        val intent = Intent(this, AddHabitActivity::class.java).apply {
            putExtra(AddHabitActivity.EXTRA_MODE, AddHabitActivity.MODE_ADD)
        }
        habitActivityLauncher.launch(intent)
    }

    private fun launchEditHabit(habit: Habit) {
        val intent = Intent(this, AddHabitActivity::class.java).apply {
            putExtra(AddHabitActivity.EXTRA_MODE, AddHabitActivity.MODE_EDIT)
            putExtra(AddHabitActivity.EXTRA_HABIT_ID, habit.id)
            putExtra(AddHabitActivity.EXTRA_HABIT_NAME, habit.name)
            putExtra(AddHabitActivity.EXTRA_HABIT_DESC, habit.description)
            putExtra(AddHabitActivity.EXTRA_HABIT_COMPLETED, habit.completed)
        }
        habitActivityLauncher.launch(intent)
    }

    private fun handleHabitActivityResult(data: Intent) {
        val mode = data.getStringExtra(AddHabitActivity.EXTRA_MODE)
        val id = data.getStringExtra(AddHabitActivity.EXTRA_HABIT_ID)
        val name = data.getStringExtra(AddHabitActivity.EXTRA_HABIT_NAME) ?: return
        val desc = data.getStringExtra(AddHabitActivity.EXTRA_HABIT_DESC)
        val completed = data.getBooleanExtra(AddHabitActivity.EXTRA_HABIT_COMPLETED, false)
        if (mode == AddHabitActivity.MODE_EDIT && id != null) {
            // Find existing habit by id
            val current = loadHabits()
            val index = current.indexOfFirst { it.id == id }
            if (index >= 0) {
                current[index] = Habit(name, desc, completed, id)
                habitAdapter.setAll(current)
                saveHabits(current)
                Toast.makeText(this, "Habit updated", Toast.LENGTH_SHORT).show()
                return
            }
        }
        // Add new habit
        val list = loadHabits()
        list.add(Habit(name, desc, completed))
        habitAdapter.setAll(list)
        saveHabits(list)
        Toast.makeText(this, "Habit added", Toast.LENGTH_SHORT).show()
    }

    private fun updateSummary(habits: List<Habit>) {
        val total = habits.size
        val completed = habits.count { it.completed }
        val remaining = total - completed
        val percent = if (total == 0) 0 else (completed * 100 / total)
        // Record daily snapshot for charts
        HabitHistoryManager(this).recordToday(total, completed)
        runOnUiThread {
            totalView?.text = total.toString()
            completedView?.text = completed.toString()
            remainingView?.text = remaining.toString()
            progressText?.text = "$percent%\nComplete"
            progressCircle?.setProgress(percent)
        }
    }

    private fun loadHabits(): MutableList<Habit> {
        val str = prefs.getString(Prefs.KEY_HABITS, null) ?: return mutableListOf()
        val arr = try { JSONArray(str) } catch (e: Exception) { Log.w(TAG, "Failed to parse habits JSON", e); return mutableListOf() }
        val list = mutableListOf<Habit>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            try { list.add(Habit.fromJson(obj)) } catch (e: Exception) { Log.w(TAG, "Failed to parse habit at index $i", e) }
        }
        return list
    }

    // Save the full list of habits
    private fun saveHabits(habits: List<Habit>) {
        val arr = JSONArray()
        for (h in habits) arr.put(h.toJson())
        prefs.edit().putString(Prefs.KEY_HABITS, arr.toString()).commit()
        Log.d(TAG, "Saved ${habits.size} habits")
    }
}