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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModelProvider

class MainActivity : LightAwareActivity() {
    private lateinit var reminderStore: ReminderStore
    private var reminderRangeMetres by mutableStateOf(ReminderRange.DEFAULT_METRES)

    private fun setReminderRange(metres: Int) {
        reminderRangeMetres = ReminderRange.normalise(metres)
        getSharedPreferences("reminder_settings", MODE_PRIVATE).edit()
            .putInt("radius_metres", reminderRangeMetres).apply()
    }

    private lateinit var locationCapture: CurrentLocationCapture
    private lateinit var arrivalMonitor: ForegroundArrivalMonitor
    private var locationPermissionPurpose: String? = null
    private var locationScreenVisible = false
    private val locationPermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        val purpose = locationPermissionPurpose
        locationPermissionPurpose = null
        when (purpose) {
            "capture" -> if (locationScreenVisible) {
                if (locationCapture.hasPermission()) locationCapture.start() else locationCapture.permissionDenied()
            }
            "arrivals" -> if (arrivalMonitor.hasPermission()) arrivalMonitor.enable() else arrivalMonitor.permissionDenied()
        }
    }

    private fun requestCurrentLocation() {
        if (locationCapture.hasPermission()) locationCapture.start()
        else {
            locationPermissionPurpose = "capture"
            locationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    private fun setArrivalMonitoring(enabled: Boolean) {
        if (!enabled) arrivalMonitor.disable()
        else if (arrivalMonitor.hasPermission()) arrivalMonitor.enable()
        else {
            locationPermissionPurpose = "arrivals"
            locationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    override fun onStart() {
        super.onStart()
        if (::arrivalMonitor.isInitialized) arrivalMonitor.onForegroundStart()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("location_permission_purpose", locationPermissionPurpose)
        super.onSaveInstanceState(outState)
    }

    override fun onStop() {
        if (::locationCapture.isInitialized) locationCapture.cancel()
        if (::arrivalMonitor.isInitialized) arrivalMonitor.onForegroundStop()
        super.onStop()
    }
    private val details = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        locationPermissionPurpose = savedInstanceState?.getString("location_permission_purpose")
        arrivalMonitor = ViewModelProvider(this)[ForegroundArrivalMonitor::class.java]
        reminderStore = ViewModelProvider(this)[ReminderStore::class.java]
        reminderRangeMetres = ReminderRange.normalise(
            getSharedPreferences("reminder_settings", MODE_PRIVATE).getInt("radius_metres", ReminderRange.DEFAULT_METRES)
        )
        locationCapture = CurrentLocationCapture(this)
        setContent {
            GMinderTheme(darkTheme = useDarkTheme) {
                val profilePreferences = remember { getSharedPreferences("local_profile", MODE_PRIVATE) }
                var profileName by remember { mutableStateOf(ProfileName.validated(profilePreferences.getString("name", "").orEmpty())) }
                val saveProfile: (String) -> Unit = { name ->
                    profilePreferences.edit().putString("name", name).apply()
                    profileName = name
                }
                if (profileName == null) {
                    WelcomeFlow(onProfileCreated = saveProfile)
                    return@GMinderTheme
                }
                val name = profileName.orEmpty()
                val reminders = reminderStore.reminders
                if (reminders == null) {
                    Page("My reminders") {
                        Text(reminderStore.loadError ?: "Loading saved reminders…")
                        if (reminderStore.loadError != null) Button(onClick = reminderStore::load) { Text("Try again") }
                    }
                    return@GMinderTheme
                }
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
                LaunchedEffect(reminders, reminderRangeMetres) {
                    arrivalMonitor.updateTargets(reminders.mapNotNull { ArrivalTarget.fromReminder(it) }, reminderRangeMetres)
                }
                val arrival = arrivalMonitor.alerts.firstOrNull()
                if (arrival != null && arrivalMonitor.enabled && arrivalMonitor.foreground && arrivalMonitor.fix != null) {
                    LaunchedEffect(arrival.target.key) { arrivalMonitor.markShown(arrival.target.key) }
                    AlertDialog(
                        onDismissRequest = arrivalMonitor::dismissFirst,
                        title = { Text("You're nearby") },
                        text = { Text("${arrival.target.title}\n\nAbout ${Math.ceil(arrival.distanceMetres).toLong()} metres away, within your $reminderRangeMetres-metre range.") },
                        confirmButton = { TextButton(enabled = !reminderStore.saving, onClick = {
                            reminderStore.setCompleted(arrival.target.id, true, arrivalMonitor::dismissFirst)
                        }) { Text("Mark completed") } },
                        dismissButton = { TextButton(onClick = arrivalMonitor::dismissFirst) { Text("Later") } }
                    )
                }
                val goBack = {
                    screen = when (screen) {
                        "map" -> "location"
                        "location" -> "create"
                        "create" -> "reminders"
                        "intro" -> "about"
                        else -> "home"
                    }
                }
                val newReminder = {
                    title = ""; note = ""; place = ""; attempted = false; screen = "create"
                }
                val drawer = rememberDrawerState(DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                BackHandler(screen != "home" && !drawer.isOpen, onBack = goBack)
                BackHandler(drawer.isOpen) { scope.launch { drawer.close() } }
                ModalNavigationDrawer(
                    drawerState = drawer,
                    gesturesEnabled = screen != "map",
                    drawerContent = { AppDrawer(name, screen) { route -> screen = route; scope.launch { drawer.close() } } }
                ) {
                    if (screen == "intro") {
                        WelcomeFlow(existingName = name, onReturn = { screen = "about" }, onProfileCreated = saveProfile)
                    } else Page(when (screen) { "home" -> "Home"; "create" -> "Create Reminder"; "location" -> "Choose Location"; "map" -> "Pin on Map"; "settings" -> "Settings"; "nearby" -> "Nearby reminders"; "profile" -> "My profile"; "about" -> "About G-Minder"; else -> "My Reminders" },
                        back = if (screen == "home") null else goBack,
                        onMenu = { scope.launch { drawer.open() } }, showTitle = screen != "home") {
                        reminderStore.writeError?.let { message ->
                            Text(message, color = MaterialTheme.colorScheme.error)
                            TextButton(onClick = reminderStore::dismissError) { Text("Dismiss") }
                        }
                        reminderStore.loadError?.let { message ->
                            Text(message, color = MaterialTheme.colorScheme.error)
                            TextButton(onClick = reminderStore::load) { Text("Try again") }
                        }
                        when (screen) {
                            "home" -> {
                                HomeScreen(
                                    name = name,
                                    activeCount = reminders.count { !it.completed },
                                    completedCount = reminders.count { it.completed },
                                    onViewReminders = { completedOnly = it; screen = "reminders" },
                                    onNewReminder = newReminder,
                                    onActivity = { suggested -> title = suggested; note = ""; place = ""; attempted = false; screen = "create" }
                                )
                            }
                            "reminders" -> {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(!completedOnly, { completedOnly = false }, { Text("Active") })
                                    FilterChip(completedOnly, { completedOnly = true }, { Text("Completed") })
                                }
                                val visible = reminders.filter { it.completed == completedOnly }
                                if (visible.isEmpty()) Text(if (completedOnly) "No completed reminders yet." else "No active reminders. Create one to get started.")
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
                            "settings" -> SettingsScreen(lightLevel, hasLightSensor, lightSensorActive, adaptToLight, useDarkTheme, ::setLightAdaptation, reminderRangeMetres, ::setReminderRange)
                            "nearby" -> NearbyScreen(arrivalMonitor, reminderRangeMetres, ::setArrivalMonitoring, { setArrivalMonitoring(true) }, { screen = "settings" })
                            "profile" -> ProfileForm(name = name, onSave = saveProfile)
                            "about" -> AboutScreen { screen = "intro" }
                            "create" -> {
                                Text("What would you like to remember?", style = MaterialTheme.typography.titleLarge)
                                OutlinedTextField(title, { title = it.take(100) }, label = { Text("Title *") }, singleLine = true, isError = attempted && title.isBlank(), modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(note, { note = it.take(500) }, label = { Text("Notes (optional)") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                                OutlinedButton(onClick = { screen = "location" }, modifier = Modifier.fillMaxWidth()) { Text(place.ifBlank { "Choose a location *" }) }
                                if (attempted && (title.isBlank() || place.isBlank())) Text("Enter a title and choose a location.", color = MaterialTheme.colorScheme.error)
                                Text("Reminders are saved on this phone. Nearby checks work while the app is open.", style = MaterialTheme.typography.bodySmall)
                                Button(enabled = !reminderStore.saving, onClick = {
                                    attempted = true
                                    if (title.isNotBlank() && place.isNotBlank()) {
                                        reminderStore.add(title, note, place) {
                                            completedOnly = false; screen = "reminders"
                                        }
                                    }
                                }, modifier = Modifier.fillMaxWidth()) { Text(if (reminderStore.saving) "Saving…" else "Save reminder") }
                            }
                            "location" -> {
                                Text("Pick a place", style = MaterialTheme.typography.headlineMedium)
                                OutlinedButton(onClick = { screen = "map" }, modifier = Modifier.fillMaxWidth()) { Text("Search places or pin on map") }
                                Text("Selected location: ${place.ifBlank { "None yet" }}")
                                if (place.isNotBlank()) {
                                    Button(onClick = { screen = "create" }, modifier = Modifier.fillMaxWidth()) { Text("Use this location") }
                                    OutlinedButton(onClick = { openMaps(place) }, modifier = Modifier.fillMaxWidth()) { Text("Preview in Maps") }
                                }
                                CurrentLocationCard(locationCapture, ::requestCurrentLocation) { place = it }
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
}

@Composable
fun Page(title: String, back: (() -> Unit)? = null, onMenu: (() -> Unit)? = null, showTitle: Boolean = true, content: @Composable ColumnScope.() -> Unit) {
    val scrollState = key(title) { rememberScrollState() }
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.safeDrawingPadding().imePadding().verticalScroll(scrollState).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (onMenu != null) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onMenu, modifier = Modifier.semantics { contentDescription = "Open menu" }) { ActivityGlyph(ActivityIcon.Menu) }
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { Text("G-MINDER", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge) }
                    Spacer(Modifier.width(48.dp))
                }
            } else Text("G-MINDER", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            if (back != null) TextButton(onClick = back) { Text("‹ Back") }
            if (showTitle) Text(title, style = MaterialTheme.typography.headlineLarge)
            content()
        }
    }
}

class ReminderDetailsActivity : LightAwareActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = ViewModelProvider(this)[ReminderStore::class.java]
        val reminderId = intent.getStringExtra("id").orEmpty()
        setContent {
            GMinderTheme(darkTheme = useDarkTheme) {
                Page("Reminder Details", back = { finish() }) {
                    store.loadError?.let { message ->
                        Text(message, color = MaterialTheme.colorScheme.error)
                        Button(onClick = store::load) { Text("Try again") }
                    }
                    val reminders = store.reminders
                    if (reminders == null) {
                        if (store.loadError == null) Text("Loading saved reminder…")
                        return@Page
                    }
                    val reminder = reminders.firstOrNull { it.id == reminderId }
                    if (reminder == null) { Text("This reminder is no longer available."); return@Page }
                    store.writeError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Text(reminder.title, style = MaterialTheme.typography.headlineMedium)
                    SuggestionChip(onClick = {}, label = { Text(if (reminder.completed) "Completed" else "Active") })
                    Text("LOCATION", style = MaterialTheme.typography.labelLarge)
                    Text(reminder.place, style = MaterialTheme.typography.titleLarge)
                    Text("NOTES", style = MaterialTheme.typography.labelLarge)
                    Text(reminder.note.ifBlank { "No notes added." })
                    OutlinedButton(onClick = { openMaps(reminder.place) }, modifier = Modifier.fillMaxWidth()) { Text("Open in Maps") }
                    Button(enabled = !store.saving, onClick = {
                        store.setCompleted(reminder.id, !reminder.completed) { finish() }
                    }, modifier = Modifier.fillMaxWidth()) { Text(if (store.saving) "Saving…" else if (reminder.completed) "Mark active" else "Mark completed") }
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
