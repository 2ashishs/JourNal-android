package ash.app.journal

import android.app.Application
import androidx.room.Room
import ash.app.journal.ui.data.JournalDatabase
import ash.app.journal.ui.data.JournalRepository
import ash.app.journal.ui.data.JournalRepositoryImpl
import ash.app.journal.ui.data.MIGRATION_1_2

class JournalApplication : Application() {

    // Instantiates database cleanly via lazy properties exactly when accessed
    val database: JournalDatabase by lazy {
        Room.databaseBuilder(
            this,
            JournalDatabase::class.java,
            "journal_database"
        )
            .addMigrations(MIGRATION_1_2) //Db migration: changed `hexColor` to `colorTag`
            .build()
    }

    val repository: JournalRepository by lazy {
        JournalRepositoryImpl(database.journalDao())
    }
}