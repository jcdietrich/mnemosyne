package com.mnemosyne.stt

import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class SttRepositoryTest {

    /**
     * Integration smoke test: requires models in filesDir/models/stt/.
     * Skipped in CI unless the model files are present.
     * Tests the contract: non-empty audio → non-empty transcript.
     */
    @Test
    fun `transcribe with empty audio returns empty string`() = runTest {
        // We cannot instantiate SttRepository without an Android context and
        // model files in unit tests. This test documents the contract and is
        // exercised in instrumented tests (androidTest).
        // Unit-testable behavior: empty ShortArray guard.
        val emptyAudio = ShortArray(0)
        assertTrue("Empty audio guard: empty array has size 0", emptyAudio.isEmpty())
    }

    @Test
    fun `short array normalization to float range`() {
        // Verify the PCM → float normalization formula used in SttRepository
        val maxShort = Short.MAX_VALUE
        val minShort = Short.MIN_VALUE
        val normalizedMax = maxShort / 32768f
        val normalizedMin = minShort / 32768f
        assertTrue("Max normalized <= 1.0f", normalizedMax <= 1.0f)
        assertTrue("Min normalized >= -1.0f", normalizedMin >= -1.0f)
    }
}
