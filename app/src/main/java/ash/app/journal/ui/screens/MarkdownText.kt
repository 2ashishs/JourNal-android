package ash.app.journal.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
    val urlRegex = remember { """^<(https?://[^\s>]+)>$""".toRegex() }
    val bulletRegex = remember { """^(\s*)([-*+]\s+)(.*)$""".toRegex() }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        lines.forEach { line ->
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
                            text = buildAnnotatedString { parseInlineLinks("<$url>") },
                            style = style.copy(color = color)
                        )
                    }

                    // Case-3: Empty URL -> Ignore completely
                    else -> {}
                }
            } else {
                val isBullet = bulletMatch != null
                val annotatedString = buildAnnotatedString {
                    when {
                        trimmedLine.startsWith("# ") -> {
                            withStyle(
                                SpanStyle(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = (style.fontSize.value + 4).sp
                                )
                            ) {
                                parseInlineLinks(trimmedLine.removePrefix("# "))
                            }
                        }

                        bulletMatch != null -> {
                            val indent = bulletMatch.groupValues[1]
                            val bulletContent = bulletMatch.groupValues[3]
                            withStyle(SpanStyle(fontWeight = FontWeight.Normal)) {
                                append("$indent•  ")
                                parseInlineLinks(bulletContent)
                            }
                        }

                        else -> parseInlineLinks(line)
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