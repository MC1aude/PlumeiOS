package com.example.plume.service

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.plume.domain.model.PlaybackState
import com.example.plume.domain.model.RepeatMode
import com.example.plume.domain.model.Track
import com.example.plume.player.MusicPlayerController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

/**
 * Android implementation of [MusicPlayerController] backed by a [MediaController].
 *
 * The actual [androidx.media3.exoplayer.ExoPlayer] instance lives in
 * [PlumeMediaService] (a MediaSessionService). This class only:
 *  - connects to that service asynchronously,
 *  - forwards UI commands to the controller,
 *  - mirrors the controller state into [playbackState] for Compose.
 *
 * This is what allows playback to survive app minimization, screen lock and
 * task removal: Media3 promotes the service to the foreground and keeps the
 * notification/lock-screen controls in sync.
 */
class PlayerController(
    private val context: Context
) : MusicPlayerController {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var positionUpdateJob: Job? = null

    private val _playbackState = MutableStateFlow(PlaybackState())
    override val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private var controller: MediaController? = null

    /** Commands issued before the controller finished connecting, executed in order. */
    private val pendingActions = ArrayDeque<(MediaController) -> Unit>()

    private val controllerFuture = MediaController.Builder(
        context,
        SessionToken(
            context,
            // The service lives in :app, so reference it by name to avoid a
            // shared -> app compile dependency (KMP module direction).
            ComponentName(context.packageName, SERVICE_CLASS_NAME)
        )
    ).buildAsync()

    init {
        controllerFuture.addListener(
            {
                runCatching { controllerFuture.get() }
                    .onSuccess { mediaController ->
                        controller = mediaController
                        mediaController.addListener(playerListener)
                        syncFromController(mediaController)

                        while (pendingActions.isNotEmpty()) {
                            pendingActions.removeFirst().invoke(mediaController)
                        }
                    }
                    .onFailure { it.printStackTrace() }
            },
            ContextCompat.getMainExecutor(context)
        )
    }

    // ── Player.Listener → PlaybackState ─────────────────────────────────────

    private val playerListener = object : Player.Listener {

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playbackState.update { it.copy(isPlaying = isPlaying) }
            if (isPlaying) startPositionTracker() else stopPositionTracker()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> _playbackState.update { it.copy(isLoading = true) }

                Player.STATE_READY -> _playbackState.update {
                    it.copy(
                        isLoading = false,
                        durationMs = controller?.duration?.coerceAtLeast(0L) ?: 0L
                    )
                }

                Player.STATE_ENDED -> {
                    _playbackState.update { it.copy(isLoading = false) }
                    handleTrackEnded()
                }

                Player.STATE_IDLE -> _playbackState.update { it.copy(isLoading = false) }
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val mediaId = mediaItem?.mediaId ?: return
            val state = _playbackState.value
            val newIndex = state.queue.indexOfFirst { it.id == mediaId }
            if (newIndex >= 0) {
                _playbackState.update {
                    it.copy(
                        currentTrack = state.queue[newIndex],
                        currentIndex = newIndex,
                        positionMs = 0L,
                        durationMs = controller?.duration?.coerceAtLeast(0L) ?: 0L
                    )
                }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            error.printStackTrace()
            _playbackState.update { it.copy(isLoading = false, isPlaying = false) }
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            if (reason == Player.DISCONTINUITY_REASON_SEEK) {
                _playbackState.update { it.copy(positionMs = newPosition.positionMs) }
            }
        }
    }

    // ── MusicPlayerController ───────────────────────────────────────────────

    override fun playTrack(track: Track, queue: List<Track>) {
        val newQueue = if (queue.contains(track)) queue else listOf(track) + queue
        val index = newQueue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)

        _playbackState.update {
            it.copy(
                queue = newQueue,
                currentIndex = index,
                currentTrack = track,
                isLoading = true
            )
        }

        withController { mediaController ->
            mediaController.setMediaItems(newQueue.map(::toMediaItem), index, 0L)
            mediaController.prepare()
            mediaController.play()
        }
    }

    override fun setQueueAndPlay(queue: List<Track>, startIndex: Int) {
        if (queue.isEmpty()) return
        val validIndex = startIndex.coerceIn(0, queue.lastIndex)
        val track = queue[validIndex]

        _playbackState.update {
            it.copy(
                queue = queue,
                currentIndex = validIndex,
                currentTrack = track,
                isLoading = true
            )
        }

        withController { mediaController ->
            mediaController.setMediaItems(queue.map(::toMediaItem), validIndex, 0L)
            mediaController.prepare()
            mediaController.play()
        }
    }

    override fun togglePlayPause() {
        withController { mediaController ->
            when {
                mediaController.isPlaying -> mediaController.pause()

                mediaController.playbackState == Player.STATE_ENDED -> {
                    mediaController.seekTo(0, 0L)
                    mediaController.play()
                }

                else -> mediaController.play()
            }
        }
    }

    override fun pause() {
        withController { mediaController ->
            if (mediaController.isPlaying) mediaController.pause()
        }
    }

    override fun stop() {
        stopPositionTracker()
        withController { mediaController ->
            mediaController.stop()
            mediaController.clearMediaItems()
        }
        _playbackState.update {
            it.copy(
                isPlaying = false,
                isLoading = false,
                currentTrack = null,
                currentIndex = -1,
                positionMs = 0L,
                durationMs = 0L
            )
        }
    }

    override fun next() {
        withController { mediaController ->
            if (mediaController.hasNextMediaItem()) {
                mediaController.seekToNextMediaItem()
            } else {
                val currentState = _playbackState.value
                if (currentState.repeatMode == RepeatMode.ALL && currentState.queue.isNotEmpty()) {
                    mediaController.seekTo(0, 0L)
                    mediaController.play()
                }
            }
        }
    }

    override fun previous() {
        withController { mediaController ->
            when {
                mediaController.currentPosition > 3_000L -> mediaController.seekTo(0L)
                mediaController.hasPreviousMediaItem() -> mediaController.seekToPreviousMediaItem()
                else -> mediaController.seekTo(0L)
            }
        }
    }

    override fun seekTo(positionMs: Long) {
        withController { it.seekTo(positionMs) }
        _playbackState.update { it.copy(positionMs = positionMs.coerceAtLeast(0L)) }
    }

    override fun toggleShuffle() {
        val newShuffle = !_playbackState.value.isShuffle
        _playbackState.update { it.copy(isShuffle = newShuffle) }
        withController { it.shuffleModeEnabled = newShuffle }
    }

    override fun toggleRepeatMode() {
        val nextMode = when (_playbackState.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        _playbackState.update { it.copy(repeatMode = nextMode) }
        withController {
            it.repeatMode = when (nextMode) {
                RepeatMode.OFF -> Player.REPEAT_MODE_OFF
                RepeatMode.ALL -> Player.REPEAT_MODE_ALL
                RepeatMode.ONE -> Player.REPEAT_MODE_ONE
            }
        }
    }

    override fun removeFromQueue(trackId: String) {
        val state = _playbackState.value
        val index = state.queue.indexOfFirst { it.id == trackId }
        if (index < 0) return

        val newQueue = state.queue.toMutableList().apply { removeAt(index) }
        val newCurrentTrack = if (state.currentTrack?.id == trackId) {
            newQueue.getOrNull(index)
        } else {
            state.currentTrack
        }

        _playbackState.update {
            it.copy(queue = newQueue, currentTrack = newCurrentTrack)
        }

        withController { mediaController ->
            // The state queue can outlive the controller items (stop() clears
            // them), so guard the native removal to avoid an out-of-bounds crash.
            if (index < mediaController.mediaItemCount) {
                mediaController.removeMediaItem(index)
            }
        }
    }

    override fun updateTrackState(trackId: String, transform: (Track) -> Track) {
        _playbackState.update { state ->
            val updatedQueue = state.queue.map { track ->
                if (track.id == trackId) transform(track) else track
            }
            val updatedCurrent = state.currentTrack?.let { track ->
                if (track.id == trackId) transform(track) else track
            }
            state.copy(queue = updatedQueue, currentTrack = updatedCurrent)
        }
    }

    // ── Internals ───────────────────────────────────────────────────────────

    private fun withController(action: (MediaController) -> Unit) {
        val current = controller
        if (current != null) action(current) else pendingActions.addLast(action)
    }

    private fun syncFromController(mediaController: MediaController) {
        val localQueue = _playbackState.value.queue
        // Prefer the locally tracked queue when it matches the controller: it may
        // contain fresher fields (favorites/downloads) than the MediaItem extras.
        val queue = if (localQueue.isNotEmpty() &&
            localQueue.size == mediaController.mediaItemCount
        ) {
            localQueue
        } else {
            (0 until mediaController.mediaItemCount).map { index ->
                trackFromMediaItem(mediaController.getMediaItemAt(index))
            }
        }
        val index = mediaController.currentMediaItemIndex

        _playbackState.update { state ->
            val mediaState = state.copy(
                isPlaying = mediaController.isPlaying,
                isLoading = mediaController.playbackState == Player.STATE_BUFFERING,
                isShuffle = mediaController.shuffleModeEnabled,
                repeatMode = mediaController.repeatMode.toRepeatMode()
            )
            if (queue.isEmpty()) {
                mediaState
            } else {
                mediaState.copy(
                    queue = queue,
                    currentIndex = index,
                    currentTrack = queue.getOrNull(index),
                    positionMs = mediaController.currentPosition.coerceAtLeast(0L),
                    durationMs = mediaController.duration.coerceAtLeast(0L)
                )
            }
        }

        if (mediaController.isPlaying) startPositionTracker()
    }

    private fun handleTrackEnded() {
        val state = _playbackState.value
        val current = controller ?: return
        when {
            state.repeatMode == RepeatMode.ONE -> {
                current.seekTo(0L)
                current.play()
            }

            state.repeatMode == RepeatMode.ALL && !current.hasNextMediaItem() &&
                state.queue.isNotEmpty() -> {
                current.seekTo(0, 0L)
                current.play()
            }
        }
    }

    private fun startPositionTracker() {
        if (positionUpdateJob?.isActive == true) return
        positionUpdateJob = scope.launch {
            while (true) {
                val current = controller
                if (current != null && current.isPlaying) {
                    _playbackState.update {
                        it.copy(
                            positionMs = current.currentPosition.coerceAtLeast(0L),
                            durationMs = current.duration.coerceAtLeast(0L)
                        )
                    }
                }
                delay(250L)
            }
        }
    }

    private fun stopPositionTracker() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }

    private fun Int.toRepeatMode(): RepeatMode = when (this) {
        Player.REPEAT_MODE_ONE -> RepeatMode.ONE
        Player.REPEAT_MODE_ALL -> RepeatMode.ALL
        else -> RepeatMode.OFF
    }

    private fun toMediaItem(track: Track): MediaItem {
        val extras = Bundle().apply {
            putString(KEY_TITLE, track.title)
            putString(KEY_ARTIST, track.artistName)
            putString(KEY_ALBUM, track.albumName)
            putString(KEY_COVER, track.coverUrl)
            putString(KEY_AUDIO, track.audioUrl)
            putString(KEY_DOWNLOAD, track.downloadUrl)
            putInt(KEY_DURATION, track.durationSeconds)
            putBoolean(KEY_FAVORITE, track.isFavorite)
            putBoolean(KEY_DOWNLOADED, track.isDownloaded)
            putString(KEY_LOCAL_PATH, track.localPath)
            putBoolean(KEY_LOCAL_FILE, track.isLocalFile)
        }

        val metadata = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artistName)
            .setAlbumTitle(track.albumName)
            .setArtworkUri(track.coverUrl.takeIf { it.isNotEmpty() }?.let(Uri::parse))
            .setExtras(extras)
            .build()

        return MediaItem.Builder()
            .setMediaId(track.id)
            .setUri(resolvePlaybackUri(track))
            .setMediaMetadata(metadata)
            .build()
    }

    private fun trackFromMediaItem(mediaItem: MediaItem): Track {
        val extras = mediaItem.mediaMetadata.extras
        val metadata = mediaItem.mediaMetadata
        return Track(
            id = mediaItem.mediaId,
            title = extras?.getString(KEY_TITLE)
                ?: metadata.title?.toString().orEmpty(),
            artistName = extras?.getString(KEY_ARTIST)
                ?: metadata.artist?.toString().orEmpty(),
            albumName = extras?.getString(KEY_ALBUM)
                ?: metadata.albumTitle?.toString().orEmpty(),
            coverUrl = extras?.getString(KEY_COVER)
                ?: metadata.artworkUri?.toString().orEmpty(),
            audioUrl = extras?.getString(KEY_AUDIO).orEmpty(),
            downloadUrl = extras?.getString(KEY_DOWNLOAD).orEmpty(),
            durationSeconds = extras?.getInt(KEY_DURATION) ?: 0,
            isFavorite = extras?.getBoolean(KEY_FAVORITE) ?: false,
            isDownloaded = extras?.getBoolean(KEY_DOWNLOADED) ?: false,
            localPath = extras?.getString(KEY_LOCAL_PATH),
            isLocalFile = extras?.getBoolean(KEY_LOCAL_FILE) ?: false
        )
    }

    /**
     * Resolves the best playback URI. Imported files and public Downloads entries
     * are exposed as content:// URIs, legacy downloads as plain files.
     */
    private fun resolvePlaybackUri(track: Track): Uri {
        val local = track.localPath
        return when {
            !local.isNullOrEmpty() && local.startsWith("content://") -> Uri.parse(local)
            !local.isNullOrEmpty() && local.startsWith("file://") -> Uri.parse(local)
            !local.isNullOrEmpty() && local.startsWith("/") -> Uri.fromFile(File(local))
            track.audioUrl.isNotEmpty() -> Uri.parse(track.audioUrl)
            else -> Uri.parse(track.playUrl)
        }
    }

    companion object {
        private const val SERVICE_CLASS_NAME = "com.example.plume.service.PlumeMediaService"

        private const val KEY_TITLE = "plume.title"
        private const val KEY_ARTIST = "plume.artist"
        private const val KEY_ALBUM = "plume.album"
        private const val KEY_COVER = "plume.cover"
        private const val KEY_AUDIO = "plume.audio"
        private const val KEY_DOWNLOAD = "plume.download"
        private const val KEY_DURATION = "plume.duration"
        private const val KEY_FAVORITE = "plume.favorite"
        private const val KEY_DOWNLOADED = "plume.downloaded"
        private const val KEY_LOCAL_PATH = "plume.localPath"
        private const val KEY_LOCAL_FILE = "plume.localFile"
    }
}
