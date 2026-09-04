package ash.app.journal.ui.models

data class SearchFilterCounts(
    val colorCounts: Map<EntryColorTag, Int> = emptyMap(),
    val mediaCounts: Map<EntryMediaType, Int> = emptyMap()
)
