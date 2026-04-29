package com.example.videoplayer.util

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object VideoMetadataExtractor {

    data class Metadata(
        val duration: Long,
        val thumbnailPath: String
    )

    fun extract(context: Context, uri: Uri, videoId: Long): Metadata {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val duration = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DURATION
            )?.toLong() ?: 0L

            val thumbnailDir = File(context.filesDir, "thumbnails")
            thumbnailDir.mkdirs()
            val thumbnailFile = File(thumbnailDir, "$videoId.jpg")

            val frame = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            if (frame != null && !thumbnailFile.exists()) {
                FileOutputStream(thumbnailFile).use { fos ->
                    frame.compress(Bitmap.CompressFormat.JPEG, 85, fos)
                }
            }

            return Metadata(
                duration = duration,
                thumbnailPath = thumbnailFile.absolutePath
            )
        } finally {
            retriever.release()
        }
    }
}
