package com.example.newmad

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

//use to schedule hydration reminders after device reboot
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        if (action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = Prefs.settings(context)
            val enabled = prefs.getBoolean(Prefs.KEY_HYDRATION_ENABLED, false)
            val interval = prefs.getInt(Prefs.KEY_HYDRATION_INTERVAL, 60)
            if (enabled) {
                HydrationScheduler.scheduleRepeating(context, interval)
            }
        }
    }
}
