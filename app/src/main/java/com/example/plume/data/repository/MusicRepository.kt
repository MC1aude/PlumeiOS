package com.example.plume.data.repository

import android.net.Uri
import com.example.plume.data.db.PlaylistEntity
import com.example.plume.data.db.PlaylistTrackCrossRef
import com.example.plume.data.db.PlumeDatabase
import com.example.plume.data.db.TrackEntity
import com.example.plume.domain.model.Playlist
import com.example.plume.domain.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Local library storage: favorites, downloads, imported files and playlists.
 * Automatically resolves SoundCloud streaming endpoints into direct CDN audio URLs
 * with guaranteed client_id fallbacks and multi-source download resilience.
 */
class MusicRepository(
    private val database: PlumeDatabase,
    private val downloadHelper: DownloadManagerHelper,
    private val localAudioImporter: LocalAudioImporter
) {
    private val soundCloudRepository = SoundCloudRepository()

    // --- Favorites ---

    fun getFavoriteTracks(): Flow<List<Track>> {
        return database.trackDao().getFavoriteTracks()
            .map { entities ->
                entities.map { entity ->
                    val track = entity.toDomainWithLocalState()
                    resolveStreamIfNeeded(track)
                }
            }
            .flowOn(Dispatchers.IO)
    }

    suspend fun setFavorite(track: Track, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        val resolvedTrack = resolveSoundCloudStream(track).copy(isFavorite = isFavorite)
        database.trackDao().upsertTrack(TrackEntity.fromDomainTrack(resolvedTrack))
        database.trackDao().updateFavoriteStatus(resolvedTrack.id, isFavorite)
    }

    // --- Downloads / Offline Library ---

    fun getDownloadedTracks(): Flow<List<Track>> {
        return database.trackDao().getDownloadedTracks()
            .map { entities ->
                entities.map { entity ->
                    val track = entity.toDomainWithLocalState()
                    resolveStreamIfNeeded(track)
                }
            }
            .flowOn(Dispatchers.IO)
    }

    /**
     * Resolves the track's stream endpoint if needed, checks internal cache,
     * and downloads the audio bytes into the public Downloads/Plume folder.
     */
    suspend fun downloadTrack(track: Track): Result<String> = withContext(Dispatchers.IO) {
        val cachedPath = downloadHelper.getLocalPath(track.id)
        if (cachedPath != null) {
            val mimeType = guessMimeType(cachedPath)
            val baseName = buildString {
                if (track.artistName.isNotBlank()) {
                    append(track.artistName)
                    append(" - ")
                }
                append(track.title)
            }
            return@withContext downloadHelper.downloadToPublicDownloads(baseName, cachedPath, mimeType)
                .map { path ->
                    val updated = track.copy(isDownloaded = true, localPath = path)
                    database.trackDao().upsertTrack(TrackEntity.fromDomainTrack(updated))
                    database.trackDao().updateDownloadStatus(track.id, true, path)
                    path
                }
        }

        val resolvedTrack = runCatching { resolveSoundCloudStream(track) }.getOrDefault(track)
        val sources = listOfNotNull(
            resolvedTrack.downloadUrl.takeIf { it.isNotBlank() },
            resolvedTrack.audioUrl.takeIf { it.isNotBlank() },
            track.downloadUrl.takeIf { it.isNotBlank() },
            track.audioUrl.takeIf { it.isNotBlank() }
        ).distinct()

        var lastResult: Result<String>? = null
        for (rawSource in sources) {
            val source = if (rawSource.startsWith("http://") || rawSource.startsWith("https://") || rawSource.startsWith("content://") || rawSource.startsWith("file://") || rawSource.startsWith("/")) {
                rawSource
            } else if (rawSource.isNotBlank()) {
                "https://$rawSource"
            } else {
                continue
            }

            val mimeType = guessMimeType(source)
            val baseName = buildString {
                if (resolvedTrack.artistName.isNotBlank()) {
                    append(resolvedTrack.artistName)
                    append(" - ")
                }
                append(resolvedTrack.title)
            }

            val result = downloadHelper.downloadToPublicDownloads(baseName, source, mimeType)
            if (result.isSuccess) {
                val path = result.getOrThrow()
                val updated = resolvedTrack.copy(isDownloaded = true, localPath = path, audioUrl = source)
                database.trackDao().upsertTrack(TrackEntity.fromDomainTrack(updated))
                database.trackDao().updateDownloadStatus(resolvedTrack.id, true, path)
                return@withContext Result.success(path)
            } else {
                lastResult = result
            }
        }

        lastResult ?: Result.failure(IllegalStateException("This track has no downloadable audio stream"))
    }

    suspend fun deleteDownload(track: Track) = withContext(Dispatchers.IO) {
        val entity = database.trackDao().getTrackById(track.id)
        val localPath = track.localPath ?: entity?.localPath

        if (!track.isLocalFile) {
            downloadHelper.deleteAudioFile(track.id, localPath)
            database.trackDao().updateDownloadStatus(track.id, false, null)
            if (entity != null) {
                database.trackDao().upsertTrack(entity.copy(isDownloaded = false, localPath = null))
            }
        }
    }

    // --- Device / Imported Tracks ---

    fun getLocalTracks(): Flow<List<Track>> {
        return database.trackDao().getLocalTracks()
            .map { entities -> entities.map { it.toDomainWithLocalState() } }
            .flowOn(Dispatchers.IO)
    }

    suspend fun importLocalAudio(uri: Uri): Result<Track> = localAudioImporter.import(uri)

    /**
     * Permanently removes an imported / on-device track:
     *  1. deletes the physical audio file behind [Track.localPath] (MediaStore entry,
     *     file path or SAF content URI),
     *  2. purges every playlist reference,
     *  3. deletes the cached embedded artwork,
     *  4. drops the Room row, which refreshes every library flow reactively.
     *
     * A failed physical deletion (for example a SAF document the app only holds
     * read access to) never blocks removing the entry from the library.
     */
    suspend fun deleteLocalTrack(track: Track): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val entity = database.trackDao().getTrackById(track.id)
            val localPath = track.localPath ?: entity?.localPath

            database.playlistDao().deleteCrossReferencesForTrack(track.id)
            downloadHelper.deleteAudioFile(track.id, localPath)
            localAudioImporter.deleteArtwork(track.id)
            database.trackDao().deleteTrack(track.id)
        }
    }

    // --- Playlists ---

    fun getPlaylists(): Flow<List<Playlist>> {
        return database.playlistDao().getAllPlaylistsWithTrackCount().map { list ->
            list.map { item ->
                Playlist(
                    id = item.playlist.id,
                    name = item.playlist.name,
                    coverUrl = item.playlist.coverUrl,
                    trackCount = item.trackCount,
                    createdAt = item.playlist.createdAt
                )
            }
        }.flowOn(Dispatchers.IO)
    }

    suspend fun createPlaylist(name: String, coverUrl: String = ""): Long =
        withContext(Dispatchers.IO) {
            database.playlistDao().insertPlaylist(PlaylistEntity(name = name, coverUrl = coverUrl))
        }

    suspend fun deletePlaylist(playlistId: Long) = withContext(Dispatchers.IO) {
        database.playlistDao().deletePlaylistTracks(playlistId)
        database.playlistDao().deletePlaylist(playlistId)
    }

    fun getPlaylistTracks(playlistId: Long): Flow<List<Track>> {
        return database.playlistDao().getTracksForPlaylist(playlistId)
            .map { entities ->
                entities.map { entity ->
                    val track = entity.toDomainWithLocalState()
                    resolveStreamIfNeeded(track)
                }
            }
            .flowOn(Dispatchers.IO)
    }

    suspend fun addTrackToPlaylist(playlistId: Long, track: Track) = withContext(Dispatchers.IO) {
        val resolvedTrack = resolveSoundCloudStream(track)
        database.trackDao().upsertTrack(TrackEntity.fromDomainTrack(resolvedTrack))
        database.playlistDao().addTrackToPlaylist(
            PlaylistTrackCrossRef(playlistId = playlistId, trackId = resolvedTrack.id)
        )
    }

    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: String) =
        withContext(Dispatchers.IO) {
            database.playlistDao().removeTrackFromPlaylist(playlistId, trackId)
        }

    // --- Helpers ---

    private suspend fun resolveSoundCloudStream(track: Track): Track {
        val url = track.audioUrl.ifBlank { track.downloadUrl }
        val needsResolution = url.contains("api-v2.soundcloud.com") ||
            url.contains("api.soundcloud.com") ||
            url.contains("/media/") ||
            url.contains("/stream/")

        if (!needsResolution || track.isLocalFile) return track

        val fallbackUrl = if (url.contains("?")) {
            "$url&client_id=${SoundCloudRepository.SOUNDCLOUD_CLIENT_ID}"
        } else {
            "$url?client_id=${SoundCloudRepository.SOUNDCLOUD_CLIENT_ID}"
        }

        val directUrl = soundCloudRepository.getStreamUrl(url).getOrDefault(fallbackUrl)
        val finalUrl = if (directUrl.contains("api-v2.soundcloud.com") && !directUrl.contains("client_id")) fallbackUrl else directUrl

        return track.copy(
            audioUrl = finalUrl,
            downloadUrl = if (track.downloadUrl.isBlank()) finalUrl else track.downloadUrl
        )
    }

    private suspend fun resolveStreamIfNeeded(track: Track): Track {
        val url = track.audioUrl.ifBlank { track.downloadUrl }
        val needsResolution = url.contains("api-v2.soundcloud.com") ||
            url.contains("api.soundcloud.com") ||
            url.contains("/media/") ||
            url.contains("/stream/")

        if (!needsResolution || track.isLocalFile || !track.localPath.isNullOrEmpty()) return track

        val fallbackUrl = if (url.contains("?")) {
            "$url&client_id=${SoundCloudRepository.SOUNDCLOUD_CLIENT_ID}"
        } else {
            "$url?client_id=${SoundCloudRepository.SOUNDCLOUD_CLIENT_ID}"
        }

        return try {
            val directUrl = soundCloudRepository.getStreamUrl(url).getOrDefault(fallbackUrl)
            val finalUrl = if (directUrl.contains("api-v2.soundcloud.com") && !directUrl.contains("client_id")) fallbackUrl else directUrl
            track.copy(audioUrl = finalUrl)
        } catch (_: Exception) {
            track.copy(audioUrl = fallbackUrl)
        }
    }

    private fun TrackEntity.toDomainWithLocalState(): Track {
        val resolvedPath = localPath?.takeIf { it.isNotBlank() }
            ?: downloadHelper.getLocalPath(id)

        return toDomainTrack().copy(
            isDownloaded = !isLocalFile && !resolvedPath.isNullOrEmpty(),
            localPath = resolvedPath
        )
    }

    private fun guessMimeType(source: String): String {
        val path = Uri.parse(source).lastPathSegment ?: source
        return when (path.substringAfterLast('.', "").lowercase(Locale.US)) {
            "mp3" -> "audio/mpeg"
            "m4a", "mp4", "aac" -> "audio/mp4"
            "ogg", "opus" -> "audio/ogg"
            "flac" -> "audio/flac"
            "wav" -> "audio/wav"
            else -> "audio/mpeg"
        }
    }
}
