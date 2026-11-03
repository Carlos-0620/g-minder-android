package com.griffith.gminder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.util.Locale

@Composable
fun AmbientLightCard(lux: Float?, available: Boolean, active: Boolean) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Surrounding light", style = MaterialTheme.typography.titleMedium)
            when {
                !available -> Text("This phone does not have an ambient light sensor.")
                !active -> Text("Light sensor is currently unavailable.")
                lux == null -> Text("Waiting for a light reading…")
                else -> {
                    Text(String.format(Locale.getDefault(), "%.1f lx", lux), style = MaterialTheme.typography.headlineMedium)
                    // Simple display bands for this demonstration, not calibrated lighting advice.
                    val description = when {
                        lux < 50f -> "Low light"
                        lux < 500f -> "Moderate light"
                        else -> "Bright surroundings"
                    }
                    Text(description, color = MaterialTheme.colorScheme.primary)
                }
            }
            Text("Cover and uncover the phone's light sensor to see the reading change.", style = MaterialTheme.typography.bodySmall)
        }
    }
}
