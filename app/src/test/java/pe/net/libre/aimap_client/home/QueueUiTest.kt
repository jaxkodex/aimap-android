package pe.net.libre.aimap_client.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QueueUiTest {
    private val now = SampleHome.now
    private val q = SampleHome.home.toUi(now, accounts = 3, initials = "AR").toQueue()

    @Test
    fun the_queue_is_act_now_then_waiting_in_api_order() {
        assertEquals(listOf(1L, 2L, 3L, 4L, 5L), q.steps.map { it.messageId })
        assertEquals(
            listOf(Bay.NeedsYou, Bay.NeedsYou, Bay.Waiting, Bay.Waiting, Bay.Waiting),
            q.steps.map { it.bay },
        )
        assertEquals(listOf(9, 7, 5, 4, 3), q.piles.map { it.count })
        assertEquals(28, q.canGo)
    }

    @Test
    fun the_head_counts_the_work_then_what_is_left_over() {
        assertEquals("5 to work, then clear 28", q.head)
        assertEquals("One at a time. Most urgent first.", q.note)
        assertEquals("2 need you · 3 waiting on you", q.legend)
        assertEquals("28 can go", q.canGoLegend)
    }

    @Test
    fun with_no_piles_the_head_only_counts_the_work() {
        assertEquals("5 to work", queueHead(5, 0))
        assertEquals("1 to work", queueHead(1, 0))
    }

    @Test
    fun with_nothing_to_work_the_head_says_so() {
        assertEquals("All worked. 28 can go.", queueHead(0, 28))
        assertEquals("All worked. 1 can go.", queueHead(0, 1))
        assertEquals("Nothing needs you. Clear the piles when you're ready.", queueNote(0, 28, "sorted 14:03"))
    }

    @Test
    fun an_empty_bay_is_all_clear_and_reports_the_sort() {
        val quiet = SampleHome.clear.toUi(now, accounts = 1, initials = "AR").toQueue()
        assertEquals("All clear.", quiet.head)
        assertEquals("Nothing in the queue · sorted 14:03", quiet.note)
        assertEquals(emptyList<StripUi>(), quiet.steps)
        assertNull(quiet.legend)
        assertNull(quiet.canGoLegend)
        assertEquals("Nothing in the queue.", queueNote(0, 0, null))
    }

    @Test
    fun a_bay_at_zero_drops_out_of_the_legend() {
        assertEquals("3 waiting on you", queueLegend(0, 3))
        assertEquals("2 need you", queueLegend(2, 0))
        assertEquals("1 needs you", queueLegend(1, 0))
        assertEquals("1 needs you · 1 waiting on you", queueLegend(1, 1))
        assertNull(queueLegend(0, 0))
    }

    @Test
    fun the_now_head_names_the_bay_of_the_message_in_hand() {
        assertEquals("Now · Needs you", nowTitle(Bay.NeedsYou))
        assertEquals("Now · Waiting on you", nowTitle(Bay.Waiting))
    }

    @Test
    fun the_position_counts_from_one() {
        assertEquals("1 / 5", queuePosition(0, 5))
        assertEquals("5 / 5", queuePosition(4, 5))
    }

    @Test
    fun the_pile_sheet_says_what_the_bar_below_it_does() {
        assertEquals(
            "Tap a pile to look inside or keep something. Hold the bar below to archive all 28.",
            pileNote(28),
        )
    }

    @Test
    fun the_message_in_hand_carries_every_stored_reason() {
        assertEquals(
            listOf("Mentions a deadline", "Asks you to do something", "Written to you by a person"),
            q.steps.first().reasons,
        )
        assertTrue(q.steps.any { it.cocked })
    }
}
