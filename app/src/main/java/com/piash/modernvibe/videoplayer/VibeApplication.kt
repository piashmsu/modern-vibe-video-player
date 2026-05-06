package com.piash.modernvibe.videoplayer

import android.app.Application
import com.piash.modernvibe.videoplayer.data.PlaybackHistory
import com.piash.modernvibe.videoplayer.data.SecureKeyStore
import com.piash.modernvibe.videoplayer.data.SettingsRepository
import com.piash.modernvibe.videoplayer.data.VideoScanner

class VibeApplication : Application() {
    val settings: SettingsRepository by lazy { SettingsRepository(this) }
    val keys: SecureKeyStore by lazy { SecureKeyStore(this) }
    val scanner: VideoScanner by lazy { VideoScanner(this) }
    val history: PlaybackHistory by lazy { PlaybackHistory(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
    companion object {
        lateinit var instance: VibeApplication
            private set
    }
}
