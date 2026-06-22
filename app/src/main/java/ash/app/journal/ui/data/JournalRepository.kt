package ash.app.journal.ui.data

import ash.app.journal.ui.models.JournalEntry
import kotlinx.coroutines.flow.Flow

interface JournalRepository {
    fun getAllEntries(): Flow<List<JournalEntry>>
    suspend fun insertEntry(entry: JournalEntry): Long
    suspend fun deleteEntry(entry: JournalEntry)
    suspend fun updateEntries(entries: List<JournalEntry>)
}