package ash.app.journal.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ash.app.journal.ui.data.JournalRepository
import ash.app.journal.ui.models.EntryMediaType
import ash.app.journal.ui.models.JournalDraftState
import ash.app.journal.ui.models.JournalEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.content.Context
import android.media.MediaRecorder
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File
import java.io.IOException

class JournalViewModel(
    private val repository: JournalRepository
) : ViewModel() {

    // 1. STREAM FROM DB: Automatically reads from Room and converts it into a StateFlow for Compose
    val journalEntries: StateFlow<List<JournalEntry>> = repository.getAllEntries()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // 2. SESSION PRESERVATION: Holds the active text/photo state of the creation bottom sheet
    private val _draftState = MutableStateFlow(JournalDraftState())
    val draftState: StateFlow<JournalDraftState> = _draftState.asStateFlow()

    // --- Draft State Mutations (Called by UI as user types/interacts) ---

    fun onTitleChanged(newTitle: String) {
        _draftState.update { it.copy(title = newTitle) }
    }

    fun onDetailsChanged(newDetails: String) {
        _draftState.update { it.copy(details = newDetails) }
    }

    fun onColorSelected(hexColor: String?) {
        _draftState.update { it.copy(selectedHexColor = hexColor) }
    }

    fun onMediaCaptured(path: String?, type: EntryMediaType) {
        _draftState.update { currentDraft ->
            val autoTitle = if (path != null) {
                when (type) {
                    EntryMediaType.PHOTO -> "Photo on ${formatJournalDate(System.currentTimeMillis())}"
                    EntryMediaType.VIDEO -> "Video on ${formatJournalDate(System.currentTimeMillis())}"
                    EntryMediaType.AUDIO -> "Audio on ${formatJournalDate(System.currentTimeMillis())}"
                    else -> ""
                }
            } else {
                ""
            }
            currentDraft.copy(
                capturedMediaPath = path,
                capturedMediaType = if (path != null) type else EntryMediaType.TEXT,
                autoTitlePlaceholder = autoTitle
            )
        }
    }

    /**
     * Converts @param:timestamp to user readable date
     */
    fun formatJournalDate(timestamp: Long): String {
        val formatter = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        return formatter.format(Date(timestamp))
    }

    private var mediaRecorder: MediaRecorder? = null
    private var currentAudioFile: File? = null

    // Expose a public UI state tracking whether the microphone is actively listening
    var isRecordingAudio by mutableStateOf(false)
        private set

    fun startAudioRecording(context: Context) {
        val directory = File(context.cacheDir, "journal_audio").apply { mkdirs() }
        currentAudioFile = File.createTempFile("voice_note_", ".m4a", directory)

        // Instantly lock the UI media type to AUDIO so other buttons vanish immediately
        _draftState.update { it.copy(capturedMediaType = EntryMediaType.AUDIO) }

        // Handle initialization based on Android API versions safely
        mediaRecorder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION") MediaRecorder()
        }.apply {
            try {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(currentAudioFile?.absolutePath)
                prepare()
                start()
                isRecordingAudio = true
            } catch (e: IOException) {
                e.printStackTrace()
                // Reset to text if hardware fails initialization
                _draftState.update { it.copy(capturedMediaType = EntryMediaType.TEXT) }
            } catch (e: IllegalStateException) {
                e.printStackTrace()
                // Reset to text if hardware fails initialization
                _draftState.update { it.copy(capturedMediaType = EntryMediaType.TEXT) }
            }
        }
    }

    fun stopAudioRecording(cancel: Boolean = false) {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaRecorder = null
            isRecordingAudio = false
        }

        if (cancel) {
            currentAudioFile?.delete()
            currentAudioFile = null
            // Revert cleanly to standard text mode on deletion
            _draftState.update { it.copy(capturedMediaType = EntryMediaType.TEXT, capturedMediaPath = null, autoTitlePlaceholder = "") }
        } else {
            // Hand off the valid file path string directly to your unified placeholder layout engine!
            currentAudioFile?.let { file ->
                onMediaCaptured(file.absolutePath, EntryMediaType.AUDIO)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // If the app process destroys the ViewModel, kill the hardware connection immediately
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaRecorder = null
            isRecordingAudio = false
        }
    }

    // --- Database Actions ---
    // Refactored Save function with empty-title validation guard

    fun saveCurrentEntry() {
        val currentDraft = _draftState.value

        // CRITICAL VALIDATION: Prevent saving if the title is completely empty or whitespace
        val finalTitle = currentDraft.title.ifBlank { currentDraft.autoTitlePlaceholder }
        if (finalTitle.isBlank()) {
            return
        }

        viewModelScope.launch {
            if (currentDraft.editingEntryId != null) {
                // --- EDIT MODE: Fetch the existing entry configuration to preserve order index ---
                val existingList = journalEntries.value
                val existingEntry =
                    existingList.firstOrNull { it.id == currentDraft.editingEntryId }
                val currentOrderIndex = existingEntry?.orderIndex ?: 0

                val updatedEntry = JournalEntry(
                    id = currentDraft.editingEntryId, // Matching ID triggers Room's REPLACE / Update mechanism
                    title = finalTitle,
                    details = currentDraft.details,
                    hexColor = currentDraft.selectedHexColor,
                    mediaPath = currentDraft.capturedMediaPath,
                    mediaType = currentDraft.capturedMediaType,
                    timestamp = System.currentTimeMillis(),
                    orderIndex = currentOrderIndex
                )
                repository.insertEntry(updatedEntry)
            } else {
                // --- NEW ENTRY MODE ---
                val newEntry = JournalEntry(
                    title = finalTitle,
                    details = currentDraft.details,
                    hexColor = currentDraft.selectedHexColor,
                    mediaPath = currentDraft.capturedMediaPath,
                    mediaType = currentDraft.capturedMediaType,
                    timestamp = System.currentTimeMillis(),
                    orderIndex = journalEntries.value.size
                )
                repository.insertEntry(newEntry)
            }

            // Clear state back to default after saving
            _draftState.value = JournalDraftState()
        }
    }

    fun deleteEntry(entry: JournalEntry) {
        viewModelScope.launch {
            repository.deleteEntry(entry)
            // Optional: After deletion, re-index remaining entries so orderIndex stays sequential
            reindexEntries()
        }
    }

    fun moveEntry(fromIndex: Int, toIndex: Int) {
        val currentList = journalEntries.value.toMutableList()
        if (fromIndex in currentList.indices && toIndex in currentList.indices) {
            // Swap positions in the local list copy
            val movedItem = currentList.removeAt(fromIndex)
            currentList.add(toIndex, movedItem)

            // Update the orderIndex property of each item based on its new position
            val updatedList = currentList.mapIndexed { index, item ->
                item.copy(orderIndex = index)
            }

            // Persist the batch update to Room DB via repository
            viewModelScope.launch {
                repository.updateEntries(updatedList)
            }
        }
    }

    private suspend fun reindexEntries() {
        val currentList = journalEntries.value
        val updatedList = currentList.mapIndexed { index, item ->
            item.copy(orderIndex = index)
        }
        repository.updateEntries(updatedList)
    }

    // Triggered when the user clicks "Edit" from either the Home menu or Detail Sheet
    fun startEditing(entry: JournalEntry) {
        _draftState.update {
            JournalDraftState(
                editingEntryId = entry.id,
                title = entry.title,
                details = entry.details,
                selectedHexColor = entry.hexColor,
                capturedMediaPath = entry.mediaPath,
                capturedMediaType = entry.mediaType,
            )
        }
    }

    // Clear draft explicitly if user discards changes
    fun clearDraft() {
        _draftState.value = JournalDraftState()
    }
}
