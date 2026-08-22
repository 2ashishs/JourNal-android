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
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.material3.TextButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Velocity
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
import ash.app.journal.ui.models.LinkMetadataEntity
import ash.app.journal.ui.theme.FadedGreyClose
import ash.app.journal.ui.theme.JournalTheme
import coil3.compose.AsyncImage
import coil3.compose.rememberAsyncImagePainter
import kotlinx.coroutines.delay
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

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
            onMagicWandPress = viewModel::processMagicWand,
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

@Composable
fun AudioRecordingIcon(
    isRecording: Boolean,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    // 1. Set up the infinite animation loop engine
    val transition = rememberInfiniteTransition(label = "EqualizerTransition")

    // Animate structural progress from 0.0 to 1.0 back and forth
    val animationProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "EqualizerProgress"
    )

    // 2. Draw via Canvas matching the exact 24dp footprint from your resource file
    Canvas(modifier = modifier.size(24.dp)) {
        // Compute layout scale coefficients mapping directly to your 960x960 viewport setup
        val scaleX = size.width / 960f
        val scaleY = size.height / 960f
        val barWidth = 80f * scaleX

        // --- MATH RULES FROM DECONSTRUCTED PATH DATA ---
        // Your vector base configuration lists these strict properties:
        // Mid Bars (X: 280, 600): Top Y = 240, Height = 480 (Static center at Y = 480)
        // Center Bar (X: 440): Top Y = 80, Height = 800 (Static center at Y = 480)
        // Extreme Bars (X: 120, 760): Top Y = 400, Height = 160 (Static center at Y = 480)

        val centerY = 480f * scaleY

        // Mid bars stay completely static at height 480
        val midBarHeight = 480f * scaleY

        // Center bar decreases with time (from 800 down to 240)
        val centerBarHeight = if (isRecording) {
            (800f - (560f * animationProgress)) * scaleY
        } else {
            800f * scaleY
        }

        // Extreme bars increase at the exact same time (from 160 up to 480)
        val extremeBarHeight = if (isRecording) {
            (160f + (320f * animationProgress)) * scaleY
        } else {
            160f * scaleY
        }

        // --- DRAW PATTERNS MATRICES ---

        // 1. Extreme Left (Base X: 120, Y: 400)
        drawRect(
            color = color,
            topLeft = Offset(120f * scaleX, centerY - (extremeBarHeight / 2f)),
            size = Size(barWidth, extremeBarHeight)
        )

        // 2. Mid Left (Base X: 280, Y: 240)
        drawRect(
            color = color,
            topLeft = Offset(280f * scaleX, centerY - (midBarHeight / 2f)),
            size = Size(barWidth, midBarHeight)
        )

        // 3. Center Bar (Base X: 440, Y: 80)
        drawRect(
            color = color,
            topLeft = Offset(440f * scaleX, centerY - (centerBarHeight / 2f)),
            size = Size(barWidth, centerBarHeight)
        )

        // 4. Mid Right (Base X: 600, Y: 240)
        drawRect(
            color = color,
            topLeft = Offset(600f * scaleX, centerY - (midBarHeight / 2f)),
            size = Size(barWidth, midBarHeight)
        )

        // 5. Extreme Right (Base X: 760, Y: 400)
        drawRect(
            color = color,
            topLeft = Offset(760f * scaleX, centerY - (extremeBarHeight / 2f)),
            size = Size(barWidth, extremeBarHeight)
        )
    }
}

