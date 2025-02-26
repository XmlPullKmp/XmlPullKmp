package io.github.xmlpullkmp.utils

import io.github.xmlpullkmp.codePoints.CodePoints

/**
 * Converts a code point (an integer) to a char array, similar to `Character.toChars` in Java.
 * This function works on JVM and multiplatform Kotlin projects.
 */
fun Char.Companion.toChars(codePoint: Int): CharArray {
    return when {
        // If the code point is a basic multilingual plane (BMP) character
        CodePoints.isBmpCodePoint(codePoint) -> charArrayOf(codePoint.toChar())

        // If the code point is a supplementary character
        CodePoints.isSupplementaryCodePoint(codePoint) -> {
            val highSurrogate = CodePoints.highSurrogate(codePoint)
            val lowSurrogate = CodePoints.lowSurrogate(codePoint)
            charArrayOf(highSurrogate, lowSurrogate)
        }

        // For invalid code points
        else -> throw IllegalArgumentException("Invalid Unicode code point: $codePoint")
    }
}
