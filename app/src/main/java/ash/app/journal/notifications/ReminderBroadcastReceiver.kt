package ash.app.journal.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import androidx.core.app.NotificationCompat
import ash.app.journal.JournalApplication
import ash.app.journal.MainActivity
import ash.app.journal.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.core.net.toUri

class ReminderBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val entryId = intent.getLongExtra(EXTRA_REMINDER_ENTRY_ID, -1L)
        val title = intent.getStringExtra(EXTRA_REMINDER_ENTRY_TITLE) ?: "Reminder"
        val details = intent.getStringExtra(EXTRA_REMINDER_ENTRY_DETAILS) ?: ""
        val action = intent.action

        if (entryId == -1L) return

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = (entryId % Int.MAX_VALUE).toInt()

        // Custom Sound Uri
        val soundUri =
            "${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/${R.raw.reminder_tone}".toUri()

        // --- BRANCH 1: SNOOZE ACTION CLICKED ---
        if (action == ACTION_SNOOZE) {
            notificationManager.cancel(notificationId)

            val snoozeDuration = 60 * 60 * 1000L
            val newReminderTime = System.currentTimeMillis() + snoozeDuration

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val app = context.applicationContext as JournalApplication
                    val entry = app.repository.getEntryById(entryId)
                    if (entry != null) {
                        app.repository.updateEntry(
                            entry.copy(
                                reminderTimestamp = newReminderTime,
                                isReminderCompleted = false,
                            )
                        )
                    }
                    // Schedule the snoozed alarm
                    ReminderScheduler.scheduleReminder(
                        context = context,
                        entryId = entryId,
                        title = title,
                        details = details,
                        reminderTimeMillis = newReminderTime
                    )
                } finally {
                    pendingResult.finish()
                }
            }
            return
        }

        // --- BRANCH 2: SHOW NOTIFICATION ---
        createNotificationChannel(
            notificationManager,
            soundUri
        ) // pass `context` to extract text from `strings.xml`

        // Update Room so the UI elements instantly drop the reminder pill when reminder completed
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as JournalApplication
                app.repository.markReminderCompleted(entryId)
            } finally {
                pendingResult.finish()
            }
        }

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_REMINDER_ENTRY_ID, entryId)
        }

        val pendingTapIntent = PendingIntent.getActivity(
            context,
            notificationId,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // PendingIntent for the "Snooze" action button
        val snoozeIntent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            this.action = ACTION_SNOOZE
            putExtra(EXTRA_REMINDER_ENTRY_ID, entryId)
            putExtra(EXTRA_REMINDER_ENTRY_TITLE, title)
            putExtra(EXTRA_REMINDER_ENTRY_DETAILS, details)
        }

        val pendingSnoozeIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 100000, // Distinct requestCode for action
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_REMINDER)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(title)
            .setContentText(details.ifBlank { "Journal Reminder" })
            .setStyle(NotificationCompat.BigTextStyle().bigText(details.ifBlank { title }))
            .setCategory(NotificationCompat.CATEGORY_REMINDER) // Survives phone call interruptions
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(soundUri) // Legacy fallback for API < 26
            .setAutoCancel(true)
            .setContentIntent(pendingTapIntent)
            .addAction(R.drawable.ic_alarm, "Snooze (1h)", pendingSnoozeIntent) // Snooze button
            .build()

        notificationManager.notify(notificationId, notification)
    }

    private fun createNotificationChannel(manager: NotificationManager, soundUri: Uri) {
        // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        // Delete old notification channel with default sound
        manager.deleteNotificationChannel(OLD_CHANNEL_ID_REMINDERS)
        // Setup new notification channel with custom sound
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
            .build()
        val channel = NotificationChannel(
            CHANNEL_ID_REMINDER,
            "Journal Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications for scheduled journal reminders"
            enableVibration(true)
            setSound(soundUri, audioAttributes)
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val OLD_CHANNEL_ID_REMINDERS = "journal_reminder_channel"
        const val CHANNEL_ID_REMINDER = "journal_reminders_channel"
        const val EXTRA_REMINDER_ENTRY_ID = "extra_reminder_entry_id"
        const val EXTRA_REMINDER_ENTRY_TITLE = "extra_reminder_entry_title"
        const val EXTRA_REMINDER_ENTRY_DETAILS = "extra_reminder_entry_details"
        const val ACTION_SNOOZE = "ash.app.journal.action.SNOOZE_REMINDER"
    }
}