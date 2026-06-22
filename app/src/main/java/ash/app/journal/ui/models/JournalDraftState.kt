package ash.app.journal.ui.models

// Tracks the active state of the current creation session
data class JournalDraftState(
    val editingEntryId: Long? = null, // Null means "New Entry Mode", Long value means "Edit Mode"
    val title: String = "",
    val details: String = "",
    val selectedHexColor: String? = null,
    val capturedPhotoPath: String? = null,
)