package ash.app.journal

import android.app.Application
import androidx.room.Room
import ash.app.journal.ui.data.JournalDatabase
import ash.app.journal.ui.data.JournalRepository
import ash.app.journal.ui.data.JournalRepositoryImpl
import ash.app.journal.ui.data.LinkMetadataRepository
import ash.app.journal.ui.data.MIGRATION_1_2
import ash.app.journal.ui.data.MIGRATION_2_3
import okhttp3.OkHttpClient

class JournalApplication : Application() {

    // Instantiates database cleanly via lazy properties exactly when accessed
    val database: JournalDatabase by lazy {
        Room.databaseBuilder(
            this,
            JournalDatabase::class.java,
            "journal_database"
        )
            .addMigrations(
                MIGRATION_1_2, // changed `hexColor` to `colorTag` in `journal_entries`
                MIGRATION_2_3 // added table `link_metadata`
            )
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
    val linkMetadataRepository: LinkMetadataRepository by lazy {
        LinkMetadataRepository(okHttpClient, linkMetadataDao = database.linkMetadataDao())
    }

}