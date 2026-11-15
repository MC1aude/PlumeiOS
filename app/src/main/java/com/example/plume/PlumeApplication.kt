package com.example.plume

import android.app.Application
import com.example.plume.data.db.PlumeDatabase
import com.example.plume.data.repository.DownloadManagerHelper
import com.example.plume.data.repository.LocalAudioImporter
import com.example.plume.data.repository.MusicRepository
import com.example.plume.data.repository.SettingsRepository
import com.example.plume.player.MusicPlayerController
import com.example.plume.service.PlayerController

class PlumeApplication : Application() {

    lateinit var database: PlumeDatabase
        private set

    lateinit var repository: MusicRepository
        private set

    lateinit var settingsRepository: SettingsRepository
        private set

    lateinit var playerController: MusicPlayerController
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = PlumeDatabase.getInstance(this)
        val downloadHelper = DownloadManagerHelper(this)
        val localAudioImporter = LocalAudioImporter(this, database.trackDao())
        repository = MusicRepository(database, downloadHelper, localAudioImporter)
        settingsRepository = SettingsRepository(this, database, downloadHelper)
        playerController = PlayerController(this)
    }

    companion object {
        lateinit var instance: PlumeApplication
            private set
    }
}
