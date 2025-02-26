package io.github.xmlpullkmp

import io.github.xmlpullkmp.utils.arraycopy
import kotlin.test.Test
import kotlin.test.assertContentEquals

class UtilsTestCommon {

    @Test
    fun `test arraycopy with valid input`() {
        val src = arrayOf(1, 2, 3, 4, 5)
        val dest = arrayOf(0, 0, 0, 0, 0)
        arraycopy(src, 1, dest, 2, 3)
        assertContentEquals(arrayOf(0, 0, 2, 3, 4), dest)
    }

    @Test
    fun `test arraycopy with overlapping ranges`() {
        val src = arrayOf(1, 2, 3, 4, 5)
        val dest = arrayOf(1, 2, 3, 4, 5)
        arraycopy(src, 1, dest, 2, 3)
        assertContentEquals(arrayOf(1, 2, 2, 3, 4), dest)
    }

    @Test
    fun `test arraycopy with zero length`() {
        val src = arrayOf(1, 2, 3, 4, 5)
        val dest = arrayOf(0, 0, 0, 0, 0)
        arraycopy(src, 1, dest, 2, 0)
        assertContentEquals(arrayOf(0, 0, 0, 0, 0), dest)
    }

    @Test
    fun `test arraycopy with full copy`() {
        val src = arrayOf(1, 2, 3, 4, 5)
        val dest = arrayOf(0, 0, 0, 0, 0)
        arraycopy(src, 0, dest, 0, 5)
        assertContentEquals(arrayOf(1, 2, 3, 4, 5), dest)
    }

    @Test
    fun `test arraycopy with different types`() {
        val src = arrayOf("a", "b", "c", "d", "e")
        val dest = arrayOf("", "", "", "", "")
        arraycopy(src, 1, dest, 2, 3)
        assertContentEquals(arrayOf("", "", "b", "c", "d"), dest)
    }
}