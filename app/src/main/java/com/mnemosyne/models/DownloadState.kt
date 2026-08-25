package com.mnemosyne.models

sealed interface DownloadState {
    data object Idle : DownloadState
    data class Downloading(val progressPercent: Int, val downloadedBytes: Long, val totalBytes: Long) : DownloadState
    data object Ready : DownloadState
    data class Error(val message: String) : DownloadState
}
