package ash.app.journal.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ash.app.journal.R

@Composable
fun MarkdownToolbar(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Bold (**text**)
            ToolbarButton(onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onValueChange(MarkdownToolbarUtils.wrapSelection(value, "**"))
            }) {
                Text("B", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            }

            // Italic (*text*)
            ToolbarButton(onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onValueChange(MarkdownToolbarUtils.wrapSelection(value, "*"))
            }) {
                Text(
                    "I",
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            // H1 (# Heading)
            ToolbarButton(onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onValueChange(MarkdownToolbarUtils.toggleLinePrefix(value, "# "))
            }) {
                Text("H1", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            // H2 (## Subheading)
            ToolbarButton(onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onValueChange(MarkdownToolbarUtils.toggleLinePrefix(value, "## "))
            }) {
                Text("H2", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }

            // Bullet list (- item)
            ToolbarButton(onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onValueChange(MarkdownToolbarUtils.toggleLinePrefix(value, "- "))
            }) {
                Text("•—", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            // Quote (> quote)
            ToolbarButton(onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onValueChange(MarkdownToolbarUtils.toggleLinePrefix(value, "> "))
            }) {
                Text("”", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }

            // Inline Code (`code`)
            ToolbarButton(onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onValueChange(MarkdownToolbarUtils.wrapSelection(value, "`"))
            }) {
                Text(
                    "<>",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            // Link (<URL> or [title](url))
            ToolbarButton(onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onValueChange(MarkdownToolbarUtils.insertLink(value))
            }) {
                Icon(
                    painter = painterResource(R.drawable.ic_media_link),
                    contentDescription = "Insert Link",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun ToolbarButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(36.dp)
    ) {
        content()
    }
}