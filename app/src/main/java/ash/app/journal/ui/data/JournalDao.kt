package ash.app.journal.ui.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import ash.app.journal.ui.models.ColorTagCount
import ash.app.journal.ui.models.EntryColorTag
import ash.app.journal.ui.models.EntryMediaType
import ash.app.journal.ui.models.JournalEntry
import ash.app.journal.ui.models.MediaTypeCount
import ash.app.journal.ui.models.RecentSearchEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDao {

    @Query("SELECT * FROM journal_entries ORDER BY timestamp DESC")
    fun getAllEntries(): Flow<List<JournalEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: JournalEntry): Long

    @Delete
    suspend fun deleteEntry(entry: JournalEntry)

    @Update
    suspend fun updateEntry(entry: JournalEntry)

    @Update
    suspend fun updateEntries(entries: List<JournalEntry>)

    // --- Search Query Matching Title or Details ---
    @Query("""
        SELECT * FROM journal_entries 
        WHERE (title LIKE '%' || :query || '%' OR details LIKE '%' || :query || '%')
        AND (:colorTag IS NULL OR colorTag = :colorTag)
        AND (:mediaType IS NULL OR mediaType = :mediaType)
        ORDER BY id DESC
    """)
    fun searchEntries(
        query: String,
        colorTag: EntryColorTag? = null,
        mediaType: EntryMediaType? = null
    ): Flow<List<JournalEntry>>

    // --- Dynamic Count Aggregations for Filter Chips ---
    @Query("SELECT colorTag, COUNT(*) as count FROM journal_entries GROUP BY colorTag")
    fun getColorTagCounts(): Flow<List<ColorTagCount>>

    @Query("SELECT mediaType, COUNT(*) as count FROM journal_entries GROUP BY mediaType")
    fun getMediaTypeCounts(): Flow<List<MediaTypeCount>>

    // --- Recent Searches Queries ---
    @Query("SELECT * FROM recent_searches ORDER BY timestamp DESC LIMIT 5")
    fun getRecentSearches(): Flow<List<RecentSearchEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentSearch(search: RecentSearchEntity)

    @Query("DELETE FROM recent_searches WHERE `query` = :query")
    suspend fun deleteRecentSearch(query: String)

    @Query("DELETE FROM recent_searches")
    suspend fun clearAllRecentSearches()
}