package com.griffith.gminder

import android.Manifest
import android.app.Activity
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

class CurrentLocationCapture(private val activity: Activity) {
    private val manager = activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val handler = Handler(Looper.getMainLooper())
    private var listener: LocationListener? = null
    var loading by mutableStateOf(false)
        private set
    var fix by mutableStateOf<LocationFix?>(null)
        private set
    var message by mutableStateOf("Capture your phone's current position for this reminder.")
        private set

    private val timeout = Runnable {
        cancel()
        message = "No fresh location received. Try again with a clearer view of the sky, or use place search or a map pin."
    }

    fun hasPermission(): Boolean = granted(Manifest.permission.ACCESS_COARSE_LOCATION) || granted(Manifest.permission.ACCESS_FINE_LOCATION)

    private fun granted(permission: String) = ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED

    fun permissionDenied() {
        cancel()
        message = "Location access was not granted. You can still search for a place or select a map pin. Enable access in Android's app settings before retrying."
    }

    fun start() {
        cancel()
        fix = null
        if (!hasPermission()) {
            permissionDenied()
            return
        }
        val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER).filter {
            (it != LocationManager.GPS_PROVIDER || granted(Manifest.permission.ACCESS_FINE_LOCATION)) &&
                runCatching { manager.isProviderEnabled(it) }.getOrDefault(false)
        }
        if (providers.isEmpty()) {
            message = "No suitable location provider is enabled. Turn on phone location services, or use place search or a map pin."
            return
        }
        loading = true
        message = "Finding a fresh location… This can take up to 20 seconds."
        val request = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (listener !== this || !loading) return
                val ageMillis = (SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos) / 1_000_000L
                val fresh = LocationFix.from(location.latitude, location.longitude, location.accuracy.takeIf { location.hasAccuracy() }, ageMillis) ?: return
                fix = fresh
                cancel()
                message = if (granted(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    "Location captured. Check the accuracy, then select these coordinates."
                } else {
                    "Approximate location captured. This may cover a wide area; check it in Maps before saving."
                }
            }

            override fun onProviderDisabled(provider: String) = Unit
            override fun onProviderEnabled(provider: String) = Unit
            @Deprecated("Required for older Android versions")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
        }
        listener = request
        var registered = false
        for (provider in providers) {
            try {
                manager.requestLocationUpdates(provider, 1_000L, 0f, request, Looper.getMainLooper())
                registered = true
            } catch (_: SecurityException) {
                // Another provider may still be available with approximate permission.
            } catch (_: IllegalArgumentException) {
                // A provider may disappear after checking availability.
            }
        }
        if (registered) handler.postDelayed(timeout, 20_000L) else {
            cancel()
            message = "Location could not be started. Check permission and phone location services, or use place search or a map pin."
        }
    }

    fun cancel() {
        handler.removeCallbacks(timeout)
        listener?.let { current -> runCatching { manager.removeUpdates(current) } }
        listener = null
        if (loading) message = "Location request stopped. Tap again to retry."
        loading = false
    }
}
