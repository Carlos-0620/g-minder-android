package com.griffith.gminder

internal object ProfileName {
    const val MAX_LENGTH = 40
    fun validated(value: String): String? = value.trim().takeIf {
        it.isNotEmpty() && it.length <= MAX_LENGTH && it.none(Char::isISOControl)
    }
}
