package com.example.plume.data.repository

import android.util.Log
import com.example.plume.data.api.soundcloud.SoundCloudApiService
import com.example.plume.data.api.soundcloud.SoundCloudTrackDto
import com.example.plume.domain.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Single source of music for Plume: SoundCloud API v2.
 *
 * The [SOUNDCLOUD_CLIENT_ID] is used for every request. When SoundCloud responds
 * with 401 (the hardcoded id expired), a fresh id is scraped from the JS bundles
 * of https://soundcloud.com and the request is retried automatically.
 */
class SoundCloudRepository {

    companion object {
        private const val TAG = "SoundCloud"

        /** Public SoundCloud client_id used by the web player. */
        const val SOUNDCLOUD_CLIENT_ID = "QhVEWRrJKvW4uSU5u5EolOFTXYUgz"

        private const val BASE_URL = "https://api-v2.soundcloud.com/"
        private const val SOUNDCLOUD_HOME = "https://soundcloud.com/"
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

        private val CLIENT_ID_PATTERN =
            Regex("""client_id["']?\s*[:=]\s*["']([A-Za-z0-9]{32})["']""")
        private val SCRIPT_SRC_PATTERN =
            Regex("<script[^>]+src=\"([^\"]+)\"")

        /** Last client_id successfully scraped from soundcloud.com (shared by all repositories). */
        @Volatile
        private var sharedClientId: String? = null
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    private val apiService: SoundCloudApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SoundCloudApiService::class.java)
    }

    private fun currentClientId(): String = sharedClientId ?: SOUNDCLOUD_CLIENT_ID

    // ---------------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------------

    /** Searches SoundCloud tracks by a free-text query. */
    suspend fun searchSongs(query: String): Result<List<Song>> = withContext(Dispatchers.IO) {
        runCatching {
            val normalizedQuery = query.ifBlank { "phonk" }
            val response = withClientId { clientId ->
                apiService.searchTracks(
                    query = normalizedQuery,
                    clientId = clientId,
                    limit = 30
                )
            }
            response.collection.orEmpty().mapNotNull { it.toSong() }
        }.onFailure { Log.e(TAG, "Search failed for \"$query\"", it) }
    }

    /** Loads the SoundCloud trending chart. */
    suspend fun getTrendingSongs(): Result<List<Song>> = withContext(Dispatchers.IO) {
        runCatching {
            val response = withClientId { clientId ->
                apiService.getTrendingTracks(clientId = clientId, limit = 30)
            }
            response.collection.orEmpty().mapNotNull { it.track?.toSong() }
        }.onFailure { Log.e(TAG, "Failed to load trending chart", it) }
    }

    /**
     * Home feed: fresh trending tracks plus a phonk selection, de-duplicated by id.
     */
    suspend fun getHomeFeed(): Result<List<Song>> = withContext(Dispatchers.IO) {
        val trending = getTrendingSongs().getOrDefault(emptyList())
        val phonk = searchSongs("phonk").getOrDefault(emptyList())

        val merged = (trending + phonk).distinctBy { it.id }
        if (merged.isNotEmpty()) {
            Result.success(merged)
        } else {
            Result.failure(Exception("Не удалось загрузить треки SoundCloud"))
        }
    }

    /**
     * Resolves a SoundCloud transcoding endpoint into a direct audio URL.
     * Direct http(s) URLs are returned unchanged.
     */
    suspend fun getStreamUrl(streamEndpoint: String): Result<String> = withContext(Dispatchers.IO) {
        if (streamEndpoint.isBlank()) {
            return@withContext Result.failure(Exception("Пустая ссылка на аудиопоток"))
        }

        val needsResolution = streamEndpoint.contains("api-v2.soundcloud.com") ||
            streamEndpoint.contains("api.soundcloud.com") ||
            streamEndpoint.contains("/media/") ||
            streamEndpoint.contains("/stream/")

        if (!needsResolution) {
            return@withContext Result.success(streamEndpoint)
        }

        runCatching {
            val response = withClientId { clientId ->
                apiService.getTrackStream(streamUrl = streamEndpoint, clientId = clientId)
            }
            response.url?.takeIf { it.startsWith("http") }
                ?: throw Exception("SoundCloud не вернул прямую ссылку на аудиопоток")
        }.onFailure { Log.e(TAG, "Failed to resolve stream URL", it) }
    }

    // ---------------------------------------------------------------------
    // Auth (401 -> scrape fresh client_id)
    // ---------------------------------------------------------------------

    private suspend fun <T> withClientId(block: suspend (String) -> T): T {
        val clientId = currentClientId()
        return try {
            block(clientId)
        } catch (e: Exception) {
            if (!isUnauthorized(e)) throw e

            Log.w(TAG, "Received 401 for client_id, refreshing from soundcloud.com")
            val fresh = refreshClientId() ?: throw e
            sharedClientId = fresh
            block(fresh)
        }
    }

    private fun isUnauthorized(error: Throwable): Boolean {
        if (error is HttpException && error.code() == 401) return true
        val message = error.message ?: return false
        return message.contains("401")
    }

    /**
     * Scrapes a fresh client_id from the SoundCloud homepage or its JS bundles.
     * Runs blocking I/O, so it is only called from [Dispatchers.IO] contexts.
     */
    private fun refreshClientId(): String? {
        return try {
            val html = fetchText(SOUNDCLOUD_HOME)

            CLIENT_ID_PATTERN.find(html)?.groupValues?.get(1)?.let { return it }

            SCRIPT_SRC_PATTERN.findAll(html)
                .map { it.groupValues[1] }
                .filter { it.contains("sndcdn.com") && it.endsWith(".js") }
                .distinct()
                .take(25)
                .forEach { scriptUrl ->
                    val bundle = fetchText(scriptUrl)
                    CLIENT_ID_PATTERN.find(bundle)?.groupValues?.get(1)?.let { return it }
                }

            null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to scrape a fresh client_id", e)
            null
        }
    }

    private fun fetchText(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "*/*")
            .build()

        return httpClient.newCall(request).execute().use { response ->
            response.body?.string().orEmpty()
        }
    }

    // ---------------------------------------------------------------------
    // Mapping
    // ---------------------------------------------------------------------

    private fun SoundCloudTrackDto.toSong(): Song? {
        if (streamable == false) return null

        val trackTitle = title?.takeIf { it.isNotBlank() } ?: return null
        val transcodings = media?.transcodings.orEmpty()

        val transcoding = transcodings.firstOrNull {
            it.url != null && it.format?.protocol == "progressive"
        } ?: transcodings.firstOrNull { it.url != null } ?: return null

        val artwork = (artwork_url ?: user?.avatar_url)
            ?.replace("-large.", "-t500x500.")
            ?.replace("large.jpg", "t500x500.jpg")
            ?: ""

        return Song(
            id = id.toString(),
            title = trackTitle,
            artist = user?.username?.takeIf { it.isNotBlank() } ?: "SoundCloud",
            thumbnailUrl = artwork,
            streamEndpoint = transcoding.url!!,
            durationMs = duration ?: 0L
        )
    }
}
