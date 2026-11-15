package com.example.plume.ui.screens.search

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.plume.data.repository.SoundCloudRepository
import com.example.plume.domain.model.Song
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * ExoPlayer wrapper for the SoundCloud mini-player and full-screen player.
 * Supports playlist queue, next/previous, seek, and position tracking.
 */
class SoundCloudPlayerViewModel(context: Context) : ViewModel() {

    private val repository = SoundCloudRepository()

    val currentPlayingSong = MutableStateFlow<Song?>(null)
    val isPlaying = MutableStateFlow(false)
    val isLoading = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)

    /** Current queue of songs. */
    val queue = MutableStateFlow<List<Song>>(emptyList())
    private var currentIndex = -1

    /** Playback position tracking. */
    val positionMs = MutableStateFlow(0L)
    val durationMs = MutableStateFlow(0L)

    private var positionJob: Job? = null

    private val player: ExoPlayer = ExoPlayer.Builder(context).build()

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying.value = playing
                if (playing) startPositionTracker() else stopPositionTracker()
            }

            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_READY -> {
                        isLoading.value = false
                        durationMs.value = player.duration.coerceAtLeast(0L)
                    }
                    Player.STATE_BUFFERING -> {
                        isLoading.value = true
                    }
                    Player.STATE_ENDED -> {
                        isLoading.value = false
                        // Auto-advance to next track
                        if (currentIndex < queue.value.lastIndex) {
                            playNext()
                        }
                    }
                    Player.STATE_IDLE -> {
                        isLoading.value = false
                    }
                }
            }
        })
    }

    /**
     * Play a single song. Optionally provide the full playlist for next/prev navigation.
     */
    fun playSong(song: Song, playlist: List<Song>? = null) {
        if (playlist != null && playlist.isNotEmpty()) {
            queue.value = playlist
            currentIndex = playlist.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        } else {
            // If no playlist provided, keep existing queue or create a single-item queue
            val existingQueue = queue.value
            val idx = existingQueue.indexOfFirst { it.id == song.id }
            if (idx >= 0) {
                currentIndex = idx
            } else {
                queue.value = listOf(song)
                currentIndex = 0
            }
        }

        currentPlayingSong.value = song
        isLoading.value = true
        error.value = null
        positionMs.value = 0L
        durationMs.value = 0L

        // Explicitly stop previous playback and clear items
        player.stop()
        player.clearMediaItems()

        viewModelScope.launch {
            val result = repository.getStreamUrl(song.streamEndpoint)
            result.onSuccess { directUrl ->
                player.stop()
                player.clearMediaItems()
                player.setMediaItem(MediaItem.fromUri(directUrl))
                player.prepare()
                player.play()
                isPlaying.value = true
            }.onFailure { throwable ->
                Log.e("SoundCloudPlayer", "Unable to play ${song.title}", throwable)
                isPlaying.value = false
                error.value = throwable.message ?: "Не удалось воспроизвести трек"
            }
            isLoading.value = false
        }
    }

    fun togglePlayPause() {
        if (player.isPlaying) {
            player.pause()
        } else {
            if (player.playbackState == Player.STATE_ENDED) {
                player.seekTo(0L)
            }
            player.play()
            isPlaying.value = true
        }
    }

    /** Pauses streaming playback (used when the local player takes over). */
    fun pause() {
        if (player.isPlaying) {
            player.pause()
            isPlaying.value = false
        }
    }

    /** Completely stops streaming playback, clears media items and resets active state. */
    fun stop() {
        stopPositionTracker()
        player.stop()
        player.clearMediaItems()
        currentPlayingSong.value = null
        isPlaying.value = false
        isLoading.value = false
        positionMs.value = 0L
        durationMs.value = 0L
    }

    fun playNext() {
        val q = queue.value
        if (q.isEmpty()) return
        val nextIndex = currentIndex + 1
        if (nextIndex <= q.lastIndex) {
            playSong(q[nextIndex])
        }
    }

    fun playPrevious() {
        // If more than 3 seconds played, restart current song
        if (player.currentPosition > 3000L) {
            player.seekTo(0L)
            positionMs.value = 0L
            return
        }
        val q = queue.value
        if (q.isEmpty()) return
        val prevIndex = currentIndex - 1
        if (prevIndex >= 0) {
            playSong(q[prevIndex])
        } else {
            player.seekTo(0L)
            positionMs.value = 0L
        }
    }

    fun seekTo(position: Long) {
        player.seekTo(position)
        positionMs.value = position
    }

    private fun startPositionTracker() {
        stopPositionTracker()
        positionJob = viewModelScope.launch {
            while (true) {
                if (player.isPlaying) {
                    positionMs.value = player.currentPosition.coerceAtLeast(0L)
                    durationMs.value = player.duration.coerceAtLeast(0L)
                }
                delay(500L)
            }
        }
    }

    private fun stopPositionTracker() {
        positionJob?.cancel()
        positionJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopPositionTracker()
        player.release()
    }
}
