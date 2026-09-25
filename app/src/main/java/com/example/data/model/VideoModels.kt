package com.example.data.model

enum class DownloadStatus {
    QUEUED,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class QualityOption(
    val quality: VideoQuality,
    val fileSizeBytes: Long,
    val directDownloadUrl: String,
    val format: String = "MP4"
)

data class VideoMetadata(
    val id: String,
    val originalUrl: String,
    val title: String,
    val author: String,
    val thumbnailUrl: String,
    val durationText: String,
    val platform: Platform,
    val availableQualities: List<QualityOption>,
    val isAuthorized: Boolean = true,
    val restrictionReason: String? = null
)

data class ActiveDownload(
    val id: String,
    val originalUrl: String,
    val title: String,
    val platform: Platform,
    val quality: VideoQuality,
    val directUrl: String,
    val thumbnailUrl: String,
    var downloadedBytes: Long = 0L,
    var totalBytes: Long = 0L,
    var speedBytesPerSec: Long = 0L,
    var etaSeconds: Long = 0L,
    var status: DownloadStatus = DownloadStatus.QUEUED,
    var localUri: String? = null,
    var localPath: String? = null,
    var errorMessage: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    val progress: Float
        get() = if (totalBytes > 0) (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f

    val progressPercent: Int
        get() = (progress * 100).toInt()
}
