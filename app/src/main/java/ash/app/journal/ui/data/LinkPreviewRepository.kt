package ash.app.journal.ui.data

import ash.app.journal.ui.models.LinkMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup

class LinkPreviewRepository(private val okHttpClient: OkHttpClient) {

    /**
     * Scrapes a URL for Open Graph or fallback metadata.
     * Returns a valid data model, or null if it fails or fields are entirely missing.
     */
    suspend fun fetchMetadata(targetUrl: String): LinkMetadata? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(targetUrl)
            .header("User-Agent", "Mozilla/150.0 (Linux; Android 17 ; Mobile)")
            .header("Accept-Charset", "UTF-8")
            .header("Accept-Language", "en")
            .header("Connection", "keep-alive")
            .build()

        try {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val html = response.body?.string() ?: return@withContext null

                val document = Jsoup.parse(html, targetUrl)

                // Parse out Title (OG tag fallback to standard title tag)
                val title = document.select("meta[property=og:title]").attr("content")
                    .ifEmpty { document.title() }
                    .trim()

                // Parse out Description (OG tag fallback to description name tag)
                val description = document.select("meta[property=og:description]").attr("content")
                    .ifEmpty { document.select("meta[name=description]").attr("content") }
                    .trim()

                // Parse out Preview Image URL
                val imageUrl = document.select("meta[property=og:image]").attr("content").trim()

                // Ensure we have at least a fallback title before committing
                if (title.isEmpty()) return@withContext null

                LinkMetadata(
                    url = targetUrl,
                    title = title,
                    description = description.ifEmpty { "View link" },
                    imageUrl = imageUrl
                )
            }
        } catch (e: Exception) {
            null // Network timeout or unreachable host boundary failure
        }
    }
}