package ash.app.journal.notifications

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object ReminderScheduler {

    @SuppressLint("ScheduleExactAlarm")
    fun scheduleReminder(
        context: Context,
        entryId: Long,
        title: String,
        details: String,
        reminderTimeMillis: Long
    ) {
        if (reminderTimeMillis <= System.currentTimeMillis()) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // On Android 12+, verify exact alarm permission if required
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                // If permission is not granted, fallback to inexact idle window
                val pendingIntent = buildPendingIntent(context, entryId, title, details)
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminderTimeMillis,
                    pendingIntent
                )
                return
            }
        }

        val pendingIntent = buildPendingIntent(context, entryId, title, details)
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            reminderTimeMillis,
            pendingIntent
        )
    }

    fun cancelReminder(context: Context, entryId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderBroadcastReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            entryId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun buildPendingIntent(
        context: Context,
        entryId: Long,
        title: String,
        details: String
    ): PendingIntent {
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            putExtra(ReminderBroadcastReceiver.EXTRA_ENTRY_ID, entryId)
            putExtra(ReminderBroadcastReceiver.EXTRA_ENTRY_TITLE, title)
            putExtra(ReminderBroadcastReceiver.EXTRA_ENTRY_DETAILS, details)
        }
        return PendingIntent.getBroadcast(
            context,
            entryId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}