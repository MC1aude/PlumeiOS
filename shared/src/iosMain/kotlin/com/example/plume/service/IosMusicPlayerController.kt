@file:OptIn(ExperimentalForeignApi::class)

package com.example.plume.service

import com.example.plume.domain.model.PlaybackState
import com.example.plume.domain.model.RepeatMode
import com.example.plume.domain.model.Track
import com.example.plume.player.MusicPlayerController
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
// AVPlayer playback helpers (`play`, `pause`, `rate`, `seekToTime`) are exposed
// as category extensions in the platform package, so they are only reachable
// through a wildcard import.
import platform.AVFoundation.*
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSURL
import platform.darwin.NSObjectProtocol

/**
 * iOS implementation of [MusicPlayerController] backed by `AVPlayer`.
 *
 * This class is the only place in the shared module that touches AVFoundation,
 * so commonMain stays free of Apple frameworks (mirroring how the Android
 * controller keeps Media3 out of common code).
 *
 * Threading: every method must be called from the main thread. SwiftUI/Compose
 * controllers already dispatch UI actions there, and the playback state is a
 * `StateFlow` consumed by Compose on the main dispatcher.
 *
 * Background playback: `AVAudioSession` is configured with the `.playback`
 * category and the app declares `UIBackgroundModes = audio` in Info.plist, so
 * audio keeps playing when the app is minimised or the screen is locked.
 */
class IosMusicPlayerController : MusicPlayerController {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _playbackState = MutableStateFlow(PlaybackState())
    override val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private var player: AVPlayer? = null
    private var endObserver: NSObjectProtocol? = null
    private var ticker: Job? = null

    // ── MusicPlayerController ───────────────────────────────────────────────

