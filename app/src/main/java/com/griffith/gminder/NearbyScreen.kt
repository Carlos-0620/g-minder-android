package com.griffith.gminder

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kotlin.math.ceil

@Composable
fun NearbyScreen(monitor: ForegroundArrivalMonitor, radiusMetres: Int, onEnabled: (Boolean) -> Unit, onRetry: () -> Unit, onRange: () -> Unit) {
    Text("Location checks run only while G-Minder's main screens are visible.", style = MaterialTheme.typography.bodySmall)
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(monitor.enabled, enabled = monitor.targets.isNotEmpty() || monitor.enabled, role = Role.Switch, onValueChange = onEnabled),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Check nearby reminders", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                Switch(checked = monitor.enabled, onCheckedChange = null, enabled = monitor.targets.isNotEmpty() || monitor.enabled)
            }
            Text(monitor.status)
            Text("Range: $radiusMetres m · ${monitor.targets.size} pinned reminders", style = MaterialTheme.typography.bodySmall)
            monitor.fix?.let { fix -> Text(fix.accuracyMetres?.let { "Reported accuracy: ±${ceil(it.toDouble()).toLong()} m" } ?: "Accuracy unavailable", style = MaterialTheme.typography.bodySmall) }
            if (monitor.enabled) OutlinedButton(onClick = onRetry, modifier = Modifier.fillMaxWidth()) { Text("Retry location") }
            TextButton(onClick = onRange) { Text("Change range") }
        }
    }
    Text("Each reminder alerts once per monitoring session. Stop and restart checks to begin a new session.", style = MaterialTheme.typography.bodySmall)
    if (monitor.targets.isEmpty()) {
        Text("Create an active reminder using place search, a map pin or your current location.")
    } else {
        monitor.targets.forEach { target ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(target.title, style = MaterialTheme.typography.titleMedium)
                    Text(monitor.distances[target.id]?.let { "About ${ceil(it).toLong()} m away" } ?: "Waiting for location", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
