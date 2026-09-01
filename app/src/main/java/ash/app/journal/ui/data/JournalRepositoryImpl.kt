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

    //SEARCH

    override fun searchEntries(
        query: String,
        colorTag: EntryColorTag?,
        mediaType: EntryMediaType?
    ): Flow<List<JournalEntry>> = journalDao.searchEntries(query, colorTag, mediaType)

    override fun getColorTagCounts(mediaType: EntryMediaType?): Flow<List<ColorTagCount>> =
        journalDao.getColorTagCounts(mediaType)

    override fun getMediaTypeCounts(colorTag: EntryColorTag?): Flow<List<MediaTypeCount>> =
        journalDao.getMediaTypeCounts(colorTag)

    override fun getRecentSearches(): Flow<List<RecentSearchEntity>> = journalDao.getRecentSearches()

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