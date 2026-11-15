package com.example.plume.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.plume.data.repository.AppLanguage
import com.example.plume.data.repository.PlumeStrings
import com.example.plume.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val language: AppLanguage = AppLanguage.RUSSIAN,
    val cacheSize: String = "0.0 MB",
    val downloadedSize: String = "0.0 MB",
    val message: String? = null
)

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private var currentLang: AppLanguage = AppLanguage.RUSSIAN

    init {
        viewModelScope.launch {
            repository.currentLanguage.collect { lang ->
                currentLang = lang
                _uiState.update { it.copy(language = lang) }
            }
        }
        loadStorageInfo()
    }

    fun loadStorageInfo() {
        viewModelScope.launch {
            val cache = repository.getCacheSizeMb()
            val downloads = repository.getDownloadedSizeMb()
            _uiState.update {
                it.copy(cacheSize = cache, downloadedSize = downloads)
            }
        }
    }

    fun selectLanguage(language: AppLanguage) {
        repository.setLanguage(language)
        _uiState.update {
            it.copy(
                language = language,
                message = PlumeStrings.get("languageChangedOn", language) + " " + language.displayName
            )
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            val success = repository.clearAudioCache()
            loadStorageInfo()
            _uiState.update {
                it.copy(message = if (success) PlumeStrings.get("cacheCleared", currentLang) else PlumeStrings.get("cacheClearError", currentLang))
            }
        }
    }

    fun clearDownloads() {
        viewModelScope.launch {
            val success = repository.clearAllDownloads()
            loadStorageInfo()
            _uiState.update {
                it.copy(message = if (success) PlumeStrings.get("downloadsDeleted", currentLang) else PlumeStrings.get("downloadsDeleteError", currentLang))
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
