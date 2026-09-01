package ash.app.journal.ui.data

import ash.app.journal.ui.models.ColorTagCount
import ash.app.journal.ui.models.EntryColorTag
import ash.app.journal.ui.models.EntryMediaType
import ash.app.journal.ui.models.JournalEntry
import ash.app.journal.ui.models.MediaTypeCount
import ash.app.journal.ui.models.RecentSearchEntity
import kotlinx.coroutines.flow.Flow

interface JournalRepository {
    fun getAllEntries(): Flow<List<JournalEntry>>
    suspend fun insertEntry(entry: JournalEntry): Long
    suspend fun deleteEntry(entry: JournalEntry)
    suspend fun updateEntry(entry: JournalEntry)
    suspend fun updateEntries(entries: List<JournalEntry>)

    // Search Query Matching Title or Details
    fun searchEntries(
        query: String,
        colorTag: EntryColorTag? = null,
        mediaType: EntryMediaType? = null
    ): Flow<List<JournalEntry>>
    // Dynamic Count Aggregations for Filter Chips
    fun getColorTagCounts(mediaType: EntryMediaType? = null): Flow<List<ColorTagCount>>
    fun getMediaTypeCounts(colorTag: EntryColorTag? = null): Flow<List<MediaTypeCount>>
    // Recent Searches Queries
    fun getRecentSearches(): Flow<List<RecentSearchEntity>>
    suspend fun saveRecentSearch(query: String)
    suspend fun deleteRecentSearch(query: String)
    suspend fun clearAllRecentSearches()

}