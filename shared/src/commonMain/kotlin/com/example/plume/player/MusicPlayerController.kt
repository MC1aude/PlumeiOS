package com.example.plume.player

import com.example.plume.domain.model.PlaybackState
import com.example.plume.domain.model.Track
import kotlinx.coroutines.flow.StateFlow

/**
 * Platform-agnostic playback contract used by the shared UI layer.
 *
 * Android is backed by Media3/ExoPlayer (`PlayerController` in `androidMain`),
 * iOS by an `AVPlayer`-based stub (`IosMusicPlayerController` in `iosMain`).
 */
interface MusicPlayerController {

    /** Current playback state; collected by the UI as Compose state. */
    val playbackState: StateFlow<PlaybackState>

    /** Plays [track], optionally within the given [queue]. */
    fun playTrack(track: Track, queue: List<Track> = listOf(track))

    /** Replaces the queue and starts playing the item at [startIndex]. */
    fun setQueueAndPlay(queue: List<Track>, startIndex: Int = 0)

    fun togglePlayPause()

    /** Pauses local playback without replacing the current queue. */
    fun pause()

    /** Completely stops playback, clears the queue and resets playback state. */
    fun stop()

    fun next()

    fun previous()

    fun seekTo(positionMs: Long)

    fun toggleShuffle()

    fun toggleRepeatMode()

    fun removeFromQueue(trackId: String)

    /** Keeps the playback state in sync when a library action changes a track. */
    fun updateTrackState(trackId: String, transform: (Track) -> Track)
}
