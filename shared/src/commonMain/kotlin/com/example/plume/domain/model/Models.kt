package com.example.plume.domain.model

data class Track(
    val id: String,
    val title: String,
    val artistName: String,
    val albumName: String = "",
    val coverUrl: String = "",
    val audioUrl: String = "",
    val downloadUrl: String = "",
    val durationSeconds: Int = 0,
    val isFavorite: Boolean = false,
    val isDownloaded: Boolean = false,
    val localPath: String? = null,
    /** True when the audio was imported from the device (SAF content URI). */
    val isLocalFile: Boolean = false
) {
    /** Best available source for Media3/ExoPlayer. */
    val playUrl: String
        get() = when {
            !localPath.isNullOrEmpty() -> localPath
            audioUrl.isNotEmpty() -> audioUrl
            else -> ""
        }

    val hasPlayableSource: Boolean
        get() = playUrl.isNotEmpty()
}

data class Playlist(
    val id: Long = 0,
    val name: String,
    val trackCount: Int = 0,
    val coverUrl: String = "",
    // Epoch millis. Kept as a plain value so the model stays platform-agnostic;
    // Room fills the real timestamp via PlaylistEntity when a playlist is created.
    val createdAt: Long = 0L
)

enum class RepeatMode {
    OFF, ALL, ONE
}

data class PlaybackState(
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val queue: List<Track> = emptyList(),
    val currentIndex: Int = -1,
    val isShuffle: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val isLoading: Boolean = false
)
