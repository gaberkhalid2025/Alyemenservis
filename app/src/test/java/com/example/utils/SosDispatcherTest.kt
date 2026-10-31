package com.example.utils

import org.junit.Assert.assertTrue
import org.junit.Test

class SosDispatcherTest {
    @Test fun messageContainsLinkAndCoordinates() {
        val m = SosDispatcher.buildMessage(15.3694, 44.1910, 12f)
        assertTrue(m.contains("mlat=15.369400"))
        assertTrue(m.contains("mlon=44.191000"))
        assertTrue(m.contains("12"))
    }
}
