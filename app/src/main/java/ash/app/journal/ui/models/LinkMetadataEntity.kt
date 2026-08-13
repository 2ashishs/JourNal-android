package ash.app.journal.ui.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "link_metadata")
data class LinkMetadataEntity(
    @PrimaryKey val url: String,
    val title: String,
    val description: String,
    val imageUrl: String,
    val fetchedAt: Long = System.currentTimeMillis()
)