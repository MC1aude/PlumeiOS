package com.example.plume.ui.screens.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.plume.data.repository.MusicRepository
import com.example.plume.domain.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlaylistDetailUiState(
    val playlistId: Long = 0,
    val playlistName: String = "",
    val tracks: List<Track> = emptyList(),
    val isLoading: Boolean = true
)

class PlaylistViewModel(private val repository: MusicRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaylistDetailUiState())
    val uiState: StateFlow<PlaylistDetailUiState> = _uiState.asStateFlow()

    fun loadPlaylist(playlistId: Long, playlistName: String) {
        _uiState.update { it.copy(playlistId = playlistId, playlistName = playlistName, isLoading = true) }
        repository.getPlaylistTracks(playlistId).onEach { tracks ->
            _uiState.update { it.copy(tracks = tracks, isLoading = false) }
        }.launchIn(viewModelScope)
    }

    fun removeTrackFromPlaylist(trackId: String) {
        val currentPlaylistId = _uiState.value.playlistId
        viewModelScope.launch {
            repository.removeTrackFromPlaylist(currentPlaylistId, trackId)
        }
    }
}
