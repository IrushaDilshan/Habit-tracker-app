package com.example.newmad

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.progressindicator.CircularProgressIndicator
import java.time.LocalDate

class HydrationFragment : Fragment() {

    private lateinit var prefs: android.content.SharedPreferences

    private var progressIndicator: CircularProgressIndicator? = null
    private var percentView: TextView? = null
    private var amountView: TextView? = null
    private var goalView: TextView? = null
    private var switchEnabled: MaterialSwitch? = null
    private var spinnerInterval: Spinner? = null
    private var btnGlass: Button? = null
    private var btnDouble: Button? = null
    private var btnCustom: Button? = null
    private var btnSetTarget: Button? = null
    private var btnReset: Button? = null

    private val intervals = listOf(15, 30, 60, 120) // minutes
    private val intervalLabels by lazy { intervals.map { if (it < 60) "${it} min" else "${it/60} hr" } }

    private val defaultTargetMl = 1600
    private val glassMl = 200

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs.settings(requireContext())
        ensureDailyReset()
        if (!prefs.contains(Prefs.KEY_HYDRATION_DAILY_TARGET)) {
            prefs.edit().putInt(Prefs.KEY_HYDRATION_DAILY_TARGET, defaultTargetMl).apply()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_hydration, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        progressIndicator = view.findViewById(R.id.hydration_progress_indicator)
        percentView = view.findViewById(R.id.tv_progress_percent)
        amountView = view.findViewById(R.id.tv_progress_amount)
        goalView = view.findViewById(R.id.tv_daily_goal)
        switchEnabled = view.findViewById(R.id.switch_hydration_enabled)
        spinnerInterval = view.findViewById(R.id.spinner_interval)
        btnGlass = view.findViewById(R.id.btn_log_glass)
        btnDouble = view.findViewById(R.id.btn_log_double)
        btnCustom = view.findViewById(R.id.btn_log_custom)
        btnSetTarget = view.findViewById(R.id.btn_set_target)
        btnReset = view.findViewById(R.id.btn_reset_day)

        // Spinner setup
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, intervalLabels).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinnerInterval?.adapter = adapter

        val enabled = prefs.getBoolean(Prefs.KEY_HYDRATION_ENABLED, false)
        val interval = prefs.getInt(Prefs.KEY_HYDRATION_INTERVAL, 60)
        val index = intervals.indexOf(interval).let { if (it>=0) it else intervals.indexOf(60).coerceAtLeast(0) }
        spinnerInterval?.setSelection(index)
        switchEnabled?.isChecked = enabled
        spinnerInterval?.isEnabled = enabled

        updateProgressUI()

        btnGlass?.setOnClickListener { addIntake(glassMl) }
        btnDouble?.setOnClickListener { addIntake(glassMl * 2) }
        btnCustom?.setOnClickListener { promptCustomAmount() }
        btnSetTarget?.setOnClickListener { promptSetTarget() }
        btnReset?.setOnClickListener { resetToday() }

