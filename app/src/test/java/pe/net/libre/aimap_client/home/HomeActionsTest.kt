package pe.net.libre.aimap_client.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import pe.net.libre.aimap_client.api.MessageState

/** The optimistic reducer: what Handled, Later and Undo do to the Bay before the API answers. */
class HomeActionsTest {
    private val home = SampleHome.home

    @Test
    fun handled_takes_the_card_off_its_section_and_counts_it() {
        val after = home.handled(1)
        assertEquals(listOf(2L), after.actNow.map { it.messageId })
        assertEquals(home.waiting.map { it.messageId }, after.waiting.map { it.messageId })
        assertEquals(1, after.brief.actNow)
        assertEquals(3, after.brief.waiting)
        assertEquals(1, after.brief.handledToday)
    }

    @Test
    fun handled_counts_down_the_section_the_card_was_in() {
        val after = home.handled(3)
        assertEquals(listOf(4L, 5L), after.waiting.map { it.messageId })
        assertEquals(2, after.brief.actNow)
        assertEquals(2, after.brief.waiting)
    }

    @Test
    fun handling_twice_only_counts_once() {
        val after = home.handled(1).handled(1)
        assertEquals(1, after.brief.actNow)
        assertEquals(1, after.brief.handledToday)
    }

    @Test
    fun a_card_that_is_not_on_the_bay_changes_nothing() {
        assertEquals(home, home.handled(404))
        assertEquals(home, home.later(404))
        assertEquals(home, home.normal(404))
    }

    @Test
    fun later_keeps_the_card_in_its_section_at_the_end() {
        val after = home.later(3)
        assertEquals(listOf(4L, 5L, 3L), after.waiting.map { it.messageId })
        assertEquals(MessageState.Later, after.waiting.last().state)
        // It is still waiting on you, so the plate still counts it.
        assertEquals(3, after.brief.waiting)
    }

    @Test
    fun the_newest_later_sorts_last() {
        val after = home.later(3).later(5)
        assertEquals(listOf(4L, 3L, 5L), after.waiting.map { it.messageId })
        assertEquals(listOf(false, true, true), after.waiting.map { it.state == MessageState.Later })
    }

    @Test
    fun later_in_act_now_leaves_the_rest_in_priority_order() {
        val priced = home.copy(
            actNow = listOf(
                home.actNow[0].copy(priority = 0.9),
                home.actNow[1].copy(priority = 0.5),
            ),
        )
        val after = priced.later(1)
        assertEquals(listOf(2L, 1L), after.actNow.map { it.messageId })
    }

    @Test
    fun undo_puts_a_later_card_back_in_order() {
        val after = home.later(3).normal(3)
        assertEquals(home.waiting.map { it.messageId }, after.waiting.map { it.messageId })
        assertNull(after.waiting.first().state)
    }

    @Test
    fun waiting_keeps_the_longest_wait_first() {
        val shuffled = home.copy(waiting = home.waiting.reversed())
        val after = shuffled.later(4).normal(4)
        assertEquals(listOf(3L, 4L, 5L), after.waiting.map { it.messageId })
    }
}
