package com.piash.modernvibe.videoplayer.data

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri

/**
 * Tracks the last watched position for every video the user has played
 * so playback can be resumed exactly where it was left.
 *
 * Keys are URI strings; values store position in milliseconds plus a
 * "completed" flag (95% threshold) so finished videos auto-reset.
 */
class PlaybackHistory(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("modern_vibe_history", Context.MODE_PRIVATE)

    fun savePosition(uri: Uri, positionMs: Long, durationMs: Long) {
        if (durationMs <= 0) return
        // Reset to 0 if the user finished the video (>95% watched or near the end).
        val effective = if (positionMs >= durationMs - 5_000 || positionMs.toDouble() / durationMs >= 0.95) 0L
        else positionMs
        prefs.edit()
            .putLong("pos_${uri}", effective)
            .putLong("dur_${uri}", durationMs)
            .putLong("ts_${uri}", System.currentTimeMillis())
            .apply()
    }

    fun position(uri: Uri): Long = prefs.getLong("pos_${uri}", 0L)

    fun duration(uri: Uri): Long = prefs.getLong("dur_${uri}", 0L)

    fun lastWatchedAt(uri: Uri): Long = prefs.getLong("ts_${uri}", 0L)

    fun clear(uri: Uri) {
        prefs.edit()
            .remove("pos_${uri}")
            .remove("dur_${uri}")
            .remove("ts_${uri}")
            .apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
