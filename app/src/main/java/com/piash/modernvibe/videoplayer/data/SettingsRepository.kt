package com.piash.modernvibe.videoplayer.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Lightweight key-value settings store backed by SharedPreferences.
 * Holds the 200+ feature toggles and tunables that drive the UI.
 */
class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("modern_vibe_prefs", Context.MODE_PRIVATE)

    fun getBoolean(key: String, default: Boolean): Boolean = prefs.getBoolean(key, default)
    fun setBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    fun getFloat(key: String, default: Float): Float = prefs.getFloat(key, default)
    fun setFloat(key: String, value: Float) {
        prefs.edit().putFloat(key, value).apply()
    }

    fun getInt(key: String, default: Int): Int = prefs.getInt(key, default)
    fun setInt(key: String, value: Int) {
        prefs.edit().putInt(key, value).apply()
    }

    fun getString(key: String, default: String): String =
        prefs.getString(key, default) ?: default
    fun setString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    fun toggleAll(items: List<FeatureItem>, value: Boolean) {
        val editor = prefs.edit()
        items.forEach { editor.putBoolean(it.key, value) }
        editor.apply()
    }
}
