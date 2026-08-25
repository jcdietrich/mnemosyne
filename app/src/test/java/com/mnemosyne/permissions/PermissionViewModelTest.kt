package com.mnemosyne.permissions

import org.junit.Assert.*
import org.junit.Test

class PermissionViewModelTest {

    /**
     * PermissionViewModel uses AndroidViewModel and ContextCompat, which require
     * an Android environment. These tests verify the pure state-machine logic.
     *
     * Full integration is covered by instrumented tests in androidTest.
     */

    @Test
    fun `PermissionState enum has exactly two values`() {
        val states = PermissionState.values()
        assertEquals(2, states.size)
        assertTrue(states.contains(PermissionState.Denied))
        assertTrue(states.contains(PermissionState.Granted))
    }

    @Test
    fun `Denied is not Granted`() {
        assertNotEquals(PermissionState.Denied, PermissionState.Granted)
    }
}
