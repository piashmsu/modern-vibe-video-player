package com.piash.modernvibe.videoplayer

import android.app.Application
import com.piash.modernvibe.videoplayer.data.SettingsRepository

class VibeApplication : Application() {
    val settings: SettingsRepository by lazy { SettingsRepository(this) }
    override fun onCreate() {
        super.onCreate()
        instance = this
    }
    companion object {
        lateinit var instance: VibeApplication
            private set
    }
}
