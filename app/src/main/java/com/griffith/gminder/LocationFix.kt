package com.griffith.gminder

import java.util.Locale

class LocationFix private constructor(
    val latitude: Double,
    val longitude: Double,
    val accuracyMetres: Float?
) {
    val mapQuery: String get() = String.format(Locale.US, "%.5f, %.5f", latitude, longitude)

    companion object {
        fun from(latitude: Double, longitude: Double, accuracyMetres: Float?, ageMillis: Long): LocationFix? {
            if (!latitude.isFinite() || latitude !in -90.0..90.0) return null
            if (!longitude.isFinite() || longitude !in -180.0..180.0) return null
            if (ageMillis !in 0L..30_000L) return null
            val accuracy = accuracyMetres?.takeIf { it.isFinite() && it >= 0f }
            return LocationFix(latitude, longitude, accuracy)
        }
    }
}