        switchEnabled?.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(Prefs.KEY_HYDRATION_ENABLED, isChecked).apply()
            spinnerInterval?.isEnabled = isChecked
            if (isChecked) {
                ensureNotificationPermission {
                    // Ensure master notifications also enabled or receiver will bail out.
                    prefs.edit().putBoolean(Prefs.KEY_NOTIFICATIONS_ENABLED, true).apply()
                    val value = intervals[spinnerInterval?.selectedItemPosition ?: 0]
                    prefs.edit().putInt(Prefs.KEY_HYDRATION_INTERVAL, value).apply()
                    HydrationScheduler.scheduleRepeating(requireContext(), value, immediateFirst = true)
                    // Fire an on-demand test broadcast if user wants instant confirmation (already scheduled immediate alarm but this guarantees fast feedback)
                    requireContext().sendBroadcast(Intent(requireContext(), HydrationReminderReceiver::class.java))
                    toast(getString(R.string.hydration_reminders_enabled))
                }
            } else {
                HydrationScheduler.cancel(requireContext())
                toast(getString(R.string.hydration_reminders_disabled))
            }
        }

        spinnerInterval?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, position: Int, id: Long) {
                val value = intervals[position]
                prefs.edit().putInt(Prefs.KEY_HYDRATION_INTERVAL, value).apply()
                if (switchEnabled?.isChecked == true) {
                    HydrationScheduler.scheduleRepeating(requireContext(), value, immediateFirst = false)
                    toast(getString(R.string.hydration_interval_set))
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun addIntake(delta: Int) {
        ensureDailyReset()
        val target = prefs.getInt(Prefs.KEY_HYDRATION_DAILY_TARGET, defaultTargetMl)
        var current = prefs.getInt(Prefs.KEY_HYDRATION_INTAKE, 0)
        current += delta
        if (current > target) current = target
        prefs.edit().putInt(Prefs.KEY_HYDRATION_INTAKE, current).apply()
        updateProgressUI()
    }

    private fun promptCustomAmount() {
        val input = EditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            hint = getString(R.string.hydration_dialog_hint_ml)
        }
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.hydration_dialog_log_title))
            .setView(input)
            .setPositiveButton(getString(R.string.hydration_dialog_add)) { d,_ ->
                val ml = input.text.toString().toIntOrNull()
                if (ml != null && ml > 0) addIntake(ml) else toast(getString(R.string.hydration_invalid_amount))
                d.dismiss()
            }
            .setNegativeButton(getString(R.string.hydration_dialog_cancel), null)
            .show()
    }

    private fun promptSetTarget() {
        val input = EditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            hint = getString(R.string.hydration_dialog_hint_target)
            setText(prefs.getInt(Prefs.KEY_HYDRATION_DAILY_TARGET, defaultTargetMl).toString())
        }
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.hydration_dialog_set_title))
            .setView(input)
            .setPositiveButton(getString(R.string.hydration_dialog_save)) { d,_ ->
                val ml = input.text.toString().toIntOrNull()
                if (ml != null && ml >= glassMl) {
                    prefs.edit().putInt(Prefs.KEY_HYDRATION_DAILY_TARGET, ml).apply()
                    if (prefs.getInt(Prefs.KEY_HYDRATION_INTAKE,0) > ml) {
                        prefs.edit().putInt(Prefs.KEY_HYDRATION_INTAKE, ml).apply()
                    }
                    updateProgressUI()
                } else toast(getString(R.string.hydration_invalid_target))
                d.dismiss()
            }
            .setNegativeButton(getString(R.string.hydration_dialog_cancel), null)
            .show()
    }

    private fun resetToday() {
        prefs.edit().putInt(Prefs.KEY_HYDRATION_INTAKE, 0)
            .putString(Prefs.KEY_HYDRATION_LAST_DATE, LocalDate.now().toString())
            .apply()
        updateProgressUI()
    }

    private fun updateProgressUI() {
        ensureDailyReset()
        val target = prefs.getInt(Prefs.KEY_HYDRATION_DAILY_TARGET, defaultTargetMl)
        val current = prefs.getInt(Prefs.KEY_HYDRATION_INTAKE, 0)
        val percent = if (target == 0) 0 else (current * 100 / target).coerceIn(0,100)
        // Use compat API for determinate progress
        progressIndicator?.setProgressCompat(percent, true)
        percentView?.text = getString(R.string.hydration_progress_percent, percent)
        amountView?.text = getString(R.string.hydration_progress_amount, current, target)
        goalView?.text = getString(R.string.hydration_daily_goal_label, target)
    }

    private fun ensureDailyReset() {
        val today = LocalDate.now().toString()
        val last = prefs.getString(Prefs.KEY_HYDRATION_LAST_DATE, null)
        if (last != today) {
            prefs.edit().putInt(Prefs.KEY_HYDRATION_INTAKE, 0).putString(Prefs.KEY_HYDRATION_LAST_DATE, today).apply()
        }
    }

    private fun ensureNotificationPermission(onGranted: ()->Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 5002)
                pendingPermissionCallback = {
                    // Also set master notifications when permission is granted
                    prefs.edit().putBoolean(Prefs.KEY_NOTIFICATIONS_ENABLED, true).apply()
                    onGranted()
                }
                return
            }
        }
        // Already granted
        prefs.edit().putBoolean(Prefs.KEY_NOTIFICATIONS_ENABLED, true).apply()
        onGranted()
    }

    private var pendingPermissionCallback: (()->Unit)? = null

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 5002) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pendingPermissionCallback?.invoke()
            } else {
                switchEnabled?.isChecked = false
                toast(getString(R.string.hydration_permission_required))
            }
            pendingPermissionCallback = null
        }
    }

    private fun toast(msg: String) = Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
}
