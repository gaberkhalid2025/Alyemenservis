package com.example

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileHeaderTest {

    @Test
    fun testProfileCoverImageValidation() {
        val nullCover: String? = null
        val blankCover = "  "
        val validCover = "data:image/png;base64,iVBORw0KGgoAAA..."

        assertTrue(nullCover.isNullOrBlank())
        assertTrue(blankCover.isBlank())
        assertFalse(validCover.isBlank())
    }
}
