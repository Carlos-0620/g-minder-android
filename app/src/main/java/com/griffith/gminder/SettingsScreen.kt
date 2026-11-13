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

@Composable
fun SettingsScreen(
    lux: Float?,
    sensorAvailable: Boolean,
    sensorActive: Boolean,
    adaptToLight: Boolean,
    darkTheme: Boolean,
    onAdaptChanged: (Boolean) -> Unit
) {
    Text("Appearance", style = MaterialTheme.typography.titleLarge)
    AmbientLightCard(lux, sensorAvailable, sensorActive, adaptToLight, darkTheme, onAdaptChanged)
    Text(
        "Automatic mode uses a dark theme below 30 lux and a light theme above 80 lux. " +
            "Between these levels, the current theme stays unchanged to avoid flickering. " +
            "Turning automatic mode off selects the light theme. Your choice is saved.",
        style = MaterialTheme.typography.bodyMedium
    )
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("About this preview", style = MaterialTheme.typography.titleMedium)
            Text("The light sensor changes the app theme, not the phone's screen brightness.")
            Text("Locations are sample places. GPS monitoring and reminder notifications are not enabled yet.")
            Text("Reminders are kept for the current session. Permanent reminder storage is planned.")
        }
    }
}
