package com.piash.modernvibe.videoplayer.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Stores user-supplied API keys (Groq, Gemini, OpenAI, etc.) using
 * Android's [EncryptedSharedPreferences] backed by the Android Keystore.
 *
 * Falls back to plain SharedPreferences if the keystore is unavailable
 * (e.g. on emulators with broken Keystore implementations) so the app
 * still functions, but in that case the keys are stored in clear.
 */
class SecureKeyStore(context: Context) {

    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "modern_vibe_secure_keys",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (t: Throwable) {
        context.getSharedPreferences("modern_vibe_keys_fallback", Context.MODE_PRIVATE)
    }

    fun get(key: ApiKey): String = prefs.getString(key.prefKey, null).orEmpty()

    fun set(key: ApiKey, value: String) {
        prefs.edit().putString(key.prefKey, value).apply()
    }

    fun isSet(key: ApiKey): Boolean = get(key).isNotBlank()
}

enum class ApiKey(val prefKey: String, val label: String, val provider: String, val helpUrl: String) {
    GROQ("groq_api_key", "Groq API key", "Groq (Whisper-large-v3)", "https://console.groq.com/keys"),
    GEMINI("gemini_api_key", "Gemini API key", "Google Gemini", "https://aistudio.google.com/app/apikey"),
    OPENAI("openai_api_key", "OpenAI API key", "OpenAI Whisper / GPT", "https://platform.openai.com/api-keys"),
    OPENSUBTITLES("opensubtitles_api_key", "OpenSubtitles API key", "OpenSubtitles.com", "https://www.opensubtitles.com/en/consumers"),
}