    override fun playTrack(track: Track, queue: List<Track>) {
        val newQueue = if (queue.any { it.id == track.id }) queue else listOf(track) + queue
        val index = newQueue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        setQueueAndPlay(newQueue, index)
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
                positionMs = 0L,
                durationMs = track.durationSeconds * 1000L,
                isPlaying = false,
                isLoading = true
            )
        }
        startPlayback(track)
    }

    override fun togglePlayPause() {
        val state = _playbackState.value
        when {
            state.isPlaying -> pause()

            player != null -> {
                configureAudioSession()
                player?.play()
                _playbackState.update { it.copy(isPlaying = true) }
                startTicker()
            }

            else -> {
                val track = state.currentTrack
                if (track != null) startPlayback(track)
            }
        }
    }

    override fun pause() {
        player?.pause()
        stopTicker()
        _playbackState.update { it.copy(isPlaying = false) }
    }

    override fun stop() {
        detachPlayer()
        stopTicker()
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
        advance(forward = true)
    }

    override fun previous() {
        val state = _playbackState.value
        if (state.positionMs > RESTART_THRESHOLD_MS || state.currentIndex <= 0) {
            seekTo(0L)
            return
        }
        advance(forward = false)
    }

    override fun seekTo(positionMs: Long) {
        val safePosition = positionMs.coerceAtLeast(0L)
        player?.seekToTime(CMTimeMakeWithSeconds(safePosition / 1000.0, 600))
        _playbackState.update { it.copy(positionMs = safePosition) }
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
        val state = _playbackState.value
        val removedIndex = state.queue.indexOfFirst { it.id == trackId }
        if (removedIndex < 0) return

        val updatedQueue = state.queue.filterNot { it.id == trackId }
        val wasCurrent = state.currentTrack?.id == trackId

        if (wasCurrent) {
            if (updatedQueue.isEmpty()) {
                stop()
                return
            }
            // Keep playing the item that took the removed slot (or the previous
            // one when the tail was removed).
            val nextIndex = removedIndex.coerceAtMost(updatedQueue.lastIndex)
            _playbackState.update {
                it.copy(queue = updatedQueue, currentIndex = nextIndex, currentTrack = updatedQueue[nextIndex])
            }
            startPlayback(updatedQueue[nextIndex])
            return
        }

        val newIndex = when {
            state.currentIndex < 0 -> -1
            removedIndex < state.currentIndex -> state.currentIndex - 1
            else -> state.currentIndex.coerceAtMost(updatedQueue.lastIndex)
        }
        _playbackState.update {
            it.copy(queue = updatedQueue, currentIndex = newIndex)
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

    // ── AVPlayer internals ──────────────────────────────────────────────────

    /**
     * Replaces the current [AVPlayer] with one bound to [track] and starts it.
     *
     * A new player is created per track on purpose: `AVPlayer` retains its
     * `AVPlayerItem`, and reusing the instance adds no benefit for a plain
     * streaming player while making end-of-item bookkeeping harder.
     */
    private fun startPlayback(track: Track) {
        val source = track.playUrl
        if (source.isBlank()) {
            _playbackState.update { it.copy(isPlaying = false, isLoading = false) }
            return
        }

        detachPlayer()
        stopTicker()
        configureAudioSession()

        val itemUrl = NSURL(string = source)
        val newPlayer = AVPlayer(uRL = itemUrl)
        player = newPlayer

        endObserver = NSNotificationCenter.defaultCenter.addObserverForName(
            AVPlayerItemDidPlayToEndTimeNotification,
            newPlayer.currentItem,
            NSOperationQueue.mainQueue
        ) {
            onTrackFinished()
        }

        newPlayer.play()
        _playbackState.update { it.copy(isPlaying = true, isLoading = true, positionMs = 0L) }
        startTicker()
    }

    private fun detachPlayer() {
        endObserver?.let { NSNotificationCenter.defaultCenter.removeObserver(it) }
        endObserver = null
        player?.pause()
        player = null
    }

    /**
     * Sets the shared audio session to `.playback` so audio is audible with the
     * silent switch on and keeps running in the background. Activating the
     * session explicitly is not required: AVPlayer activates it on `play()`.
     */
    private fun configureAudioSession() {
        AVAudioSession.sharedInstance().setCategory(AVAudioSessionCategoryPlayback, null)
    }

    private fun onTrackFinished() {
        if (_playbackState.value.repeatMode == RepeatMode.ONE) {
            val track = _playbackState.value.currentTrack ?: return
            startPlayback(track)
            return
        }

        val moved = advance(forward = true)
        if (!moved) {
            stopTicker()
            _playbackState.update { it.copy(isPlaying = false, isLoading = false) }
        }
    }

    /**
     * Moves the queue cursor. Returns `false` when the end of the queue was
     * reached and the repeat mode prevented wrapping.
     */
    private fun advance(forward: Boolean): Boolean {
        val state = _playbackState.value
        if (state.queue.isEmpty()) return false

        val rawIndex = if (forward) state.currentIndex + 1 else state.currentIndex - 1
        val targetIndex = when {
            rawIndex in state.queue.indices -> rawIndex
            state.repeatMode == RepeatMode.ALL -> if (forward) 0 else state.queue.lastIndex
            // `next()` on the last item without repeat is a no-op for manual
            // presses, but an auto-advance at the end should stop playback.
            else -> return false
        }

        val track = state.queue[targetIndex]
        _playbackState.update {
            it.copy(
                currentIndex = targetIndex,
                currentTrack = track,
                positionMs = 0L,
                durationMs = track.durationSeconds * 1000L
            )
        }
        startPlayback(track)
        return true
    }

    private fun startTicker() {
        if (ticker?.isActive == true) return

        ticker = scope.launch {
            var firstTick = true
            while (isActive) {
                val currentPlayer = player
                if (currentPlayer != null) {
                    val positionMs = secondsToMillis(CMTimeGetSeconds(currentPlayer.currentTime()))
                    val durationMs = currentDurationMillis()
                    _playbackState.update { state ->
                        state.copy(
                            positionMs = positionMs,
                            durationMs = if (durationMs > 0L) durationMs else state.durationMs,
                            isLoading = if (firstTick) false else state.isLoading
                        )
                    }
                }
                firstTick = false
                delay(POSITION_UPDATE_INTERVAL_MS)
            }
        }
    }

    private fun stopTicker() {
        ticker?.cancel()
        ticker = null
    }

    private fun currentDurationMillis(): Long {
        val item = player?.currentItem ?: return 0L
        return secondsToMillis(CMTimeGetSeconds(item.duration))
    }

    private companion object {
        private const val RESTART_THRESHOLD_MS = 3_000L
        private const val POSITION_UPDATE_INTERVAL_MS = 500L

        /**
         * `CMTimeGetSeconds` returns NaN while an item is not prepared yet and
         * `+inf` for live streams; both must never reach `PlaybackState`.
         */
        private fun secondsToMillis(seconds: Double): Long {
            if (seconds.isNaN() || seconds.isInfinite() || seconds <= 0.0) return 0L
            return (seconds * 1000.0).toLong()
        }
    }
}
