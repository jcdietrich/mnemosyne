package com.mnemosyne.models

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ModelDownloadViewModel @Inject constructor(
    application: Application,
    private val modelManager: ModelManager
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow<DownloadState>(
        if (modelManager.areModelsReady()) DownloadState.Ready else DownloadState.Idle
    )
    val state: StateFlow<DownloadState> = _state.asStateFlow()

    init {
        if (!modelManager.areModelsReady()) {
            downloadModels()
        }
    }

    fun downloadModels() {
        viewModelScope.launch {
            _state.value = DownloadState.Downloading(0, 0, 0)
            val result = modelManager.ensureModels { progressPercent, downloadedBytes, totalBytes ->
                _state.value = DownloadState.Downloading(progressPercent, downloadedBytes, totalBytes)
            }

            _state.value = when (result) {
                ModelStatus.Ready -> DownloadState.Ready
                else -> DownloadState.Error("Failed to initialize on-device models. Please check your internet connection and retry.")
            }
        }
    }
}
