package com.griffith.gminder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import java.util.Locale

@Composable
fun AmbientLightCard(lux: Float?, available: Boolean, active: Boolean, adaptToLight: Boolean, darkTheme: Boolean, onAdaptChanged: (Boolean) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(
                    value = adaptToLight,
                    enabled = available,
                    role = Role.Switch,
                    onValueChange = onAdaptChanged
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Automatic theme", style = MaterialTheme.typography.titleMedium)
                    Text(
                        when {
                            !available -> "Light sensor unavailable"
                            !active -> "Sensor paused"
                            lux == null -> "Waiting for light reading"
                            else -> "${if (darkTheme) "Dark" else "Light"} theme · ${String.format(Locale.getDefault(), "%.0f lx", lux)}"
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Switch(checked = adaptToLight, onCheckedChange = null, enabled = available)
            }
        }
    }
}
