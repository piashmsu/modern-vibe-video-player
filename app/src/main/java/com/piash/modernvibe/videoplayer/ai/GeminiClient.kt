package com.piash.modernvibe.videoplayer.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Thin client for the Google Gemini REST API
 * (https://ai.google.dev/api/generate-content).
 *
 * Used for text-only post-processing of subtitles produced by
 * [GroqWhisperClient]: translation, punctuation, summarization, keyword
 * extraction. The user supplies their own API key from the in-app
 * Settings → API Keys screen.
 */
class GeminiClient(
    private val apiKey: String,
    private val model: String = "gemini-1.5-flash",
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun generate(prompt: String): String = withContext(Dispatchers.IO) {
        require(apiKey.isNotBlank()) { "Gemini API key is missing. Set it in Settings." }

        val payload = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                    })
                })
            })
        }.toString()

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey")
            .post(payload.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw GeminiException("Gemini error ${response.code}: $raw")
            }
            extractText(raw)
        }
    }

    suspend fun translateSrt(srt: String, targetLanguage: String): String =
        generate(
            """
            Translate the following SRT subtitle file to $targetLanguage.
            Preserve the index lines and timestamp lines exactly as-is. Translate ONLY the dialogue lines.
            Output the translated SRT and nothing else.

            $srt
            """.trimIndent()
        )

    suspend fun punctuate(text: String): String =
        generate(
            "Add proper punctuation, capitalization and paragraph breaks to the following transcript. " +
                "Do not change words. Output only the corrected text:\n\n$text"
        )

    suspend fun summarize(text: String): String =
        generate("Summarize the following transcript in 4-6 bullet points:\n\n$text")

    private fun extractText(raw: String): String {
        val json = JSONObject(raw)
        val candidates = json.optJSONArray("candidates") ?: return ""
        if (candidates.length() == 0) return ""
        val parts = candidates.getJSONObject(0).optJSONObject("content")?.optJSONArray("parts") ?: return ""
        val sb = StringBuilder()
        for (i in 0 until parts.length()) {
            sb.append(parts.getJSONObject(i).optString("text", ""))
        }
        return sb.toString()
    }
}

class GeminiException(message: String) : RuntimeException(message)
