package com.example.plume.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.plume.data.repository.SoundCloudRepository
import com.example.plume.domain.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

data class SongCategory(
    val title: String,
    val songs: List<Song>
)

data class HomeSoundCloudUiState(
    val songs: List<Song> = emptyList(),
    val categories: List<SongCategory> = emptyList(),
    val quickPicks: List<Song> = emptyList(),
    val availableFilters: List<String> = listOf("All", "Phonk", "Drift", "Hits", "Chill"),
    val selectedFilter: String = "All",
    val recommendationBadge: String = "DYNAMIC FLOW",
    val isLoading: Boolean = true,
    val error: String? = null
)

class HomeViewModel : ViewModel() {
    private val repository = SoundCloudRepository()

    private val _uiState = MutableStateFlow(HomeSoundCloudUiState())
    val uiState: StateFlow<HomeSoundCloudUiState> = _uiState.asStateFlow()

    private val playCounts = mutableMapOf<String, Int>()

    init {
        loadFeed()
    }

    /** Loads the SoundCloud trending chart together with fresh phonk & drift tracks. */
    fun loadFeed() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val trendingResult = repository.getTrendingSongs()
            val phonkResult = repository.searchSongs("phonk")
            val driftResult = repository.searchSongs("aggressive phonk drift")
            val chillResult = repository.searchSongs("lofi chill beats")

            val trending = trendingResult.getOrDefault(emptyList())
            val phonk = phonkResult.getOrDefault(emptyList())
            val drift = driftResult.getOrDefault(emptyList())
            val chill = chillResult.getOrDefault(emptyList())

            val allSongs = (trending + phonk + drift + chill).distinctBy { it.id }

            if (allSongs.isNotEmpty()) {
                val badge = getDynamicTimeBadge()
                updateRecommendations(
                    allSongs = allSongs,
                    trending = trending,
                    phonk = phonk,
                    drift = drift,
                    chill = chill,
                    filter = _uiState.value.selectedFilter,
                    badge = badge
                )
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Unable to load music feed"
                    )
                }
            }
        }
    }

    fun selectFilter(filter: String) {
        val currentSongs = _uiState.value.songs
        if (currentSongs.isEmpty()) return

        _uiState.update { it.copy(selectedFilter = filter) }
        val badge = when (filter) {
            "Phonk" -> "PHONK & NIGHT RIDE"
            "Drift" -> "AGGRESSIVE DRIFT"
            "Hits" -> "TOP TRENDING CHARTS"
            "Chill" -> "LO-FI & CHILLOUT"
            else -> getDynamicTimeBadge()
        }

        val filteredSongs = when (filter) {
            "Phonk" -> currentSongs.filter { it.title.contains("phonk", ignoreCase = true) || it.artist.contains("phonk", ignoreCase = true) }
            "Drift" -> currentSongs.filter { it.title.contains("drift", ignoreCase = true) || it.title.contains("aggressive", ignoreCase = true) }
            "Hits" -> currentSongs.take(15)
            "Chill" -> currentSongs.filter { it.title.contains("chill", ignoreCase = true) || it.title.contains("lofi", ignoreCase = true) }
            else -> currentSongs
        }.ifEmpty { currentSongs }

        val quickPicks = buildDynamicQuickPicks(filteredSongs)

        val categories = buildList {
            add(SongCategory("Recommended Mix 🔥", filteredSongs.shuffled().take(12)))
            add(SongCategory("Trending Now ⚡", currentSongs.sortedByDescending { playCounts[it.id] ?: 0 }.take(12)))
            add(SongCategory("Fresh Wave 🌊", filteredSongs.takeLast(12)))
        }

        _uiState.update {
            it.copy(
                quickPicks = quickPicks,
                categories = categories,
                recommendationBadge = badge
            )
        }
    }

    /** Record user track interaction to dynamically score recommendations. */
    fun recordTrackPlayed(song: Song) {
        playCounts[song.id] = (playCounts[song.id] ?: 0) + 1
        val currentSongs = _uiState.value.songs
        if (currentSongs.isNotEmpty()) {
            val quickPicks = buildDynamicQuickPicks(currentSongs)
            _uiState.update { it.copy(quickPicks = quickPicks) }
        }
    }

    private fun buildDynamicQuickPicks(songs: List<Song>): List<Song> {
        // Sort songs favoring played tracks + top list
        val sorted = songs.sortedWith(
            compareByDescending<Song> { playCounts[it.id] ?: 0 }
                .thenBy { it.id }
        )
        return sorted.take(6)
    }

    private fun updateRecommendations(
        allSongs: List<Song>,
        trending: List<Song>,
        phonk: List<Song>,
        drift: List<Song>,
        chill: List<Song>,
        filter: String,
        badge: String
    ) {
        val quickPicks = buildDynamicQuickPicks(allSongs)

        val categories = buildList {
            if (trending.isNotEmpty()) {
                add(SongCategory("Trending Charts 🔥", trending.take(15)))
            }
            if (phonk.isNotEmpty()) {
                add(SongCategory("Phonk Essentials 🏎️", phonk.take(15)))
            }
            if (drift.isNotEmpty()) {
                add(SongCategory("Aggressive Drift ⚡", drift.take(15)))
            }
            if (chill.isNotEmpty()) {
                add(SongCategory("Lo-Fi & Midnight 🌌", chill.take(15)))
            }
        }

        _uiState.update {
            it.copy(
                songs = allSongs,
                categories = categories,
                quickPicks = quickPicks,
                selectedFilter = filter,
                recommendationBadge = badge,
                isLoading = false,
                error = null
            )
        }
    }

    private fun getDynamicTimeBadge(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..11 -> "MORNING FLOW"
            in 12..17 -> "DAYTIME BOOSTER"
            in 18..22 -> "EVENING PHONK & VIBES"
            else -> "NIGHT RIDE & DRIFT"
        }
    }
}
