package com.example.newmad

import android.Manifest
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.materialswitch.MaterialSwitch

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences

    private val REQ_POST_NOTIFICATIONS = 3001

    // common intervals in minutes
    private val intervals = listOf(15, 30, 60, 120)

    private lateinit var switchNotifications: MaterialSwitch
    private lateinit var switchHydration: MaterialSwitch
    private lateinit var spinnerInterval: Spinner
    private lateinit var switchDarkMode: MaterialSwitch
    private lateinit var tvVersion: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        NavigationUtil.setupBottomNav(this, R.id.menu_settings)

        prefs = Prefs.settings(this)

        // View refs
        switchNotifications = findViewById(R.id.switch_notifications)
        switchHydration = findViewById(R.id.switch_hydration)
        spinnerInterval = findViewById(R.id.spinner_hydration_interval)
        switchDarkMode = findViewById(R.id.switch_dark_mode)
        tvVersion = findViewById(R.id.tv_app_version)
        val btnLogout = findViewById<Button>(R.id.button_logout)

        // Populate spinner labels
        val labels = intervals.map { if (it < 60) "${it} min" else "${it / 60} hr" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, labels).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinnerInterval.adapter = adapter

        // Load saved state
        val notificationsEnabled = prefs.getBoolean(Prefs.KEY_NOTIFICATIONS_ENABLED, false)
        val hydrationEnabled = prefs.getBoolean(Prefs.KEY_HYDRATION_ENABLED, false)
        val savedInterval = prefs.getInt(Prefs.KEY_HYDRATION_INTERVAL, 60)
        val darkMode = prefs.getBoolean(Prefs.KEY_DARK_MODE, false)

        switchNotifications.isChecked = notificationsEnabled
        switchHydration.isChecked = hydrationEnabled && notificationsEnabled
        switchDarkMode.isChecked = darkMode
        applyDarkMode(darkMode)

        val selIndex = intervals.indexOf(savedInterval).let { if (it >= 0) it else intervals.indexOf(60).coerceAtLeast(0) }
        spinnerInterval.setSelection(selIndex)
        spinnerInterval.isEnabled = hydrationEnabled && notificationsEnabled
        switchHydration.isEnabled = notificationsEnabled

        // Version text
        tvVersion.text = getVersionLabel()

        // Listeners
        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(Prefs.KEY_NOTIFICATIONS_ENABLED, isChecked).apply()
            handleNotificationsMasterChange(isChecked)
        }

        switchHydration.setOnCheckedChangeListener { _, isChecked ->
            if (!switchNotifications.isChecked) {
                // Shouldn't happen due to disabled state, but guard.
                switchHydration.isChecked = false
                return@setOnCheckedChangeListener
            }
            spinnerInterval.isEnabled = isChecked
            prefs.edit().putBoolean(Prefs.KEY_HYDRATION_ENABLED, isChecked).apply()
            if (isChecked) {
                ensureNotificationsPermissionThen {
                    val interval = intervals[spinnerInterval.selectedItemPosition]
                    HydrationScheduler.scheduleRepeating(this, interval)
                    toast("Hydration reminders enabled")
                }
            } else {
                HydrationScheduler.cancel(this)
                toast("Hydration reminders disabled")
            }
        }

        spinnerInterval.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val interval = intervals[position]
                prefs.edit().putInt(Prefs.KEY_HYDRATION_INTERVAL, interval).apply()
                if (switchHydration.isChecked && switchNotifications.isChecked) {
                    HydrationScheduler.scheduleRepeating(this@SettingsActivity, interval)
                    toast("Reminder interval set to ${if (interval < 60) "$interval min" else "${interval / 60} hr"}")
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(Prefs.KEY_DARK_MODE, isChecked).apply()
            applyDarkMode(isChecked)
        }

        btnLogout.setOnClickListener {
            toast("Logged out (placeholder)")
        }
    }

    private fun handleNotificationsMasterChange(enabled: Boolean) {
        if (enabled) {
            ensureNotificationsPermissionThen {
                switchHydration.isEnabled = true
                if (switchHydration.isChecked) {
                    val interval = intervals[spinnerInterval.selectedItemPosition]
                    HydrationScheduler.scheduleRepeating(this, interval)
                }
                spinnerInterval.isEnabled = switchHydration.isChecked
                toast("Notifications enabled")
            }
        } else {
            // disable subordinate features
            switchHydration.isChecked = false
            switchHydration.isEnabled = false
            spinnerInterval.isEnabled = false
            HydrationScheduler.cancel(this)
            prefs.edit().putBoolean(Prefs.KEY_HYDRATION_ENABLED, false).apply()
            toast("Notifications disabled")
        }
    }

    private fun ensureNotificationsPermissionThen(onGranted: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQ_POST_NOTIFICATIONS)
                // Defer action until result; stash callback? For simplicity, call immediately after grant.
                pendingOnPermissionGranted = onGranted
                return
            }
        }
        onGranted()
    }

    private var pendingOnPermissionGranted: (() -> Unit)? = null

    private fun applyDarkMode(enabled: Boolean) {
        AppCompatDelegate.setDefaultNightMode(
            if (enabled) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    private fun getVersionLabel(): String = try {
        val pInfo = packageManager.getPackageInfo(packageName, 0)
        val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) pInfo.longVersionCode else pInfo.versionCode.toLong()
        "Version ${pInfo.versionName} ($code)"
    } catch (e: Exception) { "Version unknown" }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_POST_NOTIFICATIONS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                toast("Notifications permission granted")
                pendingOnPermissionGranted?.invoke()
                pendingOnPermissionGranted = null
            } else {
                // Revert master switch if this was for master OR hydration enabling
                if (switchNotifications.isChecked.not()) {
                    prefs.edit().putBoolean(Prefs.KEY_NOTIFICATIONS_ENABLED, false).apply()
                }
                // Ensure hydration disabled
                switchHydration.isChecked = false
                switchHydration.isEnabled = false
                spinnerInterval.isEnabled = false
                prefs.edit().putBoolean(Prefs.KEY_HYDRATION_ENABLED, false).apply()
                toast("Notifications permission required")
            }
        }
    }
}
