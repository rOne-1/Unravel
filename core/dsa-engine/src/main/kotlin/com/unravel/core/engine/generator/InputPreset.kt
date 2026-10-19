package com.unravel.core.engine.generator

import kotlinx.serialization.Serializable

@Serializable
enum class InputPreset(val displayName: String) {
    RANDOM("Random"),
    NEARLY_SORTED("Nearly Sorted"),
    REVERSED("Reversed"),
    DUPLICATES("Duplicates")
}
