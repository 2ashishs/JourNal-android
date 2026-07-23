package ash.app.journal.ui.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ash.app.journal.ui.models.LinkMetadataEntity

@Dao
interface LinkMetadataDao {
    @Query("SELECT * FROM link_metadata WHERE url = :url LIMIT 1")
    suspend fun getMetadataForUrl(url: String): LinkMetadataEntity?

    @Query("SELECT * FROM link_metadata WHERE url IN (:urls)")
    suspend fun getMetadataForUrls(urls: List<String>): List<LinkMetadataEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetadata(metadata: LinkMetadataEntity)
}