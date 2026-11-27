package com.griffith.gminder

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel

class ForegroundArrivalMonitor(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val handler = Handler(Looper.getMainLooper())
    private val session = ArrivalSession()
    private var listener: LocationListener? = null
    private var readingTimeNanos = 0L
    private var radius = ReminderRange.DEFAULT_METRES

    var enabled by mutableStateOf(false)
        private set
    var foreground by mutableStateOf(false)
        private set
    var status by mutableStateOf("Nearby checks are off.")
        private set
    var targets by mutableStateOf<List<ArrivalTarget>>(emptyList())
        private set
    var fix by mutableStateOf<LocationFix?>(null)
        private set
    var distances by mutableStateOf<Map<String, Double>>(emptyMap())
        private set
    var alerts by mutableStateOf<List<ArrivalAlert>>(emptyList())
        private set

    private val freshnessCheck = object : Runnable {
        override fun run() {
            if (listener == null || !foreground || !enabled) return
            if (fix != null && readingAgeMillis() > 30_000L) {
                clearReading()
                status = "Waiting for a fresh location…"
            }
            handler.postDelayed(this, 5_000L)
        }
    }

    fun hasPermission() = granted(Manifest.permission.ACCESS_COARSE_LOCATION) || granted(Manifest.permission.ACCESS_FINE_LOCATION)
    private fun granted(permission: String) = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    fun updateTargets(next: List<ArrivalTarget>, radiusMetres: Int) {
        targets = next
        radius = ReminderRange.normalise(radiusMetres)
        session.updateTargets(next)
        alerts = alerts.mapNotNull { alert -> next.firstOrNull { it.key == alert.target.key }?.let { alert.copy(target = it) } }
        if (enabled && foreground) {
            if (targets.isEmpty()) {
                stopProviders()
                status = "Add an active reminder with a map pin to begin."
            } else if (listener == null) startProviders() else evaluateReading()
        }
    }

    fun enable() {
        if (!enabled) {
            session.reset()
            alerts = emptyList()
        }
        enabled = true
        if (foreground) startProviders() else status = "Paused while G-Minder is not visible."
    }

    fun disable() {
        enabled = false
        stopProviders()
        alerts = emptyList()
        session.reset()
        status = "Nearby checks are off."
    }

    fun permissionDenied() {
        disable()
        status = "Location access was not granted. You can still create and view reminders."
    }

    fun onForegroundStart() {
        foreground = true
        if (enabled) startProviders()
    }

    fun onForegroundStop() {
        foreground = false
        stopProviders()
        if (enabled) status = "Paused while G-Minder is not visible."
    }

    fun markShown(key: ArrivalKey) = session.markShown(key)
    fun dismissFirst() {
        alerts.firstOrNull()?.let { session.markShown(it.target.key) }
        alerts = alerts.drop(1)
    }

    private fun enabledProviders() = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).filter {
        (it != LocationManager.GPS_PROVIDER || granted(Manifest.permission.ACCESS_FINE_LOCATION)) &&
            runCatching { manager.isProviderEnabled(it) }.getOrDefault(false)
    }

    private fun startProviders() {
        stopProviders()
        if (!foreground || !enabled) return
        if (targets.isEmpty()) { status = "Add an active reminder with a map pin to begin."; return }
        if (!hasPermission()) { status = "Location permission is needed. Tap Retry location."; return }
        val providers = enabledProviders()
        if (providers.isEmpty()) { status = "Phone location is off or unavailable. Enable it, then tap Retry location."; return }
        status = "Waiting for a fresh location…"
        val request = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (listener !== this || !foreground || !enabled || location.elapsedRealtimeNanos < readingTimeNanos) return
                val ageMillis = (SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos) / 1_000_000L
                val next = LocationFix.from(location.latitude, location.longitude, location.accuracy.takeIf { location.hasAccuracy() }, ageMillis) ?: return
                fix = next
                readingTimeNanos = location.elapsedRealtimeNanos
                evaluateReading()
            }
            override fun onProviderDisabled(provider: String) {
                if (listener === this && enabledProviders().isEmpty()) {
                    clearReading()
                    status = "Phone location is off. Enable it, then tap Retry location."
                }
            }
            override fun onProviderEnabled(provider: String) {
                if (listener === this) status = "Waiting for a fresh location…"
            }
            @Deprecated("Required for older Android versions")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
        }
        listener = request
        var registered = false
        for (provider in providers) {
            try {
                manager.requestLocationUpdates(provider, 5_000L, 0f, request, Looper.getMainLooper())
                registered = true
            } catch (_: SecurityException) {
                continue
            } catch (_: IllegalArgumentException) {
                continue
            }
        }
        if (registered) handler.postDelayed(freshnessCheck, 5_000L) else {
            stopProviders()
            status = "Location could not start. Check permission and phone location, then retry."
        }
    }

    private fun evaluateReading() {
        val current = fix ?: return
        val age = readingAgeMillis()
        if (age !in 0L..30_000L) { clearReading(); status = "Waiting for a fresh location…"; return }
        distances = targets.associate { it.id to ArrivalGeometry.distanceMetres(current, it.point) }
        alerts = alerts.mapNotNull { alert ->
            val distance = distances[alert.target.id] ?: return@mapNotNull null
            if (ArrivalGeometry.isInside(distance, current.accuracyMetres, radius, age)) alert.copy(distanceMetres = distance) else {
                session.releaseIfUnseen(alert.target.key)
                null
            }
        }
        alerts = alerts + session.evaluate(current, targets, radius, age)
        status = if (current.accuracyMetres == null || current.accuracyMetres > radius) {
            "Location is too broad for this range. Precise location works best."
        } else "Checking nearby places."
    }

    private fun readingAgeMillis() = (SystemClock.elapsedRealtimeNanos() - readingTimeNanos) / 1_000_000L
    private fun clearReading() { fix = null; distances = emptyMap() }
    private fun stopProviders() {
        handler.removeCallbacks(freshnessCheck)
        listener?.let { runCatching { manager.removeUpdates(it) } }
        listener = null
        clearReading()
        readingTimeNanos = 0L
    }

    override fun onCleared() { stopProviders(); super.onCleared() }
}
