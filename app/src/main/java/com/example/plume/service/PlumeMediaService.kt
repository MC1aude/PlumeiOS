package com.example.plume.service

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.plume.MainActivity

/**
 * Foreground media playback service.
 *
 * Owns the single [ExoPlayer] instance and exposes it to the rest of the app
 * (and to system clients such as the lock screen, Bluetooth headsets and Android
 * Auto) through a [MediaSession].
 *
 * `MediaSessionService` automatically:
 *  - promotes the service to the foreground while playback is active
 *    (requires `FOREGROUND_SERVICE_MEDIA_PLAYBACK` + `foregroundServiceType`),
 *  - shows and updates the media notification,
 *  - keeps playback alive when the app is minimized or the task is removed.
 */
class PlumeMediaService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        instance = this

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val exoPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            // Keeps the CPU awake during background playback (WAKE_LOCK permission).
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()

        val sessionActivityIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            sessionActivityIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, exoPlayer)
            .setSessionActivity(pendingIntent)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    /**
     * Called when the user swipes the task away. If audio is actively playing we
     * keep the service alive (foreground notification remains); otherwise we shut
     * down so no empty media notification is left behind.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        val isActivelyPlaying = player != null &&
            player.playWhenReady &&
            player.playbackState != Player.STATE_ENDED &&
            player.mediaItemCount > 0

        if (!isActivelyPlaying) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        instance = null
        super.onDestroy()
    }

    companion object {
        /** Non-owning reference kept for diagnostics / legacy call sites. */
        var instance: PlumeMediaService? = null
            private set
    }
}
