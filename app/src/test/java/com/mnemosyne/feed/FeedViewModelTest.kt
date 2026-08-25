package com.mnemosyne.feed

import com.mnemosyne.data.Memory
import com.mnemosyne.data.MemoryRepository
import com.mnemosyne.embedding.EmbeddingRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FeedViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val memoryRepository: MemoryRepository = mockk(relaxed = true)
    private val embeddingRepository: EmbeddingRepository = mockk(relaxed = true)
    private val locationRepository: com.mnemosyne.location.LocationRepository = mockk(relaxed = true)
    private val audioRecorder: AudioRecorder = mockk(relaxed = true)
    private val sttRepository: SttRepository = mockk(relaxed = true)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `when search query is empty, displayed memories match repository flow`() = runTest {
        val memories = listOf(
            Memory(id = 1, transcript = "First memory", timestampUtcMs = 2000),
            Memory(id = 2, transcript = "Second memory", timestampUtcMs = 1000)
        )
        coEvery { memoryRepository.memoriesFlow } returns flowOf(memories)
        coEvery { memoryRepository.getPaged(0, any()) } returns memories

        val viewModel = FeedViewModel(memoryRepository, embeddingRepository, locationRepository, audioRecorder, sttRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(memories, viewModel.displayedMemories.value)
    }

    @Test
    fun `setting search query invokes embedding and vector search`() = runTest {
        val dummyVector = FloatArray(100) { 0.1f }
        val searchResult = listOf(
            Memory(id = 2, transcript = "Search match", timestampUtcMs = 1000)
        )

        coEvery { memoryRepository.memoriesFlow } returns flowOf(emptyList())
        coEvery { embeddingRepository.embed("doctor") } returns dummyVector
        coEvery { memoryRepository.searchByVector(dummyVector, any()) } returns searchResult
        coEvery { locationRepository.getCurrentLocation() } returns null

        val viewModel = FeedViewModel(memoryRepository, embeddingRepository, locationRepository, audioRecorder, sttRepository)
        viewModel.onSearchQueryChanged("doctor")

        testDispatcher.scheduler.advanceTimeBy(350)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { embeddingRepository.embed("doctor") }
        coVerify { memoryRepository.searchByVector(dummyVector, limit = 25) }
        assertEquals(searchResult, viewModel.displayedMemories.value)
    }

    @Test
    fun `clearing search resets displayed memories back to full chronological feed`() = runTest {
        val allMemories = listOf(Memory(id = 1, transcript = "General note"))
        coEvery { memoryRepository.memoriesFlow } returns flowOf(allMemories)
        coEvery { embeddingRepository.embed("query") } returns FloatArray(100)
        coEvery { memoryRepository.searchByVector(any(), any()) } returns emptyList()
        coEvery { locationRepository.getCurrentLocation() } returns null

        val viewModel = FeedViewModel(memoryRepository, embeddingRepository, locationRepository, audioRecorder, sttRepository)
        viewModel.onSearchQueryChanged("query")
        testDispatcher.scheduler.advanceTimeBy(350)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.clearSearch()
        testDispatcher.scheduler.advanceTimeBy(350)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("", viewModel.searchQuery.value)
        assertEquals(allMemories, viewModel.displayedMemories.value)
    }
}
