package ash.app.journal.ui.data

import androidx.room.Database
import androidx.room.RoomDatabase
import ash.app.journal.ui.models.JournalEntry

@Database(entities = [JournalEntry::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun journalDao(): JournalDao
}