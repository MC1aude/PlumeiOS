package com.example.plume.data.repository

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.example.plume.data.db.PlumeDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

enum class AppLanguage(val code: String, val displayName: String) {
    RUSSIAN("ru", "Русский"),
    ENGLISH("en", "English"),
    UKRAINIAN("uk", "Українська"),
    POLISH("pl", "Polski")
}

class SettingsRepository(
    private val context: Context,
    private val database: PlumeDatabase,
    private val downloadHelper: DownloadManagerHelper
) {
    private val prefs = context.getSharedPreferences("plume_settings", Context.MODE_PRIVATE)

    private val _currentLanguage = MutableStateFlow(getSavedLanguage())
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    private fun getSavedLanguage(): AppLanguage {
        val code = prefs.getString("app_lang", AppLanguage.RUSSIAN.code) ?: AppLanguage.RUSSIAN.code
        return AppLanguage.entries.find { it.code == code } ?: AppLanguage.RUSSIAN
    }

    fun setLanguage(language: AppLanguage) {
        prefs.edit().putString("app_lang", language.code).apply()

        // Apply system-wide locale using AppCompatDelegate (API 23+)
        val locale = Locale(language.code)
        val localeList = LocaleListCompat.create(locale)
        AppCompatDelegate.setApplicationLocales(localeList)

        _currentLanguage.value = language
    }

    /**
     * Apply the saved locale to the given context (for Activity wrapping).
     */
    fun applyLocaleToContext(context: Context): Context {
        val code = prefs.getString("app_lang", AppLanguage.RUSSIAN.code) ?: AppLanguage.RUSSIAN.code
        val locale = Locale(code)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val config = Configuration()
            config.setLocale(locale)
            val res = context.resources
            val newConfig = Configuration(res.configuration)
            newConfig.setLocale(locale)
            context.createConfigurationContext(newConfig)
        } else {
            val res = context.resources
            val config = res.configuration
            config.locale = locale
            res.updateConfiguration(config, res.displayMetrics)
            context
        }
    }

    suspend fun getCacheSizeMb(): String = withContext(Dispatchers.IO) {
        val cacheDir = context.cacheDir
        val bytes = getDirSize(cacheDir)
        formatSizeMb(bytes)
    }

    suspend fun getDownloadedSizeMb(): String = withContext(Dispatchers.IO) {
        val downloadsDir = File(context.filesDir, "downloads")
        val bytes = getDirSize(downloadsDir)
        formatSizeMb(bytes)
    }

    suspend fun clearAudioCache(): Boolean = withContext(Dispatchers.IO) {
        try {
            val cacheDir = context.cacheDir
            deleteDirContents(cacheDir)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun clearAllDownloads(): Boolean = withContext(Dispatchers.IO) {
        try {
            val downloadsDir = File(context.filesDir, "downloads")
            deleteDirContents(downloadsDir)
            // Also remove public Downloads copies registered in the library.
            database.trackDao().getDownloadedTracks().first().forEach { entity ->
                downloadHelper.deleteAudioFile(entity.id, entity.localPath)
                database.trackDao().updateDownloadStatus(entity.id, false, null)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun getDirSize(dir: File): Long {
        if (!dir.exists()) return 0L
        var size = 0L
        dir.listFiles()?.forEach { file ->
            size += if (file.isDirectory) getDirSize(file) else file.length()
        }
        return size
    }

    private fun deleteDirContents(dir: File) {
        if (!dir.exists()) return
        dir.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                deleteDirContents(file)
            }
            file.delete()
        }
    }

    private fun formatSizeMb(bytes: Long): String {
        val mb = bytes.toDouble() / (1024 * 1024)
        return String.format(java.util.Locale.US, "%.1f MB", mb)
    }
}
