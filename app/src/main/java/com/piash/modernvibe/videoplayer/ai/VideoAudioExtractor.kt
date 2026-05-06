package com.piash.modernvibe.videoplayer.ai

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

/**
 * Extracts the audio track of a video into a standalone .m4a file using
 * [MediaExtractor] + [MediaMuxer]. The resulting file is small enough to
 * upload to Groq's `/audio/transcriptions` endpoint and is in a format
 * (AAC in MP4 container) supported by Groq Whisper.
 */
object VideoAudioExtractor {

    /** Returns the extracted .m4a [File] in the app's cache directory. */
    suspend fun extractAudio(context: Context, videoUri: Uri, outputName: String = "audio.m4a"): File =
        withContext(Dispatchers.IO) {
            val outFile = File(context.cacheDir, outputName)
            if (outFile.exists()) outFile.delete()

            val extractor = MediaExtractor()
            val pfd = context.contentResolver.openFileDescriptor(videoUri, "r")
                ?: throw IllegalStateException("Cannot open video URI: $videoUri")
            pfd.use { descriptor ->
                extractor.setDataSource(descriptor.fileDescriptor)

                var audioTrackIndex = -1
                var audioFormat: MediaFormat? = null
                for (i in 0 until extractor.trackCount) {
                    val format = extractor.getTrackFormat(i)
                    val mime = format.getString(MediaFormat.KEY_MIME).orEmpty()
                    if (mime.startsWith("audio/")) {
                        audioTrackIndex = i
                        audioFormat = format
                        break
                    }
                }

                if (audioTrackIndex < 0 || audioFormat == null) {
                    throw IllegalStateException("No audio track in video")
                }

                extractor.selectTrack(audioTrackIndex)
                val muxer = MediaMuxer(outFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                val outTrack = muxer.addTrack(audioFormat)
                muxer.start()

                val bufferSize = if (audioFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE))
                    audioFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE).coerceAtLeast(256 * 1024)
                else 1 * 1024 * 1024
                val buffer = ByteBuffer.allocate(bufferSize)
                val info = MediaCodec.BufferInfo()

                while (true) {
                    info.offset = 0
                    info.size = extractor.readSampleData(buffer, 0)
                    if (info.size < 0) break
                    info.presentationTimeUs = extractor.sampleTime
                    val sampleFlags = extractor.sampleFlags
                    info.flags = if ((sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0)
                        MediaCodec.BUFFER_FLAG_KEY_FRAME else 0
                    muxer.writeSampleData(outTrack, buffer, info)
                    extractor.advance()
                }

                muxer.stop()
                muxer.release()
            }
            extractor.release()
            outFile
        }
}
