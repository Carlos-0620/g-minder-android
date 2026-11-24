package com.griffith.gminder

import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderRangeTest {
    @Test
    fun keepsAllSupportedSliderValuesIncludingDefault() {
        for (metres in 100..1000 step 100) assertEquals(metres, ReminderRange.normalise(metres))
        assertEquals(200, ReminderRange.normalise(ReminderRange.DEFAULT_METRES))
    }

    @Test
    fun clampsOutOfRangeSavedValuesSafely() {
        for (metres in listOf(Int.MIN_VALUE, -1, 0, 99)) assertEquals(100, ReminderRange.normalise(metres))
        for (metres in listOf(1001, Int.MAX_VALUE)) assertEquals(1000, ReminderRange.normalise(metres))
    }

    @Test
    fun roundsNonStepValuesToNearestSupportedRadius() {
        assertEquals(100, ReminderRange.normalise(149))
        assertEquals(200, ReminderRange.normalise(150))
        assertEquals(500, ReminderRange.normalise(549))
        assertEquals(600, ReminderRange.normalise(550))
        assertEquals(1000, ReminderRange.normalise(999))
    }
}
