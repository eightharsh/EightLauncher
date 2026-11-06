package com.eightharsh.eightharsh.core.domain.personal

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class ShuffleTest {
    @Test fun `empty and single favorites remain usable`() {
        assertEquals(emptyList<String>(), shuffledDifferently(emptyList<String>()))
        assertEquals(listOf("a"), shuffledDifferently(listOf("a")))
    }
    @Test fun `every unlock changes order without losing or duplicating favorites`() {
        var previous = listOf("a", "b", "c", "d", "e")
        repeat(100) { seed ->
            val next = shuffledDifferently(previous, Random(seed))
            assertEquals(previous.toSet(), next.toSet())
            assertEquals(previous.size, next.size)
            assertNotEquals(previous, next)
            previous = next
        }
    }
    @Test fun `two favorites always swap`() {
        repeat(20) { assertEquals(listOf("b", "a"), shuffledDifferently(listOf("a", "b"), Random(it))) }
    }
}
