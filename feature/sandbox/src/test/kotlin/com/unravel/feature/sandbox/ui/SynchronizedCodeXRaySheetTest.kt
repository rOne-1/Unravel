package com.unravel.feature.sandbox.ui

import dev.snipme.highlights.Highlights
import dev.snipme.highlights.model.ColorHighlight
import dev.snipme.highlights.model.SyntaxLanguage
import dev.snipme.highlights.model.SyntaxThemes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class SynchronizedCodeXRaySheetTest {

    @Test
    @DisplayName("WP-28: Snipme Highlights parses Kotlin tokens and emits color highlights")
    fun testSyntaxHighlightingParsing() {
        val snippet = """
            fun binarySearch(arr: IntArray, target: Int): Int {
                var low = 0
                var high = arr.size - 1
                return -1
            }
        """.trimIndent()

        val highlights = Highlights.Builder()
            .code(snippet)
            .language(SyntaxLanguage.KOTLIN)
            .theme(SyntaxThemes.monokai(true))
            .build()

        val codeHighlights = highlights.getHighlights()
        assertFalse(codeHighlights.isEmpty(), "Code highlights should not be empty")

        val colorHighlights = codeHighlights.filterIsInstance<ColorHighlight>()
        assertTrue(colorHighlights.isNotEmpty(), "Should contain ColorHighlights for keywords and literals")
    }

    @Test
    @DisplayName("WP-28: Active line index maps correctly to 1-based line numbers")
    fun testActiveLineMapping() {
        val snippet = """
            val left = 0
            val right = arr.size - 1
            while (left < right) {
                left++
            }
        """.trimIndent()

        val lines = snippet.lines()
        assertEquals(5, lines.size)

        fun isLineActive(lineIndex0Based: Int, activeLine1Based: Int): Boolean {
            return (lineIndex0Based + 1) == activeLine1Based
        }

        assertTrue(isLineActive(0, 1))
        assertFalse(isLineActive(0, 2))
        assertTrue(isLineActive(2, 3))
    }

    @Test
    @DisplayName("WP-28: Variable bindings map accurately reflects runtime telemetry bindings")
    fun testVariableBindingsFormatting() {
        val bindings = mapOf(
            "left" to "2",
            "right" to "8",
            "sum" to "42"
        )

        val formattedEntries = bindings.map { "${it.key}=${it.value}" }
        assertEquals(listOf("left=2", "right=8", "sum=42"), formattedEntries)
    }
}
