package com.griffith.gminder

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

data class Reminder(val id: String, val title: String, val note: String, val place: String, val completed: Boolean = false)
private val places = listOf("Trinity College Dublin", "St Stephen's Green Dublin", "Dublin Connolly Station")
private val reminderSaver = listSaver<List<Reminder>, String>(
    save = { items -> items.flatMap { listOf(it.id, it.title, it.note, it.place, it.completed.toString()) } },
    restore = { values -> values.chunked(5).map { Reminder(it[0], it[1], it[2], it[3], it[4].toBoolean()) } }
)

@Composable
fun GMinderTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    val colors = if (darkTheme) {
        darkColorScheme(primary = Color(0xFFFFB596), secondary = Color(0xFFB8CDB4), background = Color(0xFF1B1512), surface = Color(0xFF1B1512))
    } else {
        lightColorScheme(primary = Color(0xFFB94E20), secondary = Color(0xFF536653), background = Color(0xFFFFF9F4), surface = Color(0xFFFFF9F4))
    }
    MaterialTheme(colorScheme = colors, content = content)
}

class MainActivity : LightAwareActivity() {
    private var completion by mutableStateOf<Pair<String, Boolean>?>(null)
    private val details = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) result.data?.let {
            completion = it.getStringExtra("id").orEmpty() to it.getBooleanExtra("completed", false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GMinderTheme(darkTheme = useDarkTheme) {
                var reminders by rememberSaveable(stateSaver = reminderSaver) { mutableStateOf(listOf(
                    Reminder("1", "Return library books", "Bring the two borrowed books.", places[0]),
                    Reminder("2", "Take a walking break", "Enjoy a short walk after class.", places[1]),
                    Reminder("3", "Collect train tickets", "Check the booking reference.", places[2])
                )) }
                var screen by rememberSaveable { mutableStateOf("home") }
                var title by rememberSaveable { mutableStateOf("") }
                var note by rememberSaveable { mutableStateOf("") }
                var place by rememberSaveable { mutableStateOf("") }
                var attempted by rememberSaveable { mutableStateOf(false) }
                var completedOnly by rememberSaveable { mutableStateOf(false) }
                LaunchedEffect(completion) {
                    completion?.let { result -> reminders = reminders.map { if (it.id == result.first) it.copy(completed = result.second) else it } }
                }
                BackHandler(screen != "home") { screen = if (screen == "location") "create" else "home" }
                Page(when (screen) { "create" -> "Create Reminder"; "location" -> "Choose Location"; else -> "My Reminders" },
                    back = if (screen == "home") null else ({ screen = if (screen == "location") "create" else "home" })) {
                    when (screen) {
                        "home" -> {
                            Text("A little reminder, in the right place.", style = MaterialTheme.typography.titleMedium)
                            Text("Milestone 1 preview · Sample places and session-only reminders", style = MaterialTheme.typography.bodySmall)
                            AmbientLightCard(lightLevel, hasLightSensor, lightSensorActive, adaptToLight, useDarkTheme, ::setLightAdaptation)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(!completedOnly, { completedOnly = false }, { Text("Active") })
                                FilterChip(completedOnly, { completedOnly = true }, { Text("Completed") })
                            }
                            val visible = reminders.filter { it.completed == completedOnly }
                            if (visible.isEmpty()) Text(if (completedOnly) "No completed reminders yet." else "You're all caught up.")
                            visible.forEach { reminder ->
                                Card(onClick = {
                                    // Explicit Android intent: open our own details activity.
                                    details.launch(Intent(this@MainActivity, ReminderDetailsActivity::class.java).apply {
                                        putExtra("id", reminder.id); putExtra("title", reminder.title)
                                        putExtra("note", reminder.note); putExtra("place", reminder.place)
                                        putExtra("completed", reminder.completed)
                                    })
                                }, modifier = Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(reminder.title, style = MaterialTheme.typography.titleLarge)
                                        Text(reminder.place)
                                        Text(if (reminder.completed) "Completed · View details" else "Active · View details", color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                            Button(onClick = { title = ""; note = ""; place = ""; attempted = false; screen = "create" }, modifier = Modifier.fillMaxWidth()) { Text("+ New reminder") }
                        }
                        "create" -> {
                            Text("What would you like to remember?", style = MaterialTheme.typography.titleLarge)
                            OutlinedTextField(title, { title = it.take(100) }, label = { Text("Title *") }, singleLine = true, isError = attempted && title.isBlank(), modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(note, { note = it.take(500) }, label = { Text("Notes (optional)") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                            OutlinedButton(onClick = { screen = "location" }, modifier = Modifier.fillMaxWidth()) { Text(place.ifBlank { "Choose a sample location *" }) }
                            if (attempted && (title.isBlank() || place.isBlank())) Text("Enter a title and choose a location.", color = MaterialTheme.colorScheme.error)
                            Text("This preview saves reminders for the current session. Location alerts will come in a later milestone.", style = MaterialTheme.typography.bodySmall)
                            Button(onClick = {
                                attempted = true
                                if (title.isNotBlank() && place.isNotBlank()) {
                                    reminders = reminders + Reminder(java.util.UUID.randomUUID().toString(), title.trim(), note.trim(), place)
                                    completedOnly = false; screen = "home"
                                }
                            }, modifier = Modifier.fillMaxWidth()) { Text("Save reminder") }
                        }
                        "location" -> {
                            Text("Pick a place", style = MaterialTheme.typography.headlineMedium)
                            Text("Choose one of the sample locations for your reminder.")
                            places.forEach { candidate ->
                                Card(onClick = { place = candidate }, modifier = Modifier.fillMaxWidth()) {
                                    Row(Modifier.padding(16.dp)) {
                                        RadioButton(selected = place == candidate, onClick = { place = candidate })
                                        Text(candidate, Modifier.padding(top = 12.dp, start = 8.dp))
                                    }
                                }
                            }
                            OutlinedButton(onClick = { openMaps(place) }, enabled = place.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Preview in Maps") }
                            Button(onClick = { screen = "create" }, enabled = place.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Use this location") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Page(title: String, back: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("G-MINDER", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            if (back != null) TextButton(onClick = back) { Text("‹ Back") }
            Text(title, style = MaterialTheme.typography.headlineLarge)
            content()
        }
    }
}

class ReminderDetailsActivity : LightAwareActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GMinderTheme(darkTheme = useDarkTheme) {
                var completed by rememberSaveable { mutableStateOf(intent.getBooleanExtra("completed", false)) }
                val place = intent.getStringExtra("place").orEmpty()
                Page("Reminder Details", back = { finish() }) {
                    Text(intent.getStringExtra("title").orEmpty(), style = MaterialTheme.typography.headlineMedium)
                    SuggestionChip(onClick = {}, label = { Text(if (completed) "Completed" else "Active") })
                    Text("LOCATION", style = MaterialTheme.typography.labelLarge)
                    Text(place, style = MaterialTheme.typography.titleLarge)
                    Text("NOTES", style = MaterialTheme.typography.labelLarge)
                    Text(intent.getStringExtra("note").orEmpty().ifBlank { "No notes added." })
                    OutlinedButton(onClick = { openMaps(place) }, modifier = Modifier.fillMaxWidth()) { Text("Open in Maps") }
                    Button(onClick = {
                        completed = !completed
                        setResult(Activity.RESULT_OK, Intent().putExtra("id", intent.getStringExtra("id")).putExtra("completed", completed))
                        finish()
                    }, modifier = Modifier.fillMaxWidth()) { Text(if (completed) "Mark active" else "Mark completed") }
                }
            }
        }
    }
}

private fun Activity.openMaps(place: String) {
    // Implicit Android intent: Android chooses an installed map application.
    try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(place)}"))) }
    catch (_: ActivityNotFoundException) { Toast.makeText(this, "No maps app installed. Install a maps app to preview this location.", Toast.LENGTH_LONG).show() }
}
