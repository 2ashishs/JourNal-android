package ash.app.journal.ui.data

import androidx.room.Database
import androidx.room.RoomDatabase
import ash.app.journal.ui.models.JournalEntry
import ash.app.journal.ui.models.LinkMetadataEntity

@Database(entities = [JournalEntry::class, LinkMetadataEntity::class], version = 3, exportSchema = false)
abstract class JournalDatabase : RoomDatabase() {
    abstract fun journalDao(): JournalDao
    abstract fun linkMetadataDao(): LinkMetadataDao
}