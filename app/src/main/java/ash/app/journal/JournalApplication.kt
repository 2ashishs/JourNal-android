package ash.app.journal

import android.app.Application
import androidx.room.Room
import ash.app.journal.ui.data.AppDatabase
import ash.app.journal.ui.data.JournalRepository
import ash.app.journal.ui.data.JournalRepositoryImpl

class JournalApplication : Application() {

    // Instantiates database cleanly via lazy properties exactly when accessed
    val database: AppDatabase by lazy {
        Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "journal_database"
        ).build()
    }

    val repository: JournalRepository by lazy {
        JournalRepositoryImpl(database.journalDao())
    }
}