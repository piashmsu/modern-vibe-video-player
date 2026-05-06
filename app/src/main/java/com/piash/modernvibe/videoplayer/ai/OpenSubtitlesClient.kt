package com.piash.modernvibe.videoplayer.ai

import com.piash.modernvibe.videoplayer.data.ApiKey
import com.piash.modernvibe.videoplayer.data.SecureKeyStore
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Minimal client for the OpenSubtitles REST API
 * (https://opensubtitles.stoplight.io/docs/opensubtitles-api/).
 *
 * Supports searching by movie/episode title and downloading the SRT
 * payload for a chosen result. Requires an API key from
 * https://www.opensubtitles.com/en/consumers (free tier).
 */
class OpenSubtitlesClient(
    private val keys: SecureKeyStore,
    private val userAgent: String = "ModernVibeVideoPlayer v1.0",
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
) {
    data class SubtitleHit(
        val fileId: Long,
        val title: String,
        val language: String,
        val release: String,
        val downloads: Int
    )

    fun search(query: String, language: String = "en"): List<SubtitleHit> {
        val apiKey = keys.get(ApiKey.OPENSUBTITLES)
        if (apiKey.isBlank()) error("OpenSubtitles API key missing. Add it in Settings → AI API Keys.")

        val url = "https://api.opensubtitles.com/api/v1/subtitles" +
            "?query=${java.net.URLEncoder.encode(query, "UTF-8")}" +
            "&languages=$language"
        val req = Request.Builder()
            .url(url)
            .header("Api-Key", apiKey)
            .header("User-Agent", userAgent)
            .header("Accept", "application/json")
            .get()
            .build()

        client.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) throw IOException("OpenSubtitles HTTP ${resp.code}: $body")
            return parseSearch(body)
        }
    }

    /** Returns a temporary SRT download link for the given file id. */
    fun fetchDownloadUrl(fileId: Long): String {
        val apiKey = keys.get(ApiKey.OPENSUBTITLES)
        if (apiKey.isBlank()) error("OpenSubtitles API key missing.")

        val payload = JSONObject().apply { put("file_id", fileId) }
        val req = Request.Builder()
            .url("https://api.opensubtitles.com/api/v1/download")
            .header("Api-Key", apiKey)
            .header("User-Agent", userAgent)
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) throw IOException("OpenSubtitles HTTP ${resp.code}: $body")
            val link = JSONObject(body).optString("link", "")
            if (link.isBlank()) throw IOException("No download link in response: $body")
            return link
        }
    }

    /** Downloads the file referenced by `url` (typically SRT text). */
    fun downloadText(url: String): String {
        val req = Request.Builder().url(url).get().build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("Download HTTP ${resp.code}")
            return resp.body?.string().orEmpty()
        }
    }

    private fun parseSearch(json: String): List<SubtitleHit> {
        val out = mutableListOf<SubtitleHit>()
        val root = JSONObject(json)
        val data = root.optJSONArray("data") ?: return out
        for (i in 0 until data.length()) {
            val item = data.optJSONObject(i) ?: continue
            val attrs = item.optJSONObject("attributes") ?: continue
            val files = attrs.optJSONArray("files") ?: continue
            if (files.length() == 0) continue
            val fileId = files.optJSONObject(0)?.optLong("file_id", -1) ?: -1
            if (fileId < 0) continue
            out += SubtitleHit(
                fileId = fileId,
                title = attrs.optJSONObject("feature_details")?.optString("movie_name").orEmpty()
                    .ifBlank { attrs.optString("release", "Unknown") },
                language = attrs.optString("language", "?"),
                release = attrs.optString("release", "—"),
                downloads = attrs.optInt("download_count", 0)
            )
        }
        return out
    }

}
