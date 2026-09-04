package ash.app.journal.ui.screens

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

object MarkdownToolbarUtils {

    /**
     * Wraps current selection with [prefix] and [suffix].
     * If no selection, inserts them and puts cursor between them.
     */
    fun wrapSelection(
        current: TextFieldValue,
        prefix: String,
        suffix: String = prefix
    ): TextFieldValue {
        val text = current.text
        val selection = current.selection
        val start = selection.min
        val end = selection.max

        return if (selection.collapsed) {
            // No selection: insert `prefix` and `suffix`, place cursor in the middle
            val newText = text.substring(0, start) + prefix + suffix + text.substring(start)
            TextFieldValue(
                text = newText,
                selection = TextRange(start + prefix.length)
            )
        } else {
            // Wrap selected text
            val selectedText = text.substring(start, end)
            val newText =
                text.substring(0, start) + prefix + selectedText + suffix + text.substring(end)
            TextFieldValue(
                text = newText,
                selection = TextRange(start + prefix.length, end + prefix.length)
            )
        }
    }

    /**
     * Toggles a line prefix (e.g. "# ", "## ", "- ", "> ") at the start of the line where the cursor sits.
     */
    fun toggleLinePrefix(
        current: TextFieldValue,
        prefix: String
    ): TextFieldValue {
        val text = current.text
        val cursor = current.selection.start

        // Identify current line boundary
        val lineStart = text.lastIndexOf('\n', (cursor - 1).coerceAtLeast(0)).let {
            if (it == -1) 0 else it + 1
        }
        val lineEnd = text.indexOf('\n', cursor).let {
            if (it == -1) text.length else it
        }

        val currentLine = text.substring(lineStart, lineEnd)

        return if (currentLine.startsWith(prefix)) {
            // Remove prefix
            val updatedLine = currentLine.removePrefix(prefix)
            val newText = text.substring(0, lineStart) + updatedLine + text.substring(lineEnd)
            val newCursor = (cursor - prefix.length).coerceAtLeast(lineStart)
            TextFieldValue(text = newText, selection = TextRange(newCursor))
        } else {
            // Strip any competing prefix (# , ## , > , - ) if switching heading/quote styles
            val cleanLine = currentLine
                .removePrefix("## ")
                .removePrefix("# ")
                .removePrefix("> ")
                .removePrefix("- ")
                .removePrefix("* ")
                .removePrefix("+ ")

            val lengthDiff = cleanLine.length - currentLine.length
            val updatedLine = prefix + cleanLine
            val newText = text.substring(0, lineStart) + updatedLine + text.substring(lineEnd)
            val newCursor = (cursor + prefix.length + lengthDiff).coerceIn(
                lineStart,
                lineStart + updatedLine.length
            )
            TextFieldValue(text = newText, selection = TextRange(newCursor))
        }
    }

    /**
     * Inserts <URL> link template or wraps selection in <...>
     */
    fun insertLink(current: TextFieldValue): TextFieldValue {
        val text = current.text
        val selection = current.selection
        val start = selection.min
        val end = selection.max

        return if (selection.collapsed) {
            val template = "https://"
            val newText = text.substring(0, start) + template + text.substring(start)
            TextFieldValue(
                text = newText,
                selection = TextRange(start, start + template.length)
            )
        } else {
            val selected = text.substring(start, end)
            val wrapped =
                if (selected.startsWith("http")) "<$selected>" else "[${selected}](https://)"
            val newText = text.substring(0, start) + wrapped + text.substring(end)
            TextFieldValue(
                text = newText,
                selection = TextRange(start + wrapped.length)
            )
        }
    }

    /**
     * Indents the line(s) at the current cursor or selection by [spaces] (default: 2 spaces).
     */
    fun indentText(
        current: TextFieldValue,
        spaces: String = "  "
    ): TextFieldValue {
        val text = current.text
        val selection = current.selection
        val start = selection.min
        val end = selection.max

        // Single-line / cursor placement
        if (selection.collapsed) {
            val lineStart = text.lastIndexOf('\n', (start - 1).coerceAtLeast(0)).let {
                if (it == -1) 0 else it + 1
            }
            val newText = text.substring(0, lineStart) + spaces + text.substring(lineStart)
            return TextFieldValue(
                text = newText,
                selection = TextRange(start + spaces.length)
            )
        }

        // Multi-line selection: indent every line touched by the selection
        val lineStart = text.lastIndexOf('\n', (start - 1).coerceAtLeast(0)).let {
            if (it == -1) 0 else it + 1
        }
        val lineEnd = text.indexOf('\n', end).let {
            if (it == -1) text.length else it
        }

        val selectedBlock = text.substring(lineStart, lineEnd)
        val lines = selectedBlock.split("\n")
        val indentedBlock = lines.joinToString("\n") { spaces + it }
        val newText = text.substring(0, lineStart) + indentedBlock + text.substring(lineEnd)

        // Adjust selection range to encompass the newly added spaces
        val addedSpacesCount = lines.size * spaces.length
        return TextFieldValue(
            text = newText,
            selection = TextRange(start + spaces.length, end + addedSpacesCount)
        )
    }
}