package com.griffith.gminder

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import java.io.ByteArrayInputStream
import java.util.concurrent.atomic.AtomicBoolean

private class PickerWebView(context: Context) : WebView(context) {
    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        if (width > 0 && height > 0) post {
            evaluateJavascript("window.refreshMapSize && window.refreshMapSize();", null)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        parent?.requestDisallowInterceptTouchEvent(event.action != MotionEvent.ACTION_UP && event.action != MotionEvent.ACTION_CANCEL)
        return super.onTouchEvent(event)
    }
}

internal class MapBridge(private val selectPoint: (Double, Double) -> Unit, private val tilesChanged: (Boolean) -> Unit) {
    @JavascriptInterface fun select(latitude: Double, longitude: Double) = selectPoint(latitude, longitude)
    @JavascriptInterface fun tilesAvailable(available: Boolean) = tilesChanged(available)
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MapLocationPicker(initialLocation: LocationFix?, onChoose: (String) -> Unit) {
    val context = LocalContext.current
    var selectedQuery by rememberSaveable { mutableStateOf<String?>(null) }
    var tilesReady by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Loading map… Internet access is needed for map tiles.") }
    val alive = remember { AtomicBoolean(true) }
    val handler = remember { Handler(Looper.getMainLooper()) }
    DisposableEffect(Unit) { onDispose { alive.set(false) } }
    Text("Pan or zoom, then tap to drop or move a pin.")
    Text("Online map by OpenStreetMap. Viewed areas are sent to the map provider; no location permission is needed.", style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
    Text(status)
    AndroidView(
        modifier = Modifier.fillMaxWidth().height(360.dp),
        factory = { context ->
            PickerWebView(context).apply {
                settings.javaScriptEnabled = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.setGeolocationEnabled(false)
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                settings.userAgentString = "G-Minder/1.0 (+https://github.com/Carlos-0620/g-minder-android)"
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String) {
                        view.evaluateJavascript("window.refreshMapSize && window.refreshMapSize();", null)
                    }
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        if (request.url.toString() == "https://www.openstreetmap.org/copyright") {
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, request.url)) }
                        }
                        return true
                    }

                    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                        val uri = request.url
                        val bundled = uri.scheme == "file" && uri.path?.startsWith("/android_asset/map/") == true
                        val tile = uri.scheme == "https" && uri.host == "tile.openstreetmap.org" && uri.path?.endsWith(".png") == true
                        return if (bundled || tile) null else WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0)))
                    }
                }
                addJavascriptInterface(MapBridge(
                    selectPoint = { latitude, longitude ->
                        val point = LocationFix.from(latitude, longitude, null, 0L)
                        handler.post { if (alive.get() && point != null) selectedQuery = point.mapQuery }
                    },
                    tilesChanged = { available -> handler.post {
                        if (alive.get()) {
                            tilesReady = available
                            status = if (available) "Tap the map to choose your reminder location." else "Some map tiles could not load. Check internet access or go back to use coordinates or sample places."
                        }
                    } }
                ), "GMinderMap")
                val center = selectedQuery?.let { LocationFix.fromMapQuery(it) } ?: initialLocation ?: LocationFix.from(53.3498, -6.2603, null, 0L)!!
                val url = Uri.parse("file:///android_asset/map/picker.html").buildUpon()
                    .appendQueryParameter("lat", center.latitude.toString())
                    .appendQueryParameter("lon", center.longitude.toString())
                    .appendQueryParameter("selected", (selectedQuery != null).toString()).build()
                loadUrl(url.toString())
            }
        },
        onRelease = { it.removeJavascriptInterface("GMinderMap"); it.stopLoading(); it.destroy() }
    )
    TextButton(onClick = {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.openstreetmap.org/copyright"))) }
    }) { Text("© OpenStreetMap contributors") }
    Text(selectedQuery?.let { "Pinned location: $it" } ?: "No pin selected yet. The initial map centre is only a starting point.")
    Button(onClick = { selectedQuery?.let(onChoose) }, enabled = tilesReady && selectedQuery != null, modifier = Modifier.fillMaxWidth()) {
        Text("Use pinned location")
    }
}
