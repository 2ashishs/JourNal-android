package ash.app.journal.ui.data

import ash.app.journal.ui.models.LinkMetadataEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup

class LinkMetadataRepository(
    private val okHttpClient: OkHttpClient,
    private val linkMetadataDao: LinkMetadataDao,
) {

    /**
     * Scrapes a URL for Open Graph or fallback metadata.
     * Returns a valid data model, or null if it fails or fields are entirely missing.
     */
    suspend fun getOrFetchMetadata(targetUrl: String): LinkMetadataEntity? =
        withContext(Dispatchers.IO) {
            // Check local DB cache first
            val cached = linkMetadataDao.getMetadataForUrl(targetUrl)
            if (cached != null) return@withContext cached

            // Fetch from network if not in DB
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

                    val title = document.select("meta[property=og:title]").attr("content")
                        .ifEmpty { document.title() }
                        .trim()

                    val description =
                        document.select("meta[property=og:description]").attr("content")
                            .ifEmpty { document.select("meta[name=description]").attr("content") }
                            .trim()

                    val imageUrl = document.select("meta[property=og:image]").attr("content").trim()

                    // If metadata is completely missing, return null to trigger <url> fallback
                    if (title.isEmpty()) return@withContext null

                    val entity = LinkMetadataEntity(
                        url = targetUrl,
                        title = title,
                        description = description.ifEmpty { "View link" },
                        imageUrl = imageUrl
                    )
                    // Save to Room database for future reuse
                    linkMetadataDao.insertMetadata(entity)
                    entity
                }
            } catch (e: Exception) {
                null
            }
        }

    suspend fun getMetadataListForUrls(urls: List<String>): Map<String, LinkMetadataEntity> =
        withContext(Dispatchers.IO) {
            if (urls.isEmpty()) return@withContext emptyMap()
            linkMetadataDao.getMetadataForUrls(urls).associateBy { it.url }
        }
}