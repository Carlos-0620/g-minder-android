package com.griffith.gminder

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color

@Composable
fun HomeScreen(
    name: String,
    activeCount: Int,
    completedCount: Int,
    onViewReminders: (Boolean) -> Unit,
    onNewReminder: () -> Unit,
    onActivity: (String) -> Unit
) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)) {
                Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) { Icon(painterResource(R.drawable.gminder_mark), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(44.dp)) }
            }
            Text("Hello, $name", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            Text("Your day, a little\nmore organised.", style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
        }
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            activityShortcuts.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    row.forEach { activity ->
                        FilledTonalIconButton(
                            onClick = { onActivity(activity.suggestedTitle) },
                            modifier = Modifier.size(64.dp).semantics { contentDescription = "${activity.description}: create reminder" },
                            colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                        ) { ActivityGlyph(activity.icon) }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ReminderCount(activeCount, "Active", Modifier.weight(1f)) { onViewReminders(false) }
            ReminderCount(completedCount, "Completed", Modifier.weight(1f)) { onViewReminders(true) }
        }
        Button(onClick = onNewReminder, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), shape = RoundedCornerShape(16.dp)) { Text("+ New reminder") }
    }
}

@Composable
private fun ReminderCount(count: Int, label: String, modifier: Modifier, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))) {
        Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(count.toString(), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
