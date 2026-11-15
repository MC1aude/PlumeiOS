package com.example.plume.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.plume.data.repository.SoundCloudRepository
import com.example.plume.domain.model.Song
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SoundCloudSearchState(
    val query: String = "",
    val songs: List<Song> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class SoundCloudSearchViewModel : ViewModel() {
    private val repository = SoundCloudRepository()

    private val _uiState = MutableStateFlow(SoundCloudSearchState())
    val uiState: StateFlow<SoundCloudSearchState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.update { it.copy(songs = emptyList(), isLoading = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(400L)
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = repository.searchSongs(query)
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        songs = result.getOrDefault(emptyList()),
                        isLoading = false,
                        error = null
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "SoundCloud search error"
                    )
                }
            }
        }
    }
}
