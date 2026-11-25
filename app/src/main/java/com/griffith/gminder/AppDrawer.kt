package com.griffith.gminder

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AppDrawer(name: String, screen: String, onNavigate: (String) -> Unit) {
    ModalDrawerSheet(Modifier.widthIn(max = 320.dp)) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("G-Minder", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 24.dp))
            Text(name, modifier = Modifier.padding(bottom = 24.dp))
            val entries = listOf(
                Triple("home", "Home", ActivityIcon.Home), Triple("reminders", "My reminders", ActivityIcon.Library),
                Triple("profile", "My profile", ActivityIcon.Profile), Triple("settings", "Settings", ActivityIcon.Settings),
                Triple("about", "About G-Minder", ActivityIcon.About)
            )
            entries.forEach { (route, label, icon) ->
                NavigationDrawerItem(label = { Text(label) }, icon = { ActivityGlyph(icon) },
                    selected = screen == route || (route == "reminders" && screen in listOf("create", "location", "map")),
                    onClick = { onNavigate(route) })
            }
        }
    }
}

@Composable
fun AboutScreen(onWelcome: () -> Unit) {
    Text("A little reminder, in the right place.", style = MaterialTheme.typography.titleLarge)
    Text("Create tasks, choose places and keep your day organised.")
    Text("This preview supports map pins and current-location capture. Automatic arrival alerts and permanent reminder storage are still in development.", style = MaterialTheme.typography.bodySmall)
    Text("Your profile is local to this phone. Online sign-in is planned for Milestone 3.", style = MaterialTheme.typography.bodySmall)
    OutlinedButton(onClick = onWelcome, modifier = Modifier.fillMaxWidth()) { Text("View introduction") }
}
