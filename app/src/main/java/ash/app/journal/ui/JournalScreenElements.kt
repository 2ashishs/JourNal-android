package ash.app.journal.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
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
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import ash.app.journal.R
import ash.app.journal.ui.models.EntryColorTag
import ash.app.journal.ui.models.EntryMediaType
import ash.app.journal.ui.models.JournalDraftState
import ash.app.journal.ui.models.JournalEntry
import ash.app.journal.ui.theme.JournalTheme
import ash.app.journal.ui.utils.DragDropState
import coil3.compose.rememberAsyncImagePainter
import kotlinx.coroutines.delay
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainJournalScreen(viewModel: JournalViewModel) {
    val entries by viewModel.journalEntries.collectAsState()
    val draftState by viewModel.draftState.collectAsState()

    var selectedEntryForDetail by remember { mutableStateOf<JournalEntry?>(null) }

    val lazyListState = rememberLazyListState()
    val dragDropState = rememberDragDropState(lazyListState = lazyListState) { from, to ->
        viewModel.moveEntry(from, to)
    }

    val context = LocalContext.current

    val shareViaTitle = stringResource(R.string.share_entry_via)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("JourNaL", fontWeight = FontWeight.Bold) },
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
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { offset -> dragDropState.onDragStart(offset) },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragDropState.onDrag(dragAmount)
                            },
                            onDragCancel = { dragDropState.onDragInterrupted() },
                            onDragEnd = { dragDropState.onDragInterrupted() }
                        )
                    },
                contentPadding = PaddingValues(
                    bottom = 100.dp,
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(entries, key = { _, entry -> entry.id }) { index, entry ->
                    val isCurrentDraggedItem = index == dragDropState.currentIndexOfDraggedItem

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                translationY =
                                    if (isCurrentDraggedItem) dragDropState.draggedDistance else 0f
                                scaleX = if (isCurrentDraggedItem) 1.04f else 1.0f
                                scaleY = if (isCurrentDraggedItem) 1.04f else 1.0f
                                alpha = if (isCurrentDraggedItem) 0.9f else 1.0f
                            }
                    ) {
                        JournalRowItem(
                            entry = entry,
                            onClick = { selectedEntryForDetail = entry }
                        )
                    }
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

    selectedEntryForDetail?.let { entry ->
        DetailEntryBottomSheet(
            entry = entry,
            onDismiss = { selectedEntryForDetail = null },
            onEditClick = {
                viewModel.startEditing(entry)
                selectedEntryForDetail = null
                viewModel.setCreateSheetVisibility(true)
            },
            onDeleteClick = {
                viewModel.deleteEntry(entry)
                selectedEntryForDetail = null
            },
            onShareClick = {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    putExtra(Intent.EXTRA_TITLE, entry.title)
                    putExtra(Intent.EXTRA_SUBJECT, entry.title)
                    putExtra(Intent.EXTRA_TEXT, entry.details)

                    if (entry.mediaPath != null) {
                        type = when (entry.mediaType) {
                            EntryMediaType.PHOTO -> "image/*"
                            EntryMediaType.VIDEO -> "video/*"
                            EntryMediaType.AUDIO -> "audio/*"
                            EntryMediaType.TEXT -> "text/plain"
                        }
                        val mediaFile = File(entry.mediaPath)
                        val authority = "${context.packageName}.fileprovider"
                        val mediaUri = FileProvider.getUriForFile(context, authority, mediaFile)
                        putExtra(Intent.EXTRA_STREAM, mediaUri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    } else {
                        type = "text/plain"
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

            OutlinedTextField(
                value = draftState.details,
                onValueChange = onDetailsChange,
                label = { Text(stringResource(R.string.details)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
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
                            start = androidx.compose.ui.geometry.Offset(0f, size.height),
                            end = androidx.compose.ui.geometry.Offset(size.width, 0f),
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f) // Takes up remaining left-side real estate dynamically
                ) {
                    if (draftState.capturedMediaType == EntryMediaType.TEXT || draftState.capturedMediaType == EntryMediaType.PHOTO) {
                        Button(
                            onClick = {
                                val file = createTempImageFile(context)
                                val authority = "${context.packageName}.fileprovider"
                                val uri = FileProvider.getUriForFile(context, authority, file)
                                tempPhotoPath = file.absolutePath
                                cameraLauncher.launch(uri)
                            }
                        ) {
                            // Since we added the precise "X" close button above, we can simplify this text to just "Photo"
                            Text(
                                text = when {
                                    draftState.capturedMediaPath != null -> stringResource(R.string.retake_photo)
                                    else -> stringResource(R.string.photo)
                                }
                            )
                        }
                    }

                    if (draftState.capturedMediaType == EntryMediaType.TEXT || draftState.capturedMediaType == EntryMediaType.VIDEO) {
                        Button(
                            onClick = {
                                val file = createTempVideoFile(context)
                                val authority = "${context.packageName}.fileprovider"
                                tempVideoPath = file.absolutePath
                                val uri = FileProvider.getUriForFile(context, authority, file)
                                tempVideoUri = uri
                                videoLauncher.launch(uri)
                            }
                        ) {
                            Text(
                                text = when {
                                    draftState.capturedMediaPath != null -> stringResource(R.string.retake_video)
                                    else -> stringResource(R.string.video)
                                }
                            )
                        }
                    }

                    if (draftState.capturedMediaType == EntryMediaType.TEXT || draftState.capturedMediaType == EntryMediaType.AUDIO) {
                        Button(
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
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRecordingAudio) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        ) {
                            // The button label text changes state interactively
                            Text(
                                text = when {
                                    isRecordingAudio -> stringResource(R.string.stop_recording)
                                    draftState.capturedMediaPath != null -> stringResource(R.string.retake_audio)
                                    else -> stringResource(R.string.audio)
                                }
                            )
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

@Composable
fun MarkdownText(text: String, style: TextStyle, color: Color) {
    val annotatedString = remember(text) {
        buildAnnotatedString {
            val lines = text.split("\n")

            lines.forEachIndexed { index, line ->
                when {
                    // --- HEADER SUPPORT (# Text) ---
                    line.startsWith("# ") -> {
                        withStyle(
                            style = SpanStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = (style.fontSize.value + 4).sp
                            )
                        ) {
                            parseInlineLinks(line.removePrefix("# "))
                        }
                    }

                    // --- LIST SUPPORT (- Text) ---
                    line.startsWith("- ") -> {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Normal)) {
                            // Prefix with a clean bullet character symbol
                            append("•  ")
                            parseInlineLinks(line.removePrefix("- "))
                        }
                    }

                    // --- LIST SUPPORT (* Text) ---
                    line.startsWith("* ") -> {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Normal)) {
                            // Prefix with a clean bullet character symbol
                            append("•  ")
                            parseInlineLinks(line.removePrefix("* "))
                        }
                    }

                    // --- LIST SUPPORT (+ Text) ---
                    line.startsWith("+ ") -> {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Normal)) {
                            // Prefix with a clean bullet character symbol
                            append("•  ")
                            parseInlineLinks(line.removePrefix("+ "))
                        }
                    }

                    // --- STANDARD LINE ---
                    else -> {
                        parseInlineLinks(line)
                    }
                }

                // Add a line break for all elements except the final line block
                if (index < lines.lastIndex) {
                    append("\n")
                }
            }
        }
    }

    // Checking if any line is a list item to dynamically add padding block elements
    val hasList = remember(text) { text.lines().any { it.startsWith("- ") } }

    BasicText(
        text = annotatedString,
        style = style.copy(color),
        modifier = Modifier.padding(vertical = if (hasList) 4.dp else 0.dp)
    )
}

/**
 * Regex-driven parser to extract and render Markdown link syntaxes cleanly inline.
 */
private fun AnnotatedString.Builder.parseInlineLinks(text: String) {
    // Matches explicit brackets [Title](URL) or raw bracket hooks <URL>
    val linkRegex = """(\[([^]]+)]\((https?://[^\s)]+)\))|(<(https?://[^\s>]+)>)""".toRegex()

    var lastIndex = 0

    linkRegex.findAll(text).forEach { matchResult ->
        // Append any plain leading text preceding the regex match point
        if (matchResult.range.first > lastIndex) {
            appendLineText(text.substring(lastIndex, matchResult.range.first))
        }

        val isNamedLink = matchResult.groups[1] != null
        val displayText =
            if (isNamedLink) matchResult.groups[2]!!.value else matchResult.groups[5]!!.value
        val urlTarget =
            if (isNamedLink) matchResult.groups[3]!!.value else matchResult.groups[5]!!.value

        // Apply distinct Link Annotation architecture directly inline
        withLink(
            link = LinkAnnotation.Url(
                url = urlTarget,
                styles = androidx.compose.ui.text.TextLinkStyles(
                    style = SpanStyle(
                        color = Color(0xFF2196F3), // Sleek hyperlink blue color highlight
                        textDecoration = TextDecoration.Underline
                    )
                )
            )
        ) {
            appendLineText(displayText)
        }

        lastIndex = matchResult.range.last + 1
    }

    // Append any remaining plain trailing text blocks
    if (lastIndex < text.length) {
        appendLineText(text.substring(lastIndex))
    }
}

// Internal extension function to continue parsing **bold** and *italic* inside any line type
private fun AnnotatedString.Builder.appendLineText(lineText: String) {
    var currentIndex = 0
    val pattern = Regex("(\\*\\*.*?\\*\\*|\\*.*?\\*)")
    val matches = pattern.findAll(lineText)

    for (match in matches) {
        if (match.range.first > currentIndex) {
            append(lineText.substring(currentIndex, match.range.first))
        }

        val token = match.value
        when {
            token.startsWith("**") && token.endsWith("**") -> {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(token.removeSurrounding("**"))
                }
            }

            token.startsWith("*") && token.endsWith("*") -> {
                withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                    append(token.removeSurrounding("*"))
                }
            }

            else -> append(token)
        }
        currentIndex = match.range.last + 1
    }

    if (currentIndex < lineText.length) {
        append(lineText.substring(currentIndex))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailEntryBottomSheet(
    entry: JournalEntry,
    onDismiss: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onShareClick: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            // ANCHORED ZONE (Title & Dynamic Divider)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                entry.mediaPath?.let { path ->
                    when (entry.mediaType) {
                        EntryMediaType.PHOTO -> {
                            var isImageFullscreen by remember { mutableStateOf(false) }

                            Image(
                                painter = rememberAsyncImagePainter(File(path)),
                                contentDescription = entry.title,
                                contentScale = ContentScale.FillWidth,
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
                                Dialog(
                                    onDismissRequest = { isImageFullscreen = false },
                                    properties = DialogProperties(usePlatformDefaultWidth = false)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black)
                                            .pointerInput(Unit) {
                                                detectTapGestures(onDoubleTap = {
                                                    isImageFullscreen = false
                                                })
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Image(
                                            painter = rememberAsyncImagePainter(File(path)),
                                            contentDescription = null,
                                            contentScale = ContentScale.Fit, // Fits image inside boundaries without clipping details
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }

                        EntryMediaType.VIDEO -> {
                            LoopingVideoPlayer(videoPath = path)
                        }

                        EntryMediaType.AUDIO -> {
                            AudioPlayerRegion(audioPath = path)
                        }

                        EntryMediaType.TEXT -> {
                            // Standard text layout flows cleanly with zero extra attachment
                        }
                    }
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

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun LoopingVideoPlayer(videoPath: String) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current // Access the host activity's lifecycle state
    var isFullscreen by remember { mutableStateOf(false) }

    // Master unified ExoPlayer instance tied to this lifecycle
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(File(videoPath).toURI().toString()))
            repeatMode = Player.REPEAT_MODE_ALL
            playWhenReady = true
            prepare()
        }
    }

    DisposableEffect(lifecycleOwner) {
        // Added this code to prevent exoplayer from running in background
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                // The app went to background or user switched apps -> Pause the hardware stream
                Lifecycle.Event.ON_PAUSE -> {
                    exoPlayer.pause()
                }
                // User came back to the app foreground -> Resume playback smoothly
                Lifecycle.Event.ON_RESUME -> {
                    exoPlayer.play()
                }

                else -> {}
            }
        }

        // Register our observer onto the activity lifecycle
        lifecycleOwner.lifecycle.addObserver(observer)

        // Clean up: unregister observer and completely release the video decoder on view death
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
        }
    }

    // Inline standard layout container player
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                useController = false
            }
        },
        update = { playerView ->
            // --- THE FIXED SURFACE HAND-OFF ---
            if (isFullscreen) {
                // If full-screen is active, release the player from this view
                // so the dialog's PlayerView can claim the surface safely
                playerView.player = null
            } else {
                // When coming back, explicitly re-attach the engine to force a surface redraw
                if (playerView.player != exoPlayer) {
                    playerView.player = null // Clear old texture cache reference
                    playerView.player = exoPlayer
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp)
            .clip(RoundedCornerShape(12.dp))
            .pointerInput(isFullscreen) {
                detectTapGestures(
                    onTap = { if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play() },
                    onDoubleTap = { isFullscreen = true }
                )
            }
    )

    // Fullscreen Overlay Dialog
    if (isFullscreen) {
        Dialog(
            onDismissRequest = { isFullscreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play() },
                            onDoubleTap = { isFullscreen = false }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            useController = false
                        }
                    },
                    update = { fullscreenPlayerView ->
                        // Claim the player engine for the fullscreen view layer
                        if (fullscreenPlayerView.player != exoPlayer) {
                            fullscreenPlayerView.player = exoPlayer
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun AudioPlayerRegion(audioPath: String) {
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableFloatStateOf(0f) }
    var totalDuration by remember { mutableFloatStateOf(0f) }

    // Initialize MediaPlayer instance bound to the life of this view
    LaunchedEffect(audioPath) {
        val player = MediaPlayer().apply {
            setDataSource(File(audioPath).absolutePath)
            prepare()
            start()
        }
        mediaPlayer = player
        totalDuration = player.duration.toFloat()
        isPlaying = true

        // Set up completion listener to clean up UI state when playback ends naturally
        player.setOnCompletionListener {
            isPlaying = false
            currentPosition = 0f
        }
    }

    // Coroutine loop to track and update the seek bar thumb track dynamically
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isPlaying && mediaPlayer != null) {
                mediaPlayer?.let {
                    currentPosition = it.currentPosition.toFloat()
                }
                delay(100.milliseconds) // Poll every 100 milliseconds for fluid feedback transitions
            }
        }
    }

    // Clean up and free decoder hardware instances on sheet dismiss
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.apply {
                stop()
                release()
            }
            mediaPlayer = null
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Play / Pause Toggle Action Node
            IconButton(
                onClick = {
                    mediaPlayer?.let { player ->
                        if (player.isPlaying) {
                            player.pause()
                            isPlaying = false
                        } else {
                            player.start()
                            isPlaying = true
                        }
                    }
                },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    painter = painterResource(
                        id = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                    ),
                    contentDescription = if (isPlaying) stringResource(R.string.pause_audio) else stringResource(
                        R.string.play_audio
                    ),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Stream Progress Slider Tracker Panel
            Column(modifier = Modifier.weight(1f)) {
                Slider(
                    value = currentPosition,
                    valueRange = 0f..totalDuration.coerceAtLeast(1f),
                    onValueChange = { seekTarget ->
                        currentPosition = seekTarget
                        mediaPlayer?.seekTo(seekTarget.toInt())
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Optional: Dynamic duration timestamp tracker layout strings
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatMs(currentPosition.toInt()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatMs(totalDuration.toInt()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// Simple internal helper to convert milliseconds to standard mm:ss format strings cleanly
@SuppressLint("DefaultLocale")
private fun formatMs(ms: Int): String {
    val seconds = (ms / 1000) % 60
    val minutes = (ms / (1000 * 60)) % 60
    return String.format("%02d:%02d", minutes, seconds)
}

@Composable
fun rememberDragDropState(
    lazyListState: LazyListState,
    onMove: (Int, Int) -> Unit
): DragDropState {
    val currentOnMove by rememberUpdatedState(onMove)
    return remember(lazyListState) {
        DragDropState(lazyListState, currentOnMove)
    }
}
