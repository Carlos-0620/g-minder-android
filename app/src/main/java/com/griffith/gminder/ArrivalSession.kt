package com.griffith.gminder

import kotlin.math.*

data class ArrivalKey(val id: String, val latitude: Double, val longitude: Double)

data class ArrivalTarget(val id: String, val title: String, val point: LocationFix) {
    val key get() = ArrivalKey(id, point.latitude, point.longitude)

    companion object {
        fun fromReminder(reminder: Reminder): ArrivalTarget? {
            if (reminder.completed) return null
            val point = LocationFix.fromMapQuery(reminder.place) ?: return null
            return ArrivalTarget(reminder.id, reminder.title, point)
        }
    }
}

data class ArrivalAlert(val target: ArrivalTarget, val distanceMetres: Double)

object ArrivalGeometry {
    fun distanceMetres(from: LocationFix, to: LocationFix): Double {
        val latitudeDifference = Math.toRadians(to.latitude - from.latitude)
        val longitudeDifference = Math.toRadians(to.longitude - from.longitude)
        val a = sin(latitudeDifference / 2).pow(2) +
            cos(Math.toRadians(from.latitude)) * cos(Math.toRadians(to.latitude)) * sin(longitudeDifference / 2).pow(2)
        return 6_371_000.0 * 2 * atan2(sqrt(a.coerceIn(0.0, 1.0)), sqrt((1.0 - a).coerceIn(0.0, 1.0)))
    }

    fun isInside(distanceMetres: Double, accuracyMetres: Float?, radiusMetres: Int, ageMillis: Long): Boolean {
        if (ageMillis !in 0L..30_000L || !distanceMetres.isFinite() || distanceMetres < 0) return false
        val accuracy = accuracyMetres ?: return false
        if (!accuracy.isFinite() || accuracy < 0 || radiusMetres <= 0) return false
        return distanceMetres + accuracy <= radiusMetres
    }
}

class ArrivalSession {
    private val alerted = mutableSetOf<ArrivalKey>()
    private val shown = mutableSetOf<ArrivalKey>()

    fun updateTargets(targets: List<ArrivalTarget>) {
        alerted.retainAll(targets.map { it.key }.toSet())
        shown.retainAll(targets.map { it.key }.toSet())
    }

    fun evaluate(fix: LocationFix, targets: List<ArrivalTarget>, radiusMetres: Int, ageMillis: Long): List<ArrivalAlert> {
        updateTargets(targets)
        return targets.mapNotNull { target ->
            val distance = ArrivalGeometry.distanceMetres(fix, target.point)
            if (ArrivalGeometry.isInside(distance, fix.accuracyMetres, radiusMetres, ageMillis) && alerted.add(target.key)) {
                ArrivalAlert(target, distance)
            } else null
        }
    }

    fun markShown(key: ArrivalKey) { shown.add(key) }

    fun releaseIfUnseen(key: ArrivalKey) {
        if (key !in shown) alerted.remove(key)
    }

    fun reset() { alerted.clear(); shown.clear() }
}
