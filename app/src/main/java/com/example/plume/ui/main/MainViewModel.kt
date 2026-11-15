package com.example.plume.ui.main

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.plume.PlumeApplication
import com.example.plume.data.repository.PlumeStrings
import com.example.plume.domain.model.PlaybackState
import com.example.plume.domain.model.Playlist
import com.example.plume.domain.model.Song
import com.example.plume.domain.model.Track
import com.example.plume.ui.screens.home.HomeViewModel
import com.example.plume.ui.screens.library.LibraryViewModel
import com.example.plume.ui.screens.playlist.PlaylistViewModel
import com.example.plume.ui.screens.search.SoundCloudPlayerViewModel
import com.example.plume.ui.screens.search.SoundCloudSearchViewModel
import com.example.plume.ui.screens.settings.SettingsViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ToastNotification(
    val message: String,
    val icon: ImageVector,
    val accent: Color
)

class MainViewModel : ViewModel() {

    private val repository = PlumeApplication.instance.repository
    private val settingsRepository = PlumeApplication.instance.settingsRepository
    private val playerController = PlumeApplication.instance.playerController

    val homeViewModel = HomeViewModel()
    val soundCloudSearchViewModel = SoundCloudSearchViewModel()
    val libraryViewModel = LibraryViewModel(repository)
    val playlistViewModel = PlaylistViewModel(repository)
    val settingsViewModel = SettingsViewModel(settingsRepository)
    val soundCloudPlayerViewModel = SoundCloudPlayerViewModel(PlumeApplication.instance)

    val playbackState: StateFlow<PlaybackState> = playerController.playbackState

    private val _isSplashVisible = MutableStateFlow(true)
    val isSplashVisible: StateFlow<Boolean> = _isSplashVisible.asStateFlow()

    private val _isFullPlayerVisible = MutableStateFlow(false)
    val isFullPlayerVisible: StateFlow<Boolean> = _isFullPlayerVisible.asStateFlow()

    /** Full player for SoundCloud songs (separate from Track-based player). */
    private val _isSoundCloudFullPlayerVisible = MutableStateFlow(false)
    val isSoundCloudFullPlayerVisible: StateFlow<Boolean> = _isSoundCloudFullPlayerVisible.asStateFlow()

    private val _isQueueVisible = MutableStateFlow(false)
    val isQueueVisible: StateFlow<Boolean> = _isQueueVisible.asStateFlow()

    private val _selectedTrackForPlaylist = MutableStateFlow<Track?>(null)
    val selectedTrackForPlaylist: StateFlow<Track?> = _selectedTrackForPlaylist.asStateFlow()

    private val _isCreatePlaylistDialogVisible = MutableStateFlow(false)
    val isCreatePlaylistDialogVisible: StateFlow<Boolean> = _isCreatePlaylistDialogVisible.asStateFlow()

    /** Playlist awaiting explicit user confirmation before it is destroyed. */
    private val _playlistPendingDeletion = MutableStateFlow<Playlist?>(null)
    val playlistPendingDeletion: StateFlow<Playlist?> = _playlistPendingDeletion.asStateFlow()

    /** Tracks currently being written to the public Downloads folder. */
    private val _downloadingTrackIds = MutableStateFlow<Set<String>>(emptySet())
    val downloadingTrackIds: StateFlow<Set<String>> = _downloadingTrackIds.asStateFlow()

    /** Guards against double-tapping the delete action while a file is being removed. */
    private val _deletingTrackIds = MutableStateFlow<Set<String>>(emptySet())

    private val _isImportingAudio = MutableStateFlow(false)
    val isImportingAudio: StateFlow<Boolean> = _isImportingAudio.asStateFlow()

    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage: StateFlow<String?> = _uiMessage.asStateFlow()

    private val _toastNotification = MutableStateFlow<ToastNotification?>(null)
    val toastNotification: StateFlow<ToastNotification?> = _toastNotification.asStateFlow()

    // --- Splash Actions ---

    fun dismissSplash() {
        _isSplashVisible.value = false
    }