fun handleBulletAutoContinue(
    oldValue: TextFieldValue,
    newValue: TextFieldValue
): TextFieldValue {
    val oldText = oldValue.text
    val newText = newValue.text

    // Check if exactly 1 character was added and that character is a newline
    if (newText.length == oldText.length + 1 &&
        newValue.selection.start > 0 &&
        newText[newValue.selection.start - 1] == '\n'
    ) {
        val cursorPosition = newValue.selection.start
        val textBeforeNewline = newText.substring(0, cursorPosition - 1)
        val lastLine = textBeforeNewline.substringAfterLast('\n')

        // Matches optional leading whitespace (spaces/tabs) followed by bullet marker
        val bulletRegex = """^(\s*)([-*+]\s+)""".toRegex()
        val matchResult = bulletRegex.find(lastLine)

        if (matchResult != null) {
            val indent = matchResult.groupValues[1]
            val bulletMarker = matchResult.groupValues[2]
            val fullPrefix = indent + bulletMarker

            // Case 1: Empty bullet item -> delete bullet prefix on enter (exit list)
            if (lastLine == fullPrefix.dropLastWhile { it == ' ' } || lastLine == fullPrefix) {
                val startOfLineIndex =
                    textBeforeNewline.lastIndexOf('\n').let { if (it == -1) 0 else it + 1 }
                val updatedText =
                    newText.substring(0, startOfLineIndex) + newText.substring(cursorPosition)
                return TextFieldValue(
                    text = updatedText,
                    selection = TextRange(startOfLineIndex)
                )
            }

            // Case 2: Continue list/sub-list with exact matching indentation
            val updatedText = newText.substring(
                0,
                cursorPosition
            ) + fullPrefix + newText.substring(cursorPosition)
            val newCursorPos = cursorPosition + fullPrefix.length
            return TextFieldValue(
                text = updatedText,
                selection = TextRange(newCursorPos)
            )
        }
    }

    return newValue
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
    onMagicWandPress: (String) -> Unit,
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

                IconButton(
                    modifier = Modifier.border(
                        1.dp,
                        MaterialTheme.colorScheme.onBackground,
                        RoundedCornerShape(8.dp)
                    ),
                    onClick = { onMagicWandPress(draftState.details) },
                ) {
                    Icon(
                        contentDescription = stringResource(R.string.magic_wand),
                        painter = painterResource(R.drawable.ic_magic_wand),
                        tint = MaterialTheme.colorScheme.onBackground,
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

@Composable
fun MarkdownText(
    text: String,
    style: TextStyle,
    color: Color,
    metadataMap: Map<String, LinkMetadataEntity> = emptyMap(),
    onFetchMetadata: (String) -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val lines = remember(text) { text.split("\n") }
    // Matches [card](url=https://...)
    val cardRegex = remember { """^\[card\]\(url=(.*?)\)$""".toRegex() }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        lines.forEach { line ->
            val cardMatch = cardRegex.matchEntire(line.trim())

            if (cardMatch != null) {
                val url = cardMatch.groups[1]?.value.orEmpty()
                val metadata = metadataMap[url]

                when {
                    // Case-1: Metadata is available -> Render Card
                    (metadata != null) -> {
                        LinkPreviewCard(
                            url = metadata.url,
                            title = metadata.title,
                            description = metadata.description,
                            imageUrl = metadata.imageUrl,
                            onCardClick = { uriHandler.openUri(metadata.url) }
                        )
                    }

                    // Case-2: URL exists but missing metadata -> Render <URL> & fetch metadata
                    url.isNotBlank() -> {
                        // Fetch metadata via lambda
                        LaunchedEffect(url) {
                            onFetchMetadata(url)
                        }
                        // Render as clean clickable link
                        BasicText(
                            text = buildAnnotatedString { parseInlineLinks("<$url>") },
                            style = style.copy(color = color)
                        )
                    }

                    // Case-3: Empty URL -> Ignore completely
                    else -> {}
                }
            } else {
                // --- STANDARD MARKDOWN TEXT ROW RUN ---[cite: 2]
                val annotatedString = buildAnnotatedString {
                    when {
                        line.startsWith("# ") -> {
                            withStyle(
                                SpanStyle(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = (style.fontSize.value + 4).sp
                                )
                            ) {
                                parseInlineLinks(line.removePrefix("# "))
                            }
                        }

                        line.startsWith("- ") || line.startsWith("* ") || line.startsWith("+ ") -> {
                            withStyle(SpanStyle(fontWeight = FontWeight.Normal)) {
                                append("•  ")
                                parseInlineLinks(
                                    line.removePrefix("- ").removePrefix("* ").removePrefix("+ ")
                                )
                            }
                        }

                        else -> parseInlineLinks(line)
                    }
                }

                if (annotatedString.isNotEmpty()) {
                    BasicText(
                        text = annotatedString,
                        style = style.copy(color = color),
                        modifier = Modifier.padding(
                            vertical = if (line.startsWith("- ") || line.startsWith("* ") || line.startsWith(
                                    "+ "
                                )
                            ) 2.dp else 0.dp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun LinkPreviewCard(
    url: String,
    title: String,
    description: String,
    imageUrl: String,
    onCardClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onCardClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Render the scraped web image token on the left if it exists safely
            if (imageUrl.isNotEmpty()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Link Preview Thumbnail",
                    modifier = Modifier
                        .width(128.dp)
                        .fillMaxHeight(),
                    contentScale = ContentScale.Crop
                )
            }

            // Title and Description text details on the right side block layout
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = url,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = TextDecoration.Underline
                )
            }
        }
    }
}

/**
 * Handles checking fallback named Markdown hooks within standard lines
 */
private fun AnnotatedString.Builder.parseInlineLinks(text: String) {
    val linkRegex = """(\[([^]]+)]\((https?://[^\s)]+)\))|(<(https?://[^\s>]+)>)""".toRegex()
    var lastIndex = 0

    linkRegex.findAll(text).forEach { matchResult ->
        if (matchResult.range.first > lastIndex) {
            appendLineText(text.substring(lastIndex, matchResult.range.first))
        }

        val isNamedLink = matchResult.groups[1] != null
        val displayText =
            if (isNamedLink) matchResult.groups[2]!!.value else matchResult.groups[5]!!.value
        val urlTarget =
            if (isNamedLink) matchResult.groups[3]!!.value else matchResult.groups[5]!!.value

        withLink(
            link = LinkAnnotation.Url(
                url = urlTarget,
                styles = TextLinkStyles(
                    style = SpanStyle(
                        color = Color(0xFF2196F3),
                        textDecoration = TextDecoration.Underline
                    )
                )
            )
        ) {
            appendLineText(displayText)
        }

        lastIndex = matchResult.range.last + 1
    }

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
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
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
                    .pointerInput(Unit) {
                        // Intercept upward drags on the header so they don't bounce the bottom sheet
                        detectDragGestures { change, dragAmount ->
                            if (dragAmount.y < 0f) {
                                change.consume() // Consumes upward drag on header
                            }
                        }
                    }
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

                            EntryMediaType.TEXT -> {
                                // Standard text layout flows cleanly with zero extra attachment
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

@Composable
private fun MissingMediaCard(
    mediaType: EntryMediaType,
    onClearMediaTag: () -> Unit
) {
    val mediaTypeName = when (mediaType) {
        EntryMediaType.PHOTO -> "Photo"
        EntryMediaType.VIDEO -> "Video"
        EntryMediaType.AUDIO -> "Audio file"
        EntryMediaType.TEXT -> "Media"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_close), // error / warning icon
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    text = stringResource(R.string.file_has_been_deleted_or_moved, mediaTypeName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }

            TextButton(onClick = onClearMediaTag) {
                Text(
                    text = stringResource(R.string.clear_tag, mediaTypeName),
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ZoomableImageView(
    imagePath: String,
    onDismiss: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) {
                    // Double-tap to toggle zoom (1x <-> 2.5x)
                    detectTapGestures(
                        onDoubleTap = { _ ->
                            if (scale > 1f) {
                                scale = 1f
                                offset = Offset.Zero
                            } else {
                                scale = 2.5f
                                offset = Offset.Zero
                            }
                        },
                        onTap = {
                            // Single tap anywhere on black background to dismiss
                            if (scale == 1f) {
                                onDismiss()
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    // Multi-finger pinch to zoom & pan
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 10f) // Cap zoom between 1x and 10x

                        if (scale > 1f) {
                            // Pan only when zoomed in
                            offset += pan
                        } else {
                            offset = Offset.Zero
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = rememberAsyncImagePainter(File(imagePath)),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    }
            )

            // Subtle close button at top-right
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 16.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.close_fullscreen),
                    tint = FadedGreyClose
                )
            }
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun LoopingVideoPlayer(videoPath: String) {
    val context = LocalContext.current
    // Access the host activity's lifecycle state
    val lifecycleOwner = LocalLifecycleOwner.current
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
        // Prevent exoplayer from running in background
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                // The app went to background or user switched apps -> Pause the hardware stream
                Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                // User came back to the app foreground -> Resume playback smoothly
                Lifecycle.Event.ON_RESUME -> exoPlayer.play()
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

    // Standard inline player view
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp)
            .clip(RoundedCornerShape(12.dp))
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply { useController = false }
            },
            update = { playerView ->
                if (isFullscreen) {
                    // If full-screen is active, release the player from this view
                    // so the dialog's PlayerView can claim the surface safely
                    playerView.player = null
                } else {
                    // When coming back, explicitly re-attach the engine to force a surface redraw
                    if (playerView.player != exoPlayer) {
                        // Clear old texture cache reference
                        playerView.player = null
                        playerView.player = exoPlayer
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay Box to handle gestures reliably without blocking playback surface
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                        },
                        onDoubleTap = {
                            // Ensure player is playing when opening full screen
                            exoPlayer.play()
                            isFullscreen = true
                        }
                    )
                }
        )
    }

    // Fullscreen Zoomable Video Dialog
    if (isFullscreen) {
        Dialog(
            onDismissRequest = { isFullscreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            var scale by remember { mutableFloatStateOf(1f) }
            var offset by remember { mutableStateOf(Offset.Zero) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                isFullscreen = false
//                                if (scale > 1f) {
//                                    scale = 1f
//                                    offset = Offset.Zero
//                                } else {
//                                    scale = 2.5f
//                                    offset = Offset.Zero
//                                }
                            },
                            onTap = {
                                if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(0.9f, 8f)

                            if (scale > 1f) {
                                offset += pan
                            } else {
                                offset = Offset.Zero
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply { useController = false }
                    },
                    update = { fullscreenPlayerView ->
                        // Claim the player engine for the fullscreen view layer
                        if (fullscreenPlayerView.player != exoPlayer) {
                            fullscreenPlayerView.player = exoPlayer
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x
                            translationY = offset.y
                        }
                )

                IconButton(
                    onClick = { isFullscreen = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 16.dp, end = 16.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.close_fullscreen),
                        tint = FadedGreyClose
                    )
                }
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
