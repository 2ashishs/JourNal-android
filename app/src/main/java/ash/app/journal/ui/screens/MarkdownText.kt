package ash.app.journal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ash.app.journal.ui.models.LinkMetadataEntity
import coil3.compose.AsyncImage

private sealed interface MarkdownBlock {
    data class CodeFence(val code: String) : MarkdownBlock
    data class RegularLine(val line: String) : MarkdownBlock
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

    // Parse text into regular lines and grouped fenced code blocks
    val blocks = remember(text) { parseMarkdownBlocks(text) }
    val urlRegex = remember { """^<(https?://[^\s>]+)>$""".toRegex() }
    val bulletRegex = remember { """^(\s*)([-*+]\s+)(.*)$""".toRegex() }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        blocks.forEach { block ->
            when (block) {
                // --- MULTI-LINE CODE BLOCK ---
                is MarkdownBlock.CodeFence -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        )
                    ) {
                        Text(
                            text = block.code,
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(12.dp)
                        )
                    }
                }

                // --- REGULAR MARKDOWN LINE ---
                is MarkdownBlock.RegularLine -> {
                    val line = block.line
                    val trimmedLine = line.trim()
                    val urlMatch = urlRegex.matchEntire(trimmedLine)
                    val bulletMatch = bulletRegex.matchEntire(line)

                    if (urlMatch != null) {
                        val url = urlMatch.groupValues[1]
                        val metadata = metadataMap[url]

                        when {
                            // Case-1: Metadata is available -> Render Card
                            metadata != null -> {
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
                                    text = buildAnnotatedString { parseInlineElements("<$url>") },
                                    style = style.copy(color = color)
                                )
                            }

                            // Case-3: Empty URL -> Ignore completely
                            else -> {}
                        }
                    } else if (trimmedLine.startsWith("> ")) {
                        // BLOCKQUOTE
                        val quoteContent = trimmedLine.removePrefix("> ").trim()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.5.dp)
                                    .height(IntrinsicSize.Min)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                            )
                            BasicText(
                                text = buildAnnotatedString {
                                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                                        parseInlineElements(quoteContent)
                                    }
                                },
                                style = style.copy(color = color.copy(alpha = 0.85f))
                            )
                        }
                    } else {
                        val isBullet = bulletMatch != null
                        val annotatedString = buildAnnotatedString {
                            when {
                                // H1 Heading
                                trimmedLine.startsWith("# ") -> {
                                    withStyle(
                                        SpanStyle(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = (style.fontSize.value + 4).sp
                                        )
                                    ) {
                                        parseInlineElements(trimmedLine.removePrefix("# "))
                                    }
                                }
                                // H2 Secondary Heading
                                trimmedLine.startsWith("## ") -> {
                                    withStyle(
                                        SpanStyle(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = (style.fontSize.value + 2).sp
                                        )
                                    ) {
                                        parseInlineElements(trimmedLine.removePrefix("## "))
                                    }
                                }
                                // Bullet Lists (*, +, -)
                                bulletMatch != null -> {
                                    val indent = bulletMatch.groupValues[1]
                                    val bulletContent = bulletMatch.groupValues[3]
                                    withStyle(SpanStyle(fontWeight = FontWeight.Normal)) {
                                        append("$indent•  ")
                                        parseInlineElements(bulletContent)
                                    }
                                }
                                // Otherwise parse for links
                                else -> parseInlineElements(line)
                            }
                        }

                        if (annotatedString.isNotEmpty() || line.isEmpty()) {
                            BasicText(
                                text = annotatedString,
                                style = style.copy(color = color),
                                modifier = Modifier.padding(vertical = if (isBullet) 2.dp else 0.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Pre-process text into either fenced code blocks or regular lines
private fun parseMarkdownBlocks(text: String): List<MarkdownBlock> {
    val result = mutableListOf<MarkdownBlock>()
    val lines = text.split("\n")
    var insideCodeFence = false
    val currentCodeLines = StringBuilder()

    for (line in lines) {
        val trimmed = line.trim()
        if (trimmed.startsWith("```")) {
            if (insideCodeFence) {
                // Closing fence
                result.add(MarkdownBlock.CodeFence(currentCodeLines.toString().trimEnd()))
                currentCodeLines.clear()
                insideCodeFence = false
            } else {
                // Opening fence
                insideCodeFence = true
            }
        } else if (insideCodeFence) {
            if (currentCodeLines.isNotEmpty()) currentCodeLines.append("\n")
            currentCodeLines.append(line)
        } else {
            result.add(MarkdownBlock.RegularLine(line))
        }
    }

    if (insideCodeFence && currentCodeLines.isNotEmpty()) {
        result.add(MarkdownBlock.CodeFence(currentCodeLines.toString().trimEnd()))
    }

    return result
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

// Parses inline links, bold, italic, and inline code snippet
private fun AnnotatedString.Builder.parseInlineElements(text: String) {
    val linkRegex = """(\[([^]]+)]\((https?://[^\s)]+)\))|(<(https?://[^\s>]+)>)""".toRegex()
    var lastIndex = 0

    linkRegex.findAll(text).forEach { matchResult ->
        if (matchResult.range.first > lastIndex) {
            appendFormattedTokens(text.substring(lastIndex, matchResult.range.first))
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
            appendFormattedTokens(displayText)
        }

        lastIndex = matchResult.range.last + 1
    }

    if (lastIndex < text.length) {
        appendFormattedTokens(text.substring(lastIndex))
    }
}

// Parses bold (**text**), italic (*text*), and inline code, in any line
private fun AnnotatedString.Builder.appendFormattedTokens(lineText: String) {
    var currentIndex = 0
    // Matches inline code (`...`), bold (**...**), or italic (*...*)
    val pattern = Regex("(`[^`]+`|\\*\\*.*?\\*\\*|\\*.*?\\*)")
    val matches = pattern.findAll(lineText)

    for (match in matches) {
        if (match.range.first > currentIndex) {
            append(lineText.substring(currentIndex, match.range.first))
        }

        val token = match.value
        when {
            // Inline Code
            token.startsWith("`") && token.endsWith("`") && token.length >= 2 -> {
                withStyle(
                    style = SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = Color.Gray.copy(alpha = 0.2f),
                        fontSize = 13.sp
                    )
                ) {
                    append(" ${token.removeSurrounding("`")} ")
                }
            }
            // Bold
            token.startsWith("**") && token.endsWith("**") -> {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(token.removeSurrounding("**"))
                }
            }
            // Italics
            token.startsWith("*") && token.endsWith("*") -> {
                withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                    append(token.removeSurrounding("*"))
                }
            }
            // as-is
            else -> append(token)
        }
        currentIndex = match.range.last + 1
    }

    if (currentIndex < lineText.length) {
        append(lineText.substring(currentIndex))
    }
}