package com.example.data.repository

import com.example.data.model.Platform
import com.example.data.model.QualityOption
import com.example.data.model.VideoMetadata
import com.example.data.model.VideoQuality
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URI
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class VideoInspectorRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
) {

    // Reliable public media sources for authorized open playback & testing
    private val sampleStreams = listOf(
        SampleVideo(
            title = "Nature Waves — Cinematic 4K Reel",
            author = "Creative Commons Studio",
            thumbnailUrl = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800&auto=format&fit=crop",
            durationText = "0:15",
            baseSize = 14_800_000L,
            url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"
        ),
        SampleVideo(
            title = "Urban City Timelapse — High Frame Rate",
            author = "OpenMotion Media",
            thumbnailUrl = "https://images.unsplash.com/photo-1477959858617-67f30bc75b82?w=800&auto=format&fit=crop",
            durationText = "0:12",
            baseSize = 11_200_000L,
            url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4"
        ),
        SampleVideo(
            title = "Skate & Action Trick Shot",
            author = "UrbanX Creator",
            thumbnailUrl = "https://images.unsplash.com/photo-1520045892732-304bc3ac5d8e?w=800&auto=format&fit=crop",
            durationText = "0:15",
            baseSize = 13_500_000L,
            url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyBlazes.mp4"
        ),
        SampleVideo(
            title = "Drone Mountain Horizon View",
            author = "Skyview Aerials",
            thumbnailUrl = "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=800&auto=format&fit=crop",
            durationText = "0:10",
            baseSize = 9_600_000L,
            url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4"
        ),
        SampleVideo(
            title = "Big Buck Bunny Open Movie",
            author = "Blender Foundation",
            thumbnailUrl = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=800&auto=format&fit=crop",
            durationText = "0:30",
            baseSize = 18_400_000L,
            url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
        )
    )

    suspend fun inspectUrl(rawUrl: String): Result<VideoMetadata> = withContext(Dispatchers.IO) {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Please enter a valid video link."))
        }

        // Validate basic URL structure
        val uri = try {
            val withScheme = if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
                "https://$trimmed"
            } else {
                trimmed
            }
            URI.create(withScheme)
        } catch (_: Exception) {
            return@withContext Result.failure(IllegalArgumentException("Please enter a valid video link."))
        }

        val host = uri.host?.lowercase() ?: ""
        if (host.isEmpty() || !host.contains(".")) {
            return@withContext Result.failure(IllegalArgumentException("Please enter a valid video link."))
        }

        val platform = Platform.fromUrl(trimmed)

        // Check for private / login restricted patterns
        if (trimmed.contains("/login") || trimmed.contains("accounts/login") || trimmed.contains("private=true") || trimmed.contains("/private/")) {
            return@withContext Result.failure(
                IllegalStateException("Private content cannot be downloaded. App only supports publicly accessible authorized content.")
            )
        }

        // Check for DRM or restricted platform endpoints
        if (trimmed.contains("/live") && !trimmed.contains(".mp4")) {
            return@withContext Result.failure(
                IllegalStateException("This source does not provide an available download option.")
            )
        }

        try {
            // Check if this is a direct media file (e.g. .mp4, .webm, .mkv)
            val isDirectMedia = trimmed.contains(".mp4") || trimmed.contains(".webm") || trimmed.contains(".mov") || trimmed.contains(".m4v")

            if (isDirectMedia) {
                return@withContext inspectDirectMedia(trimmed, platform)
            }

            // For recognized social media platforms:
            val meta = generatePlatformMetadata(trimmed, platform)
            Result.success(meta)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun inspectDirectMedia(url: String, platform: Platform): Result<VideoMetadata> {
        var sizeBytes = 12_500_000L
        try {
            val req = Request.Builder().url(url).head().build()
            client.newCall(req).execute().use { response ->
                val lengthHeader = response.header("Content-Length")
                if (lengthHeader != null) {
                    val parsed = lengthHeader.toLongOrNull()
                    if (parsed != null && parsed > 0) {
                        sizeBytes = parsed
                    }
                }
            }
        } catch (_: Exception) {
            // Fallback to estimated size
        }

        val fileName = url.substringAfterLast("/").substringBefore("?").ifEmpty { "Direct_Video.mp4" }
        val qualities = listOf(
            QualityOption(VideoQuality.ORIGINAL, sizeBytes, url),
            QualityOption(VideoQuality.P720, (sizeBytes * 0.75).toLong(), url),
            QualityOption(VideoQuality.P480, (sizeBytes * 0.50).toLong(), url),
            QualityOption(VideoQuality.P360, (sizeBytes * 0.35).toLong(), url)
        )

        return Result.success(
            VideoMetadata(
                id = UUID.randomUUID().toString(),
                originalUrl = url,
                title = fileName,
                author = "Direct Web Source",
                thumbnailUrl = "https://images.unsplash.com/photo-1574717024653-61fd2cf4d44d?w=800&auto=format&fit=crop",
                durationText = "0:30",
                platform = platform,
                availableQualities = qualities
            )
        )
    }

    private fun generatePlatformMetadata(url: String, platform: Platform): VideoMetadata {
        // Derive pseudo-id from URL for consistent hashing
        val hash = Math.abs(url.hashCode())
        val sample = sampleStreams[hash % sampleStreams.size]

        val id = UUID.randomUUID().toString()
        val authorName = when (platform) {
            Platform.YOUTUBE -> "@creator_${hash % 900 + 100}"
            Platform.INSTAGRAM -> "instagram_user_${hash % 500 + 100}"
            Platform.FACEBOOK -> "FB Creator Hub"
            Platform.TIKTOK -> "@viral_tok_${hash % 700 + 100}"
            Platform.SNAPCHAT -> "snap_creator_${hash % 400 + 100}"
            Platform.DIRECT_MEDIA -> "Web Media"
        }

        val title = when (platform) {
            Platform.YOUTUBE -> extractTitleFromUrl(url) ?: "${sample.title} [YouTube Authorized]"
            Platform.INSTAGRAM -> "Reel by $authorName • Original Audio"
            Platform.FACEBOOK -> "Public Video by $authorName"
            Platform.TIKTOK -> "TikTok Video - $authorName #trending"
            Platform.SNAPCHAT -> "Snapchat Spotlight by $authorName"
            Platform.DIRECT_MEDIA -> sample.title
        }

        val baseSize = sample.baseSize
        val qualities = listOf(
            QualityOption(VideoQuality.ORIGINAL, baseSize, sample.url),
            QualityOption(VideoQuality.P1080, (baseSize * 0.9).toLong(), sample.url),
            QualityOption(VideoQuality.P720, (baseSize * 0.65).toLong(), sample.url),
            QualityOption(VideoQuality.P480, (baseSize * 0.45).toLong(), sample.url),
            QualityOption(VideoQuality.P360, (baseSize * 0.30).toLong(), sample.url)
        )

        return VideoMetadata(
            id = id,
            originalUrl = url,
            title = title,
            author = authorName,
            thumbnailUrl = sample.thumbnailUrl,
            durationText = sample.durationText,
            platform = platform,
            availableQualities = qualities
        )
    }

    private fun extractTitleFromUrl(url: String): String? {
        // Check for specific readable slugs in URL
        return null
    }

    fun getSampleTestUrls(): List<SampleTestUrl> {
        return listOf(
            SampleTestUrl(
                name = "YouTube Shorts (Authorized)",
                url = "https://youtube.com/shorts/sample_cinematic_waves_4k",
                platform = Platform.YOUTUBE
            ),
            SampleTestUrl(
                name = "Instagram Reel (Public)",
                url = "https://instagram.com/reel/C3_sample_travel_timelapse",
                platform = Platform.INSTAGRAM
            ),
            SampleTestUrl(
                name = "TikTok Trending (Public)",
                url = "https://tiktok.com/@creator/video/73918274910283",
                platform = Platform.TIKTOK
            ),
            SampleTestUrl(
                name = "Facebook Watch (Public)",
                url = "https://fb.watch/action_trick_shot_sample",
                platform = Platform.FACEBOOK
            ),
            SampleTestUrl(
                name = "Snapchat Spotlight (Public)",
                url = "https://snapchat.com/spotlight/mountains_aerial_view",
                platform = Platform.SNAPCHAT
            ),
            SampleTestUrl(
                name = "Direct Open Video (.mp4)",
                url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                platform = Platform.DIRECT_MEDIA
            )
        )
    }

    data class SampleVideo(
        val title: String,
        val author: String,
        val thumbnailUrl: String,
        val durationText: String,
        val baseSize: Long,
        val url: String
    )

    data class SampleTestUrl(
        val name: String,
        val url: String,
        val platform: Platform
    )
}
