package ash.app.journal.ui.data

import ash.app.journal.ui.models.ColorTagCount
import ash.app.journal.ui.models.EntryColorTag
import ash.app.journal.ui.models.EntryMediaType
import ash.app.journal.ui.models.JournalEntry
import ash.app.journal.ui.models.MediaTypeCount
import ash.app.journal.ui.models.RecentSearchEntity
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

    override suspend fun updateEntry(entry: JournalEntry) {
        journalDao.updateEntry(entry)
    }

    override suspend fun updateEntries(entries: List<JournalEntry>) {
        journalDao.updateEntries(entries)
    }

    override suspend fun getEntryById(id: Long): JournalEntry? {
        return journalDao.getEntryById(id)
    }

    override suspend fun markReminderCompleted(entryId: Long) {
        journalDao.markReminderCompleted(entryId)
    }

    override suspend fun getPendingReminders(): List<JournalEntry> {
        return journalDao.getPendingReminders(System.currentTimeMillis())
    }

    //SEARCH

    override fun searchEntries(
        query: String,
        colorTag: EntryColorTag?,
        mediaType: EntryMediaType?,
        onlyActiveReminders: Boolean,
        onlyPrivate: Boolean,
        currentTime: Long,
    ): Flow<List<JournalEntry>> = journalDao.searchEntries(
        query,
        query.trim(),
        colorTag,
        mediaType,
        onlyActiveReminders,
        onlyPrivate,
        currentTime
    )

    override fun getFacetedColorTagCounts(
        mediaType: EntryMediaType?,
        onlyActiveReminders: Boolean,
        onlyPrivate: Boolean,
        currentTime: Long
    ): Flow<List<ColorTagCount>> = journalDao.getFacetedColorTagCounts(
        mediaType, onlyActiveReminders, onlyPrivate, currentTime
    )

    override fun getFacetedMediaTypeCounts(
        colorTag: EntryColorTag?,
        onlyActiveReminders: Boolean,
        onlyPrivate: Boolean,
        currentTime: Long
    ): Flow<List<MediaTypeCount>> = journalDao.getFacetedMediaTypeCounts(
        colorTag, onlyActiveReminders, onlyPrivate, currentTime
    )

    override fun getFacetedActiveRemindersCount(
        colorTag: EntryColorTag?,
        mediaType: EntryMediaType?,
        onlyPrivate: Boolean,
        currentTime: Long
    ): Flow<Int> = journalDao.getFacetedActiveRemindersCount(
        colorTag, mediaType, onlyPrivate, currentTime
    )

    override fun getFacetedPrivateEntriesCount(
        colorTag: EntryColorTag?,
        mediaType: EntryMediaType?,
        onlyActiveReminders: Boolean,
        currentTime: Long
    ): Flow<Int> = journalDao.getFacetedPrivateEntriesCount(
        colorTag, mediaType, onlyActiveReminders, currentTime
    )

    override fun getRecentSearches(): Flow<List<RecentSearchEntity>> =
        journalDao.getRecentSearches()

    override suspend fun saveRecentSearch(query: String) {
        if (query.isNotBlank()) {
            journalDao.insertRecentSearch(RecentSearchEntity(query.trim()))
        }
    }

    override suspend fun deleteRecentSearch(query: String) {
        journalDao.deleteRecentSearch(query)
    }

    override suspend fun clearAllRecentSearches() {
        journalDao.clearAllRecentSearches()
    }
}