package com.mnemosyne.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mnemosyne.data.Memory
import com.mnemosyne.data.MemoryRepository
import com.mnemosyne.embedding.EmbeddingRepository
import com.mnemosyne.location.LocationRepository
import com.mnemosyne.stt.AudioRecorder
import com.mnemosyne.stt.SttRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface VoiceSearchState {
    data object Idle : VoiceSearchState
    data object Recording : VoiceSearchState
    data object Processing : VoiceSearchState
    data class Error(val message: String) : VoiceSearchState
}

@OptIn(FlowPreview::class)
@HiltViewModel
class FeedViewModel @Inject constructor(
    private val memoryRepository: MemoryRepository,
    private val embeddingRepository: EmbeddingRepository,
    private val locationRepository: LocationRepository,
    private val audioRecorder: AudioRecorder,
    private val sttRepository: SttRepository
) : ViewModel() {

    val searchQuery = MutableStateFlow("")
    private val _searchResults = MutableStateFlow<List<Memory>?>(null)

    private var currentOffset = 0L
    private val pageSize = 30L
    private var isEndReached = false
    private val _pagedMemories = MutableStateFlow<List<Memory>>(emptyList())

    private val _voiceSearchState = MutableStateFlow<VoiceSearchState>(VoiceSearchState.Idle)
    val voiceSearchState: StateFlow<VoiceSearchState> = _voiceSearchState.asStateFlow()

    val displayedMemories: StateFlow<List<Memory>> = combine(
        _pagedMemories,
        _searchResults
    ) { pagedMemories, searchResults ->
        searchResults ?: pagedMemories
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch {
            val firstPage = memoryRepository.getPaged(0, pageSize)
            _pagedMemories.value = firstPage
            currentOffset = firstPage.size.toLong()

            memoryRepository.memoriesFlow.collect {
                // When a new note is added, refresh the initial slice
                currentOffset = 0L
                isEndReached = false
                val updatedPage = memoryRepository.getPaged(0, pageSize)
                _pagedMemories.value = updatedPage
                currentOffset = updatedPage.size.toLong()
            }
        }

        viewModelScope.launch {
            searchQuery
                .debounce(300)
                .collect { query ->
                    if (query.isBlank()) {
                        _searchResults.value = null
                    } else {
                        val queryEmbedding = embeddingRepository.embed(query)
                        val vectorResults = memoryRepository.searchByVector(queryEmbedding, limit = 25)

                        // Proximity boost: if user has a location fix, boost memories recorded nearby
                        val currentLocation = locationRepository.getCurrentLocation()
                        val sortedResults = if (currentLocation != null) {
                            vectorResults.sortedBy { memory ->
                                val distKm = locationRepository.calculateDistanceKm(
                                    currentLocation.latitude,
                                    currentLocation.longitude,
                                    memory.latitudeDeg,
                                    memory.longitudeDeg
                                )
                                if (distKm < 5.0) distKm else 100.0 + distKm
                            }
                        } else {
                            vectorResults
                        }

                        _searchResults.value = sortedResults
                    }
                }
        }
    }

    fun loadNextPage() {
        if (isEndReached || searchQuery.value.isNotBlank()) return

        viewModelScope.launch {
            val nextPage = memoryRepository.getPaged(currentOffset, pageSize)
            if (nextPage.isEmpty()) {
                isEndReached = true
            } else {
                currentOffset += nextPage.size
                _pagedMemories.value = _pagedMemories.value + nextPage
            }
        }
    }

    fun onSearchQueryChanged(newQuery: String) {
        searchQuery.value = newQuery
    }

    fun startVoiceSearch() {
        if (_voiceSearchState.value is VoiceSearchState.Recording) return
        _voiceSearchState.value = VoiceSearchState.Recording
        audioRecorder.start()
    }

    fun stopVoiceSearch() {
        if (_voiceSearchState.value !is VoiceSearchState.Recording) return
        _voiceSearchState.value = VoiceSearchState.Processing

        viewModelScope.launch {
            try {
                val samples = audioRecorder.stop()
                val transcript = sttRepository.transcribe(samples)
                if (transcript.isNotBlank()) {
                    searchQuery.value = transcript
                }
                _voiceSearchState.value = VoiceSearchState.Idle
            } catch (e: Exception) {
                _voiceSearchState.value = VoiceSearchState.Error("Voice search failed")
            }
        }
    }

    fun clearSearch() {
        searchQuery.value = ""
        _searchResults.value = null
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch {
            memoryRepository.delete(id)
        }
    }
}
