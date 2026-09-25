package com.example.ui.viewmodel

import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Platform
import com.example.data.model.VideoMetadata
import com.example.data.model.VideoQuality
import com.example.data.repository.DownloadRepository
import com.example.data.repository.VideoInspectorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val inspectorRepository: VideoInspectorRepository,
    private val downloadRepository: DownloadRepository
) : ViewModel() {

    private val _inputUrl = MutableStateFlow("")
    val inputUrl: StateFlow<String> = _inputUrl.asStateFlow()

    private val _isInspecting = MutableStateFlow(false)
    val isInspecting: StateFlow<Boolean> = _isInspecting.asStateFlow()

    private val _detectedMetadata = MutableStateFlow<VideoMetadata?>(null)
    val detectedMetadata: StateFlow<VideoMetadata?> = _detectedMetadata.asStateFlow()

    private val _selectedQuality = MutableStateFlow(VideoQuality.ORIGINAL)
    val selectedQuality: StateFlow<VideoQuality> = _selectedQuality.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _detectedPlatform = MutableStateFlow<Platform?>(null)
    val detectedPlatform: StateFlow<Platform?> = _detectedPlatform.asStateFlow()

    private val _downloadTriggeredMessage = MutableStateFlow<String?>(null)
    val downloadTriggeredMessage: StateFlow<String?> = _downloadTriggeredMessage.asStateFlow()

    // Batch download state
    private val _isBatchMode = MutableStateFlow(false)
    val isBatchMode: StateFlow<Boolean> = _isBatchMode.asStateFlow()

    private val _batchUrlsText = MutableStateFlow("")
    val batchUrlsText: StateFlow<String> = _batchUrlsText.asStateFlow()

    private val _batchItems = MutableStateFlow<List<Pair<VideoMetadata, VideoQuality>>>(emptyList())
    val batchItems: StateFlow<List<Pair<VideoMetadata, VideoQuality>>> = _batchItems.asStateFlow()

    private val _isBatchProcessing = MutableStateFlow(false)
    val isBatchProcessing: StateFlow<Boolean> = _isBatchProcessing.asStateFlow()

    val sampleUrls = inspectorRepository.getSampleTestUrls()

    fun onUrlChanged(url: String) {
        _inputUrl.value = url
        _errorMessage.value = null
        if (url.trim().isNotEmpty()) {
            _detectedPlatform.value = Platform.fromUrl(url)
        } else {
            _detectedPlatform.value = null
        }
    }

    fun clearInput() {
        _inputUrl.value = ""
        _detectedMetadata.value = null
        _errorMessage.value = null
        _detectedPlatform.value = null
    }

    fun pasteFromClipboard(context: Context) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val text = clip.getItemAt(0).text?.toString() ?: ""
            if (text.isNotBlank()) {
                onUrlChanged(text)
                inspectUrl()
            }
        }
    }

    fun selectSample(sample: VideoInspectorRepository.SampleTestUrl) {
        _inputUrl.value = sample.url
        _detectedPlatform.value = sample.platform
        inspectUrl()
    }

    fun inspectUrl() {
        val url = _inputUrl.value.trim()
        if (url.isEmpty()) {
            _errorMessage.value = "Please enter a valid video link."
            return
        }

        viewModelScope.launch {
            _isInspecting.value = true
            _errorMessage.value = null
            _detectedMetadata.value = null

            val result = inspectorRepository.inspectUrl(url)
            result.onSuccess { metadata ->
                _detectedMetadata.value = metadata
                _selectedQuality.value = metadata.availableQualities.firstOrNull()?.quality ?: VideoQuality.ORIGINAL
                _detectedPlatform.value = metadata.platform
            }.onFailure { err ->
                _errorMessage.value = err.message ?: "Failed to inspect video link. Please check network."
            }
            _isInspecting.value = false
        }
    }

    fun onQualitySelected(quality: VideoQuality) {
        _selectedQuality.value = quality
    }

    fun startDownload(): Boolean {
        val metadata = _detectedMetadata.value ?: return false
        val quality = _selectedQuality.value

        downloadRepository.startDownload(metadata, quality)
        _downloadTriggeredMessage.value = "Download started! Saving to Movies/Social Video Saver"
        return true
    }

    fun clearDownloadMessage() {
        _downloadTriggeredMessage.value = null
    }

    fun toggleBatchMode() {
        _isBatchMode.update { !it }
    }

    fun onBatchUrlsTextChanged(text: String) {
        _batchUrlsText.value = text
    }

    fun parseAndInspectBatch() {
        val lines = _batchUrlsText.value
            .split("\n", ",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (lines.isEmpty()) {
            _errorMessage.value = "Please enter at least one valid link."
            return
        }

        viewModelScope.launch {
            _isBatchProcessing.value = true
            _errorMessage.value = null
            val collected = mutableListOf<Pair<VideoMetadata, VideoQuality>>()

            for (link in lines) {
                val res = inspectorRepository.inspectUrl(link)
                if (res.isSuccess) {
                    val meta = res.getOrThrow()
                    collected.add(Pair(meta, VideoQuality.ORIGINAL))
                }
            }

            _batchItems.value = collected
            _isBatchProcessing.value = false
            if (collected.isEmpty()) {
                _errorMessage.value = "Could not parse valid authorized videos from batch list."
            }
        }
    }

    fun downloadAllBatch() {
        val items = _batchItems.value
        if (items.isEmpty()) return

        downloadRepository.startBatchDownload(items)
        _downloadTriggeredMessage.value = "Started batch download for ${items.size} videos!"
        _batchItems.value = emptyList()
        _batchUrlsText.value = ""
    }

    fun removeBatchItem(index: Int) {
        _batchItems.update { current ->
            current.toMutableList().apply { removeAt(index) }
        }
    }
}
