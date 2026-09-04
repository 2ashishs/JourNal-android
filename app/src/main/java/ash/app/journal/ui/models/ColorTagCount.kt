package ash.app.journal.ui.models

import androidx.room.ColumnInfo

data class ColorTagCount(
    @ColumnInfo(name = "colorTag") val colorTag: EntryColorTag,
    @ColumnInfo(name = "count") val count: Int
)
