package com.eightharsh.eightharsh.core.data.personal

import com.eightharsh.eightharsh.core.domain.personal.QuickNote
import org.junit.Assert.*
import org.junit.Test

class NotesCodecTest {
    @Test fun `notes preserve unicode checklist and notification state across restarts`() {
        val notes = listOf(QuickNote("a", "Task \"one\"\nहैलो", false, 1900000000000L, true), QuickNote("b", "Done", true))
        assertEquals(notes, decodeNotes(encodeNotes(notes)))
    }
    @Test fun `missing optional fields are backward compatible`() {
        assertEquals(listOf(QuickNote("a", "Task")), decodeNotes("""[{"id":"a","text":"Task"}]"""))
    }
    @Test fun `duplicate identities are rejected before restore`() {
        assertThrows(IllegalArgumentException::class.java) { decodeNotes("""[{"id":"a","text":"One"},{"id":"a","text":"Two"}]""") }
    }
    @Test fun `empty note is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { decodeNotes("""[{"id":"a","text":"  "}]""") }
    }
    @Test fun `invalid reminder timestamp is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { decodeNotes("""[{"id":"a","text":"Task","dueAt":-1}]""") }
    }
}
