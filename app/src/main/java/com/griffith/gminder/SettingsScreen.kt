package com.griffith.gminder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    lux: Float?,
    sensorAvailable: Boolean,
    sensorActive: Boolean,
    adaptToLight: Boolean,
    darkTheme: Boolean,
    onAdaptChanged: (Boolean) -> Unit,
    reminderRangeMetres: Int,
    onRangeChanged: (Int) -> Unit
) {
    AmbientLightCard(lux, sensorAvailable, sensorActive, adaptToLight, darkTheme, onAdaptChanged)
    var range by rememberSaveable(reminderRangeMetres) { mutableStateOf(reminderRangeMetres) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Reminder range", style = MaterialTheme.typography.titleMedium)
            Text("$range metres", style = MaterialTheme.typography.headlineMedium)
            Text("Radius around a reminder's location.", style = MaterialTheme.typography.bodySmall)
            Slider(
                value = range.toFloat(),
                onValueChange = { range = ReminderRange.normalise(it.toInt()) },
                onValueChangeFinished = { onRangeChanged(range) },
                valueRange = ReminderRange.MIN_METRES.toFloat()..ReminderRange.MAX_METRES.toFloat(),
                steps = (ReminderRange.MAX_METRES - ReminderRange.MIN_METRES) / ReminderRange.STEP_METRES - 1,
                modifier = Modifier.fillMaxWidth().semantics {
                    contentDescription = "Reminder radius"
                    stateDescription = "$range metres"
                }
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("100 m", style = MaterialTheme.typography.bodySmall)
                Text("1 km", style = MaterialTheme.typography.bodySmall)
            }
            Text("Used by Nearby reminders while the app is open.", style = MaterialTheme.typography.bodySmall)
        }
    }
}
