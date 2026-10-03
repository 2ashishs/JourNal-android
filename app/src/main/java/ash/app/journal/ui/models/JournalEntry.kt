package ash.app.journal.ui.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Entity(tableName = "journal_entries")
data class JournalEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val details: String,
    val colorTag: EntryColorTag = EntryColorTag.DEFAULT,
    val mediaType: EntryMediaType = EntryMediaType.TEXT, // Single Source of Truth for type
    val mediaPath: String? = null,  // Nullable: path to internal app storage
    val timestamp: Long = System.currentTimeMillis(),
    val reminderTimestamp: Long? = null,
    val isReminderCompleted: Boolean = false,
    val isPrivateEntry: Boolean = false,
)

val JournalEntry.hasActiveReminder: Boolean
    get() = !isReminderCompleted &&
            reminderTimestamp != null &&
            reminderTimestamp > System.currentTimeMillis()

fun JournalEntry.formattedReminderText(pattern: String = "MMM d, h:mm a"): String? {
    val timestamp = reminderTimestamp ?: return null
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(timestamp))
}

// Checks whether an entry has an expired or finished reminder
val JournalEntry.canRemindAgain: Boolean
    get() = !hasActiveReminder && reminderTimestamp != null

enum class RemindAgainCadence(val label: String) {
    ONE_WEEK("1 week"),
    ONE_MONTH("1 month"),
    ONE_YEAR("1 year")
}

// Computes the next reminder timestamp
fun JournalEntry.calculateNextReminder(cadence: RemindAgainCadence): Long {
    val baseTime = reminderTimestamp ?: System.currentTimeMillis()
    val calendar = Calendar.getInstance().apply {
        timeInMillis = baseTime
    }

    val now = System.currentTimeMillis()
    do {
        when (cadence) {
            RemindAgainCadence.ONE_WEEK -> calendar.add(Calendar.DAY_OF_YEAR, 7)
            RemindAgainCadence.ONE_MONTH -> calendar.add(Calendar.MONTH, 1)
            RemindAgainCadence.ONE_YEAR -> calendar.add(Calendar.YEAR, 1)
        }
    } while (calendar.timeInMillis <= now)

    return calendar.timeInMillis
}