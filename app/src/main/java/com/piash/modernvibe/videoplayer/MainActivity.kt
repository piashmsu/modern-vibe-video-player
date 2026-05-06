package com.piash.modernvibe.videoplayer

import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.piash.modernvibe.videoplayer.ui.VibeApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val intentUri: Uri? = intent?.data
        setContent {
            val initialUri = remember { mutableStateOf(intentUri) }
            VibeApp(initialUri = initialUri.value)
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        // Compose handles configuration changes via configChanges in the manifest;
        // no additional work needed here.
    }
}
