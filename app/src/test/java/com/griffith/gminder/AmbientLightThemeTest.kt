package com.griffith.gminder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AmbientLightThemeTest {
    @Test
    fun dimSurroundingsSelectDarkTheme() {
        assertTrue(AmbientLightTheme.isDark(0f, false))
        assertTrue(AmbientLightTheme.isDark(29.9f, false))
    }

    @Test
    fun brightSurroundingsSelectLightTheme() {
        assertFalse(AmbientLightTheme.isDark(80.1f, true))
        assertFalse(AmbientLightTheme.isDark(10_000f, true))
    }

    @Test
    fun transitionBandRetainsEitherPreviousThemeIncludingBoundaries() {
        for (previous in listOf(false, true)) {
            for (lux in listOf(30f, 50f, 80f)) {
                assertEquals(previous, AmbientLightTheme.isDark(lux, previous))
            }
        }
    }

    @Test
    fun invalidReadingsRetainPreviousTheme() {
        for (previous in listOf(false, true)) {
            for (lux in listOf(-1f, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)) {
                assertEquals(previous, AmbientLightTheme.isDark(lux, previous))
            }
        }
    }

    @Test
    fun fluctuatingReadingsDoNotToggleThemeUntilOppositeThresholdIsCrossed() {
        var dark = false
        val readings = listOf(29f, 31f, 79f, 80f, 81f, 79f, 31f, 30f, 29f)
        val expected = listOf(true, true, true, true, false, false, false, false, true)
        readings.zip(expected).forEach { (lux, expectedDark) ->
            dark = AmbientLightTheme.isDark(lux, dark)
            assertEquals("Reading: $lux lx", expectedDark, dark)
        }
    }
}
