package com.piash.modernvibe.videoplayer.ui.screens

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.piash.modernvibe.videoplayer.VibeApplication
import com.piash.modernvibe.videoplayer.ai.AiSubtitleController
import com.piash.modernvibe.videoplayer.playback.AudioFx
import com.piash.modernvibe.videoplayer.playback.EqPreset
import com.piash.modernvibe.videoplayer.playback.FitMode
import com.piash.modernvibe.videoplayer.playback.OrientationLock
import com.piash.modernvibe.videoplayer.playback.PlayerUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class SrtCue(val startMs: Long, val endMs: Long, val text: String)

@Composable
fun PlayerScreen(uri: Uri?) {
    val context = LocalContext.current
    val activity = context as? Activity
    val app = context.applicationContext as VibeApplication
    val scope = rememberCoroutineScope()

    var showControls by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var duration by remember { mutableLongStateOf(0L) }
    var currentPos by remember { mutableLongStateOf(0L) }
    var speed by remember { mutableFloatStateOf(1f) }
    var fitMode by remember { mutableStateOf(FitMode.FIT) }
    var orientation by remember { mutableStateOf(OrientationLock.AUTO) }
    var videoWidth by remember { mutableIntStateOf(16) }
    var videoHeight by remember { mutableIntStateOf(9) }

    var aiCues by remember { mutableStateOf<List<SrtCue>>(emptyList()) }
    var aiBanner by remember { mutableStateOf("") }
    var aiBusy by remember { mutableStateOf(false) }
    var showAiCaptions by remember { mutableStateOf(true) }

    var hudType by remember { mutableStateOf(HudType.NONE) }
    var hudValue by remember { mutableFloatStateOf(0f) }
    var hudLabel by remember { mutableStateOf("") }
    var hudVisibleUntil by remember { mutableLongStateOf(0L) }

    var showEqDialog by remember { mutableStateOf(false) }

    val controller = remember { AiSubtitleController(context, app.keys) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply { playWhenReady = true }
    }
    var playerView by remember { mutableStateOf<PlayerView?>(null) }
    var audioFx by remember { mutableStateOf<AudioFx?>(null) }

    DisposableEffect(uri) {
        if (uri != null) {
            exoPlayer.setMediaItem(MediaItem.fromUri(uri))
            exoPlayer.prepare()
            val savedPos = app.history.position(uri)
            if (savedPos > 0) {
                exoPlayer.seekTo(savedPos)
                hudType = HudType.SEEK
                hudLabel = "Resumed from ${formatTime(savedPos)}"
                hudVisibleUntil = System.currentTimeMillis() + 2500
            }
        }
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    videoWidth = videoSize.width
                    videoHeight = videoSize.height
                }
            }
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                audioFx?.release()
                if (audioSessionId != 0) {
                    audioFx = runCatching { AudioFx(audioSessionId) }.getOrNull()
                }
            }
        }
        exoPlayer.addListener(listener)
        runCatching {
            val sid = exoPlayer.audioSessionId
            if (sid != 0) audioFx = AudioFx(sid)
        }
        onDispose {
            if (uri != null) {
                app.history.savePosition(uri, exoPlayer.currentPosition, exoPlayer.duration)
            }
            audioFx?.release()
            audioFx = null
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    LaunchedEffect(exoPlayer) {
        while (true) {
            delay(250)
            duration = exoPlayer.duration.coerceAtLeast(1L)
            currentPos = exoPlayer.currentPosition
            progress = if (duration > 0) currentPos.toFloat() / duration else 0f
            if (uri != null && currentPos > 1000 && currentPos % 5_000 < 250) {
                app.history.savePosition(uri, currentPos, duration)
            }
            if (hudVisibleUntil != 0L && System.currentTimeMillis() > hudVisibleUntil) {
                hudType = HudType.NONE
                hudVisibleUntil = 0L
            }
        }
    }

    LaunchedEffect(showControls) {
        if (showControls && hudType == HudType.NONE) {
            delay(5000); showControls = false
        }
    }

    LaunchedEffect(fitMode) {
        playerView?.resizeMode = fitMode.resizeMode
    }

    LaunchedEffect(orientation) {
        activity?.let { PlayerUtils.setOrientation(it, orientation) }
    }

    val currentCue by remember {
        derivedStateOf {
            aiCues.firstOrNull { currentPos in it.startMs..it.endMs }?.text.orEmpty()
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val widthPx = constraints.maxWidth.coerceAtLeast(1)
        val heightPx = constraints.maxHeight.coerceAtLeast(1)

        if (uri != null) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        resizeMode = fitMode.resizeMode
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        playerView = this
                    }
                },
                update = { it.resizeMode = fitMode.resizeMode },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("▶", style = MaterialTheme.typography.displayLarge, color = Color.White.copy(alpha = 0.3f))
                Text("Open a video to start", style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.5f))
            }
        }

        // Gesture surface — taps + vertical swipes for brightness/volume
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(uri) {
                    detectTapGestures(
                        onTap = { showControls = !showControls },
                        onDoubleTap = { offset ->
                            val rightHalf = offset.x > size.width / 2
                            val seekDeltaMs = if (rightHalf) 10_000L else -10_000L
                            val target = (exoPlayer.currentPosition + seekDeltaMs)
                                .coerceIn(0L, exoPlayer.duration.coerceAtLeast(0L))
                            exoPlayer.seekTo(target)
                            hudType = HudType.SEEK
                            hudLabel = if (rightHalf) "+10s" else "-10s"
                            hudVisibleUntil = System.currentTimeMillis() + 1500
                        }
                    )
                }
                .pointerInput(uri) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            val onLeft = change.position.x < size.width / 2
                            val rangePx = size.height.toFloat().coerceAtLeast(1f)
                            val deltaFraction = -dragAmount / rangePx
                            if (onLeft) {
                                val current = activity?.let { PlayerUtils.currentBrightness(it) } ?: 0.5f
                                val next = (current + deltaFraction).coerceIn(0.01f, 1f)
                                activity?.let { PlayerUtils.setBrightness(it, next) }
                                hudType = HudType.BRIGHTNESS
                                hudValue = next
                                hudLabel = "Brightness ${(next * 100).toInt()}%"
                                hudVisibleUntil = System.currentTimeMillis() + 1500
                            } else {
                                val steps = if (deltaFraction > 0.02f) 1 else if (deltaFraction < -0.02f) -1 else 0
                                if (steps != 0) {
                                    val frac = PlayerUtils.adjustVolume(context, steps)
                                    hudType = HudType.VOLUME
                                    hudValue = frac
                                    hudLabel = "Volume ${(frac * 100).toInt()}%"
                                    hudVisibleUntil = System.currentTimeMillis() + 1500
                                } else {
                                    hudType = HudType.VOLUME
                                    hudValue = PlayerUtils.currentVolumeFraction(context)
                                    hudLabel = "Volume ${(hudValue * 100).toInt()}%"
                                    hudVisibleUntil = System.currentTimeMillis() + 1200
                                }
                            }
                        }
                    )
                }
        )

        // Captions
        if (showAiCaptions && (currentCue.isNotEmpty() || aiBanner.isNotEmpty())) {
            Text(
                text = if (currentCue.isNotEmpty()) currentCue else aiBanner,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 110.dp, start = 16.dp, end = 16.dp)
                    .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }

        // HUD overlay (brightness/volume/seek)
        if (hudType != HudType.NONE) {
            HudOverlay(hudType, hudValue, hudLabel, modifier = Modifier.align(Alignment.Center))
        }

        // Player controls
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(16.dp)
            ) {
                Slider(
                    value = progress,
                    onValueChange = { newProgress ->
                        progress = newProgress
                        exoPlayer.seekTo((newProgress * duration).toLong())
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatTime(currentPos), color = Color.White, style = MaterialTheme.typography.labelSmall)
                    Text("${videoWidth}x${videoHeight} • ${fitMode.display}", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                    Text(formatTime(duration), color = Color.White, style = MaterialTheme.typography.labelSmall)
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { exoPlayer.seekBack() }) {
                        Icon(Icons.Filled.FastRewind, contentDescription = "Rewind", tint = Color.White)
                    }
                    Spacer(Modifier.width(16.dp))
                    IconButton(
                        onClick = { if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play() },
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    IconButton(onClick = { exoPlayer.seekForward() }) {
                        Icon(Icons.Filled.FastForward, contentDescription = "Forward", tint = Color.White)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PlayerActionButton(Icons.Filled.AspectRatio, fitMode.display) {
                        fitMode = fitMode.next()
                        hudType = HudType.SEEK
                        hudLabel = "Aspect: ${fitMode.display}"
                        hudVisibleUntil = System.currentTimeMillis() + 1200
                    }
                    PlayerActionButton(Icons.Filled.ScreenRotation, orientation.display) {
                        orientation = orientation.next()
                        hudType = HudType.SEEK
                        hudLabel = orientation.display
                        hudVisibleUntil = System.currentTimeMillis() + 1200
                    }
                    PlayerActionButton(
                        Icons.Filled.Subtitles,
                        if (showAiCaptions) "Subs on" else "Subs off"
                    ) { showAiCaptions = !showAiCaptions }
                    PlayerActionButton(
                        Icons.Filled.AutoAwesome,
                        if (aiBusy) "AI…" else "AI subs",
                        enabled = !aiBusy && uri != null
                    ) {
                        if (uri == null) return@PlayerActionButton
                        aiBusy = true
                        aiBanner = "AI: extracting audio…"
                        scope.launch {
                            val stage = controller.generate(uri)
                            aiBusy = false
                            when (stage) {
                                is AiSubtitleController.Stage.Done -> {
                                    aiCues = parseSrt(stage.srt)
                                    aiBanner = if (aiCues.isEmpty()) "AI captions ready." else ""
                                }
                                is AiSubtitleController.Stage.Error -> aiBanner = "AI error: ${stage.message}"
                                else -> aiBanner = "AI captions ready."
                            }
                        }
                    }
                    PlayerActionButton(Icons.Filled.Equalizer, "EQ") { showEqDialog = true }
                    PlayerActionButton(Icons.Filled.PictureInPicture, "PiP") {
                        activity?.let {
                            if (PlayerUtils.supportsPip(it)) {
                                PlayerUtils.enterPip(it, videoWidth, videoHeight)
                            }
                        }
                    }
                    PlayerActionButton(Icons.Filled.Speed, "${speed}x") {
                        speed = when {
                            speed < 1.25f -> 1.25f
                            speed < 1.5f -> 1.5f
                            speed < 2.0f -> 2.0f
                            else -> 1.0f
                        }
                        exoPlayer.setPlaybackSpeed(speed)
                    }
                }
            }
        }

        if (aiBusy) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 24.dp)
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text("Generating AI subtitles…", color = Color.White, style = MaterialTheme.typography.labelMedium)
            }
        }
    }

    if (showEqDialog) {
        EqualizerDialog(
            audioFx = audioFx,
            onDismiss = { showEqDialog = false }
        )
    }
}

