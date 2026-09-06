package ash.app.journal.ui.data

import androidx.room.Database
import androidx.room.RoomDatabase
import ash.app.journal.ui.models.JournalEntry
import ash.app.journal.ui.models.LinkMetadataEntity
import ash.app.journal.ui.models.RecentSearchEntity

@Database(
    entities = [JournalEntry::class, LinkMetadataEntity::class, RecentSearchEntity::class],
    version = 7,
    exportSchema = false
)
abstract class JournalDatabase : RoomDatabase() {
    abstract fun journalDao(): JournalDao
    abstract fun linkMetadataDao(): LinkMetadataDao
}