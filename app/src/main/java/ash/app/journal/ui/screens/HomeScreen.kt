package ash.app.journal.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import ash.app.journal.R
import ash.app.journal.ui.JournalViewModel
import ash.app.journal.ui.models.EntryColorTag
import ash.app.journal.ui.models.EntryMediaType
import ash.app.journal.ui.models.JournalDraftState
import ash.app.journal.ui.models.JournalEntry
import ash.app.journal.ui.models.LinkMetadataEntity
import ash.app.journal.ui.theme.JournalTheme
import coil3.compose.rememberAsyncImagePainter
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainJournalScreen(viewModel: JournalViewModel) {
    val entries by viewModel.journalEntries.collectAsState()
    val draftState by viewModel.draftState.collectAsState()

    var selectedEntryId by remember { mutableStateOf<Long?>(null) }
    val selectedEntryForDetail = entries.find { it.id == selectedEntryId }

    val lazyListState = rememberLazyListState()
    val context = LocalContext.current
    val shareViaTitle = stringResource(R.string.share_entry_via)

    var isSearchScreenOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("JourNaL", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { isSearchScreenOpen = true }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_search),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            contentDescription = "Search",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background // Soft pastel foundation background tint
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    bottom = 100.dp,
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = entries,
                    key = { entry -> entry.id }
                ) { entry ->
                    JournalRowItem(
                        entry = entry,
                        onClick = { selectedEntryId = entry.id }
                    )
                }
            }

            FloatingActionButton(
                onClick = { viewModel.setCreateSheetVisibility(true) },
                shape = RoundedCornerShape(50),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = stringResource(R.string.add_entry)
                )
            }
        }
    }


    if (viewModel.isCreateSheetOpen) {
        CreateEntryBottomSheet(
            draftState = draftState,
            onTitleChange = viewModel::onTitleChanged,
            onDetailsChange = viewModel::onDetailsChanged,
            onColorSelect = viewModel::onColorSelected,
            onMediaCapture = viewModel::onMediaCaptured,
            isRecordingAudio = viewModel.isRecordingAudio,
            startAudioRecording = viewModel::startAudioRecording,
            stopAudioRecording = viewModel::stopAudioRecording,
            onSave = {
                viewModel.saveCurrentEntry()
                viewModel.setCreateSheetVisibility(false)
            },
            onDismiss = {
                viewModel.setCreateSheetVisibility(false)
            }
        )
    }

    val linkMetadataMap by viewModel.linkMetadataState.collectAsState()

    selectedEntryForDetail?.let { entry ->
        DetailEntryBottomSheet(
            entry = entry,
            metadataMap = linkMetadataMap,
            isMediaFileAvailable = viewModel.isMediaFileAvailable(entry),
            onFetchMetadata = { url -> viewModel.fetchAndCacheMetadataForUrl(url) },
            onClearMissingMedia = { viewModel.removeMissingMediaFromEntry(entry) },
            onDismiss = { selectedEntryId = null },
            onEditClick = {
                viewModel.startEditing(entry)
                selectedEntryId = null
                viewModel.setCreateSheetVisibility(true)
            },
            onDeleteClick = {
                viewModel.deleteEntry(entry)
                selectedEntryId = null
            },
            onShareClick = {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    putExtra(Intent.EXTRA_TITLE, entry.title)
                    putExtra(Intent.EXTRA_SUBJECT, entry.title)
                    putExtra(Intent.EXTRA_TEXT, entry.details)

                    if (viewModel.isMediaFileAvailable(entry)) {
                        type = when (entry.mediaType) {
                            EntryMediaType.PHOTO -> "image/*"
                            EntryMediaType.VIDEO -> "video/*"
                            EntryMediaType.AUDIO -> "audio/*"
                            EntryMediaType.LINK -> "text/plain"
                            EntryMediaType.TEXT -> "text/plain"
                        }
                        val mediaFile = File(entry.mediaPath!!)
                        val authority = "${context.packageName}.fileprovider"
                        val mediaUri = FileProvider.getUriForFile(context, authority, mediaFile)
                        putExtra(Intent.EXTRA_STREAM, mediaUri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    } else {
                        type = "text/plain"
                        if (entry.details.isBlank()) putExtra(Intent.EXTRA_TEXT, entry.title)
                    }
                }
                val chooserIntent = Intent.createChooser(
                    shareIntent,
                    shareViaTitle
                )
                context.startActivity(chooserIntent)
            }
        )
    }

    // Collect ViewModel Search States:
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedColorFilter by viewModel.selectedColorFilter.collectAsState()
    val selectedMediaFilter by viewModel.selectedMediaFilter.collectAsState()
    val filterCounts by viewModel.filterCounts.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val recentSearches by viewModel.recentSearches.collectAsState()

    if (isSearchScreenOpen) {
        SearchScreen(
            query = searchQuery,
            onQueryChange = viewModel::onSearchQueryChanged,
            selectedColorFilter = selectedColorFilter,
            selectedMediaFilter = selectedMediaFilter,
            filterCounts = filterCounts,
            searchResults = searchResults,
            recentSearches = recentSearches,
            onColorFilterSelected = viewModel::onColorFilterSelected,
            onMediaFilterSelected = viewModel::onMediaFilterSelected,
            onClearFilterChips = viewModel::clearFilterChips,
            onSearchExecuted = { term -> viewModel.saveRecentSearch(term) },
            onDeleteRecentSearch = viewModel::deleteRecentSearch,
            onClearAllRecentSearches = viewModel::clearAllRecentSearches,
            onEntryClick = { entry ->
                selectedEntryId = entry.id // Opens your DetailEntryBottomSheet
            },
            onBackClick = {
                viewModel.clearFilters()
                isSearchScreenOpen = false
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun JournalRowItem(
    entry: JournalEntry,
    onClick: () -> Unit,
) {
    val prefixIconRes = when (entry.mediaType) {
        EntryMediaType.PHOTO -> R.drawable.ic_media_photo
        EntryMediaType.AUDIO -> R.drawable.ic_media_audio
        EntryMediaType.VIDEO -> R.drawable.ic_media_video
        EntryMediaType.LINK -> R.drawable.ic_media_link
        EntryMediaType.TEXT -> R.drawable.ic_media_text
    }
    val tagColor = when (entry.colorTag) {
        EntryColorTag.RED -> JournalTheme.tagColors.tagRed
        EntryColorTag.YELLOW -> JournalTheme.tagColors.tagYellow
        EntryColorTag.GREEN -> JournalTheme.tagColors.tagGreen
        EntryColorTag.BLUE -> JournalTheme.tagColors.tagBlue
        else -> JournalTheme.tagColors.tagDefault
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp,
            pressedElevation = 2.dp,
            draggedElevation = 8.dp
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(prefixIconRes),
                contentDescription = stringResource(R.string.content_type_indicator),
                tint = tagColor,
                modifier = Modifier
                    .padding(start = 16.dp)
                    .size(24.dp)
            )

            Text(
                text = entry.title.ifBlank { stringResource(R.string.untitled_entry) },
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(24.dp)
            )

            Box(
                modifier = Modifier
                    .width(20.dp)
                    .fillMaxHeight()
                    .background(tagColor)
            )
        }
    }
}

private fun createTempImageFile(context: Context): File {
    val directory = File(context.cacheDir, "journal_images").apply { mkdirs() }
    return File.createTempFile("captured_photo_", ".jpg", directory)
}

private fun createTempVideoFile(context: Context): File {
    val directory = File(context.cacheDir, "journal_videos").apply { mkdirs() }
    return File.createTempFile("captured_video_", ".mp4", directory)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEntryBottomSheet(
    draftState: JournalDraftState,
    onTitleChange: (String) -> Unit,
    onDetailsChange: (String) -> Unit,
    onColorSelect: (EntryColorTag) -> Unit,
    onMediaCapture: (String?, EntryMediaType) -> Unit,
    isRecordingAudio: Boolean,
    startAudioRecording: (Context) -> Unit,
    stopAudioRecording: (Boolean, Context) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var tempPhotoPath by remember { mutableStateOf<String?>(null) }
    var tempVideoPath by remember { mutableStateOf<String?>(null) }
    var tempVideoUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempPhotoPath != null) {
            onMediaCapture(tempPhotoPath, EntryMediaType.PHOTO)
        }
    }

    val videoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo()
    ) { success ->
        if (success && tempVideoUri != null) {
            onMediaCapture(tempVideoPath, EntryMediaType.VIDEO)
        }
    }

    val dynamicAutoTitleLabel = if (draftState.autoTitlePlaceholder.isNotBlank()) {
        draftState.autoTitlePlaceholder
    } else {
        stringResource(R.string.title)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startAudioRecording(context)
        }
    }

    var detailsTextFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = draftState.details,
                selection = TextRange(draftState.details.length)
            )
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = draftState.title,
                onValueChange = onTitleChange,
                label = { Text(dynamicAutoTitleLabel) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                maxLines = 1,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
            )

            LaunchedEffect(draftState.details) {
                if (detailsTextFieldValue.text != draftState.details) {
                    val newCursor =
                        detailsTextFieldValue.selection.start.coerceAtMost(draftState.details.length)
                    detailsTextFieldValue = detailsTextFieldValue.copy(
                        text = draftState.details,
                        selection = TextRange(newCursor)
                    )
                }
            }

            OutlinedTextField(
                value = detailsTextFieldValue,
                onValueChange = { incomingValue: TextFieldValue ->
                    val processedValue =
                        handleBulletAutoContinue(detailsTextFieldValue, incomingValue)
                    detailsTextFieldValue = processedValue
                    onDetailsChange(processedValue.text)
                },
                label = { Text(stringResource(R.string.details)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 96.dp, max = 480.dp),
                minLines = 2,
                maxLines = 8,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. "Default Tag Color Selected" Option Button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(
                            width = if (draftState.selectedColorTag == EntryColorTag.DEFAULT) 2.dp else 1.dp,
                            color = if (draftState.selectedColorTag == EntryColorTag.DEFAULT) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape
                        )
                        .clickable { onColorSelect(EntryColorTag.DEFAULT) },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(24.dp)) {
                        drawLine(
                            color = Color.Red,
                            start = Offset(0f, size.height),
                            end = Offset(size.width, 0f),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                }

                // 2. The Standard Primary Palette Colors List Loop
                EntryColorTag.entries.filter { it != EntryColorTag.DEFAULT }.forEach { colorTag ->

                    val tagDisplayColor = when (colorTag) {
                        EntryColorTag.RED -> JournalTheme.tagColors.tagRed
                        EntryColorTag.YELLOW -> JournalTheme.tagColors.tagYellow
                        EntryColorTag.GREEN -> JournalTheme.tagColors.tagGreen
                        EntryColorTag.BLUE -> JournalTheme.tagColors.tagBlue
                        else -> Color.Gray
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(tagDisplayColor)
                            .clickable { onColorSelect(colorTag) }
                            .border(
                                width = if (draftState.selectedColorTag == colorTag) 2.dp else 0.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            )
                    )
                }
            }

            draftState.capturedMediaPath?.let { path ->
                Box(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .size(80.dp) // Slightly larger to comfortably accommodate the clear button
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(File(path)),
                        contentDescription = stringResource(R.string.captured_media_preview),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(72.dp)
                            .align(Alignment.BottomStart)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                // Quick UX Win: Clicking the thumbnail itself can trigger a retake context
                            }
                    )

                    val overlayIconRes = when (draftState.capturedMediaType) {
                        EntryMediaType.PHOTO -> R.drawable.ic_media_photo
                        EntryMediaType.VIDEO -> R.drawable.ic_media_video
                        EntryMediaType.AUDIO -> R.drawable.ic_media_audio
                        EntryMediaType.LINK -> R.drawable.ic_media_link
                        EntryMediaType.TEXT -> null
                    }

                    overlayIconRes?.let { iconRes ->
                        Box(
                            modifier = Modifier
                                .padding(bottom = 4.dp, end = 12.dp)
                                .size(18.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                .align(Alignment.BottomEnd),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(iconRes),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }
                    // Clear/Remove Media Cross Button ---
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(MaterialTheme.colorScheme.error, CircleShape)
                            .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                            .align(Alignment.TopEnd) // This will resolve the scope mismatch cleanly here
                            .clickable {
                                // Invoke a callback to pass null up to the ViewModel to clear out the media file references
                                onMediaCapture(null, EntryMediaType.TEXT)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close), // Make sure you have a close/clear vector icon
                            contentDescription = stringResource(R.string.remove_media),
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            // A single, cohesive row container handling all bottom sheet controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Segment: Packs all mutually exclusive media choice chips cleanly together
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f) // Takes up remaining left-side real estate dynamically
                ) {
                    if (draftState.capturedMediaType == EntryMediaType.TEXT || draftState.capturedMediaType == EntryMediaType.PHOTO) {
                        IconButton(
                            modifier = Modifier.border(
                                1.dp,
                                MaterialTheme.colorScheme.onBackground,
                                RoundedCornerShape(8.dp)
                            ),
                            onClick = {
                                val file = createTempImageFile(context)
                                val authority = "${context.packageName}.fileprovider"
                                val uri = FileProvider.getUriForFile(context, authority, file)
                                tempPhotoPath = file.absolutePath
                                cameraLauncher.launch(uri)
                            },
                        ) {
                            // Since we added the precise "X" close button above, we can simplify this text to just "Photo"
                            Icon(
                                contentDescription = when {
                                    draftState.capturedMediaPath != null -> stringResource(R.string.retake_photo)
                                    else -> stringResource(R.string.photo)
                                },
                                painter = when {
                                    draftState.capturedMediaPath != null -> painterResource(R.drawable.ic_refresh)
                                    else -> painterResource(R.drawable.ic_photo_camera)
                                },
                                tint = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                    }

                    if (draftState.capturedMediaType == EntryMediaType.TEXT || draftState.capturedMediaType == EntryMediaType.VIDEO) {
                        IconButton(
                            modifier = Modifier.border(
                                1.dp,
                                MaterialTheme.colorScheme.onBackground,
                                RoundedCornerShape(8.dp)
                            ),
                            onClick = {
                                val file = createTempVideoFile(context)
                                val authority = "${context.packageName}.fileprovider"
                                tempVideoPath = file.absolutePath
                                val uri = FileProvider.getUriForFile(context, authority, file)
                                tempVideoUri = uri
                                videoLauncher.launch(uri)
                            }
                        ) {
                            Icon(
                                contentDescription = when {
                                    draftState.capturedMediaPath != null -> stringResource(R.string.retake_video)
                                    else -> stringResource(R.string.video)
                                },
                                painter = when {
                                    draftState.capturedMediaPath != null -> painterResource(R.drawable.ic_refresh)
                                    else -> painterResource(R.drawable.ic_video_camera)
                                },
                                tint = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                    }

                    if (draftState.capturedMediaType == EntryMediaType.TEXT || draftState.capturedMediaType == EntryMediaType.AUDIO) {
                        IconButton(
                            modifier = Modifier.border(
                                1.dp,
                                if (isRecordingAudio) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                RoundedCornerShape(8.dp)
                            ),
                            onClick = {
                                if (isRecordingAudio) {
                                    stopAudioRecording(false, context)
                                } else {
                                    // Request permission dynamically; if granted, start recording
                                    val hasPermission = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (hasPermission) {
                                        startAudioRecording(context)
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                }
                            }
                        ) {
                            if (isRecordingAudio) {
                                AudioRecordingIcon(true, color = MaterialTheme.colorScheme.error)
                            } else {
                                Icon(
                                    contentDescription = when {
//                                        isRecordingAudio -> stringResource(R.string.stop_recording)
                                        draftState.capturedMediaPath != null -> stringResource(R.string.retake_audio)
                                        else -> stringResource(R.string.audio)
                                    },
                                    painter = when {
//                                        isRecordingAudio -> painterResource(R.drawable.ic_recording_audio)
                                        draftState.capturedMediaPath != null -> painterResource(R.drawable.ic_refresh)
                                        else -> painterResource(R.drawable.ic_record_audio)
                                    },
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                // Right Segment: Positioned perfectly on the same line between the center and right edge
                Button(
                    onClick = onSave,
                    enabled = draftState.title.isNotBlank() || draftState.autoTitlePlaceholder.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.padding(start = 16.dp)
                ) {
                    Text(stringResource(R.string.save))
                }
            }

        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailEntryBottomSheet(
    entry: JournalEntry,
    metadataMap: Map<String, LinkMetadataEntity>,
    isMediaFileAvailable: Boolean,
    onFetchMetadata: (String) -> Unit,
    onClearMissingMedia: () -> Unit,
    onDismiss: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onShareClick: () -> Unit
) {
    // Get the device screen height dynamically
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    // Calculate 80% of current window height cleanly in DP
    val maxSheetHeight = with(density) {
        (windowInfo.containerSize.height * 0.80f).toDp()
    }
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    val preventUpwardBounceConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                return if (available.y < 0f) {
                    Offset(0f, available.y)
                } else {
                    Offset.Zero
                }
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                return if (available.y < 0f) {
                    Velocity(0f, available.y)
                } else {
                    Velocity.Zero
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        // Single parent container holding Header, Body, AND Action Bar together
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxSheetHeight)
                .nestedScroll(preventUpwardBounceConnection)
                .navigationBarsPadding()
        ) {
            // ANCHORED ZONE (Title & Dynamic Divider)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // CUSTOM DRAG HANDLE (Lives inside nestedScroll tree)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(32.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    )
                }

                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                HorizontalDivider(
                    thickness = DividerDefaults.Thickness,
                    color = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            // SCROLLABLE ZONE (Details & Photo)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                entry.details.takeIf { it.isNotBlank() }?.let { entryDetails ->
                    MarkdownText(
                        text = entryDetails,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        metadataMap = metadataMap,
                        onFetchMetadata = onFetchMetadata
                    )
                }

                entry.mediaPath?.let { path ->
                    if (isMediaFileAvailable) {
                        when (entry.mediaType) {
                            EntryMediaType.PHOTO -> {
                                var isImageFullscreen by remember { mutableStateOf(false) }
                                Image(
                                    painter = rememberAsyncImagePainter(File(path)),
                                    contentDescription = entry.title,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .pointerInput(Unit) {
                                            detectTapGestures(onDoubleTap = {
                                                isImageFullscreen = true
                                            })
                                        }
                                )
                                if (isImageFullscreen) {
                                    ZoomableImageView(
                                        imagePath = path,
                                        onDismiss = { isImageFullscreen = false }
                                    )
                                }
                            }

                            EntryMediaType.VIDEO -> {
                                LoopingVideoPlayer(videoPath = path)
                            }

                            EntryMediaType.AUDIO -> {
                                AudioPlayerRegion(audioPath = path)
                            }

                            EntryMediaType.LINK -> {
                                // Standard link layout flows cleanly
                            }

                            EntryMediaType.TEXT -> {
                                // Standard text layout flows cleanly
                            }
                        }
                    } else {
                        // --- FALLBACK WHEN FILE IS DELETED/CLEARED ---
                        MissingMediaCard(
                            mediaType = entry.mediaType,
                            onClearMediaTag = onClearMissingMedia
                        )
                    }
                }
            }

            HorizontalDivider(
                thickness = DividerDefaults.Thickness,
                color = MaterialTheme.colorScheme.surfaceVariant
            )

            // FIXED BOTTOM ACTION ZONE
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_delete),
                            contentDescription = stringResource(R.string.delete),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = onShareClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_share),
                            contentDescription = stringResource(R.string.share),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = onEditClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_edit),
                            contentDescription = stringResource(R.string.edit),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

