package com.example.plume.service

import com.example.plume.domain.model.PlaybackState
import com.example.plume.domain.model.RepeatMode
import com.example.plume.domain.model.Track
import com.example.plume.player.MusicPlayerController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import platform.AVFoundation.AVPlayer

/**
 * iOS implementation stub of [MusicPlayerController].
 *
 * Playback state management is already shared; actual `AVPlayerItem`/`AVURLAsset`
 * wiring will be added together with the iOS app target. The only platform call
 * used here is `AVPlayer.rate` / `pause()` so the module links against AVFoundation.
 */
class IosMusicPlayerController : MusicPlayerController {

    private val avPlayer: AVPlayer = AVPlayer()

    private val _playbackState = MutableStateFlow(PlaybackState())
    override val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    override fun playTrack(track: Track, queue: List<Track>) {
        val newQueue = if (queue.contains(track)) queue else listOf(track) + queue
        val index = newQueue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        _playbackState.update {
            it.copy(
                queue = newQueue,
                currentIndex = index,
                currentTrack = track,
                isPlaying = true,
                isLoading = true
            )
        }
        avPlayer.rate = 1.0f
    }

    override fun setQueueAndPlay(queue: List<Track>, startIndex: Int) {
        if (queue.isEmpty()) return
        val validIndex = startIndex.coerceIn(0, queue.lastIndex)
        _playbackState.update {
            it.copy(
                queue = queue,
                currentIndex = validIndex,
                currentTrack = queue[validIndex],
                isPlaying = true,
                isLoading = true
            )
        }
        avPlayer.rate = 1.0f
    }

    override fun togglePlayPause() {
        if (_playbackState.value.isPlaying) {
            pause()
        } else {
            _playbackState.update { it.copy(isPlaying = true) }
            avPlayer.rate = 1.0f
        }
    }

    override fun pause() {
        avPlayer.pause()
        _playbackState.update { it.copy(isPlaying = false) }
    }

    override fun stop() {
        avPlayer.pause()
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
        val state = _playbackState.value
        val nextIndex = state.currentIndex + 1
        val track = when {
            nextIndex <= state.queue.lastIndex -> state.queue[nextIndex]
            state.repeatMode == RepeatMode.ALL && state.queue.isNotEmpty() -> state.queue.first()
            else -> return
        }
        val index = state.queue.indexOf(track)
        _playbackState.update {
            it.copy(currentIndex = index, currentTrack = track, positionMs = 0L)
        }
        avPlayer.rate = 1.0f
    }

    override fun previous() {
        val state = _playbackState.value
        if (state.positionMs > 3000L || state.currentIndex <= 0) {
            seekTo(0L)
            return
        }
        val index = state.currentIndex - 1
        _playbackState.update {
            it.copy(currentIndex = index, currentTrack = state.queue[index], positionMs = 0L)
        }
        avPlayer.rate = 1.0f
    }

    override fun seekTo(positionMs: Long) {
        // TODO: wire up AVPlayer.seek(to:) once AVPlayerItem is created.
        _playbackState.update { it.copy(positionMs = positionMs.coerceAtLeast(0L)) }
    }

    override fun toggleShuffle() {
        _playbackState.update { it.copy(isShuffle = !it.isShuffle) }
    }

    override fun toggleRepeatMode() {
        val nextMode = when (_playbackState.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        _playbackState.update { it.copy(repeatMode = nextMode) }
    }

    override fun removeFromQueue(trackId: String) {
        _playbackState.update { state ->
            val updatedQueue = state.queue.filterNot { it.id == trackId }
            val updatedCurrent = if (state.currentTrack?.id == trackId) {
                updatedQueue.getOrNull(state.currentIndex.coerceAtMost(updatedQueue.lastIndex))
            } else {
                state.currentTrack
            }
            state.copy(queue = updatedQueue, currentTrack = updatedCurrent)
        }
    }

    override fun updateTrackState(trackId: String, transform: (Track) -> Track) {
        _playbackState.update { state ->
            val updatedQueue = state.queue.map { if (it.id == trackId) transform(it) else it }
            val updatedCurrent = state.currentTrack?.let {
                if (it.id == trackId) transform(it) else it
            }
            state.copy(queue = updatedQueue, currentTrack = updatedCurrent)
        }
    }
}