package com.griffith.gminder

import org.junit.Assert.*
import org.junit.Test

class PlaceResultsTest {
    private fun feature(name: String = "Tesco", street: String = "Main Street", lat: String = "53.3", lon: String = "-6.2") =
        """{"geometry":{"type":"Point","coordinates":[$lon,$lat]},"properties":{"name":"$name","street":"$street","city":"Dublin","country":"Ireland"}}"""

    private fun response(vararg features: String) = """{"features":[${features.joinToString(",") }]}"""

    @Test fun preservesDistinctBranchesAndTheirAddresses() {
        val results = PlaceResults.parse(response(feature(street = "First Street"), feature(street = "Second Street", lat = "53.4")))
        assertEquals(2, results.size)
        assertEquals("Tesco", results[0].name)
        assertEquals("First Street, Dublin, Ireland", results[0].address)
        assertEquals("Second Street, Dublin, Ireland", results[1].address)
    }

    @Test fun convertsGeoJsonLongitudeLatitudeOrderCorrectly() {
        val point = PlaceResults.parse(response(feature())).single().point
        assertEquals(53.3, point.latitude, 0.000001)
        assertEquals(-6.2, point.longitude, 0.000001)
    }

    @Test fun skipsInvalidCoordinatesMissingGeometryAndNonPoints() {
        val results = PlaceResults.parse(response(feature(lat = "91"), feature(lon = "181"), "{}",
            """{"geometry":{"type":"LineString","coordinates":[-6,53]},"properties":{"name":"Road"}}""",
            feature()))
        assertEquals(1, results.size)
    }

    @Test fun deduplicatesRepeatedPlacesButKeepsDifferentBusinessesAtOnePoint() {
        val results = PlaceResults.parse(response(feature(), feature(), feature(name = "Coffee shop")))
        assertEquals(listOf("Tesco", "Coffee shop"), results.map { it.name })
    }

    @Test fun supportsAddressOnlyResultsWithoutDisplayingNullFields() {
        val json = """{"features":[{"geometry":{"type":"Point","coordinates":[-6,53]},"properties":{"name":null,"street":"Main Street","housenumber":"12","city":"Dublin","postcode":null}}]}"""
        val result = PlaceResults.parse(json).single()
        assertEquals("12 Main Street", result.name)
        assertEquals("Dublin", result.address)
    }

    @Test fun limitsResultsAndHandlesEmptyResponses() {
        assertTrue(PlaceResults.parse(response()).isEmpty())
        val features = (1..12).map { feature(name = "Place $it") }.toTypedArray()
        assertEquals(8, PlaceResults.parse(response(*features)).size)
    }

    @Test(expected = org.json.JSONException::class)
    fun malformedServiceResponseIsAnErrorInsteadOfAnEmptySuccess() {
        PlaceResults.parse("""{"message":"Unavailable"}""")
    }
}
