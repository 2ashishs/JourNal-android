package ash.app.journal.ui.data

import ash.app.journal.ui.models.JournalEntry
import kotlinx.coroutines.flow.Flow

class JournalRepositoryImpl(
    private val journalDao: JournalDao
) : JournalRepository {

    override fun getAllEntries(): Flow<List<JournalEntry>> {
        return journalDao.getAllEntries()
    }

    override suspend fun insertEntry(entry: JournalEntry): Long {
        return journalDao.insertEntry(entry)
    }

    override suspend fun deleteEntry(entry: JournalEntry) {
        journalDao.deleteEntry(entry)
    }

    override suspend fun updateEntries(entries: List<JournalEntry>) {
        journalDao.updateEntries(entries)
    }
}