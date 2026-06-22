package ash.app.journal.ui.utils

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset

class DragDropState internal constructor(
    val lazyListState: LazyListState,
    private val onMove: (Int, Int) -> Unit
) {
    // Visual translation distance applied to the graphicsLayer
    var draggedDistance by mutableFloatStateOf(0f)

    var currentIndexOfDraggedItem by mutableStateOf<Int?>(null)

    // Absolute viewport coordinate of the dragging finger
    private var fingerAbsoluteY by mutableFloatStateOf(0f)

    // THE FIX: Remembers exactly where your thumb is pressing *inside* the card bounds
    private var touchOffsetWithinItem by mutableFloatStateOf(0f)

    fun onDragStart(offset: Offset) {
        lazyListState.layoutInfo.visibleItemsInfo
            .firstOrNull { item -> offset.y.toInt() in item.offset..(item.offset + item.size) }
            .also { item ->
                if (item != null) {
                    currentIndexOfDraggedItem = item.index

                    // 1. Calculate how many pixels down from the top of the card your thumb is pressing
                    touchOffsetWithinItem = offset.y - item.offset

                    // 2. Initialize the absolute finger position
                    fingerAbsoluteY = offset.y

                    // Start with 0 translation because the item is exactly where it belongs structurally
                    draggedDistance = 0f
                }
            }
    }

    fun onDrag(offset: Offset) {
        fingerAbsoluteY += offset.y
        draggedDistance += offset.y

        val currentIdx = currentIndexOfDraggedItem ?: return

        // Find which item is sitting under your finger's absolute position right now
        val targetItem = lazyListState.layoutInfo.visibleItemsInfo
            .firstOrNull { item -> fingerAbsoluteY.toInt() in item.offset..(item.offset + item.size) }

        if (targetItem != null) {
            val targetIdx = targetItem.index

            if (targetIdx != currentIdx) {
                // Trigger the database/state swap array shift
                onMove(currentIdx, targetIdx)
                currentIndexOfDraggedItem = targetIdx

                // 3. Find where the item structurally moved to after the list recomposed
                val currentVisibleElement = lazyListState.layoutInfo.visibleItemsInfo
                    .firstOrNull { item -> item.index == targetIdx }

                if (currentVisibleElement != null) {
                    // --- THE EXACT ALIGNMENT FIX ---
                    // The visual translation distance is your finger's absolute position,
                    // minus where the item structurally starts, minus that initial touch anchor offset.
                    draggedDistance =
                        fingerAbsoluteY - currentVisibleElement.offset - touchOffsetWithinItem
                }
            }
        }
    }

    fun onDragInterrupted() {
        draggedDistance = 0f
        fingerAbsoluteY = 0f
        touchOffsetWithinItem = 0f
        currentIndexOfDraggedItem = null
    }
}