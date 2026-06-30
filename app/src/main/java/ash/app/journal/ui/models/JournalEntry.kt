package ash.app.journal.ui.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "journal_entries")
data class JournalEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val details: String,
    val hexColor: String? = null,   // Nullable: falls back to default Theme color
    val mediaType: EntryMediaType = EntryMediaType.TEXT, // Single Source of Truth for type
    val mediaPath: String? = null,  // Nullable: path to internal app storage
    val orderIndex: Int,             // For the custom list sorting feature
    val timestamp: Long = System.currentTimeMillis(),
)