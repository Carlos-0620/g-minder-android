package com.griffith.gminder

internal object AmbientLightTheme {
    fun isDark(lux: Float, previouslyDark: Boolean): Boolean = when {
        !lux.isFinite() || lux < 0f -> previouslyDark
        lux < 30f -> true
        lux > 80f -> false
        else -> previouslyDark
    }
}
