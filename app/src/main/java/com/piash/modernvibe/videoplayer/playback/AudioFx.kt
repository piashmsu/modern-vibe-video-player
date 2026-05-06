package com.piash.modernvibe.videoplayer.playback

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer

/**
 * Wraps Android's built-in audio effects (Equalizer, BassBoost,
 * Virtualizer, LoudnessEnhancer) so they can be attached to ExoPlayer's
 * `audioSessionId` and driven from the UI.
 *
 * Presets mirror the names users expect from desktop players (Flat, Pop,
 * Rock, Bass, Vocal, Movie, Headphones).
 */
class AudioFx(audioSessionId: Int) {

    val equalizer: Equalizer = Equalizer(0, audioSessionId).apply { enabled = true }
    val bassBoost: BassBoost = BassBoost(0, audioSessionId).apply { enabled = true }
    val virtualizer: Virtualizer = Virtualizer(0, audioSessionId).apply { enabled = true }
    val loudness: LoudnessEnhancer = LoudnessEnhancer(audioSessionId).apply { enabled = false }

    val numberOfBands: Short get() = equalizer.numberOfBands
    val bandLevelRange: ShortArray get() = equalizer.bandLevelRange.toList().toShortArray()

    fun bandFreqHz(band: Short): Int = equalizer.getCenterFreq(band) / 1000

    fun setBandLevel(band: Short, millibels: Short) {
        equalizer.setBandLevel(band, millibels)
    }

    fun bandLevel(band: Short): Short = equalizer.getBandLevel(band)

    fun applyPreset(preset: EqPreset) {
        val range = bandLevelRange
        val low = range[0]
        val high = range[1]
        val n = numberOfBands.toInt()
        val gains = preset.gains(n)
        for (i in 0 until n) {
            val raw = (gains.getOrElse(i) { 0f } * 100).toInt()
            val clamped = raw.coerceIn(low.toInt(), high.toInt()).toShort()
            equalizer.setBandLevel(i.toShort(), clamped)
        }
    }

    fun setBassBoost(strength01: Float) {
        bassBoost.setStrength((strength01.coerceIn(0f, 1f) * 1000).toInt().toShort())
    }

    fun setVirtualizer(strength01: Float) {
        virtualizer.setStrength((strength01.coerceIn(0f, 1f) * 1000).toInt().toShort())
    }

    fun setLoudnessGainMb(millibels: Int) {
        loudness.setTargetGain(millibels)
        loudness.enabled = millibels > 0
    }

    fun release() {
        runCatching { equalizer.release() }
        runCatching { bassBoost.release() }
        runCatching { virtualizer.release() }
        runCatching { loudness.release() }
    }
}

/** Equalizer preset: gain values in dB applied evenly across bands. */
enum class EqPreset(val displayName: String) {
    FLAT("Flat"),
    POP("Pop"),
    ROCK("Rock"),
    BASS("Bass boost"),
    VOCAL("Vocal"),
    MOVIE("Movie"),
    HEADPHONES("Headphones");

    fun gains(numBands: Int): FloatArray = when (this) {
        FLAT -> FloatArray(numBands) { 0f }
        POP -> distribute(numBands, listOf(-1f, 2f, 4f, 4f, 1f, -1f))
        ROCK -> distribute(numBands, listOf(4f, 3f, -1f, -2f, 1f, 4f))
        BASS -> distribute(numBands, listOf(7f, 5f, 2f, 0f, -1f, -2f))
        VOCAL -> distribute(numBands, listOf(-2f, 0f, 2f, 4f, 3f, 0f))
        MOVIE -> distribute(numBands, listOf(2f, 1f, 0f, 2f, 3f, 4f))
        HEADPHONES -> distribute(numBands, listOf(3f, 2f, -1f, 0f, 2f, 3f))
    }

    private fun distribute(numBands: Int, profile: List<Float>): FloatArray {
        if (numBands <= 0) return floatArrayOf()
        val out = FloatArray(numBands)
        for (i in 0 until numBands) {
            val ratio = i.toFloat() / (numBands - 1).coerceAtLeast(1)
            val pos = (ratio * (profile.size - 1)).coerceAtLeast(0f)
            val lo = pos.toInt().coerceIn(0, profile.size - 1)
            val hi = (lo + 1).coerceAtMost(profile.size - 1)
            val frac = pos - lo
            out[i] = profile[lo] * (1f - frac) + profile[hi] * frac
        }
        return out
    }
}
