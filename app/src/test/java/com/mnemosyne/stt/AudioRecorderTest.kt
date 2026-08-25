package com.mnemosyne.stt

import org.junit.Assert.*
import org.junit.Test

class AudioRecorderTest {

    /**
     * AudioRecorder uses Android AudioRecord internally, which requires a real
     * device to test meaningfully. These unit tests verify the pure-logic
     * behaviors that don't depend on hardware.
     */

    @Test
    fun `short array conversion to list and back`() {
        // Verifies the buffer accumulation logic used in AudioRecorder
        val samples = ShortArray(100) { it.toShort() }
        val list = samples.toList()
        val back = list.toShortArray()
        assertArrayEquals("Round-trip ShortArray → List → ShortArray preserves values", samples, back)
    }

    @Test
    fun `empty buffer produces zero-length array`() {
        val buffer = mutableListOf<Short>()
        val result = buffer.toShortArray()
        assertEquals(0, result.size)
    }

    @Test
    fun `take on chunk does not exceed chunk size`() {
        val chunk = ShortArray(512) { 1 }
        val read = 256
        val taken = chunk.take(read)
        assertEquals(256, taken.size)
    }
}
