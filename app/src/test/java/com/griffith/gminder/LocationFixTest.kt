package com.griffith.gminder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class LocationFixTest {
    @Test
    fun mapQueryRoundTripsWithoutLosingTheChosenPoint() {
        val point = LocationFix.fromMapQuery(" 53.12345, -6.54321 ")!!
        assertEquals("53.12345, -6.54321", point.mapQuery)
        assertEquals(point.latitude, LocationFix.fromMapQuery(point.mapQuery)!!.latitude, 0.000001)
        assertEquals(point.longitude, LocationFix.fromMapQuery(point.mapQuery)!!.longitude, 0.000001)
    }

    @Test
    fun mapQueryRejectsSampleNamesExtraFieldsAndInvalidCoordinates() {
        for (query in listOf("Griffith College Dublin", "53,-6,10", "NaN,0", "91,0", "0,-181", "", "53,", "<script>,0")) {
            assertNull(LocationFix.fromMapQuery(query))
        }
    }

    @Test
    fun acceptsFreshCoordinatesAndRetainsAccuracy() {
        val fix = LocationFix.from(53.0, -6.0, 25f, 30_000L)!!
        assertEquals(25f, fix.accuracyMetres)
        assertEquals("53.00000, -6.00000", fix.mapQuery)
    }

    @Test
    fun rejectsOldOrFutureReadings() {
        assertNull(LocationFix.from(53.0, -6.0, 25f, 30_001L))
        assertNull(LocationFix.from(53.0, -6.0, 25f, -1L))
    }

    @Test
    fun rejectsInvalidCoordinates() {
        for (latitude in listOf(-90.1, 90.1, Double.NaN, Double.POSITIVE_INFINITY)) {
            assertNull(LocationFix.from(latitude, 0.0, null, 0L))
        }
        for (longitude in listOf(-180.1, 180.1, Double.NaN, Double.NEGATIVE_INFINITY)) {
            assertNull(LocationFix.from(0.0, longitude, null, 0L))
        }
        assertNotNull(LocationFix.from(-90.0, 180.0, null, 0L))
        assertNotNull(LocationFix.from(0.0, 0.0, null, 0L))
    }

    @Test
    fun invalidAccuracyIsShownAsUnknownWithoutDiscardingValidCoordinates() {
        for (accuracy in listOf(-1f, Float.NaN, Float.POSITIVE_INFINITY)) {
            assertNull(LocationFix.from(53.0, -6.0, accuracy, 0L)!!.accuracyMetres)
        }
    }

    @Test
    fun mapCoordinatesUseDecimalPointsEvenInCommaDecimalLocales() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            assertEquals("53.12345, -6.54321", LocationFix.from(53.12345, -6.54321, null, 0L)!!.mapQuery)
        } finally {
            Locale.setDefault(original)
        }
    }
}
