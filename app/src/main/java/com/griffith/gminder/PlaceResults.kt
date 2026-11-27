package com.griffith.gminder

import org.json.JSONObject

data class PlaceResult(val name: String, val address: String, val point: LocationFix) {
    val key: String get() = "$name|${point.mapQuery}"
}

internal object PlaceResults {
    fun parse(json: String): List<PlaceResult> {
        val features = JSONObject(json).getJSONArray("features")
        return (0 until features.length()).mapNotNull { index ->
            val feature = features.optJSONObject(index) ?: return@mapNotNull null
            val geometry = feature.optJSONObject("geometry") ?: return@mapNotNull null
            if (geometry.optString("type") != "Point") return@mapNotNull null
            val coordinates = geometry.optJSONArray("coordinates") ?: return@mapNotNull null
            if (coordinates.length() != 2) return@mapNotNull null
            val point = LocationFix.from(coordinates.optDouble(1), coordinates.optDouble(0), null, 0L) ?: return@mapNotNull null
            val properties = feature.optJSONObject("properties") ?: return@mapNotNull null
            fun value(key: String) = if (properties.isNull(key)) "" else properties.optString(key).trim()
            val street = listOf(value("housenumber"), value("street")).filter(String::isNotBlank).joinToString(" ")
            val locality = listOf("city", "town", "village", "district", "county").firstNotNullOfOrNull { key -> value(key).takeIf(String::isNotBlank) }.orEmpty()
            val name = value("name").ifBlank { street }.ifBlank { locality }
            if (name.isBlank()) return@mapNotNull null
            val address = listOf(street, locality, value("postcode"), value("country"))
                .filter { it.isNotBlank() && !it.equals(name, ignoreCase = true) }.distinct().joinToString(", ")
            PlaceResult(name, address, point)
        }.distinctBy(PlaceResult::key).take(8)
    }
}
