package com.example.newmad

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class HydrationReminderReceiver : BroadcastReceiver() {
    companion object {
        const val CHANNEL_ID = "hydration_reminder_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_LOG_GLASS = "com.example.newmad.ACTION_LOG_GLASS"
        private const val GLASS_ML = 200
        private const val TAG = "HydrationReceiver"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val receivedAction = intent?.action
        Log.d(TAG, "onReceive action=$receivedAction")
        val settings = Prefs.settings(context)
        val notificationsEnabled = settings.getBoolean(Prefs.KEY_NOTIFICATIONS_ENABLED, false)
        val hydrationEnabled = settings.getBoolean(Prefs.KEY_HYDRATION_ENABLED, false)
        if (!notificationsEnabled || !hydrationEnabled) {
            Log.d(TAG, "Notifications or hydration disabled; aborting notification.")
            return
        }

        if (receivedAction == ACTION_LOG_GLASS) {
            incrementIntake(context, GLASS_ML)
            Log.d(TAG, "Logged one glass via notification action")
            return
        }
        createChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            Log.d(TAG, "POST_NOTIFICATIONS granted=$permGranted")
            if (!permGranted) return
        }
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: Intent(context, HomeActivity::class.java)
        val pending = PendingIntent.getActivity(
            context,0,launchIntent,PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val logIntent = Intent(context, HydrationReminderReceiver::class.java).setAction(ACTION_LOG_GLASS)
        val logPending = PendingIntent.getBroadcast(
            context,1,logIntent,PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.hydration_notification_title))
            .setContentText(context.getString(R.string.hydration_notification_text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pending)
            .addAction(0, context.getString(R.string.hydration_action_log_glass), logPending)
            .setAutoCancel(true)
            .build()
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, notification)
        Log.d(TAG, "Hydration notification posted")
    }

    private fun incrementIntake(context: Context, delta: Int) {
        val prefs = Prefs.settings(context)
        ensureDailyReset(prefs)
        val target = prefs.getInt(Prefs.KEY_HYDRATION_DAILY_TARGET, 1600)
        val updated = (prefs.getInt(Prefs.KEY_HYDRATION_INTAKE, 0) + delta).coerceAtMost(target)
        prefs.edit().putInt(Prefs.KEY_HYDRATION_INTAKE, updated).apply()
        Log.d(TAG, "Intake updated to $updated ml (delta=$delta)")
    }

    private fun ensureDailyReset(prefs: android.content.SharedPreferences) {
        val today = java.time.LocalDate.now().toString()
        val last = prefs.getString(Prefs.KEY_HYDRATION_LAST_DATE, null)
        if (last != today) {
            prefs.edit().putInt(Prefs.KEY_HYDRATION_INTAKE, 0).putString(Prefs.KEY_HYDRATION_LAST_DATE, today).apply()
            Log.d(TAG, "Daily hydration reset")
        }
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val existing = nm.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Hydration Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                )
                channel.description = "Reminders to drink water"
                channel.enableLights(true)
                channel.lightColor = Color.BLUE
                channel.enableVibration(true)
                nm.createNotificationChannel(channel)
                Log.d(TAG, "Notification channel created")
            }
        }
    }
}
