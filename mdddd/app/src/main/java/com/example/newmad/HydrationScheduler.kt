package com.example.newmad
// Schedules and cancels hydration reminder alarms
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock

object HydrationScheduler {
    private const val REQUEST_CODE = 2001
    private const val REQUEST_CODE_IMMEDIATE = 2002
    private const val FIRST_DELAY_MS = 10_000L // 10 seconds for quick user feedback

    fun scheduleRepeating(context: Context, intervalMinutes: Int, immediateFirst: Boolean = true) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, HydrationReminderReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Cancel existing repeating alarm
        am.cancel(pi)

        val intervalMillis = intervalMinutes * 60 * 1000L
        val triggerAt = SystemClock.elapsedRealtime() + intervalMillis

        am.setInexactRepeating(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            triggerAt,
            intervalMillis,
            pi
        )

        if (immediateFirst) {
            val immediateIntent = Intent(context, HydrationReminderReceiver::class.java)
            val immediatePi = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_IMMEDIATE,
                immediateIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            // Cancel any stale immediate alarm
            am.cancel(immediatePi)
            am.setExactAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + FIRST_DELAY_MS,
                immediatePi
            )
        }
    }

    fun cancel(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, HydrationReminderReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        am.cancel(pi)
        pi.cancel()
        // Also cancel immediate alarm
        val immediateIntent = Intent(context, HydrationReminderReceiver::class.java)
        val immediatePi = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_IMMEDIATE,
            immediateIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        am.cancel(immediatePi)
        immediatePi.cancel()
    }
}