    // --- Toast Actions ---

    fun showToast(message: String, icon: ImageVector, accent: Color = Color.White) {
        _toastNotification.value = ToastNotification(message, icon, accent)
    }

    fun clearToast() {
        _toastNotification.value = null
    }

    // --- Player Actions ---

    fun playTrack(track: Track, queue: List<Track> = listOf(track)) {
        // Guarantee single instance playback: stop SoundCloud player and clear its items completely
        soundCloudPlayerViewModel.stop()
        playerController.playTrack(track, queue)
    }

    fun playSong(song: Song) {
        homeViewModel.recordTrackPlayed(song)
        // Pass the full current home feed as the playlist for next/prev
        val playlist = homeViewModel.uiState.value.songs
        // Guarantee single instance playback: stop library track player and clear its items completely
        playerController.stop()
        soundCloudPlayerViewModel.playSong(song, playlist.ifEmpty { null })
    }

    fun playQueue(queue: List<Track>, startIndex: Int = 0) {
        soundCloudPlayerViewModel.stop()
        playerController.setQueueAndPlay(queue, startIndex)
    }

    /** Play/pause for the local Media3 player (the SoundCloud player has its own). */
    fun toggleTrackPlayPause() {
        if (!playerController.playbackState.value.isPlaying) {
            soundCloudPlayerViewModel.pause()
        }
        playerController.togglePlayPause()
    }

    fun togglePlayPause() {
        if (!soundCloudPlayerViewModel.isPlaying.value) {
            playerController.pause()
        }
        soundCloudPlayerViewModel.togglePlayPause()
    }

    fun next() {
        playerController.next()
    }

    fun previous() {
        playerController.previous()
    }

    fun seekTo(positionMs: Long) {
        playerController.seekTo(positionMs)
    }

    fun toggleShuffle() {
        playerController.toggleShuffle()
    }

    fun toggleRepeat() {
        playerController.toggleRepeatMode()
    }

    fun removeFromQueue(trackId: String) {
        playerController.removeFromQueue(trackId)
    }

    // --- Track Actions ---

    fun toggleFavorite(track: Track) {
        viewModelScope.launch {
            val newFavoriteState = !track.isFavorite
            playerController.updateTrackState(track.id) { it.copy(isFavorite = newFavoriteState) }
            repository.setFavorite(track, newFavoriteState)
            val msg = if (newFavoriteState) string("addedToFavorites") else string("removedFromFavorites")
            showToast(msg, Icons.Default.Favorite, Color.White)
        }
    }

    /**
     * Downloads the track's audio stream into the public Downloads/Plume folder.
     * If the track is already downloaded, the local copy is removed instead.
     */
    fun downloadTrack(track: Track) {
        viewModelScope.launch {
            if (track.isDownloaded) {
                repository.deleteDownload(track)
                playerController.updateTrackState(track.id) {
                    it.copy(isDownloaded = false, localPath = if (track.isLocalFile) track.localPath else null)
                }
                showToast(string("downloadRemoved"), Icons.Default.Download, Color.White)
                return@launch
            }

            if (track.id in _downloadingTrackIds.value) return@launch
            _downloadingTrackIds.update { it + track.id }

            val result = repository.downloadTrack(track)

            _downloadingTrackIds.update { it - track.id }
            result.onSuccess { path ->
                if (!track.isLocalFile) {
                    playerController.updateTrackState(track.id) {
                        it.copy(isDownloaded = true, localPath = path)
                    }
                }
                showToast(string("savedToDownloads"), Icons.Default.DownloadDone, Color.White)
            }.onFailure {
                showToast(string("downloadFailed"), Icons.Default.Download, Color.White)
            }
        }
    }

    fun notifyDownloadPermissionDenied() {
        showToast(string("downloadFailed"), Icons.Default.Download, Color.White)
    }

