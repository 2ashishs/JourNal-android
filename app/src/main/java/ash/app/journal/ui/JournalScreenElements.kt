package ash.app.journal.ui

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.Composable
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
import androidx.core.content.FileProvider
import ash.app.journal.R
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

    val context = LocalContext.current // Grab the Android Context for Intent launching

    Scaffold(
        topBar = { TopAppBar(title = { Text("JourNaL") }) }
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
                // Using itemsIndexed lets us bind animations to stable physical layout index slots
                itemsIndexed(entries, key = { _, entry -> entry.id }) { index, entry ->
                    // FIXED: Compare the static item index directly with our tracking state pointer
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

            // Standard Bottom Central "+" Placement Trigger Button
            FloatingActionButton(
                onClick = { isCreateSheetOpen = true },
                shape = RoundedCornerShape(50),
                containerColor = MaterialTheme.colorScheme.primary,
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
            onPhotoCapture = viewModel::onPhotoCaptured,
            onSave = {
                viewModel.saveCurrentEntry()
                isCreateSheetOpen = false
            },
            onDismiss = {
                viewModel.clearDraft() // Reset state cleanup on dismiss
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

                    // EXTRA_SUBJECT handles the email header line gracefully
                    putExtra(Intent.EXTRA_SUBJECT, entry.title)

                    if (entry.photoPath != null) {
                        // --- TEXT + IMAGE SHARING FLOW ---
                        type = "image/*" // Tells Android the primary attachment payload is an image

                        putExtra(Intent.EXTRA_TEXT, shareBody)

                        // Convert the private file path string back into a file handle
                        val imageFile = File(entry.photoPath)
                        val authority = "${context.packageName}.fileprovider"

                        // Convert to secure content:// URI via FileProvider
                        val secureImageUri =
                            FileProvider.getUriForFile(context, authority, imageFile)

                        // Attach the secure media stream path hook
                        putExtra(Intent.EXTRA_STREAM, secureImageUri)

                        // CRITICAL SECURITY FLAG: Explicitly grants read permissions to the receiving app
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    } else {
                        // --- TEXT-ONLY FALLBACK FLOW ---
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareBody)
                    }
                }

                // Wrap in a system layout chooser container
                val chooserIntent =
                    Intent.createChooser(shareIntent, "Share entry via")
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .border(
                width = 1.dp,
                color = Color.LightGray,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = JournalColors.DefaultSurface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = entry.title.ifBlank { "Untitled Entry" },
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1C1C1A),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEntryBottomSheet(
    draftState: JournalDraftState,
    onTitleChange: (String) -> Unit,
    onDetailsChange: (String) -> Unit,
    onColorSelect: (String?) -> Unit,
    onPhotoCapture: (String?) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var tempPhotoPath by remember { mutableStateOf<String?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempPhotoPath != null) {
            onPhotoCapture(tempPhotoPath)
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
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
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
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

            // Color Selection Row (Including the Clear Option)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. "No Color Selected" Option Button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(
                            width = if (draftState.selectedHexColor == null) 2.dp else 1.dp,
                            color = if (draftState.selectedHexColor == null) MaterialTheme.colorScheme.primary else Color.LightGray,
                            shape = CircleShape
                        )
                        .clickable { onColorSelect(null) },
                    contentAlignment = Alignment.Center
                ) {
                    // Draws a clean diagonal "No" slash indicator line
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

            // Square Thumbnail Preview Layout Container Block
            draftState.capturedPhotoPath?.let { path ->
                Image(
                    painter = rememberAsyncImagePainter(File(path)),
                    contentDescription = "Captured thumbnail",
                    contentScale = ContentScale.Crop, // Crops target visually inside layout boundaries
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = {
                        val file = createTempImageFile(context)
                        val authority = "${context.packageName}.fileprovider"
                        val uri = FileProvider.getUriForFile(context, authority, file)

                        tempPhotoPath = file.absolutePath
                        cameraLauncher.launch(uri)
                    }
                ) {
                    Text(if (draftState.capturedPhotoPath != null) "Retake Photo" else "Photo")
                }

                Button(
                    onClick = onSave,
                    enabled = draftState.title.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
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
        containerColor = Color.White
    ) {
        // Main container that handles safe system navigation spacing at the bottom
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {

            // ==========================================
            // ANCHORED ZONE (Always visible at the top)
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.Black
                )

                HorizontalDivider(
                    thickness = DividerDefaults.Thickness,
                    color = DividerDefaults.color
                )
            }

            // ==========================================
            // SCROLLABLE ZONE (Only Details & Photo scroll)
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(
                        1f,
                        fill = false
                    ) // Shrinks to fit short content, caps at max screen height
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = 24.dp,
                        end = 24.dp,
                        top = 8.dp,
                        bottom = 8.dp
                    ), // Tighter vertical gaps
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Text(
                    text = entry.details,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.DarkGray
                )

                // Photo Layout: Preserves aspect ratio completely without cropping details
                entry.photoPath?.let { path ->
                    Image(
                        painter = rememberAsyncImagePainter(File(path)),
                        contentDescription = entry.title,
                        contentScale = ContentScale.FillWidth, // Preserves every pixel detail
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
            }

            // Divider separating content from action target zone
            HorizontalDivider(thickness = 1.dp, color = Color.LightGray)

            // ==========================================
            // FIXED ACTION ZONE (Low-profile, uncompressed)
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp), // Reduced from 56.dp+ to keep it low-profile and tight
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Target Box: Delete Action
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

                // Center Target Box: Share Action
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
                            tint = Color.DarkGray
                        )
                    }
                }

                // Right Target Box: Edit Action (Ergonomic thumb placement)
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
                            tint = Color.DarkGray
                        )
                    }
                }
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