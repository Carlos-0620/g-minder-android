package com.griffith.gminder

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler

@Composable
fun WelcomeFlow(existingName: String? = null, onReturn: () -> Unit = {}, onProfileCreated: (String) -> Unit) {
    var creating by rememberSaveable { mutableStateOf(false) }
    BackHandler(creating) { creating = false }
    if (creating) {
        Page("Make it yours", back = { creating = false }) { ProfileForm("", true, onProfileCreated) }
    } else {
        Surface(Modifier.fillMaxSize()) {
            Column(
                Modifier.safeDrawingPadding().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp, vertical = 48.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)) {
                    Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) { ActivityGlyph(ActivityIcon.Place, Modifier.size(44.dp)) }
                }
                Text("G-Minder", style = MaterialTheme.typography.headlineLarge)
                Text("Remember the\nlittle things.", style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
                Text("Keep everyday tasks linked to the places in your life.", textAlign = TextAlign.Center)
                Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                    ActivityGlyph(ActivityIcon.Groceries); ActivityGlyph(ActivityIcon.Coffee); ActivityGlyph(ActivityIcon.Library)
                }
                Button(onClick = { if (existingName == null) creating = true else onReturn() }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text(if (existingName == null) "Get started" else "Back to app") }
                Text("Set up a local profile on this phone. No online account required.", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
fun ProfileForm(name: String, firstUse: Boolean = false, onSave: (String) -> Unit) {
    var draft by rememberSaveable(name) { mutableStateOf(name) }
    var attempted by rememberSaveable { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current
    val save = {
        attempted = true
        ProfileName.validated(draft)?.let { keyboard?.hide(); onSave(it); saved = true }
    }
    Text(if (firstUse) "What should we call you?" else "Your details", style = MaterialTheme.typography.titleLarge)
    OutlinedTextField(
        value = draft, onValueChange = { draft = it.take(ProfileName.MAX_LENGTH); saved = false },
        label = { Text("Your name") }, singleLine = true,
        isError = attempted && ProfileName.validated(draft) == null,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { save() }),
        modifier = Modifier.fillMaxWidth()
    )
    if (attempted && ProfileName.validated(draft) == null) Text("Enter a name to continue.", color = MaterialTheme.colorScheme.error)
    Text("This profile stays on this phone. It is not an online account or a security lock.", style = MaterialTheme.typography.bodySmall)
    Button(onClick = { save() }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text(if (firstUse) "Continue to Home" else "Save profile") }
    if (saved && !firstUse) Text("Profile saved.", style = MaterialTheme.typography.bodySmall)
}
