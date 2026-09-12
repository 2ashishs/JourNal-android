package ash.app.journal.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ash.app.journal.JournalApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val app = context.applicationContext as JournalApplication
                    val pendingReminders = app.repository.getPendingReminders()

                    pendingReminders.forEach { entry ->
                        val reminderTime = entry.reminderTimestamp ?: return@forEach
                        ReminderScheduler.scheduleReminder(
                            context = context,
                            entryId = entry.id,
                            title = entry.title,
                            details = entry.details,
                            reminderTimeMillis = reminderTime
                        )
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
