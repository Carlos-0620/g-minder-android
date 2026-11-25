package com.griffith.gminder

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

enum class ActivityIcon(val outline: String) {
    Groceries("M3 6H7L10 22H25 M8 10H28L25 18H10 M14 27A2 2 0 1 1 10 27A2 2 0 1 1 14 27 M26 27A2 2 0 1 1 22 27A2 2 0 1 1 26 27"),
    Medication("M8 24C1 17 17 1 24 8C31 15 15 31 8 24Z M11 11L21 21"),
    Coffee("M5 11H23V20Q23 26 17 26H11Q5 26 5 20Z M23 13H26Q30 17 26 21H23 M4 29H26 M10 4V7 M17 3V7"),
    Exercise("M10 16H22 M6 9H10V23H6Z M22 9H26V23H22Z M3 12V20 M29 12V20"),
    Library("M4 7H10V28H4Z M13 4H19V28H13Z M22 7L28 27L31 26L25 6Z M4 12H10 M13 10H19"),
    Work("M5 10H27Q29 10 29 12V26Q29 28 27 28H5Q3 28 3 26V12Q3 10 5 10Z M11 10V5H21V10 M3 17H13 M19 17H29 M13 15H19V20H13Z"),
    Place("M16 30C13 25 5 18 5 12C5 -1 27 -1 27 12C27 18 19 25 16 30Z M20 11A4 4 0 1 1 12 11A4 4 0 1 1 20 11"),
    Menu("M5 9H27 M5 16H27 M5 23H27"),
    Home("M3 14L16 3L29 14 M6 12V28H26V12 M13 28V19H19V28"),
    Profile("M21 10A5 5 0 1 1 11 10A5 5 0 1 1 21 10 M5 28C5 15 27 15 27 28"),
    Settings("M4 8H8 M14 8H28 M4 16H19 M25 16H28 M4 24H10 M16 24H28 M14 8A3 3 0 1 1 8 8A3 3 0 1 1 14 8 M25 16A3 3 0 1 1 19 16A3 3 0 1 1 25 16 M16 24A3 3 0 1 1 10 24A3 3 0 1 1 16 24"),
    About("M28 16A12 12 0 1 1 4 16A12 12 0 1 1 28 16 M16 14V23 M16 9V9.5")
}

data class ActivityShortcut(val icon: ActivityIcon, val description: String, val suggestedTitle: String)
val activityShortcuts = listOf(
    ActivityShortcut(ActivityIcon.Groceries, "Groceries", "Buy groceries"),
    ActivityShortcut(ActivityIcon.Medication, "Medication", "Collect medication"),
    ActivityShortcut(ActivityIcon.Coffee, "Coffee", "Buy coffee beans"),
    ActivityShortcut(ActivityIcon.Exercise, "Exercise", "Go for a walk"),
    ActivityShortcut(ActivityIcon.Library, "Library", "Return library books"),
    ActivityShortcut(ActivityIcon.Work, "Work", "Check work rota")
)

@Composable
fun ActivityGlyph(icon: ActivityIcon, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    val path = remember(icon) { PathParser().parsePathString(icon.outline).toPath() }
    Canvas(modifier.size(28.dp)) {
        scale(size.width / 32f, size.height / 32f, pivot = Offset.Zero) {
            drawPath(path, color, style = Stroke(1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}
