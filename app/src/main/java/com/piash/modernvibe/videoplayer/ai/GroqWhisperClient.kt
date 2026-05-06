package com.piash.modernvibe.videoplayer.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Thin client for the Groq audio transcription API
 * (https://console.groq.com/docs/speech-to-text).
 *
 * Uploads an audio/video file to Groq's `whisper-large-v3` (or
 * `whisper-large-v3-turbo`) endpoint and returns a `GroqTranscriptionResult`
 * with plain text and per-segment timestamps suitable for SRT output.
 *
 * The user supplies their own API key from the in-app Settings → API Keys
 * screen; the key is stored via [com.piash.modernvibe.videoplayer.data.SecureKeyStore].
 */
class GroqWhisperClient(
    private val apiKey: String,
    private val model: String = "whisper-large-v3",
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    suspend fun transcribe(
        audioFile: File,
        language: String? = null,
        prompt: String? = null,
        timestampGranularity: String = "segment",
    ): GroqTranscriptionResult = withContext(Dispatchers.IO) {
        require(apiKey.isNotBlank()) { "Groq API key is missing. Set it in Settings." }
        require(audioFile.exists()) { "Audio file does not exist: ${audioFile.absolutePath}" }

        val mediaType = guessMediaType(audioFile.extension)
        val bodyBuilder = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("file", audioFile.name, audioFile.asRequestBody(mediaType.toMediaType()))
            .addFormDataPart("model", model)
            .addFormDataPart("response_format", "verbose_json")
            .addFormDataPart("timestamp_granularities[]", timestampGranularity)
        language?.takeIf { it.isNotBlank() }?.let { bodyBuilder.addFormDataPart("language", it) }
        prompt?.takeIf { it.isNotBlank() }?.let { bodyBuilder.addFormDataPart("prompt", it) }

        val request = Request.Builder()
            .url("https://api.groq.com/openai/v1/audio/transcriptions")
            .addHeader("Authorization", "Bearer $apiKey")
            .post(bodyBuilder.build())
            .build()

        client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw GroqException("Groq error ${response.code}: $raw")
            }
            parseGroqResponse(raw)
        }
    }

    suspend fun translateToEnglish(audioFile: File): GroqTranscriptionResult = withContext(Dispatchers.IO) {
        require(apiKey.isNotBlank()) { "Groq API key is missing. Set it in Settings." }
        val mediaType = guessMediaType(audioFile.extension)
        val body = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("file", audioFile.name, audioFile.asRequestBody(mediaType.toMediaType()))
            .addFormDataPart("model", model)
            .addFormDataPart("response_format", "verbose_json")
            .build()

        val request = Request.Builder()
            .url("https://api.groq.com/openai/v1/audio/translations")
            .addHeader("Authorization", "Bearer $apiKey")
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw GroqException("Groq error ${response.code}: $raw")
            parseGroqResponse(raw)
        }
    }

    private fun guessMediaType(ext: String): String = when (ext.lowercase()) {
        "mp3" -> "audio/mpeg"
        "wav" -> "audio/wav"
        "m4a" -> "audio/mp4"
        "flac" -> "audio/flac"
        "ogg" -> "audio/ogg"
        "mp4" -> "video/mp4"
        "mkv" -> "video/x-matroska"
        "webm" -> "video/webm"
        else -> "application/octet-stream"
    }

    private fun parseGroqResponse(raw: String): GroqTranscriptionResult {
        val json = JSONObject(raw)
        val text = json.optString("text", "")
        val segments = mutableListOf<GroqSegment>()
        val arr = json.optJSONArray("segments")
        if (arr != null) {
            for (i in 0 until arr.length()) {
                val seg = arr.getJSONObject(i)
                segments.add(
                    GroqSegment(
                        id = seg.optInt("id", i),
                        start = seg.optDouble("start", 0.0).toFloat(),
                        end = seg.optDouble("end", 0.0).toFloat(),
                        text = seg.optString("text", ""),
                    )
                )
            }
        }
        return GroqTranscriptionResult(text = text, segments = segments, language = json.optString("language", null))
    }
}

data class GroqSegment(
    val id: Int,
    val start: Float,
    val end: Float,
    val text: String,
)

data class GroqTranscriptionResult(
    val text: String,
    val segments: List<GroqSegment>,
    val language: String?,
) {
    /** Convert the segments into SRT format. */
    fun toSrt(): String {
        if (segments.isEmpty()) {
            return "1\n00:00:00,000 --> 00:00:30,000\n${text.trim()}\n"
        }
        return buildString {
            segments.forEachIndexed { idx, seg ->
                append(idx + 1).append('\n')
                append(formatSrtTime(seg.start)).append(" --> ").append(formatSrtTime(seg.end)).append('\n')
                append(seg.text.trim()).append("\n\n")
            }
        }
    }

    private fun formatSrtTime(seconds: Float): String {
        val total = (seconds * 1000).toLong()
        val h = total / 3_600_000
        val m = (total % 3_600_000) / 60_000
        val s = (total % 60_000) / 1000
        val ms = total % 1000
        return "%02d:%02d:%02d,%03d".format(h, m, s, ms)
    }
}

class GroqException(message: String) : RuntimeException(message)
