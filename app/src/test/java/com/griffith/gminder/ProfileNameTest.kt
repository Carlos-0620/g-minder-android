package com.griffith.gminder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileNameTest {
    @Test
    fun trimsNamesAndPreservesAccentsAndSingleLetterNames() {
        assertEquals("Carlos", ProfileName.validated("  Carlos  "))
        assertEquals("María José", ProfileName.validated("María José"))
        assertEquals("A", ProfileName.validated("A"))
    }

    @Test
    fun rejectsEmptyNamesSoOnboardingCannotBeSkippedWithWhitespace() {
        assertNull(ProfileName.validated(""))
        assertNull(ProfileName.validated("   "))
    }

    @Test
    fun acceptsMaximumLengthButRejectsOversizedNames() {
        assertEquals("A".repeat(40), ProfileName.validated("A".repeat(40)))
        assertNull(ProfileName.validated("A".repeat(41)))
    }

    @Test
    fun rejectsEmbeddedControlCharacters() {
        assertNull(ProfileName.validated("Carlos\nDavid"))
        assertNull(ProfileName.validated("Carlos\u0000David"))
    }
}
