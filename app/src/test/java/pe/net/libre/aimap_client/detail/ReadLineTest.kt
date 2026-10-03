package pe.net.libre.aimap_client.detail

import org.junit.Assert.assertEquals
import org.junit.Test

/** The line that says how many letters went into the draft. */
class ReadLineTest {
    @Test
    fun one_message_is_the_letter_in_hand() {
        assertEquals("READ THIS LETTER ONLY", readLine(1))
    }

    @Test
    fun a_draft_with_no_used_ids_still_reads() {
        assertEquals("READ THIS LETTER ONLY", readLine(0))
    }

    @Test
    fun two_messages_name_the_one_behind() {
        assertEquals("READ THIS LETTER AND THE ONE BEHIND IT", readLine(2))
    }

    @Test
    fun more_messages_count_the_ones_behind() {
        assertEquals("READ THIS LETTER AND THE 3 BEHIND IT", readLine(4))
    }
}
