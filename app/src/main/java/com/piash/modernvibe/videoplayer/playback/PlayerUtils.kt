package com.piash.modernvibe.videoplayer.playback

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import android.util.Rational
import android.view.WindowManager
import androidx.media3.ui.AspectRatioFrameLayout

/** MX Player-style aspect-ratio modes. */
enum class FitMode(val display: String, val resizeMode: Int) {
    FIT("Fit", AspectRatioFrameLayout.RESIZE_MODE_FIT),
    FILL("Fill", AspectRatioFrameLayout.RESIZE_MODE_FILL),
    ZOOM("Crop / Zoom", AspectRatioFrameLayout.RESIZE_MODE_ZOOM),
    FIXED_WIDTH("Fixed width", AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH),
    FIXED_HEIGHT("Fixed height", AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT);

    fun next(): FitMode {
        val all = values()
        return all[(ordinal + 1) % all.size]
    }
}

/** Orientation mode the user can lock the player into. */
enum class OrientationLock(val display: String, val activityInfo: Int) {
    AUTO("Auto-rotate", ActivityInfo.SCREEN_ORIENTATION_SENSOR),
    LANDSCAPE("Landscape", ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE),
    PORTRAIT("Portrait", ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT),
    LOCKED("Locked", ActivityInfo.SCREEN_ORIENTATION_LOCKED);

    fun next(): OrientationLock {
        val all = values()
        return all[(ordinal + 1) % all.size]
    }
}

object PlayerUtils {
    fun setOrientation(activity: Activity, lock: OrientationLock) {
        activity.requestedOrientation = lock.activityInfo
    }

    fun supportsPip(context: Context): Boolean =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)

    fun enterPip(activity: Activity, videoWidth: Int, videoHeight: Int) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val w = videoWidth.coerceAtLeast(1)
        val h = videoHeight.coerceAtLeast(1)
        val ratio = Rational(w, h).coerceInPipBounds()
        val params = PictureInPictureParams.Builder()
            .setAspectRatio(ratio)
            .build()
        runCatching { activity.enterPictureInPictureMode(params) }
    }

    private fun Rational.coerceInPipBounds(): Rational {
        // Android requires PiP aspect ratio between [1/2.39, 2.39/1].
        val min = Rational(100, 239)
        val max = Rational(239, 100)
        if (this.toFloat() < min.toFloat()) return min
        if (this.toFloat() > max.toFloat()) return max
        return this
    }

    /** Set the screen brightness for the given activity, value [0f, 1f]. */
    fun setBrightness(activity: Activity, value: Float) {
        val w = activity.window
        val lp: WindowManager.LayoutParams = w.attributes
        lp.screenBrightness = value.coerceIn(0.01f, 1f)
        w.attributes = lp
    }

    fun currentBrightness(activity: Activity): Float {
        val attr = activity.window.attributes.screenBrightness
        return if (attr in 0f..1f) attr else 0.5f
    }

    /** Adjust media volume by `delta` steps and return the new fraction [0f, 1f]. */
    fun adjustVolume(context: Context, delta: Int): Float {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val current = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        val next = (current + delta).coerceIn(0, max)
        am.setStreamVolume(AudioManager.STREAM_MUSIC, next, 0)
        return if (max == 0) 0f else next.toFloat() / max
    }

    fun currentVolumeFraction(context: Context): Float {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val current = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        return if (max == 0) 0f else current.toFloat() / max
    }
}
