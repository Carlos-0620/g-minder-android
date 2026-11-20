package com.griffith.gminder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.ceil

@Composable
fun CurrentLocationCard(capture: CurrentLocationCapture, onCapture: () -> Unit, onSelect: (String) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Use where I am", style = MaterialTheme.typography.titleLarge)
            Text("Location access is optional and only used for this request. No background tracking.")
            Text(capture.message)
            if (capture.loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Button(onClick = onCapture, enabled = !capture.loading, modifier = Modifier.fillMaxWidth()) {
                Text("Use my current location")
            }
            if (capture.loading) OutlinedButton(onClick = capture::cancel, modifier = Modifier.fillMaxWidth()) { Text("Cancel location request") }
            capture.fix?.let { fix ->
                Text(fix.mapQuery)
                Text(fix.accuracyMetres?.let { "Reported accuracy: approximately ${ceil(it.toDouble()).toLong()} metres" } ?: "Accuracy is not available.")
                OutlinedButton(onClick = { onSelect(fix.mapQuery) }, modifier = Modifier.fillMaxWidth()) { Text("Select these coordinates") }
            }
        }
    }
}
