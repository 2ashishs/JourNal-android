package ash.app.journal.ui.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
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
)

val JournalEntry.hasActiveReminder: Boolean
    get() = !isReminderCompleted &&
            reminderTimestamp != null &&
            reminderTimestamp > System.currentTimeMillis()

fun JournalEntry.formattedReminderText(pattern: String = "MMM d, h:mm a"): String? {
    val timestamp = reminderTimestamp ?: return null
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(timestamp))
}