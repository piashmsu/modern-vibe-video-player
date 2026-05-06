package com.piash.modernvibe.videoplayer.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val VibePurple = Color(0xFF7C4DFF)
private val VibePink = Color(0xFFFF4DD2)
private val VibeCyan = Color(0xFF4DE5FF)
private val VibeDark = Color(0xFF0B0B14)
private val VibeDark2 = Color(0xFF141422)

private val DarkColors = darkColorScheme(
    primary = VibePurple,
    secondary = VibePink,
    tertiary = VibeCyan,
    background = VibeDark,
    surface = VibeDark2,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
)

private val LightColors = lightColorScheme(
    primary = VibePurple,
    secondary = VibePink,
    tertiary = VibeCyan,
)

@Composable
fun VibeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamic: Boolean = true,
    content: @Composable () -> Unit
) {
    val colors = when {
        dynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colors, content = content)
}