private enum class HudType { NONE, BRIGHTNESS, VOLUME, SEEK }

@Composable
private fun HudOverlay(type: HudType, value: Float, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val icon = when (type) {
            HudType.BRIGHTNESS -> Icons.Filled.BrightnessHigh
            HudType.VOLUME -> Icons.Filled.VolumeUp
            HudType.SEEK -> Icons.Filled.FastForward
            HudType.NONE -> return
        }
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
        Spacer(Modifier.height(6.dp))
        Text(label, color = Color.White, style = MaterialTheme.typography.labelLarge)
        if (type == HudType.BRIGHTNESS || type == HudType.VOLUME) {
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { value.coerceIn(0f, 1f) },
                modifier = Modifier.width(160.dp),
                color = Color.White
            )
        }
    }
}

@Composable
private fun PlayerActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick, enabled = enabled) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = label, tint = if (enabled) Color.White else Color.Gray, modifier = Modifier.size(22.dp))
            Text(label, color = if (enabled) Color.White else Color.Gray, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun EqualizerDialog(audioFx: AudioFx?, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val fx = audioFx
    var version by remember { mutableIntStateOf(0) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        title = { Text("Equalizer") },
        text = {
            if (fx == null) {
                Text("Equalizer unavailable. Start playing a video first.")
            } else {
                Column {
                    Text("Presets", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        EqPreset.values().take(4).forEach { p ->
                            Button(
                                onClick = { fx.applyPreset(p); version++ },
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            ) { Text(p.displayName, style = MaterialTheme.typography.labelSmall) }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        EqPreset.values().drop(4).forEach { p ->
                            Button(
                                onClick = { fx.applyPreset(p); version++ },
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            ) { Text(p.displayName, style = MaterialTheme.typography.labelSmall) }
                        }
                    }

                    val n = fx.numberOfBands.toInt()
                    val range = fx.bandLevelRange
                    val low = range[0].toFloat()
                    val high = range[1].toFloat()
                    for (b in 0 until n) {
                        val band = b.toShort()
                        val freq = fx.bandFreqHz(band)
                        val cur = fx.bandLevel(band).toFloat()
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${freq}Hz", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(56.dp))
                            Slider(
                                value = cur,
                                onValueChange = { v ->
                                    fx.setBandLevel(band, v.toInt().toShort())
                                    version++
                                },
                                valueRange = low..high,
                                modifier = Modifier.weight(1f)
                            )
                            Text("${(cur / 100).toInt()}dB", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(40.dp))
                        }
                    }

                    val _consume = version
                }
            }
        }
    )
}

private fun formatTime(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSec = ms / 1000
    val hours = totalSec / 3600
    val minutes = (totalSec % 3600) / 60
    val seconds = totalSec % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%d:%02d".format(minutes, seconds)
}

private fun parseSrt(srt: String): List<SrtCue> {
    val cues = mutableListOf<SrtCue>()
    val blocks = srt.replace("\r", "").split(Regex("\n\\s*\n"))
    val timeRe = Regex("(\\d{2}):(\\d{2}):(\\d{2}),(\\d{3})\\s*-->\\s*(\\d{2}):(\\d{2}):(\\d{2}),(\\d{3})")
    for (block in blocks) {
        val lines = block.trim().lines().filter { it.isNotBlank() }
        if (lines.size < 2) continue
        val timeLine = lines.firstOrNull { timeRe.containsMatchIn(it) } ?: continue
        val m = timeRe.find(timeLine) ?: continue
        val start = m.groupValues[1].toLong() * 3_600_000 +
            m.groupValues[2].toLong() * 60_000 +
            m.groupValues[3].toLong() * 1000 +
            m.groupValues[4].toLong()
        val end = m.groupValues[5].toLong() * 3_600_000 +
            m.groupValues[6].toLong() * 60_000 +
            m.groupValues[7].toLong() * 1000 +
            m.groupValues[8].toLong()
        val text = lines.dropWhile { !timeRe.containsMatchIn(it) }
            .drop(1)
            .joinToString("\n")
            .trim()
        if (text.isNotEmpty()) cues.add(SrtCue(start, end, text))
    }
    return cues
}
