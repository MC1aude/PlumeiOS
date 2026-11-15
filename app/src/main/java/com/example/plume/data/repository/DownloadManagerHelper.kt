package com.example.plume.data.repository

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/**
 * Handles two kinds of downloads:
 *  - the app-private offline cache (used by legacy entries), and
 *  - user facing files written to the public Downloads/Plume folder.
 */
class DownloadManagerHelper(private val context: Context) {

    private val downloadsDir: File
        get() {
            val dir = File(context.filesDir, "downloads")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return dir
        }

    // ── Public Downloads ─────────────────────────────────────────────────────

    private data class PublicTarget(
        val output: OutputStream,
        val uri: Uri?,
        val path: String
    )

    /**
     * Streams [source] (http(s), content:// or a local file path) into the public
     * Downloads/Plume directory.
     *
     * @return the local path/URI that can be handed to Media3 for offline playback.
     */
    suspend fun downloadToPublicDownloads(
        baseFileName: String,
        source: String,
        mimeType: String = "audio/mpeg"
    ): Result<String> = withContext(Dispatchers.IO) {
        var pendingUri: Uri? = null
        try {
            if (source.isBlank()) {
                Log.e("DownloadDebug", "Download failed: empty source")
                return@withContext Result.failure(IOException("Empty audio source"))
            }

            val extension = extensionFor(mimeType, source)
            val fileName = sanitizeFileName(baseFileName).ifBlank { "track" } + "." + extension
            val target = openPublicTarget(fileName, mimeType)
            pendingUri = target.uri

            target.output.use { output ->
                val input = openSourceStream(source)
                    ?: throw IOException("Unable to open audio stream from: $source")
                input.use { it.copyTo(output) }
            }

            if (target.uri != null) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }
                runCatching {
                    context.contentResolver.update(target.uri, values, null, null)
                }
            }

