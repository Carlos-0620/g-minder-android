package com.griffith.gminder

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat

open class LightAwareActivity : ComponentActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var lightSensor: Sensor? = null
    var lightLevel by mutableStateOf<Float?>(null)
        private set
    var lightSensorActive by mutableStateOf(false)
        private set
    var adaptToLight by mutableStateOf(true)
        private set
    private var darkSurroundings by mutableStateOf(false)
    val hasLightSensor get() = lightSensor != null
    val useDarkTheme get() = adaptToLight && darkSurroundings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
    }

    override fun onResume() {
        super.onResume()
        val settings = getSharedPreferences("appearance", MODE_PRIVATE)
        adaptToLight = settings.getBoolean("adapt_to_light", true)
        darkSurroundings = hasLightSensor && settings.getBoolean("dark_surroundings", false)
        updateSystemBars()
        lightLevel = null
        lightSensorActive = lightSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        } ?: false
    }

    override fun onPause() {
        sensorManager.unregisterListener(this)
        lightSensorActive = false
        super.onPause()
    }

    fun setLightAdaptation(enabled: Boolean) {
        adaptToLight = enabled
        getSharedPreferences("appearance", MODE_PRIVATE).edit()
            .putBoolean("adapt_to_light", enabled).apply()
        updateSystemBars()
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_LIGHT) return
        val lux = event.values[0]
        if (!lux.isFinite() || lux < 0f) return
        lightLevel = lux
        // Separate thresholds keep small sensor fluctuations from flickering the theme.
        val nextDark = when {
            lux < 30f -> true
            lux > 80f -> false
            else -> darkSurroundings
        }
        if (nextDark != darkSurroundings) {
            darkSurroundings = nextDark
            getSharedPreferences("appearance", MODE_PRIVATE).edit()
                .putBoolean("dark_surroundings", nextDark).apply()
            updateSystemBars()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun updateSystemBars() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !useDarkTheme
            isAppearanceLightNavigationBars = !useDarkTheme
        }
    }
}
