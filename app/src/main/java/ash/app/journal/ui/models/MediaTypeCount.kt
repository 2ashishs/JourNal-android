package ash.app.journal.ui.models

import androidx.room.ColumnInfo

data class MediaTypeCount(
    @ColumnInfo(name = "mediaType") val mediaType: EntryMediaType,
    @ColumnInfo(name = "count") val count: Int
)
