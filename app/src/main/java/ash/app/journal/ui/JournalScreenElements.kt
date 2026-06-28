package ash.app.journal.ui

import android.content.Context
import android.content.Intent
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import ash.app.journal.R
import ash.app.journal.ui.models.EntryMediaType
import ash.app.journal.ui.models.JournalDraftState
import ash.app.journal.ui.models.JournalEntry
import ash.app.journal.ui.utils.DragDropState
import coil3.compose.rememberAsyncImagePainter
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainJournalScreen(viewModel: JournalViewModel) {
    val entries by viewModel.journalEntries.collectAsState()
    val draftState by viewModel.draftState.collectAsState()

    var isCreateSheetOpen by remember { mutableStateOf(false) }
    var selectedEntryForDetail by remember { mutableStateOf<JournalEntry?>(null) }

    val lazyListState = rememberLazyListState()
    val dragDropState = rememberDragDropState(lazyListState = lazyListState) { from, to ->
        viewModel.moveEntry(from, to)
    }

    val context = LocalContext.current

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
                onClick = { isCreateSheetOpen = true },
                shape = RoundedCornerShape(50),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = "Add Entry"
                )
            }
        }
    }


    if (isCreateSheetOpen) {
        CreateEntryBottomSheet(
            draftState = draftState,
            onTitleChange = viewModel::onTitleChanged,
            onDetailsChange = viewModel::onDetailsChanged,
            onColorSelect = viewModel::onColorSelected,
            onMediaCapture = viewModel::onMediaCaptured,
            onSave = {
                viewModel.saveCurrentEntry()
                isCreateSheetOpen = false
            },
            onDismiss = {
                isCreateSheetOpen = false
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
                isCreateSheetOpen = true
            },
            onDeleteClick = {
                viewModel.deleteEntry(entry)
                selectedEntryForDetail = null
            },
            onShareClick = {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    val shareBody = "*${entry.title}*\n\n${entry.details}"
                    putExtra(Intent.EXTRA_SUBJECT, entry.title)

                    if (entry.mediaPath != null) {
                        type = "image/*"
                        putExtra(Intent.EXTRA_TEXT, shareBody)
                        val imageFile = File(entry.mediaPath)
                        val authority = "${context.packageName}.fileprovider"
                        val secureImageUri =
                            FileProvider.getUriForFile(context, authority, imageFile)
                        putExtra(Intent.EXTRA_STREAM, secureImageUri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    } else {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareBody)
                    }
                }
                val chooserIntent = Intent.createChooser(shareIntent, "Share entry via")
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
        EntryMediaType.PHOTO -> R.drawable.ic_media_photo    // Replace with your drawable resource names
        EntryMediaType.AUDIO -> R.drawable.ic_media_audio
        EntryMediaType.VIDEO -> R.drawable.ic_media_video
        EntryMediaType.TEXT -> R.drawable.ic_media_text
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
                contentDescription = "Content Type Indicator",
                tint = if (entry.hexColor != null) JournalColors.fromHex(entry.hexColor) else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(start = 16.dp)
                    .size(24.dp)
            )

            Text(
                text = entry.title.ifBlank { "Untitled Entry" },
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .padding(24.dp)
            )

            if (entry.hexColor != null) {
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .fillMaxHeight()
                        .background(JournalColors.fromHex(entry.hexColor))
                )
            }
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
    onColorSelect: (String?) -> Unit,
    onMediaCapture: (String?, EntryMediaType) -> Unit,
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
        "Title"
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
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
            )

            OutlinedTextField(
                value = draftState.details,
                onValueChange = onDetailsChange,
                label = { Text("Details") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. "No Color Selected" Option Button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(
                            width = if (draftState.selectedHexColor == null) 2.dp else 1.dp,
                            color = if (draftState.selectedHexColor == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape
                        )
                        .clickable { onColorSelect(null) },
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
                JournalColors.Palette.forEach { colorHex ->
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(JournalColors.fromHex(colorHex, Color.Gray))
                            .clickable { onColorSelect(colorHex) }
                            .border(
                                width = if (draftState.selectedHexColor == colorHex) 2.dp else 0.dp,
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
                        contentDescription = "Captured media preview",
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
                            contentDescription = "Remove Media",
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
                            Text("Photo")
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
                            Text("Video")
                        }
                    }

                    if (draftState.capturedMediaType == EntryMediaType.TEXT || draftState.capturedMediaType == EntryMediaType.AUDIO) {
                        Button(
                            onClick = { /* Future Audio note recording trigger pipeline hooks */ }
                        ) {
                            Text("Audio")
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
                    Text("Save")
                }
            }

        }
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
                Text(
                    text = entry.details,
                    style = MaterialTheme.typography.bodyLarge,
                    color = JournalColors.SecondaryMutedText
                )

                entry.mediaPath?.let { path ->
                    if (entry.mediaType == EntryMediaType.PHOTO) {
                        var isImageFullscreen by remember { mutableStateOf(false) }

                        Image(
                            painter = rememberAsyncImagePainter(File(path)),
                            contentDescription = entry.title,
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .pointerInput(Unit) {
                                    detectTapGestures(onDoubleTap = { isImageFullscreen = true })
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
                    } else if (entry.mediaType == EntryMediaType.VIDEO) {
                        LoopingVideoPlayer(entry.mediaPath)
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
                            contentDescription = "Delete",
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
                            contentDescription = "Share",
                            tint = JournalColors.SecondaryMutedText
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
                            contentDescription = "Edit",
                            tint = JournalColors.SecondaryMutedText
                        )
                    }
                }
            }
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun LoopingVideoPlayer(videoPath: String) {
    val context = LocalContext.current
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

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
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
                    .background(Color.Black)
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
fun rememberDragDropState(
    lazyListState: LazyListState,
    onMove: (Int, Int) -> Unit
): DragDropState {
    val currentOnMove by rememberUpdatedState(onMove)
    return remember(lazyListState) {
        DragDropState(lazyListState, currentOnMove)
    }
}
