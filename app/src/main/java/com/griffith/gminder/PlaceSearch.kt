package com.griffith.gminder

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.Future

internal class PlaceLookup {
    private val handler = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor { task -> Thread(task, "Place lookup").apply { isDaemon = true } }
    @Volatile private var generation = 0
    @Volatile private var closed = false
    @Volatile private var connection: HttpURLConnection? = null
    private var job: Future<*>? = null
    private var timeout: Runnable? = null
    private var lastRequest = -2_000L
    private val cache = linkedMapOf<String, List<PlaceResult>>()

    var loading by mutableStateOf(false)
        private set
    var results by mutableStateOf<List<PlaceResult>>(emptyList())
        private set
    var message by mutableStateOf<String?>(null)
        private set

    fun cancel() {
        generation++
        timeout?.let(handler::removeCallbacks)
        timeout = null
        job?.cancel(true)
        job = null
        connection?.disconnect()
        connection = null
        loading = false
        results = emptyList()
        message = null
    }

    fun search(query: String) {
        cancel()
        val text = query.trim()
        if (text.length < 3) { message = "Enter at least 3 characters."; return }
        val cacheKey = text.lowercase(Locale.ROOT)
        cache[cacheKey]?.let { results = it; showResultMessage(); return }
        val now = SystemClock.elapsedRealtime()
        if (now - lastRequest < 1_200L) { message = "Please wait a moment before searching again."; return }
        lastRequest = now
        val request = generation
        loading = true
        message = "Searching…"
        timeout = Runnable {
            if (!closed && request == generation) {
                cancel()
                message = "Search timed out. Check your connection and try again, or choose a map pin."
            }
        }.also { handler.postDelayed(it, 15_000L) }
        job = worker.submit {
            var active: HttpURLConnection? = null
            try {
                if (closed || request != generation) return@submit
                val url = URL("https://photon.komoot.io/api/?q=${URLEncoder.encode(text, "UTF-8")}&limit=8&lang=en")
                active = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 6_000
                    readTimeout = 8_000
                    instanceFollowRedirects = false
                    setRequestProperty("User-Agent", "G-Minder/1.0 (+https://github.com/Carlos-0620/g-minder-android)")
                    setRequestProperty("Accept", "application/json")
                }
                connection = active
                if (closed || request != generation) return@submit
                val responseCode = active.responseCode
                if (responseCode != 200) throw IOException(if (responseCode == 429) "busy" else "network")
                val bytes = active.inputStream.use { it.readBytesLimited(512_000) }
                val matches = PlaceResults.parse(bytes.toString(Charsets.UTF_8))
                handler.post {
                    if (closed || request != generation) return@post
                    finishRequest()
                    cache[cacheKey] = matches
                    if (cache.size > 20) cache.remove(cache.keys.first())
                    results = matches
                    showResultMessage()
                }
            } catch (error: Exception) {
                handler.post {
                    if (closed || request != generation) return@post
                    finishRequest()
                    message = if (error.message == "busy") "Search is busy. Please wait and try again."
                        else "Search could not connect. Check mobile data or Wi-Fi and try again."
                }
            } finally {
                active?.disconnect()
                if (connection === active) connection = null
            }
        }
    }

    private fun finishRequest() {
        timeout?.let(handler::removeCallbacks)
        timeout = null
        generation++
        loading = false
    }

    private fun showResultMessage() {
        message = when (results.size) {
            0 -> "No places found. Try adding a town or street, or choose a map pin."
            1 -> "1 matching place. Check the address before choosing it, or refine your search."
            else -> "${results.size} matching places. Choose the branch or address you want."
        }
    }

    fun close() { closed = true; cancel(); worker.shutdownNow() }
}

private fun java.io.InputStream.readBytesLimited(limit: Int): ByteArray {
    val output = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(8_192)
    while (true) {
        val count = read(buffer)
        if (count == -1) break
        if (output.size() + count > limit) throw IOException("Response too large")
        output.write(buffer, 0, count)
    }
    return output.toByteArray()
}

@Composable
fun PlaceSearch(onSelect: (PlaceResult) -> Unit) {
    val lookup = remember { PlaceLookup() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val focus = LocalFocusManager.current
    var query by rememberSaveable { mutableStateOf("") }
    var selectedKey by rememberSaveable { mutableStateOf<String?>(null) }
    DisposableEffect(lookup, lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) lookup.cancel() }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); lookup.close() }
    }
    OutlinedTextField(query, { query = it.take(160); lookup.cancel(); selectedKey = null }, label = { Text("Search for a place") },
        placeholder = { Text("Tesco Dublin") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    Button(onClick = { focus.clearFocus(); selectedKey = null; lookup.search(query) }, enabled = query.trim().length >= 3 && !lookup.loading,
        modifier = Modifier.fillMaxWidth()) { Text(if (lookup.loading) "Searching…" else "Search places") }
    Text("Search by Photon · © OpenStreetMap contributors. Search text is shared with this service; internet is needed.", style = MaterialTheme.typography.bodySmall)
    lookup.message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    if (lookup.results.isNotEmpty()) {
        Column(Modifier.fillMaxWidth().heightIn(max = 320.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            lookup.results.forEach { result ->
                OutlinedCard(onClick = { selectedKey = result.key; onSelect(result); focus.clearFocus() }, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(result.name, style = MaterialTheme.typography.titleMedium)
                            if (result.address.isNotBlank()) Text(result.address, style = MaterialTheme.typography.bodySmall)
                        }
                        RadioButton(selected = selectedKey == result.key, onClick = null)
                    }
                }
            }
        }
        Text("Selecting a result previews its pin. Confirm the location below the map.", style = MaterialTheme.typography.bodySmall)
    }
}
