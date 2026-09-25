package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.Platform
import com.example.data.model.VideoQuality

@Entity(tableName = "download_history")
data class DownloadEntity(
    @PrimaryKey
    val id: String,
    val originalUrl: String,
    val title: String,
    val platformName: String,
    val qualityLabel: String,
    val fileSizeBytes: Long,
    val mediaStoreUri: String?,
    val filePath: String?,
    val thumbnailUrl: String?,
    val downloadTimestamp: Long = System.currentTimeMillis(),
    val durationText: String = ""
) {
    val platform: Platform
        get() = try {
            Platform.valueOf(platformName)
        } catch (_: Exception) {
            Platform.fromUrl(originalUrl)
        }

    val quality: VideoQuality
        get() = VideoQuality.fromString(qualityLabel)
}
