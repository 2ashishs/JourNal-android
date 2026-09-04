package ash.app.journal.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

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
