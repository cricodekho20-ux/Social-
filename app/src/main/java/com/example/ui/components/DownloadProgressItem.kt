package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.ActiveDownload
import com.example.data.model.DownloadStatus
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import com.example.util.FormatUtils

@Composable
fun DownloadProgressItem(
    download: ActiveDownload,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onPlay: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = download.progress,
        label = "downloadProgress"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurfaceElevated)
            .border(
                1.dp,
                when (download.status) {
                    DownloadStatus.COMPLETED -> SuccessGreen.copy(alpha = 0.5f)
                    DownloadStatus.FAILED -> ErrorRed.copy(alpha = 0.5f)
                    DownloadStatus.DOWNLOADING -> AccentCyan.copy(alpha = 0.4f)
                    else -> DarkSurfaceBorder
                },
                RoundedCornerShape(16.dp)
            )
            .padding(14.dp)
    ) {
        // Top row: Thumbnail + Details + Status chip
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail preview
            Box(
                modifier = Modifier
                    .size(width = 68.dp, height = 48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(download.thumbnailUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Thumbnail",
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = download.title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    PlatformBadge(
                        platform = download.platform,
                        showLabel = false,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${download.quality.label} • No Watermark",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            // Status chip
            StatusBadge(status = download.status)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Progress bar (if not completed or failed)
        if (download.status == DownloadStatus.DOWNLOADING ||
            download.status == DownloadStatus.PAUSED ||
            download.status == DownloadStatus.QUEUED
        ) {
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = if (download.status == DownloadStatus.PAUSED) WarningAmber else AccentCyan,
                trackColor = Color(0xFF1E293B)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Progress stats: Percentage • Downloaded/Total • Speed • ETA
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${download.progressPercent}%  •  ${FormatUtils.formatBytes(download.downloadedBytes)} / ${FormatUtils.formatBytes(download.totalBytes)}",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                if (download.status == DownloadStatus.DOWNLOADING) {
                    Text(
                        text = "${FormatUtils.formatSpeed(download.speedBytesPerSec)} • ${FormatUtils.formatEta(download.etaSeconds)}",
                        color = AccentCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                } else if (download.status == DownloadStatus.PAUSED) {
                    Text(
                        text = "Paused",
                        color = WarningAmber,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else if (download.status == DownloadStatus.COMPLETED) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Saved to Gallery (Movies/Social Video Saver)",
                        color = SuccessGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = FormatUtils.formatBytes(download.totalBytes),
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else if (download.status == DownloadStatus.FAILED) {
            Text(
                text = download.errorMessage ?: "Download failed. Please check network connection.",
                color = ErrorRed,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Action controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (download.status) {
                DownloadStatus.DOWNLOADING -> {
                    OutlinedButton(
                        onClick = onPause,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = WarningAmber),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Pause, contentDescription = "Pause", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "PAUSE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedIconButton(
                        onClick = onCancel,
                        modifier = Modifier.size(38.dp),
                        colors = IconButtonDefaults.outlinedIconButtonColors(contentColor = ErrorRed)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cancel", modifier = Modifier.size(16.dp))
                    }
                }
                DownloadStatus.PAUSED -> {
                    Button(
                        onClick = onResume,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentCyan, contentColor = Color.Black),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Resume", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "RESUME", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedIconButton(
                        onClick = onCancel,
                        modifier = Modifier.size(38.dp),
                        colors = IconButtonDefaults.outlinedIconButtonColors(contentColor = ErrorRed)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cancel", modifier = Modifier.size(16.dp))
                    }
                }
                DownloadStatus.FAILED -> {
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(containerColor = WarningAmber, contentColor = Color.Black),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Retry", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "RETRY", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedIconButton(
                        onClick = onCancel,
                        modifier = Modifier.size(38.dp),
                        colors = IconButtonDefaults.outlinedIconButtonColors(contentColor = TextSecondary)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                    }
                }
                DownloadStatus.COMPLETED -> {
                    FilledTonalButton(
                        onClick = onShare,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color.White.copy(alpha = 0.1f),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "SHARE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onPlay,
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = Color.Black),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Play", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "OPEN VIDEO", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                DownloadStatus.QUEUED -> {
                    Text(
                        text = "Waiting in queue...",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
                DownloadStatus.CANCELLED -> {}
            }
        }
    }
}

@Composable
fun StatusBadge(status: DownloadStatus, modifier: Modifier = Modifier) {
    val (bgColor, textColor, text) = when (status) {
        DownloadStatus.DOWNLOADING -> Triple(AccentCyan.copy(alpha = 0.2f), AccentCyan, "DOWNLOADING")
        DownloadStatus.PAUSED -> Triple(WarningAmber.copy(alpha = 0.2f), WarningAmber, "PAUSED")
        DownloadStatus.COMPLETED -> Triple(SuccessGreen.copy(alpha = 0.2f), SuccessGreen, "SAVED")
        DownloadStatus.FAILED -> Triple(ErrorRed.copy(alpha = 0.2f), ErrorRed, "FAILED")
        DownloadStatus.QUEUED -> Triple(Color.White.copy(alpha = 0.1f), Color.White, "QUEUED")
        DownloadStatus.CANCELLED -> Triple(Color.White.copy(alpha = 0.1f), TextSecondary, "CANCELLED")
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp
        )
    }
}
