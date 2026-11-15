package com.example.plume.data.db

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class PlaylistWithTrackCount(
    @Embedded val playlist: PlaylistEntity,
    val trackCount: Int
)

@Dao
interface TrackDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTrack(track: TrackEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTracks(tracks: List<TrackEntity>)

    @Query("SELECT * FROM tracks WHERE id = :id LIMIT 1")
    suspend fun getTrackById(id: String): TrackEntity?

    @Query("SELECT * FROM tracks WHERE isFavorite = 1 ORDER BY addedAt DESC")
    fun getFavoriteTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE isDownloaded = 1 ORDER BY addedAt DESC")
    fun getDownloadedTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE isLocalFile = 1 ORDER BY addedAt DESC")
    fun getLocalTracks(): Flow<List<TrackEntity>>

    @Query("UPDATE tracks SET isFavorite = :isFavorite WHERE id = :trackId")
    suspend fun updateFavoriteStatus(trackId: String, isFavorite: Boolean)

    @Query("UPDATE tracks SET isDownloaded = :isDownloaded, localPath = :localPath WHERE id = :trackId")
    suspend fun updateDownloadStatus(trackId: String, isDownloaded: Boolean, localPath: String?)

    @Query("DELETE FROM tracks WHERE id = :trackId")
    suspend fun deleteTrack(trackId: String)
}

@Dao
interface PlaylistDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun deletePlaylistTracks(playlistId: Long)

    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Query("""
        SELECT p.*,
               (SELECT COUNT(*) FROM playlist_tracks pt WHERE pt.playlistId = p.id) AS trackCount
        FROM playlists p
        ORDER BY p.createdAt DESC
    """)
    fun getAllPlaylistsWithTrackCount(): Flow<List<PlaylistWithTrackCount>>

    @Query("SELECT * FROM playlists WHERE id = :id LIMIT 1")
    suspend fun getPlaylistById(id: Long): PlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addTrackToPlaylist(crossRef: PlaylistTrackCrossRef)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: String)

    /** Purges every playlist reference to a track that is being removed from the library. */
    @Query("DELETE FROM playlist_tracks WHERE trackId = :trackId")
    suspend fun deleteCrossReferencesForTrack(trackId: String)

    @Query("""
        SELECT t.* FROM tracks t
        INNER JOIN playlist_tracks pt ON t.id = pt.trackId
        WHERE pt.playlistId = :playlistId
        ORDER BY pt.addedAt ASC
    """)
    fun getTracksForPlaylist(playlistId: Long): Flow<List<TrackEntity>>

    @Query("SELECT COUNT(*) FROM playlist_tracks WHERE playlistId = :playlistId")
    fun getPlaylistTrackCount(playlistId: Long): Flow<Int>
}
