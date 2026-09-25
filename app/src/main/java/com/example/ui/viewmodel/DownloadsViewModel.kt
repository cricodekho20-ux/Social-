package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.data.model.ActiveDownload
import com.example.data.repository.DownloadRepository
import kotlinx.coroutines.flow.StateFlow

class DownloadsViewModel(
    private val downloadRepository: DownloadRepository
) : ViewModel() {

    val activeDownloads: StateFlow<List<ActiveDownload>> = downloadRepository.activeDownloads

    fun pause(id: String) {
        downloadRepository.pauseDownload(id)
    }

    fun resume(id: String) {
        downloadRepository.resumeDownload(id)
    }

    fun cancel(id: String) {
        downloadRepository.cancelDownload(id)
    }

    fun retry(id: String) {
        downloadRepository.retryDownload(id)
    }
}
