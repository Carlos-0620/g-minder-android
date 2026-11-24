package com.griffith.gminder

import android.app.Activity
import android.Manifest
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
import androidx.compose.ui.unit.dp

data class Reminder(val id: String, val title: String, val note: String, val place: String, val completed: Boolean = false)
private val places = listOf(
    "Griffith College Dublin",
    "Roly's Bistro, Ballsbridge, Dublin",
    "Tesco, Stillorgan, Dublin",
    "Mister Magpie, Ballsbridge, Dublin"
)
private val reminderSaver = listSaver<List<Reminder>, String>(
    save = { items -> items.flatMap { listOf(it.id, it.title, it.note, it.place, it.completed.toString()) } },
    restore = { values -> values.chunked(5).map { Reminder(it[0], it[1], it[2], it[3], it[4].toBoolean()) } }
)

class MainActivity : LightAwareActivity() {
    private lateinit var locationCapture: CurrentLocationCapture
    private var locationScreenVisible = false
    private val locationPermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (locationScreenVisible) {
            if (locationCapture.hasPermission()) locationCapture.start() else locationCapture.permissionDenied()
        }
    }

    private fun requestCurrentLocation() {
        if (locationCapture.hasPermission()) locationCapture.start()
        else locationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    override fun onStop() {
        if (::locationCapture.isInitialized) locationCapture.cancel()
        super.onStop()
    }
    private var completion by mutableStateOf<Pair<String, Boolean>?>(null)
    private val details = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) result.data?.let {
            completion = it.getStringExtra("id").orEmpty() to it.getBooleanExtra("completed", false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        locationCapture = CurrentLocationCapture(this)
        setContent {
            GMinderTheme(darkTheme = useDarkTheme) {
                var reminders by rememberSaveable(stateSaver = reminderSaver) { mutableStateOf(listOf(
                    Reminder("1", "Return library books", "Bring the borrowed books back to college.", places[0]),
                    Reminder("2", "Check work rota", "Check my upcoming shifts before leaving work.", places[1]),
                    Reminder("3", "Buy groceries", "Pick up milk, bread and fruit.", places[2]),
                    Reminder("4", "Buy coffee beans", "Pick up coffee beans for home.", places[3])
                )) }
                var screen by rememberSaveable { mutableStateOf("home") }
                var title by rememberSaveable { mutableStateOf("") }
                var note by rememberSaveable { mutableStateOf("") }
                var place by rememberSaveable { mutableStateOf("") }
                var attempted by rememberSaveable { mutableStateOf(false) }
                var completedOnly by rememberSaveable { mutableStateOf(false) }
                DisposableEffect(screen) {
                    locationScreenVisible = screen == "location"
                    onDispose { locationScreenVisible = false; locationCapture.cancel() }
                }
                LaunchedEffect(completion) {
                    completion?.let { result -> reminders = reminders.map { if (it.id == result.first) it.copy(completed = result.second) else it } }
                }
                val goBack = {
                    screen = when (screen) {
                        "map" -> "location"
                        "location" -> "create"
                        "create" -> "reminders"
                        else -> "home"
                    }
                }
                val newReminder = {
                    title = ""; note = ""; place = ""; attempted = false; screen = "create"
                }
                BackHandler(screen != "home", onBack = goBack)
                Page(when (screen) { "home" -> "Home"; "create" -> "Create Reminder"; "location" -> "Choose Location"; "map" -> "Pin on Map"; "settings" -> "Settings"; else -> "My Reminders" },
                    back = if (screen == "home") null else goBack) {
                    when (screen) {
                        "home" -> {
                            HomeScreen(
                                activeCount = reminders.count { !it.completed },
                                completedCount = reminders.count { it.completed },
                                onViewReminders = { completedOnly = it; screen = "reminders" },
                                onNewReminder = newReminder,
                                onSettings = { screen = "settings" }
                            )
                        }
                        "reminders" -> {
                            OutlinedButton(onClick = { screen = "settings" }, modifier = Modifier.fillMaxWidth()) { Text("Settings") }
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
                            Button(onClick = newReminder, modifier = Modifier.fillMaxWidth()) { Text("+ New reminder") }
                        }
                        "settings" -> SettingsScreen(lightLevel, hasLightSensor, lightSensorActive, adaptToLight, useDarkTheme, ::setLightAdaptation)
                        "create" -> {
                            Text("What would you like to remember?", style = MaterialTheme.typography.titleLarge)
                            OutlinedTextField(title, { title = it.take(100) }, label = { Text("Title *") }, singleLine = true, isError = attempted && title.isBlank(), modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(note, { note = it.take(500) }, label = { Text("Notes (optional)") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                            OutlinedButton(onClick = { screen = "location" }, modifier = Modifier.fillMaxWidth()) { Text(place.ifBlank { "Choose a location *" }) }
                            if (attempted && (title.isBlank() || place.isBlank())) Text("Enter a title and choose a location.", color = MaterialTheme.colorScheme.error)
                            Text("This preview saves reminders for the current session. Location alerts will come in a later milestone.", style = MaterialTheme.typography.bodySmall)
                            Button(onClick = {
                                attempted = true
                                if (title.isNotBlank() && place.isNotBlank()) {
                                    reminders = reminders + Reminder(java.util.UUID.randomUUID().toString(), title.trim(), note.trim(), place)
                                    completedOnly = false; screen = "reminders"
                                }
                            }, modifier = Modifier.fillMaxWidth()) { Text("Save reminder") }
                        }
                        "location" -> {
                            Text("Pick a place", style = MaterialTheme.typography.headlineMedium)
                            OutlinedButton(onClick = { screen = "map" }, modifier = Modifier.fillMaxWidth()) { Text("Pin a location on map") }
                            Text("Selected location: ${place.ifBlank { "None yet" }}")
                            if (place.isNotBlank()) {
                                Button(onClick = { screen = "create" }, modifier = Modifier.fillMaxWidth()) { Text("Use this location") }
                                OutlinedButton(onClick = { openMaps(place) }, modifier = Modifier.fillMaxWidth()) { Text("Preview in Maps") }
                            }
                            CurrentLocationCard(locationCapture, ::requestCurrentLocation) { place = it }
                            Text("Or choose a sample place", style = MaterialTheme.typography.titleMedium)
                            places.forEach { candidate ->
                                Card(onClick = { locationCapture.cancel(); place = candidate }, modifier = Modifier.fillMaxWidth()) {
                                    Row(Modifier.padding(16.dp)) {
                                        RadioButton(selected = place == candidate, onClick = { locationCapture.cancel(); place = candidate })
                                        Text(candidate, Modifier.padding(top = 12.dp, start = 8.dp))
                                    }
                                }
                            }
                        }
                        "map" -> MapLocationPicker(LocationFix.fromMapQuery(place) ?: locationCapture.fix) {
                            place = it
                            screen = "location"
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Page(title: String, back: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val scrollState = key(title) { rememberScrollState() }
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.safeDrawingPadding().imePadding().verticalScroll(scrollState).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
