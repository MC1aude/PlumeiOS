package com.example.plume.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import com.example.plume.domain.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Extracts and caches embedded album artwork for local files via [MediaMetadataRetriever].
 */
object LocalArtworkLoader {
    // In-memory cache for decoded artwork bitmaps
    private val memoryCache = LruCache<String, Bitmap>(40)

    /**
     * Resolves the artwork model to pass to Coil's AsyncImage (can be a String URL, file URI, or Bitmap).
     */
    suspend fun getArtworkModel(context: Context, track: Track): Any? = withContext(Dispatchers.IO) {
        // 1. If track already has a valid coverUrl
        if (track.coverUrl.isNotBlank()) {
            if (track.coverUrl.startsWith("file://")) {
                val path = Uri.parse(track.coverUrl).path
                if (path != null && File(path).exists()) {
                    return@withContext track.coverUrl
                }
            } else if (track.coverUrl.startsWith("http://") || track.coverUrl.startsWith("https://")) {
                return@withContext track.coverUrl
            }
        }

        // 2. If it's a local track with localPath or audioUrl
        val source = track.localPath?.takeIf { it.isNotBlank() }
            ?: track.audioUrl.takeIf { it.isNotBlank() }

        if (source != null && (track.isLocalFile || source.startsWith("content://") || source.startsWith("file://") || source.startsWith("/"))) {
            val cached = memoryCache.get(track.id)
            if (cached != null) return@withContext cached

            val bitmap = extractEmbeddedPicture(context, source)
            if (bitmap != null) {
                memoryCache.put(track.id, bitmap)
                return@withContext bitmap
            }
        }

        track.coverUrl.takeIf { it.isNotBlank() }
    }

    private fun extractEmbeddedPicture(context: Context, uriString: String): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            val uri = Uri.parse(uriString)
            when {
                uriString.startsWith("content://") || uriString.startsWith("android.resource://") -> {
                    retriever.setDataSource(context, uri)
                }
                uriString.startsWith("file://") -> {
                    retriever.setDataSource(uri.path)
                }
                uriString.startsWith("/") -> {
                    retriever.setDataSource(uriString)
                }
                else -> {
                    retriever.setDataSource(context, uri)
                }
            }

            val picture = retriever.embeddedPicture ?: return null

            // Decode dimensions to downsample if necessary
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(picture, 0, picture.size, options)

            var sampleSize = 1
            val maxDim = 800
            while (options.outWidth / sampleSize > maxDim || options.outHeight / sampleSize > maxDim) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            BitmapFactory.decodeByteArray(picture, 0, picture.size, decodeOptions)
        } catch (_: Throwable) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }
}

/**
 * Composable helper that remembers and asynchronously loads embedded artwork for a [Track].
 */
@Composable
fun rememberTrackArtwork(track: Track?): Any? {
    if (track == null) return null
    val context = LocalContext.current
    return produceState<Any?>(
        initialValue = track.coverUrl.takeIf { it.isNotBlank() },
        key1 = track.id,
        key2 = track.coverUrl,
        key3 = track.localPath
    ) {
        value = LocalArtworkLoader.getArtworkModel(context, track)
    }.value
}
