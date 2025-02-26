package io.github.xmlpullkmp

import kotlin.test.Test
import kotlin.test.assertEquals

class EntityReplacementMapTestCommon {

    @Test
    fun `test entity replacement map initialization`() {
        val replacements = arrayOf(
            arrayOf("test", "value"),
            arrayOf("example", "sample")
        )
        val map = EntityReplacementMap(replacements)

        assertEquals(2, map.entityEnd)
        assertEquals("test", map.entityName[0])
        assertEquals("value", map.entityReplacement[0])
        assertEquals("example", map.entityName[1])
        assertEquals("sample", map.entityReplacement[1])
    }

    @Test
    fun `test entity replacement with existing entity`() {
        val replacements = arrayOf(
            arrayOf("test", "value"),
            arrayOf("example", "sample"),
            arrayOf("sample", "example")
        )
        val map = EntityReplacementMap(replacements)

        assertEquals(3, map.entityEnd)
        assertEquals("test", map.entityName[0])
        assertEquals("value", map.entityReplacement[0])
        assertEquals("example", map.entityName[1])
        assertEquals("sample", map.entityReplacement[1])
        assertEquals("sample", map.entityName[2])
        assertEquals("example", map.entityReplacement[2])
    }

    @Test
    fun `test entity replacement with special characters`() {
        val replacements = arrayOf(
            arrayOf("lt", "<"),
            arrayOf("gt", ">"),
            arrayOf("amp", "&")
        )
        val map = EntityReplacementMap(replacements)

        assertEquals(3, map.entityEnd)
        assertEquals("lt", map.entityName[0])
        assertEquals("<", map.entityReplacement[0])
        assertEquals("gt", map.entityName[1])
        assertEquals(">", map.entityReplacement[1])
        assertEquals("amp", map.entityName[2])
        assertEquals("&", map.entityReplacement[2])
    }

    @Test
    fun `test entity replacement with numeric entities`() {
        val replacements = arrayOf(
            arrayOf("num1", "&#1;"),
            arrayOf("num2", "&#2;")
        )
        val map = EntityReplacementMap(replacements)

        assertEquals(2, map.entityEnd)
        assertEquals("num1", map.entityName[0])
        assertEquals("&#1;", map.entityReplacement[0])
        assertEquals("num2", map.entityName[1])
        assertEquals("&#2;", map.entityReplacement[1])
    }

    @Test
    fun `test entity replacement with empty replacements`() {
        val replacements = arrayOf<Array<String>>()
        val map = EntityReplacementMap(replacements)

        assertEquals(0, map.entityEnd)
    }
}