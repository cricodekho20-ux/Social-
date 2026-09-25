package com.example.data.model

import androidx.compose.ui.graphics.Color

enum class Platform(
    val displayName: String,
    val brandColor: Color,
    val iconName: String,
    val domains: List<String>
) {
    YOUTUBE(
        displayName = "YouTube",
        brandColor = Color(0xFFFF0000),
        iconName = "youtube",
        domains = listOf("youtube.com", "youtu.be", "m.youtube.com")
    ),
    INSTAGRAM(
        displayName = "Instagram",
        brandColor = Color(0xFFE1306C),
        iconName = "instagram",
        domains = listOf("instagram.com", "instagr.am")
    ),
    FACEBOOK(
        displayName = "Facebook",
        brandColor = Color(0xFF1877F2),
        iconName = "facebook",
        domains = listOf("facebook.com", "fb.watch", "m.facebook.com", "fb.com")
    ),
    TIKTOK(
        displayName = "TikTok",
        brandColor = Color(0xFF00F2FE),
        iconName = "tiktok",
        domains = listOf("tiktok.com", "vm.tiktok.com", "vt.tiktok.com")
    ),
    SNAPCHAT(
        displayName = "Snapchat",
        brandColor = Color(0xFFFFFC00),
        iconName = "snapchat",
        domains = listOf("snapchat.com", "story.snapchat.com")
    ),
    DIRECT_MEDIA(
        displayName = "Web / Direct",
        brandColor = Color(0xFF10B981),
        iconName = "link",
        domains = emptyList()
    );

    companion object {
        fun fromUrl(url: String): Platform {
            val lower = url.lowercase().trim()
            return when {
                lower.contains("youtube.com") || lower.contains("youtu.be") -> YOUTUBE
                lower.contains("instagram.com") || lower.contains("instagr.am") -> INSTAGRAM
                lower.contains("facebook.com") || lower.contains("fb.watch") || lower.contains("fb.com") -> FACEBOOK
                lower.contains("tiktok.com") -> TIKTOK
                lower.contains("snapchat.com") -> SNAPCHAT
                else -> DIRECT_MEDIA
            }
        }
    }
}
