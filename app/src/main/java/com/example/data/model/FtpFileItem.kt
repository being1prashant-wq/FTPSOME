package com.example.data.model

enum class MediaCategory {
    DIRECTORY,
    VIDEO,
    AUDIO,
    IMAGE,
    SUBTITLE,
    OTHER
}

data class FtpFileItem(
    val name: String,
    val fullPath: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val lastModifiedTime: Long,
    val category: MediaCategory
) {
    val extension: String
        get() = name.substringAfterLast('.', "").lowercase()

    val formattedSize: String
        get() {
            if (isDirectory) return "Folder"
            if (sizeBytes <= 0) return "0 B"
            val kb = sizeBytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format(java.util.Locale.US, "%.2f GB", gb)
                mb >= 1.0 -> String.format(java.util.Locale.US, "%.1f MB", mb)
                kb >= 1.0 -> String.format(java.util.Locale.US, "%.0f KB", kb)
                else -> "$sizeBytes B"
            }
        }

    companion object {
        private val VIDEO_EXTS = setOf(
            "mp4", "mkv", "avi", "mov", "ts", "mts", "m2ts", "webm", "flv", "vob", "3gp", "wmv", "mpg", "mpeg", "m4v"
        )
        private val AUDIO_EXTS = setOf(
            "mp3", "flac", "aac", "wav", "m4a", "ogg", "opus", "wma", "ac3", "eac3", "dts", "alac", "aiff"
        )
        private val IMAGE_EXTS = setOf(
            "jpg", "jpeg", "png", "webp", "gif", "bmp", "heic"
        )
        private val SUBTITLE_EXTS = setOf(
            "srt", "vtt", "ass", "ssa", "sub"
        )

        fun determineCategory(name: String, isDir: Boolean): MediaCategory {
            if (isDir) return MediaCategory.DIRECTORY
            val ext = name.substringAfterLast('.', "").lowercase()
            return when {
                VIDEO_EXTS.contains(ext) -> MediaCategory.VIDEO
                AUDIO_EXTS.contains(ext) -> MediaCategory.AUDIO
                IMAGE_EXTS.contains(ext) -> MediaCategory.IMAGE
                SUBTITLE_EXTS.contains(ext) -> MediaCategory.SUBTITLE
                else -> MediaCategory.OTHER
            }
        }
    }
}
