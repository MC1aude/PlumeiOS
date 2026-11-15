package com.example.plume.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.plume.domain.model.Track

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artistName: String,
    val albumName: String,
    val coverUrl: String,
    val audioUrl: String,
    val downloadUrl: String,
    val durationSeconds: Int,
    val isFavorite: Boolean = false,
    val isDownloaded: Boolean = false,
    val localPath: String? = null,
    @ColumnInfo(defaultValue = "0") val isLocalFile: Boolean = false,
    val addedAt: Long = System.currentTimeMillis()
) {
    fun toDomainTrack(): Track {
        return Track(
            id = id,
            title = title,
            artistName = artistName,
            albumName = albumName,
            coverUrl = coverUrl,
            audioUrl = audioUrl,
            downloadUrl = downloadUrl,
            durationSeconds = durationSeconds,
            isFavorite = isFavorite,
            isDownloaded = isDownloaded,
            localPath = localPath,
            isLocalFile = isLocalFile
        )
    }

    companion object {
        fun fromDomainTrack(track: Track): TrackEntity {
            return TrackEntity(
                id = track.id,
                title = track.title,
                artistName = track.artistName,
                albumName = track.albumName,
                coverUrl = track.coverUrl,
                audioUrl = track.audioUrl,
                downloadUrl = track.downloadUrl,
                durationSeconds = track.durationSeconds,
                isFavorite = track.isFavorite,
                isDownloaded = track.isDownloaded,
                localPath = track.localPath,
                isLocalFile = track.isLocalFile
            )
        }
    }
}

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val coverUrl: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_tracks",
    primaryKeys = ["playlistId", "trackId"]
)
data class PlaylistTrackCrossRef(
    val playlistId: Long,
    val trackId: String,
    val addedAt: Long = System.currentTimeMillis()
)
