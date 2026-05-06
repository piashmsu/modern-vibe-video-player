package com.piash.modernvibe.videoplayer.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class VideoItem(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val mimeType: String?,
    val dateAddedSec: Long,
    val absolutePath: String?,
    val folderName: String,
    val folderPath: String,
)

data class VideoFolder(
    val name: String,
    val path: String,
    val videoCount: Int,
    val totalSizeBytes: Long,
    val coverUri: Uri?,
    val mostRecentVideo: Long,
)

/**
 * Scans the device for videos using MediaStore and groups them into folders
 * (MX Player-style). Works on Android 8.0+ and uses the `READ_MEDIA_VIDEO`
 * permission on Android 13+ (`READ_EXTERNAL_STORAGE` on older versions).
 */
class VideoScanner(private val context: Context) {

    suspend fun scanAll(): List<VideoItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<VideoItem>()
        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
        )

        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        runCatching {
            context.contentResolver.query(collection, projection, null, null, sortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val durCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val wCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
                val hCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                val dataCol = cursor.getColumnIndex(MediaStore.Video.Media.DATA)
                val bucketCol = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val data = if (dataCol >= 0) cursor.getString(dataCol) else null
                    val folderPath = data?.let { File(it).parent } ?: "/"
                    val folderName = (if (bucketCol >= 0) cursor.getString(bucketCol) else null)
                        ?: File(folderPath).name.ifEmpty { "Internal" }
                    items.add(
                        VideoItem(
                            id = id,
                            uri = ContentUris.withAppendedId(collection, id),
                            displayName = cursor.getString(nameCol) ?: "video_$id",
                            durationMs = cursor.getLong(durCol),
                            sizeBytes = cursor.getLong(sizeCol),
                            width = cursor.getInt(wCol),
                            height = cursor.getInt(hCol),
                            mimeType = cursor.getString(mimeCol),
                            dateAddedSec = cursor.getLong(dateCol),
                            absolutePath = data,
                            folderName = folderName,
                            folderPath = folderPath,
                        )
                    )
                }
            }
        }
        items
    }

    suspend fun scanFolders(): List<VideoFolder> = withContext(Dispatchers.IO) {
        val all = scanAll()
        all.groupBy { it.folderPath }
            .map { (path, vids) ->
                val first = vids.first()
                VideoFolder(
                    name = first.folderName,
                    path = path,
                    videoCount = vids.size,
                    totalSizeBytes = vids.sumOf { it.sizeBytes },
                    coverUri = vids.firstOrNull()?.uri,
                    mostRecentVideo = vids.maxOfOrNull { it.dateAddedSec } ?: 0L,
                )
            }
            .sortedByDescending { it.mostRecentVideo }
    }

    suspend fun videosInFolder(folderPath: String): List<VideoItem> = withContext(Dispatchers.IO) {
        scanAll().filter { it.folderPath == folderPath }
    }
}

object VideoFormat {
    fun durationLabel(ms: Long): String {
        if (ms <= 0) return ""
        val totalSec = ms / 1000
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }

    fun sizeLabel(bytes: Long): String {
        if (bytes <= 0) return ""
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1 -> "%.2f GB".format(gb)
            mb >= 1 -> "%.1f MB".format(mb)
            else -> "%.0f KB".format(kb)
        }
    }
}
