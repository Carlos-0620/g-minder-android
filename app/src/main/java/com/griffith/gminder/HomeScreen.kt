package com.griffith.gminder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    activeCount: Int,
    completedCount: Int,
    onViewReminders: (Boolean) -> Unit,
    onNewReminder: () -> Unit,
    onSettings: () -> Unit
) {
    Text("A little reminder, in the right place.", style = MaterialTheme.typography.titleLarge)
    Text("Keep college, work and everyday errands together.")
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Your reminders", style = MaterialTheme.typography.titleLarge)
            Text("$activeCount active · $completedCount completed", style = MaterialTheme.typography.titleMedium)
            if (activeCount == 0) Text("You're all caught up. Add a reminder for your next task.")
            Button(onClick = { onViewReminders(false) }, modifier = Modifier.fillMaxWidth()) {
                Text("View active reminders")
            }
            OutlinedButton(onClick = { onViewReminders(true) }, modifier = Modifier.fillMaxWidth()) {
                Text("View completed reminders")
            }
        }
    }
    Button(onClick = onNewReminder, modifier = Modifier.fillMaxWidth()) { Text("+ New reminder") }
    OutlinedButton(onClick = onSettings, modifier = Modifier.fillMaxWidth()) { Text("Settings") }
}
