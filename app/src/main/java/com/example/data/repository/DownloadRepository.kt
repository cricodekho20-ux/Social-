package com.example.data.repository

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.example.data.local.DownloadDao
import com.example.data.local.DownloadEntity
import com.example.data.model.ActiveDownload
import com.example.data.model.DownloadStatus
import com.example.data.model.Platform
import com.example.data.model.VideoMetadata
import com.example.data.model.VideoQuality
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class DownloadRepository(
    private val context: Context,
    private val downloadDao: DownloadDao,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _activeDownloads = MutableStateFlow<List<ActiveDownload>>(emptyList())
    val activeDownloads: StateFlow<List<ActiveDownload>> = _activeDownloads.asStateFlow()

    // Store active jobs by download ID
    private val activeJobs = ConcurrentHashMap<String, Job>()
    // Store temp target file paths for pause/resume support
    private val tempFiles = ConcurrentHashMap<String, File>()

    fun startDownload(
        metadata: VideoMetadata,
        quality: VideoQuality,
        customDownloadUrl: String? = null
    ): String {
        val downloadId = UUID.randomUUID().toString()
        val option = metadata.availableQualities.firstOrNull { it.quality == quality }
            ?: metadata.availableQualities.first()
        val directUrl = customDownloadUrl ?: option.directDownloadUrl
        val totalSize = option.fileSizeBytes

        val download = ActiveDownload(
            id = downloadId,
            originalUrl = metadata.originalUrl,
            title = metadata.title,
            platform = metadata.platform,
            quality = quality,
            directUrl = directUrl,
            thumbnailUrl = metadata.thumbnailUrl,
            downloadedBytes = 0L,
            totalBytes = totalSize,
            speedBytesPerSec = 0L,
            etaSeconds = 0L,
            status = DownloadStatus.QUEUED
        )

        _activeDownloads.update { list -> listOf(download) + list }

        val job = scope.launch {
            executeDownload(downloadId)
        }
        activeJobs[downloadId] = job

        return downloadId
    }

    fun startBatchDownload(
        items: List<Pair<VideoMetadata, VideoQuality>>
    ): List<String> {
        return items.map { (meta, quality) ->
            startDownload(meta, quality)
        }
    }

    fun pauseDownload(id: String) {
        val job = activeJobs[id]
        job?.cancel()
        activeJobs.remove(id)

        _activeDownloads.update { list ->
            list.map {
                if (it.id == id && it.status == DownloadStatus.DOWNLOADING) {
                    it.copy(status = DownloadStatus.PAUSED, speedBytesPerSec = 0L)
                } else it
            }
        }
    }

    fun resumeDownload(id: String) {
        val download = _activeDownloads.value.firstOrNull { it.id == id } ?: return
        if (download.status != DownloadStatus.PAUSED && download.status != DownloadStatus.FAILED) return

        _activeDownloads.update { list ->
            list.map {
                if (it.id == id) {
                    it.copy(status = DownloadStatus.QUEUED, errorMessage = null)
                } else it
            }
        }

        val job = scope.launch {
            executeDownload(id)
        }
        activeJobs[id] = job
    }

    fun cancelDownload(id: String) {
        val job = activeJobs[id]
        job?.cancel()
        activeJobs.remove(id)

        // Delete any temp file
        tempFiles.remove(id)?.delete()

        _activeDownloads.update { list ->
            list.filterNot { it.id == id }
        }
    }

    fun retryDownload(id: String) {
        val download = _activeDownloads.value.firstOrNull { it.id == id } ?: return
        _activeDownloads.update { list ->
            list.map {
                if (it.id == id) {
                    it.copy(
                        status = DownloadStatus.QUEUED,
                        errorMessage = null,
                        downloadedBytes = 0L
                    )
                } else it
            }
        }
        tempFiles.remove(id)?.delete()

        val job = scope.launch {
            executeDownload(id)
        }
        activeJobs[id] = job
    }

    private suspend fun executeDownload(id: String) = withContext(Dispatchers.IO) {
        val download = _activeDownloads.value.firstOrNull { it.id == id } ?: return@withContext

        updateDownloadState(id) {
            it.copy(status = DownloadStatus.DOWNLOADING)
        }

        // Prepare temp cache file
        val tempDir = File(context.cacheDir, "downloads").apply { mkdirs() }
        val tempFile = tempFiles.getOrPut(id) {
            File(tempDir, "dl_${id.take(8)}.tmp")
        }

        val existingBytes = if (tempFile.exists()) tempFile.length() else 0L

        try {
            val reqBuilder = Request.Builder().url(download.directUrl)
            if (existingBytes > 0) {
                reqBuilder.addHeader("Range", "bytes=$existingBytes-")
            }
            val request = reqBuilder.build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful && response.code != 206) {
                    throw IllegalStateException("Server returned HTTP ${response.code}: ${response.message}")
                }

                val body = response.body ?: throw IllegalStateException("Empty response body from video source")
                val responseContentLength = body.contentLength()
                val totalBytes = if (responseContentLength > 0) {
                    if (response.code == 206) existingBytes + responseContentLength else responseContentLength
                } else {
                    if (download.totalBytes > 0) download.totalBytes else 15_000_000L
                }

                updateDownloadState(id) {
                    it.copy(totalBytes = totalBytes)
                }

                val append = response.code == 206 && existingBytes > 0
                val outputStream = FileOutputStream(tempFile, append)
                val inputStream: InputStream = body.byteStream()

                val buffer = ByteArray(32 * 1024)
                var bytesRead: Int
                var currentDownloaded = if (append) existingBytes else 0L
                var lastTime = System.currentTimeMillis()
                var bytesSinceLastTime = 0L

                outputStream.use { out ->
                    inputStream.use { inStream ->
                        while (inStream.read(buffer).also { bytesRead = it } != -1) {
                            out.write(buffer, 0, bytesRead)
                            currentDownloaded += bytesRead
                            bytesSinceLastTime += bytesRead

                            val now = System.currentTimeMillis()
                            val elapsed = now - lastTime
                            if (elapsed >= 500) {
                                val speed = (bytesSinceLastTime * 1000L) / elapsed
                                val remainingBytes = (totalBytes - currentDownloaded).coerceAtLeast(0L)
                                val eta = if (speed > 0) remainingBytes / speed else 0L

                                updateDownloadState(id) {
                                    it.copy(
                                        downloadedBytes = currentDownloaded,
                                        totalBytes = totalBytes,
                                        speedBytesPerSec = speed,
                                        etaSeconds = eta
                                    )
                                }
                                lastTime = now
                                bytesSinceLastTime = 0L
                            }
                        }
                    }
                }

                // Download completed to temp file, now move to MediaStore (Movies/Social Video Saver)
                val (savedUri, savedPath) = saveVideoToMediaStore(tempFile, download)

                // Clean up temp file
                tempFile.delete()
                tempFiles.remove(id)

                // Insert into Room history database
                val entity = DownloadEntity(
                    id = id,
                    originalUrl = download.originalUrl,
                    title = download.title,
                    platformName = download.platform.name,
                    qualityLabel = download.quality.label,
                    fileSizeBytes = currentDownloaded,
                    mediaStoreUri = savedUri?.toString(),
                    filePath = savedPath,
                    thumbnailUrl = download.thumbnailUrl,
                    downloadTimestamp = System.currentTimeMillis(),
                    durationText = ""
                )
                downloadDao.insertDownload(entity)

                updateDownloadState(id) {
                    it.copy(
                        status = DownloadStatus.COMPLETED,
                        downloadedBytes = currentDownloaded,
                        totalBytes = currentDownloaded,
                        speedBytesPerSec = 0L,
                        etaSeconds = 0L,
                        localUri = savedUri?.toString(),
                        localPath = savedPath
                    )
                }

                activeJobs.remove(id)
            }
        } catch (e: CancellationException) {
            // User paused or cancelled
            Log.d("DownloadRepo", "Download $id was cancelled/paused")
        } catch (e: Exception) {
            Log.e("DownloadRepo", "Download $id error", e)
            updateDownloadState(id) {
                it.copy(
                    status = DownloadStatus.FAILED,
                    errorMessage = e.localizedMessage ?: "Download failed. Please check your internet connection.",
                    speedBytesPerSec = 0L
                )
            }
            activeJobs.remove(id)
        }
    }

    private fun updateDownloadState(id: String, transform: (ActiveDownload) -> ActiveDownload) {
        _activeDownloads.update { list ->
            list.map { if (it.id == id) transform(it) else it }
        }
    }

    private fun saveVideoToMediaStore(tempFile: File, download: ActiveDownload): Pair<Uri?, String?> {
        val cleanTitle = download.title
            .replace(Regex("[^a-zA-Z0-9._ -]"), "_")
            .take(50)
            .trim()
            .ifEmpty { "Video_${System.currentTimeMillis()}" }

        val fileName = "${cleanTitle}_${download.quality.name}.mp4"
        val subDir = "Movies/Social Video Saver"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Video.Media.TITLE, download.title)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, subDir)
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }

            val contentResolver = context.contentResolver
            val uri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                try {
                    contentResolver.openOutputStream(uri)?.use { outStream ->
                        tempFile.inputStream().use { inStream ->
                            inStream.copyTo(outStream)
                        }
                    }

                    contentValues.clear()
                    contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
                    contentResolver.update(uri, contentValues, null, null)

                    return Pair(uri, null)
                } catch (e: Exception) {
                    Log.e("DownloadRepo", "Error writing to MediaStore", e)
                }
            }
        }

        // Fallback for legacy devices or if MediaStore insert returns null
        try {
            val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
            val appDir = File(moviesDir, "Social Video Saver").apply { mkdirs() }
            val destFile = File(appDir, fileName)
            tempFile.copyTo(destFile, overwrite = true)

            // Trigger media scan so it shows up in Gallery
            MediaScannerConnection.scanFile(
                context,
                arrayOf(destFile.absolutePath),
                arrayOf("video/mp4"),
                null
            )
            return Pair(Uri.fromFile(destFile), destFile.absolutePath)
        } catch (e: Exception) {
            Log.e("DownloadRepo", "Fallback save failed", e)
            return Pair(null, null)
        }
    }

    suspend fun deleteFromHistory(entity: DownloadEntity, deleteFile: Boolean = true) {
        withContext(Dispatchers.IO) {
            downloadDao.deleteDownload(entity)
            if (deleteFile) {
                try {
                    if (entity.mediaStoreUri != null) {
                        context.contentResolver.delete(Uri.parse(entity.mediaStoreUri), null, null)
                    }
                    if (entity.filePath != null) {
                        val file = File(entity.filePath)
                        if (file.exists()) file.delete()
                    }
                } catch (e: Exception) {
                    Log.w("DownloadRepo", "Could not delete physical file: ${e.message}")
                }
            }
        }
    }
}
