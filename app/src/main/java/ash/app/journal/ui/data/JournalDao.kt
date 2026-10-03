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

    @Query("SELECT * FROM journal_entries WHERE id = :id LIMIT 1")
    suspend fun getEntryById(id: Long): JournalEntry?

    @Query("UPDATE journal_entries SET isReminderCompleted = 1 WHERE id = :entryId")
    suspend fun markReminderCompleted(entryId: Long)

    @Query(
        """
    SELECT * FROM journal_entries 
    WHERE isReminderCompleted = 0 
      AND reminderTimestamp IS NOT NULL 
      AND reminderTimestamp > :currentTime
    """
    )
    suspend fun getPendingReminders(currentTime: Long = System.currentTimeMillis()): List<JournalEntry>

    // --- Search Query Matching Title or Details (Strict Non-Null Tag Support) ---
    @Query(
        """
        SELECT * FROM journal_entries 
        WHERE (
            :query IS NULL OR :query = ''
            OR title LIKE '%' || :query || '%' 
            OR details LIKE '%' || :query || '%'
            OR title LIKE '%' || :trimmedQuery || '%' 
            OR details LIKE '%' || :trimmedQuery || '%'
        )
        AND (:colorTag IS NULL OR colorTag = :colorTag)
        AND (:mediaType IS NULL OR mediaType = :mediaType)
        AND (
          :onlyActiveReminders = 0 
          OR (
            isReminderCompleted = 0
            AND reminderTimestamp IS NOT NULL
            AND reminderTimestamp > :currentTime
          )
        )
        AND (
            :onlyPrivate = 0 
            OR isPrivateEntry = 1
        )
        ORDER BY timestamp DESC
    """
    )
    fun searchEntries(
        query: String,
        trimmedQuery: String,
        colorTag: EntryColorTag? = null,
        mediaType: EntryMediaType? = null,
        onlyActiveReminders: Boolean,
        onlyPrivate: Boolean,
        currentTime: Long = System.currentTimeMillis(),
    ): Flow<List<JournalEntry>>

    // --- Cross-Filtered Dynamic Counts ---
    // Row-1: Color tag counts
    @Query(
        """
        SELECT colorTag, COUNT(*) as count 
        FROM journal_entries
        WHERE (:mediaType IS NULL OR mediaType = :mediaType)
          AND (:onlyActiveReminders = 0 OR (isReminderCompleted = 0 AND reminderTimestamp IS NOT NULL AND reminderTimestamp > :currentTime))
          AND (:onlyPrivate = 0 OR isPrivateEntry = 1)
        GROUP BY colorTag
    """
    )
    fun getFacetedColorTagCounts(
        mediaType: EntryMediaType? = null,
        onlyActiveReminders: Boolean,
        onlyPrivate: Boolean,
        currentTime: Long = System.currentTimeMillis()
    ): Flow<List<ColorTagCount>>

    // Row-2: Media type counts
    @Query(
        """
        SELECT mediaType, COUNT(*) as count 
        FROM journal_entries
        WHERE (:colorTag IS NULL OR colorTag = :colorTag)
          AND (:onlyActiveReminders = 0 OR (isReminderCompleted = 0 AND reminderTimestamp IS NOT NULL AND reminderTimestamp > :currentTime))
          AND (:onlyPrivate = 0 OR isPrivateEntry = 1)
        GROUP BY mediaType
    """
    )
    fun getFacetedMediaTypeCounts(
        colorTag: EntryColorTag? = null,
        onlyActiveReminders: Boolean,
        onlyPrivate: Boolean,
        currentTime: Long = System.currentTimeMillis()
    ): Flow<List<MediaTypeCount>>

    // Row-3: Active Reminders and Private Notes count
    @Query(
        """
        SELECT COUNT(*) 
        FROM journal_entries
        WHERE isReminderCompleted = 0 
          AND reminderTimestamp IS NOT NULL 
          AND reminderTimestamp > :currentTime
          AND (:colorTag IS NULL OR colorTag = :colorTag)
          AND (:mediaType IS NULL OR mediaType = :mediaType)
          AND (:onlyPrivate = 0 OR isPrivateEntry = 1)
    """
    )
    fun getFacetedActiveRemindersCount(
        colorTag: EntryColorTag?,
        mediaType: EntryMediaType?,
        onlyPrivate: Boolean,
        currentTime: Long = System.currentTimeMillis()
    ): Flow<Int>

    @Query(
        """
        SELECT COUNT(*) 
        FROM journal_entries
        WHERE isPrivateEntry = 1
          AND (:colorTag IS NULL OR colorTag = :colorTag)
          AND (:mediaType IS NULL OR mediaType = :mediaType)
          AND (:onlyActiveReminders = 0 OR (isReminderCompleted = 0 AND reminderTimestamp IS NOT NULL AND reminderTimestamp > :currentTime))
    """
    )
    fun getFacetedPrivateEntriesCount(
        colorTag: EntryColorTag?,
        mediaType: EntryMediaType?,
        onlyActiveReminders: Boolean,
        currentTime: Long = System.currentTimeMillis()
    ): Flow<Int>

    // --- Recent Searches ---
    @Query("SELECT * FROM recent_searches ORDER BY timestamp DESC LIMIT 15")
    fun getRecentSearches(): Flow<List<RecentSearchEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentSearch(search: RecentSearchEntity)

    @Query("DELETE FROM recent_searches WHERE `query` = :query")
    suspend fun deleteRecentSearch(query: String)

    @Query("DELETE FROM recent_searches")
    suspend fun clearAllRecentSearches()
}