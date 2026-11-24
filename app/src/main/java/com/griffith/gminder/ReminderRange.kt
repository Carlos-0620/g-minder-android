package com.griffith.gminder

import kotlin.math.roundToInt

internal object ReminderRange {
    const val MIN_METRES = 100
    const val MAX_METRES = 1000
    const val STEP_METRES = 100
    const val DEFAULT_METRES = 200

    fun normalise(metres: Int): Int =
        (metres.coerceIn(MIN_METRES, MAX_METRES).toFloat() / STEP_METRES).roundToInt() * STEP_METRES
}
