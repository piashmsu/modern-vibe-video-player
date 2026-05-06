package com.piash.modernvibe.videoplayer.ai

import android.content.Context
import android.net.Uri
import com.piash.modernvibe.videoplayer.data.ApiKey
import com.piash.modernvibe.videoplayer.data.SecureKeyStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * High-level orchestrator that ties together the user-supplied API keys,
 * audio extraction, Groq Whisper transcription, and (optional) Gemini
 * post-processing (translate / punctuate / summarize).
 */
class AiSubtitleController(
    private val context: Context,
    private val keys: SecureKeyStore,
) {
    sealed interface Stage {
        data object Idle : Stage
        data object ExtractingAudio : Stage
        data object Transcribing : Stage
        data class Translating(val targetLang: String) : Stage
        data class Done(val srt: String, val text: String, val sourceLang: String?) : Stage
        data class Error(val message: String) : Stage
    }

    private var lastStage: Stage = Stage.Idle

    fun stage(): Stage = lastStage

    /**
     * Generate subtitles from a video URI in one pass.
     * @param targetLanguage if non-blank, Gemini will be used to translate
     *                       the resulting SRT to that language.
     */
    suspend fun generate(videoUri: Uri, sourceLanguage: String? = null, targetLanguage: String? = null): Stage =
        withContext(Dispatchers.IO) {
            try {
                val groqKey = keys.get(ApiKey.GROQ)
                if (groqKey.isBlank()) {
                    return@withContext Stage.Error("Groq API key missing. Set it in Settings.").also { lastStage = it }
                }

                lastStage = Stage.ExtractingAudio
                val audioFile = VideoAudioExtractor.extractAudio(context, videoUri)

                lastStage = Stage.Transcribing
                val groq = GroqWhisperClient(groqKey)
                val tx = groq.transcribe(audioFile, language = sourceLanguage)
                var srt = tx.toSrt()
                var text = tx.text

                if (!targetLanguage.isNullOrBlank()) {
                    val geminiKey = keys.get(ApiKey.GEMINI)
                    if (geminiKey.isNotBlank()) {
                        lastStage = Stage.Translating(targetLanguage)
                        val gemini = GeminiClient(geminiKey)
                        srt = gemini.translateSrt(srt, targetLanguage)
                        text = gemini.generate("Translate to $targetLanguage:\n\n$text")
                    }
                }

                Stage.Done(srt = srt, text = text, sourceLang = tx.language).also { lastStage = it }
            } catch (t: Throwable) {
                Stage.Error(t.message ?: "Unknown error").also { lastStage = it }
            }
        }
}
