package ash.app.journal.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import ash.app.journal.MainActivity
import ash.app.journal.R

class ReminderBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val entryId = intent.getLongExtra(EXTRA_REMINDER_ENTRY_ID, -1L)
        val title = intent.getStringExtra(EXTRA_REMINDER_ENTRY_TITLE) ?: "Reminder"
        val details = intent.getStringExtra(EXTRA_REMINDER_ENTRY_DETAILS) ?: ""

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel(context, notificationManager)

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_REMINDER_ENTRY_ID, entryId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            (entryId % Int.MAX_VALUE).toInt(), // for the edge case that number of entries exceed 2 billion
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_REMINDERS)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(title)
            .setContentText(details.ifBlank { "You have a scheduled journal reminder" })
            .setStyle(NotificationCompat.BigTextStyle().bigText(details))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(entryId.toInt(), notification)
    }

    private fun createNotificationChannel(context: Context, manager: NotificationManager) {
        val channel = NotificationChannel(
            CHANNEL_ID_REMINDERS,
            "Journal Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications for scheduled journal entry reminders"
            enableVibration(true)
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID_REMINDERS = "journal_reminders_channel"
        const val EXTRA_REMINDER_ENTRY_ID = "extra_reminder_entry_id"
        const val EXTRA_REMINDER_ENTRY_TITLE = "extra_reminder_entry_title"
        const val EXTRA_REMINDER_ENTRY_DETAILS = "extra_reminder_entry_details"
    }
}