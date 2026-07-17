package ash.app.journal

import android.app.Application
import androidx.room.Room
import ash.app.journal.ui.data.JournalDatabase
import ash.app.journal.ui.data.JournalRepository
import ash.app.journal.ui.data.JournalRepositoryImpl
import ash.app.journal.ui.data.LinkPreviewRepository
import ash.app.journal.ui.data.MIGRATION_1_2
import okhttp3.OkHttpClient

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

    // 1. Core Database Repository
    val repository: JournalRepository by lazy {
        JournalRepositoryImpl(database.journalDao())
    }

    // 2. Shared Network Engine Client
    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .retryOnConnectionFailure(true)
            .build()
    }

    // 3. Link Preview Repository Instance
    val linkPreviewRepository: LinkPreviewRepository by lazy {
        LinkPreviewRepository(okHttpClient)
    }

}