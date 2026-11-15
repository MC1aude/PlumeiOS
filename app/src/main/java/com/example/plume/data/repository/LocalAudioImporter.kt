package com.example.plume.data.repository

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.example.plume.data.db.TrackDao
import com.example.plume.data.db.TrackEntity
import com.example.plume.domain.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Imports audio files picked with the Storage Access Framework.
 *
 * The URI permission is persisted, metadata (title/artist/album/duration/artwork)
 * is read with [MediaMetadataRetriever] and the result is stored in Room so the
 * track can be played by Media3 like any other library item.
 */
class LocalAudioImporter(
    private val context: Context,
    private val trackDao: TrackDao
) {

    private data class AudioMetadata(
        val title: String?,
        val artist: String?,
        val album: String,
        val durationSeconds: Int,
        val artworkUrl: String
    )

    suspend fun import(uri: Uri): Result<Track> = withContext(Dispatchers.IO) {
        try {
            persistPermission(uri)

            val id = "local_" + UUID.nameUUIDFromBytes(uri.toString().toByteArray())
            val displayName = queryDisplayName(uri)
            val metadata = readMetadata(uri, id)

            val track = Track(
                id = id,
                title = metadata.title
                    ?: displayName?.substringBeforeLast('.')?.takeIf { it.isNotBlank() }
                    ?: "Unknown track",
                artistName = metadata.artist ?: "Unknown artist",
                albumName = metadata.album,
                coverUrl = metadata.artworkUrl,
                audioUrl = uri.toString(),
                downloadUrl = "",
                durationSeconds = metadata.durationSeconds,
                isFavorite = false,
                isDownloaded = false,
                localPath = uri.toString(),
                isLocalFile = true
            )

            trackDao.upsertTrack(TrackEntity.fromDomainTrack(track))
            Result.success(track)
        } catch (t: Throwable) {
            t.printStackTrace()
            Result.failure(t)
        }
    }

    private fun persistPermission(uri: Uri) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
    }

    private fun readMetadata(uri: Uri, trackId: String): AudioMetadata {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val artworkUrl = retriever.embeddedPicture
                ?.let { bytes -> saveArtwork(trackId, bytes) }
                .orEmpty()

            AudioMetadata(
                title = retriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() },
                artist = retriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?: retriever
                        .extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
                        ?.trim()
                        ?.takeIf { it.isNotEmpty() },
                album = retriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                    ?.trim()
                    .orEmpty(),
                durationSeconds = (
                    (retriever
                        .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        ?.toLongOrNull() ?: 0L) / 1000L
                    ).toInt(),
                artworkUrl = artworkUrl
            )
        } catch (t: Throwable) {
            t.printStackTrace()
            AudioMetadata(null, null, "", 0, "")
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun saveArtwork(trackId: String, bytes: ByteArray): String {
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return ""
        val dir = File(context.filesDir, "artwork").apply { mkdirs() }
        val file = File(dir, "$trackId.jpg")
        FileOutputStream(file).use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, output)
        }
        return Uri.fromFile(file).toString()
    }

    /** Removes the cached artwork written for an imported track, if any. */
    fun deleteArtwork(trackId: String): Boolean {
        return runCatching {
            File(File(context.filesDir, "artwork"), "$trackId.jpg").delete()
        }.getOrDefault(false)
    }

    private fun queryDisplayName(uri: Uri): String? = runCatching {
        context.contentResolver
            .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
    }.getOrNull()
}
