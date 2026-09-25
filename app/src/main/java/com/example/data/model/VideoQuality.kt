package com.example.data.model

enum class VideoQuality(
    val label: String,
    val resolution: String,
    val isHd: Boolean,
    val badge: String? = null
) {
    P360("360p", "640x360", false, null),
    P480("480p", "854x480", false, "SD"),
    P720("720p HD", "1280x720", true, "HD"),
    P1080("1080p Full HD", "1920x1080", true, "FHD"),
    ORIGINAL("Original Quality", "Source", true, "BEST");

    companion object {
        fun fromString(value: String): VideoQuality {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) || it.label.equals(value, ignoreCase = true) }
                ?: ORIGINAL
        }
    }
}
