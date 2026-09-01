package ash.app.journal.ui

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaRecorder
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ash.app.journal.ui.data.JournalRepository
import ash.app.journal.ui.data.LinkMetadataRepository
import ash.app.journal.ui.models.EntryColorTag
import ash.app.journal.ui.models.EntryMediaType
import ash.app.journal.ui.models.JournalDraftState
import ash.app.journal.ui.models.JournalEntry
import ash.app.journal.ui.models.LinkMetadataEntity
import ash.app.journal.ui.models.RecentSearchEntity
import ash.app.journal.ui.models.SearchFilterCounts
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class JournalViewModel(
    private val repository: JournalRepository,
    private val linkRepository: LinkMetadataRepository
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

    fun onColorSelected(colorTag: EntryColorTag) {
        _draftState.update { it.copy(selectedColorTag = colorTag) }
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

        // Get audio recording device
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // API 31+ replacement
            // 1. Fetch all connected communication devices
            val devices = audioManager.availableCommunicationDevices

            // 2. Look for a Bluetooth Headset or BLE Audio channel in the active connections
            val bluetoothDevice = devices.firstOrNull {
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                        it.type == AudioDeviceInfo.TYPE_BLE_HEADSET
            }

            // 3. Request the OS to bind specifically to this hardware profile
            bluetoothDevice?.let { audioManager.setCommunicationDevice(it) }
        } else {
            // Legacy fallback wrapper for older OS installs
            @Suppress("DEPRECATION")
            if (audioManager.isBluetoothScoAvailableOffCall) {
                @Suppress("DEPRECATION")
                audioManager.startBluetoothSco()
                @Suppress("DEPRECATION")
                audioManager.isBluetoothScoOn = true
            }
        }

        // Handle initialization based on Android API versions safely
        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION") MediaRecorder()
        }.apply {
            try {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)      // High-Fidelity standard studio frequency range (44.1 kHz)
                setAudioEncodingBitRate(128000)  // Boost bandwidth path to 128 kbps for crystalline clarity
                setAudioChannels(1)              // Mono tracking optimized cleanly for dictation voice registers
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

    fun stopAudioRecording(cancel: Boolean = false, context: Context) {
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
            _draftState.update {
                it.copy(
                    capturedMediaType = EntryMediaType.TEXT,
                    capturedMediaPath = null,
                    autoTitlePlaceholder = ""
                )
            }
        } else {
            // Hand off the valid file path string directly to your unified placeholder layout engine!
            currentAudioFile?.let { file ->
                onMediaCaptured(file.absolutePath, EntryMediaType.AUDIO)
            }
        }

        // Release audio recording device
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Releases your app's explicit audio device request cleanly
            audioManager.clearCommunicationDevice()
        } else {
            @Suppress("DEPRECATION")
            if (audioManager.isBluetoothScoOn) {
                @Suppress("DEPRECATION")
                audioManager.isBluetoothScoOn = false
                @Suppress("DEPRECATION")
                audioManager.stopBluetoothSco()
            }
        }
    }

    override fun onCleared() {
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
                // --- EDIT ENTRY MODE ---
                val updatedEntry = JournalEntry(
                    id = currentDraft.editingEntryId, // Matching ID triggers Room's REPLACE / Update mechanism
                    title = finalTitle,
                    details = currentDraft.details,
                    colorTag = currentDraft.selectedColorTag,
                    mediaPath = currentDraft.capturedMediaPath,
                    mediaType = currentDraft.capturedMediaType,
                    timestamp = System.currentTimeMillis(),
                )
                repository.insertEntry(updatedEntry)
            } else {
                // --- NEW ENTRY MODE ---
                val newEntry = JournalEntry(
                    title = finalTitle,
                    details = currentDraft.details,
                    colorTag = currentDraft.selectedColorTag,
                    mediaPath = currentDraft.capturedMediaPath,
                    mediaType = currentDraft.capturedMediaType,
                    timestamp = System.currentTimeMillis(),
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
        }
    }

    fun isMediaFileAvailable(entry: JournalEntry) =
        entry.mediaPath != null && File(entry.mediaPath).exists()

    // Triggered when the user clicks "Edit" from either the Home menu or Detail Sheet
    fun startEditing(entry: JournalEntry) {
        _draftState.update {
            JournalDraftState(
                editingEntryId = entry.id,
                title = entry.title,
                details = entry.details,
                selectedColorTag = entry.colorTag,
                capturedMediaPath = if (isMediaFileAvailable(entry)) entry.mediaPath else null,
                capturedMediaType = if (isMediaFileAvailable(entry)) entry.mediaType else EntryMediaType.TEXT,
            )
        }
    }

    // Clear draft explicitly if user discards changes
    @Suppress("unused")
    fun clearDraft() {
        _draftState.value = JournalDraftState()
    }

    // Public UI state to trigger opening the bottom creation sheet instantly from the activity pass
    var isCreateSheetOpen by mutableStateOf(false)
        private set

    fun setCreateSheetVisibility(visible: Boolean) {
        isCreateSheetOpen = visible
    }

    fun stageSharedTextIntoDraft(sharedText: String) {
        clearDraft()
        onDetailsChanged(sharedText)
        setCreateSheetVisibility(true)
    }

    fun stageSharedMediaIntoDraft(filePath: String, mediaType: EntryMediaType) {
        clearDraft()
        // Inject the external media directly into the draft and auto generate title placeholder
        onMediaCaptured(filePath, mediaType)
        // Pop open the sheet on screen
        setCreateSheetVisibility(true)
    }

    // Keep a map of loaded metadata for current display session
    private val _linkMetadataState = MutableStateFlow<Map<String, LinkMetadataEntity>>(emptyMap())
    val linkMetadataState: StateFlow<Map<String, LinkMetadataEntity>> =
        _linkMetadataState.asStateFlow()

    fun processMagicWand(currentDetails: String) {
        viewModelScope.launch {
            val urlRegex = """(?<!url=)(?<!<)(https?://[^\s<>)]+)(?!>)""".toRegex()
            val distinctLinks =
                urlRegex.findAll(currentDetails).map { it.value }.distinct().toList()

            if (distinctLinks.isEmpty()) return@launch

            var updatedDetails = currentDetails
            val fetchedMetadataMap = mutableMapOf<String, LinkMetadataEntity>()

            distinctLinks.forEach { url ->
                val metadata = linkRepository.getOrFetchMetadata(url)

                if (metadata != null) {
                    fetchedMetadataMap[url] = metadata
                    // Clean syntax: [card](url=https://...)
                    updatedDetails = updatedDetails.replace(url, "[card](url=$url)")
                } else {
                    // Fallback syntax: <https://...>
                    updatedDetails = updatedDetails.replace(url, "<$url>")
                }
            }

            // Update local memory map for MarkdownText rendering
            _linkMetadataState.update { currentMap -> currentMap + fetchedMetadataMap }

            _draftState.update { currentDraft ->
                val singleValidMetadata = fetchedMetadataMap.values.singleOrNull()
                //val newTitle = singleValidMetadata?.title ?: currentDraft.title

                currentDraft.copy(
                    details = updatedDetails,
                    //title = if (currentDraft.title.isBlank() && singleValidMetadata != null) newTitle else currentDraft.title,
                    autoTitlePlaceholder = singleValidMetadata?.title
                        ?: currentDraft.autoTitlePlaceholder
                )
            }
        }
    }

    fun fetchAndCacheMetadataForUrl(url: String) {
        // Avoid re-fetching if metadata is already present in state
        if (_linkMetadataState.value.containsKey(url)) return

        viewModelScope.launch {
            val metadata = linkRepository.getOrFetchMetadata(url)
            if (metadata != null) {
                _linkMetadataState.update { currentMap ->
                    currentMap + (url to metadata)
                }
            }
        }
    }

    fun removeMissingMediaFromEntry(entry: JournalEntry) {
        viewModelScope.launch {
            val updatedEntry = entry.copy(
                mediaPath = null,
                mediaType = EntryMediaType.TEXT
            )
            // Update entry in Room Database
            repository.updateEntry(updatedEntry)
        }
    }

    // SEARCH
    // --- Search Query & Filter Selection States ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedColorFilter = MutableStateFlow<EntryColorTag?>(null)
    val selectedColorFilter: StateFlow<EntryColorTag?> = _selectedColorFilter.asStateFlow()

    private val _selectedMediaFilter = MutableStateFlow<EntryMediaType?>(null)
    val selectedMediaFilter: StateFlow<EntryMediaType?> = _selectedMediaFilter.asStateFlow()

    // --- Reactive Search Results ---
    @OptIn(ExperimentalCoroutinesApi::class)
    val searchResults: StateFlow<List<JournalEntry>> = combine(
        _searchQuery,
        _selectedColorFilter,
        _selectedMediaFilter
    ) { query, color, media ->
        Triple(query, color, media)
    }.flatMapLatest { (query, color, media) ->
        repository.searchEntries(query = query, colorTag = color, mediaType = media)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Dynamic Filter Counts that automatically re-aggregate when a filter is selected
    @OptIn(ExperimentalCoroutinesApi::class)
    val filterCounts: StateFlow<SearchFilterCounts> = combine(
        _selectedMediaFilter.flatMapLatest { selectedMedia ->
            repository.getColorTagCounts(selectedMedia)
        },
        _selectedColorFilter.flatMapLatest { selectedColor ->
            repository.getMediaTypeCounts(selectedColor)
        }
    ) { colorList, mediaList ->
        SearchFilterCounts(
            colorCounts = colorList.associate { it.colorTag to it.count },
            mediaCounts = mediaList.associate { it.mediaType to it.count }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SearchFilterCounts()
    )

    // --- Recent Searches History ---
    val recentSearches: StateFlow<List<RecentSearchEntity>> = repository.getRecentSearches()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // --- Search Actions ---
    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onColorFilterSelected(colorTag: EntryColorTag?) {
        // Toggle selection: if already selected, unselect it
        _selectedColorFilter.value = if (_selectedColorFilter.value == colorTag) null else colorTag
    }

    fun onMediaFilterSelected(mediaType: EntryMediaType?) {
        // Toggle selection: if already selected, unselect it
        _selectedMediaFilter.value =
            if (_selectedMediaFilter.value == mediaType) null else mediaType
    }

    fun clearFilters() {
        _selectedColorFilter.value = null
        _selectedMediaFilter.value = null
        _searchQuery.value = ""
    }

    fun saveRecentSearch(query: String) {
        viewModelScope.launch {
            repository.saveRecentSearch(query)
        }
    }

    fun deleteRecentSearch(query: String) {
        viewModelScope.launch {
            repository.deleteRecentSearch(query)
        }
    }

    fun clearAllRecentSearches() {
        viewModelScope.launch {
            repository.clearAllRecentSearches()
        }
    }
}
