package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Platform
import com.example.ui.theme.ColorFacebook
import com.example.ui.theme.ColorInstagram
import com.example.ui.theme.ColorSnapchat
import com.example.ui.theme.ColorTikTok
import com.example.ui.theme.ColorYouTube

@Composable
fun PlatformBadge(
    platform: Platform,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true
) {
    val (color, icon) = when (platform) {
        Platform.YOUTUBE -> Pair(ColorYouTube, Icons.Default.PlayArrow)
        Platform.INSTAGRAM -> Pair(ColorInstagram, Icons.Default.CameraAlt)
        Platform.FACEBOOK -> Pair(ColorFacebook, Icons.Default.ThumbUp)
        Platform.TIKTOK -> Pair(ColorTikTok, Icons.Default.MusicNote)
        Platform.SNAPCHAT -> Pair(ColorSnapchat, Icons.Default.Movie)
        Platform.DIRECT_MEDIA -> Pair(Color(0xFF10B981), Icons.Default.Link)
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = platform.displayName,
                tint = if (platform == Platform.SNAPCHAT) Color.Black else Color.White,
                modifier = Modifier.size(12.dp)
            )
        }
        if (showLabel) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = platform.displayName,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
