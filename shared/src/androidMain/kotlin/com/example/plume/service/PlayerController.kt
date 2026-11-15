package com.example.plume.service

import android.content.Context
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
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

class PlayerController(
    private val context: Context
) : MusicPlayerController {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var positionUpdateJob: Job? = null

    private val _playbackState = MutableStateFlow(PlaybackState())
    override val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val exoPlayer: ExoPlayer by lazy {
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        ExoPlayer.Builder(context)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build().apply {
                addListener(playerListener)
            }
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playbackState.update { it.copy(isPlaying = isPlaying) }
            if (isPlaying) {
                startPositionTracker()
            } else {
                stopPositionTracker()
            }
        }

        override fun onPlaybackStateChanged(state: Int) {
            when (state) {
                Player.STATE_BUFFERING -> {
                    _playbackState.update { it.copy(isLoading = true) }
                }
                Player.STATE_READY -> {
                    _playbackState.update {
                        it.copy(
                            isLoading = false,
                            durationMs = exoPlayer.duration.coerceAtLeast(0L)
                        )
                    }
                }
                Player.STATE_ENDED -> {
                    _playbackState.update { it.copy(isLoading = false) }
                    handleTrackEnded()
                }
                Player.STATE_IDLE -> {
                    _playbackState.update { it.copy(isLoading = false) }
                }
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val mediaId = mediaItem?.mediaId
            if (mediaId != null) {
                val currentQueue = _playbackState.value.queue
                val newIndex = currentQueue.indexOfFirst { it.id == mediaId }
                if (newIndex >= 0) {
                    val track = currentQueue[newIndex]
                    _playbackState.update {
                        it.copy(
                            currentTrack = track,
                            currentIndex = newIndex,
                            positionMs = 0L,
                            durationMs = exoPlayer.duration.coerceAtLeast(0L)
                        )
                    }
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

        setupExoPlayerQueue(newQueue, index)
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

        setupExoPlayerQueue(queue, validIndex)
    }

    private fun setupExoPlayerQueue(queue: List<Track>, startIndex: Int) {
        val mediaItems = queue.map { track ->
            val metadata = MediaMetadata.Builder()
                .setTitle(track.title)
                .setArtist(track.artistName)
                .setAlbumTitle(track.albumName)
                .setArtworkUri(if (track.coverUrl.isNotEmpty()) Uri.parse(track.coverUrl) else null)
                .build()

            MediaItem.Builder()
                .setMediaId(track.id)
                .setUri(resolvePlaybackUri(track))
                .setMediaMetadata(metadata)
                .build()
        }

        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        exoPlayer.setMediaItems(mediaItems, startIndex, 0L)
        exoPlayer.prepare()
        exoPlayer.play()
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

    /** Keeps the playback state in sync when a library action changes a track. */
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

    override fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            if (exoPlayer.playbackState == Player.STATE_ENDED) {
                exoPlayer.seekTo(0, 0L)
            }
            exoPlayer.play()
        }
    }

    /** Pauses local playback without replacing the current queue. */
    override fun pause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        }
    }

    /** Completely stops playback, clears media items and resets playback state. */
    override fun stop() {
        stopPositionTracker()
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        _playbackState.update {
            it.copy(
                isPlaying = false,
                isLoading = false,
                currentTrack = null,
                positionMs = 0L,
                durationMs = 0L
            )
        }
    }

    override fun next() {
        if (exoPlayer.hasNextMediaItem()) {
            exoPlayer.seekToNextMediaItem()
        } else {
            val currentState = _playbackState.value
            if (currentState.repeatMode == RepeatMode.ALL && currentState.queue.isNotEmpty()) {
                setQueueAndPlay(currentState.queue, 0)
            }
        }
    }

    override fun previous() {
        if (exoPlayer.currentPosition > 3000L) {
            exoPlayer.seekTo(0L)
        } else if (exoPlayer.hasPreviousMediaItem()) {
            exoPlayer.seekToPreviousMediaItem()
        } else {
            exoPlayer.seekTo(0L)
        }
    }

    override fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs)
        _playbackState.update { it.copy(positionMs = positionMs) }
    }

    override fun toggleShuffle() {
        val newShuffle = !_playbackState.value.isShuffle
        _playbackState.update { it.copy(isShuffle = newShuffle) }
        exoPlayer.shuffleModeEnabled = newShuffle
    }

    override fun toggleRepeatMode() {
        val nextMode = when (_playbackState.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        _playbackState.update { it.copy(repeatMode = nextMode) }
        when (nextMode) {
            RepeatMode.OFF -> exoPlayer.repeatMode = Player.REPEAT_MODE_OFF
            RepeatMode.ALL -> exoPlayer.repeatMode = Player.REPEAT_MODE_ALL
            RepeatMode.ONE -> exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    override fun removeFromQueue(trackId: String) {
        val currentQueue = _playbackState.value.queue.toMutableList()
        val index = currentQueue.indexOfFirst { it.id == trackId }
        if (index >= 0) {
            currentQueue.removeAt(index)
            // The state queue can outlive the ExoPlayer items (stop() clears them),
            // so guard the native removal to avoid an out-of-bounds crash.
            if (index < exoPlayer.mediaItemCount) {
                exoPlayer.removeMediaItem(index)
            }
            val newCurrentTrack = _playbackState.value.currentTrack?.let { current ->
                if (current.id == trackId) currentQueue.getOrNull(index) else current
            }
            _playbackState.update {
                it.copy(
                    queue = currentQueue,
                    currentTrack = newCurrentTrack
                )
            }
        }
    }

    private fun handleTrackEnded() {
        val currentState = _playbackState.value
        if (currentState.repeatMode == RepeatMode.ONE) {
            exoPlayer.seekTo(0L)
            exoPlayer.play()
        } else if (currentState.repeatMode == RepeatMode.ALL && !exoPlayer.hasNextMediaItem()) {
            if (currentState.queue.isNotEmpty()) {
                setQueueAndPlay(currentState.queue, 0)
            }
        }
    }

    private fun startPositionTracker() {
        stopPositionTracker()
        positionUpdateJob = scope.launch {
            while (true) {
                if (exoPlayer.isPlaying) {
                    _playbackState.update {
                        it.copy(
                            positionMs = exoPlayer.currentPosition.coerceAtLeast(0L),
                            durationMs = exoPlayer.duration.coerceAtLeast(0L)
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
}