    /**
     * Permanently deletes an imported / on-device track.
     *
     * Playback is pruned first so the player never keeps a dangling MediaItem that
     * points at a file which no longer exists. The repository then removes the
     * physical audio file, its cached artwork, every playlist reference and the
     * Room row (which refreshes the library reactively).
     */
    fun deleteLocalTrack(track: Track) {
        if (track.id in _deletingTrackIds.value) return
        _deletingTrackIds.update { it + track.id }

        viewModelScope.launch {
            if (playbackState.value.currentTrack?.id == track.id) {
                playerController.stop()
            }
            playerController.removeFromQueue(track.id)

            repository.deleteLocalTrack(track)
                .onSuccess {
                    showToast(string("trackDeleted"), Icons.Default.Delete, Color.White)
                }
                .onFailure {
                    showToast(string("trackDeleteFailed"), Icons.Default.Delete, Color.White)
                }

            _deletingTrackIds.update { it - track.id }
        }
    }

    // --- Local import ---

    fun importAudio(uri: Uri) {
        if (_isImportingAudio.value) return
        viewModelScope.launch {
            _isImportingAudio.value = true
            val result = repository.importLocalAudio(uri)
            _isImportingAudio.value = false
            result
                .onSuccess { showMessage("${string("importSuccess")}: ${it.title}") }
                .onFailure { showMessage(string("importFailed")) }
        }
    }

    // --- Overlays ---

    fun showFullPlayer() {
        _isFullPlayerVisible.value = true
    }

    fun hideFullPlayer() {
        _isFullPlayerVisible.value = false
    }

    fun showSoundCloudFullPlayer() {
        _isSoundCloudFullPlayerVisible.value = true
    }

    fun hideSoundCloudFullPlayer() {
        _isSoundCloudFullPlayerVisible.value = false
    }

    fun showQueue() {
        _isQueueVisible.value = true
    }

    fun hideQueue() {
        _isQueueVisible.value = false
    }

    fun openAddToPlaylist(track: Track) {
        _selectedTrackForPlaylist.value = track
    }

    fun closeAddToPlaylist() {
        _selectedTrackForPlaylist.value = null
    }

    fun addTrackToPlaylist(playlist: Playlist, track: Track) {
        viewModelScope.launch {
            repository.addTrackToPlaylist(playlist.id, track)
            closeAddToPlaylist()
            showToast(string("addedToPlaylist"), Icons.AutoMirrored.Filled.PlaylistAdd, Color.White)
        }
    }

    fun showCreatePlaylistDialog() {
        _isCreatePlaylistDialogVisible.value = true
    }

    fun hideCreatePlaylistDialog() {
        _isCreatePlaylistDialogVisible.value = false
    }

    // --- Playlist deletion (requires explicit confirmation) ---

    /** Opens the confirmation dialog instead of deleting the playlist immediately. */
    fun requestDeletePlaylist(playlist: Playlist) {
        _playlistPendingDeletion.value = playlist
    }

    fun dismissDeletePlaylist() {
        _playlistPendingDeletion.value = null
    }

    /** Deletes the pending playlist only after the user explicitly confirmed. */
    fun confirmDeletePlaylist() {
        val playlist = _playlistPendingDeletion.value ?: return
        _playlistPendingDeletion.value = null
        viewModelScope.launch {
            repository.deletePlaylist(playlist.id)
            showToast(string("playlistDeleted"), Icons.Default.Delete, Color.White)
        }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            val newId = repository.createPlaylist(name)
            val track = _selectedTrackForPlaylist.value
            if (track != null) {
                repository.addTrackToPlaylist(newId, track)
                closeAddToPlaylist()
                showToast(string("addedToPlaylist"), Icons.AutoMirrored.Filled.PlaylistAdd, Color.White)
            }
            hideCreatePlaylistDialog()
        }
    }

    // --- Messages ---

    fun clearMessage() {
        _uiMessage.value = null
    }

    private fun showMessage(message: String) {
        _uiMessage.value = message
    }

    private fun string(key: String): String =
        PlumeStrings.get(key, settingsRepository.currentLanguage.value)

    // --- Navigation Detail Triggers ---

    fun openPlaylist(playlist: Playlist) {
        playlistViewModel.loadPlaylist(playlist.id, playlist.name)
    }
}
