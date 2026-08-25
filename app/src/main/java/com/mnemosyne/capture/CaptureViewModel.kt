package com.mnemosyne.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mnemosyne.data.Memory
import com.mnemosyne.data.MemoryRepository
import com.mnemosyne.embedding.EmbeddingRepository
import com.mnemosyne.location.LocationRepository
import com.mnemosyne.stt.AudioRecorder
import com.mnemosyne.stt.SttRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface CaptureState {
    data object Idle : CaptureState
    data object Recording : CaptureState
    data object Processing : CaptureState
    data class Done(val memoryId: Long) : CaptureState
    data class Error(val message: String) : CaptureState
}

@HiltViewModel
class CaptureViewModel @Inject constructor(
    private val audioRecorder: AudioRecorder,
    private val sttRepository: SttRepository,
    private val embeddingRepository: EmbeddingRepository,
    private val locationRepository: LocationRepository,
    private val memoryRepository: MemoryRepository
) : ViewModel() {

    private val _captureState = MutableStateFlow<CaptureState>(CaptureState.Idle)
    val captureState: StateFlow<CaptureState> = _captureState.asStateFlow()

    private var captureStartTimeMs: Long = 0

    fun onRecordPressed() {
        if (_captureState.value is CaptureState.Recording) return
        captureStartTimeMs = System.currentTimeMillis()
        _captureState.value = CaptureState.Recording
        audioRecorder.start()
    }

    fun onRecordReleased() {
        if (_captureState.value !is CaptureState.Recording) return
        _captureState.value = CaptureState.Processing

        viewModelScope.launch {
            try {
                val audioSamples = audioRecorder.stop()
                val transcript = sttRepository.transcribe(audioSamples)

                if (transcript.isBlank()) {
                    _captureState.value = CaptureState.Error("Nothing recorded — try again.")
                    return@launch
                }

                // Immediately grab cached/last known GPS fix (instant, no geocoding wait)
                val locationDeferred = async { locationRepository.getCurrentLocation() }
                val embeddingDeferred = async { embeddingRepository.embed(transcript) }

                val location = locationDeferred.await()
                val initialEmbedding = embeddingDeferred.await()

                val memory = Memory(
                    transcript = transcript,
                    timestampUtcMs = captureStartTimeMs,
                    latitudeDeg = location?.latitude ?: 0.0,
                    longitudeDeg = location?.longitude ?: 0.0,
                    embeddingVector = initialEmbedding
                )

                val savedId = memoryRepository.save(memory)
                _captureState.value = CaptureState.Done(savedId)

                // Asynchronous background post-sweep: reverse geocode and re-embed in the background
                if (location != null && (location.latitude != 0.0 || location.longitude != 0.0)) {
                    viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        try {
                            val locationName = locationRepository.getAddressDescription(location.latitude, location.longitude)
                            if (locationName.isNotBlank()) {
                                val enrichedEmbedding = embeddingRepository.embed("$transcript ($locationName)")
                                val updatedMemory = memory.copy(
                                    id = savedId,
                                    locationName = locationName,
                                    embeddingVector = enrichedEmbedding
                                )
                                memoryRepository.save(updatedMemory)
                            }
                        } catch (_: Exception) {
                            // Non-critical background enrichment
                        }
                    }
                }
            } catch (e: Exception) {
                _captureState.value = CaptureState.Error("Capture failed: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    fun resetState() {
        _captureState.value = CaptureState.Idle
    }
}
