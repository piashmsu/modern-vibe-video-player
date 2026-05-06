package com.piash.modernvibe.videoplayer.ui.screens

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.piash.modernvibe.videoplayer.VibeApplication
import com.piash.modernvibe.videoplayer.ai.AiSubtitleController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class SrtCue(val startMs: Long, val endMs: Long, val text: String)

@Composable
fun PlayerScreen(uri: Uri?) {
    val context = LocalContext.current
    val app = context.applicationContext as VibeApplication
    val scope = rememberCoroutineScope()

    var showControls by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var duration by remember { mutableLongStateOf(0L) }
    var currentPos by remember { mutableLongStateOf(0L) }
    var speed by remember { mutableFloatStateOf(1f) }

    var aiCues by remember { mutableStateOf<List<SrtCue>>(emptyList()) }
    var aiBanner by remember { mutableStateOf("") }
    var aiBusy by remember { mutableStateOf(false) }
    var showAiCaptions by remember { mutableStateOf(true) }

    val controller = remember { AiSubtitleController(context, app.keys) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }

    DisposableEffect(uri) {
        if (uri != null) {
            exoPlayer.setMediaItem(MediaItem.fromUri(uri))
            exoPlayer.prepare()
        }
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
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
        }
    }

    LaunchedEffect(showControls) {
        if (showControls) {
            delay(5000)
            showControls = false
        }
    }

    val currentCue by remember {
        derivedStateOf {
            aiCues.firstOrNull { currentPos in it.startMs..it.endMs }?.text.orEmpty()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { showControls = !showControls }
    ) {
        if (uri != null) {
            AndroidView(
                factory = {
                    PlayerView(it).apply {
                        player = exoPlayer
                        useController = false
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
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
                Text(
                    "Supports: MP4, MKV, AVI, FLV, WebM, TS, HLS, DASH, RTSP",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.3f)
                )
            }
        }

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
                    Text(formatTime(duration), color = Color.White, style = MaterialTheme.typography.labelSmall)
                }
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
                        onClick = {
                            if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                        },
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
                    IconButton(onClick = { showAiCaptions = !showAiCaptions }) {
                        Icon(
                            Icons.Filled.Subtitles,
                            contentDescription = "Toggle captions",
                            tint = if (showAiCaptions) MaterialTheme.colorScheme.primary else Color.White
                        )
                    }
                    IconButton(
                        enabled = !aiBusy && uri != null,
                        onClick = {
                            if (uri == null) return@IconButton
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
                                    is AiSubtitleController.Stage.Error -> {
                                        aiBanner = "AI error: ${stage.message}"
                                    }
                                    else -> {
                                        aiBanner = "AI captions ready."
                                    }
                                }
                            }
                        }
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.AutoAwesome,
                                contentDescription = "Generate AI subtitles",
                                tint = if (aiBusy) MaterialTheme.colorScheme.tertiary else Color.White
                            )
                            Text(
                                if (aiBusy) "AI…" else "AI subs",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                    IconButton(onClick = {
                        speed = when {
                            speed < 1.25f -> 1.25f
                            speed < 1.5f -> 1.5f
                            speed < 2.0f -> 2.0f
                            else -> 1.0f
                        }
                        exoPlayer.setPlaybackSpeed(speed)
                    }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.Speed, contentDescription = "Speed", tint = Color.White, modifier = Modifier.size(20.dp))
                            Text("${speed}x", color = Color.White, style = MaterialTheme.typography.labelSmall)
                        }
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
