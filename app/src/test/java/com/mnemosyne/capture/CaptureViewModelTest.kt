package com.mnemosyne.capture

import com.mnemosyne.data.Memory
import com.mnemosyne.data.MemoryRepository
import com.mnemosyne.embedding.EmbeddingRepository
import com.mnemosyne.location.Coordinates
import com.mnemosyne.location.LocationRepository
import com.mnemosyne.stt.AudioRecorder
import com.mnemosyne.stt.SttRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CaptureViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val audioRecorder: AudioRecorder = mockk(relaxed = true)
    private val sttRepository: SttRepository = mockk(relaxed = true)
    private val embeddingRepository: EmbeddingRepository = mockk(relaxed = true)
    private val locationRepository: LocationRepository = mockk(relaxed = true)
    private val memoryRepository: MemoryRepository = mockk(relaxed = true)

    private lateinit var viewModel: CaptureViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = CaptureViewModel(
            audioRecorder,
            sttRepository,
            embeddingRepository,
            locationRepository,
            memoryRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `pressing record transitions state to Recording and starts audioRecorder`() {
        viewModel.onRecordPressed()
        assertEquals(CaptureState.Recording, viewModel.captureState.value)
        verify { audioRecorder.start() }
    }

    @Test
    fun `releasing record with empty STT output emits Error state without saving`() = runTest {
        coEvery { audioRecorder.stop() } returns ShortArray(100)
        coEvery { sttRepository.transcribe(any()) } returns ""

        viewModel.onRecordPressed()
        viewModel.onRecordReleased()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.captureState.value is CaptureState.Error)
        coVerify(exactly = 0) { memoryRepository.save(any()) }
    }

    @Test
    fun `releasing record with valid transcript saves memory and emits Done`() = runTest {
        val samples = ShortArray(100) { 100 }
        val dummyVector = FloatArray(512) { 0.5f }

        coEvery { audioRecorder.stop() } returns samples
        coEvery { sttRepository.transcribe(samples) } returns "Dentist appointment at 3pm"
        coEvery { embeddingRepository.embed(any()) } returns dummyVector
        coEvery { locationRepository.getCurrentLocation() } returns Coordinates(37.7749, -122.4194)
        coEvery { memoryRepository.save(any()) } returns 42L

        viewModel.onRecordPressed()
        viewModel.onRecordReleased()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(CaptureState.Done(42L), viewModel.captureState.value)
        coVerify {
            memoryRepository.save(
                match {
                    it.transcript == "Dentist appointment at 3pm" &&
                    it.latitudeDeg == 37.7749 &&
                    it.longitudeDeg == -122.4194
                }
            )
        }
    }
}