            Result.success(target.path)
        } catch (t: Throwable) {
            Log.e("DownloadDebug", "Download failed for source: $source", t)
            pendingUri?.let { uri ->
                runCatching { context.contentResolver.delete(uri, null, null) }
            }
            Result.failure(t)
        }
    }

    @SuppressLint("NewApi")
    private fun openPublicTarget(fileName: String, mimeType: String): PublicTarget {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                removePreviousExport(fileName)

                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, PLUME_RELATIVE_PATH)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val uri = resolver.insert(collection, values)
                if (uri != null) {
                    val output = resolver.openOutputStream(uri)
                    if (output != null) {
                        return PublicTarget(output, uri, uri.toString())
                    }
                }
            }
        } catch (_: Throwable) {
            // Fallback to app files dir if MediaStore fails
        }

        @Suppress("DEPRECATION")
        val dir = File(
            context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
                ?: Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "Plume"
        )
        if (!dir.exists() && !dir.mkdirs()) {
            throw IOException("Could not create the Downloads directory")
        }
        val file = File(dir, fileName)
        if (file.exists() && !file.delete()) {
            throw IOException("Could not replace the existing download")
        }
        return PublicTarget(FileOutputStream(file), null, file.absolutePath)
    }

    /** Removes a previously exported copy with the same name from Downloads/Plume. */
    @SuppressLint("NewApi")
    private fun removePreviousExport(fileName: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.RELATIVE_PATH
        )
        val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ?"
        runCatching {
            context.contentResolver
                .query(collection, projection, selection, arrayOf(fileName), null)
                ?.use { cursor ->
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                    val pathColumn = cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
                    val ids = mutableListOf<Long>()
                    while (cursor.moveToNext()) {
                        val relativePath = if (pathColumn >= 0) cursor.getString(pathColumn) else null
                        if (relativePath != null && relativePath.startsWith(PLUME_RELATIVE_PATH)) {
                            ids += cursor.getLong(idColumn)
                        }
                    }
                    ids.forEach { id ->
                        runCatching {
                            context.contentResolver.delete(
                                ContentUris.withAppendedId(collection, id),
                                null,
                                null
                            )
                        }
                    }
                }
        }
    }

    private fun openSourceStream(source: String): InputStream? {
        return when {
            source.startsWith("content://") ->
                context.contentResolver.openInputStream(Uri.parse(source))

            source.startsWith("file://") ->
                Uri.parse(source).path?.let { FileInputStream(File(it)) }

            source.startsWith("/") -> FileInputStream(File(source))

            else -> {
                var currentUrl = source
                var connection: HttpURLConnection? = null
                var redirects = 0
                while (redirects < 5) {
                    val url = URL(currentUrl)
                    connection = url.openConnection() as HttpURLConnection
                    connection.connectTimeout = 20_000
                    connection.readTimeout = 60_000
                    connection.instanceFollowRedirects = true
                    connection.setRequestProperty(
                        "User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
                    )
                    connection.setRequestProperty(
                        "Accept",
                        "audio/webm,audio/ogg,audio/wav,audio/*;q=0.9,*/*;q=0.8"
                    )
                    connection.setRequestProperty("Accept-Language", "en-US,en;q=0.5")
                    connection.connect()

                    val responseCode = connection.responseCode
                    if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                        responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                        responseCode == HttpURLConnection.HTTP_SEE_OTHER
                    ) {
                        val newUrl = connection.getHeaderField("Location")
                        connection.disconnect()
                        if (newUrl != null) {
                            currentUrl = newUrl
                            redirects++
                            continue
                        }
                    }

                    if (responseCode !in 200..299) {
                        val code = connection.responseCode
                        connection.disconnect()
                        throw IOException("HTTP error code: $code")
                    }
                    break
                }
                connection?.inputStream
            }
        }
    }

    private fun sanitizeFileName(raw: String): String {
        return raw
            .replace(Regex("[\\\\/:*?\"<>|\\r\\n\\t]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(120)
    }

    private fun extensionFor(mimeType: String, source: String): String {
        when (mimeType.lowercase(Locale.US)) {
            "audio/mpeg", "audio/mp3" -> return "mp3"
            "audio/mp4", "audio/m4a", "audio/x-m4a", "audio/aac" -> return "m4a"
            "audio/ogg", "audio/opus", "audio/x-ogg" -> return "ogg"
            "audio/flac", "audio/x-flac" -> return "flac"
            "audio/wav", "audio/x-wav", "audio/wave" -> return "wav"
        }
        val path = Uri.parse(source).lastPathSegment ?: source
        val extension = path.substringAfterLast('.', "").lowercase(Locale.US)
        return if (extension in AUDIO_EXTENSIONS) extension else "mp3"
    }

    // ── Deletion ─────────────────────────────────────────────────────────────

    /**
     * Deletes an audio file regardless of whether it lives in MediaStore, in a
     * legacy public folder or in the app-private download directory.
     */
    fun deleteAudioFile(trackId: String, localPath: String?): Boolean {
        val path = localPath?.takeIf { it.isNotBlank() }
        return try {
            when {
                path == null -> deleteDownloadedAudio(trackId)
                path.startsWith("content://") ->
                    context.contentResolver.delete(Uri.parse(path), null, null) > 0

                path.startsWith("file://") ->
                    Uri.parse(path).path?.let { File(it).delete() } ?: true

                else -> File(path).delete() || !File(path).exists()
            }
        } catch (t: Throwable) {
            t.printStackTrace()
            false
        }
    }

    // ── Legacy app-private cache ─────────────────────────────────────────────

    fun isDownloaded(trackId: String): Boolean {
        val file = File(downloadsDir, "track_$trackId.mp3")
        return file.exists() && file.length() > 0
    }

    fun getLocalPath(trackId: String): String? {
        val file = File(downloadsDir, "track_$trackId.mp3")
        return if (file.exists() && file.length() > 0) file.absolutePath else null
    }

    fun deleteDownloadedAudio(trackId: String): Boolean {
        val file = File(downloadsDir, "track_$trackId.mp3")
        return if (file.exists()) file.delete() else true
    }

    private companion object {
        /** Public folder (relative to the shared storage root) used for exports. */
        const val PLUME_RELATIVE_PATH = "Download/Plume/"

        val AUDIO_EXTENSIONS = setOf(
            "mp3", "m4a", "aac", "ogg", "opus", "flac", "wav", "mp4", "wma", "alac"
        )
    }
}
