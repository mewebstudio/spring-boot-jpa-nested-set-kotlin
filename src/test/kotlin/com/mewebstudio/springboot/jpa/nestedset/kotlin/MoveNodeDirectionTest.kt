package com.mewebstudio.springboot.jpa.nestedset.kotlin

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Test for the MoveNodeDirection enum.")
internal class MoveNodeDirectionTest {
    @Test
    fun testEnumValues() {
        Assertions.assertEquals(2, MoveNodeDirection.entries.size)
        assertEquals(MoveNodeDirection.UP, MoveNodeDirection.valueOf("UP"))
        assertEquals(MoveNodeDirection.DOWN, MoveNodeDirection.valueOf("DOWN"))
    }

    @Test
    fun testEnumToString() {
        assertEquals("UP", MoveNodeDirection.UP.toString())
        assertEquals("DOWN", MoveNodeDirection.DOWN.toString())
    }

    @Test
    fun testEnumEquality() {
        assertSame(MoveNodeDirection.UP, MoveNodeDirection.valueOf("UP"))
        assertSame(MoveNodeDirection.DOWN, MoveNodeDirection.valueOf("DOWN"))
    }
}
