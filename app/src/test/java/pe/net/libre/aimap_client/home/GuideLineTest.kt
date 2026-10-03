package pe.net.libre.aimap_client.home

import org.junit.Assert.assertEquals
import org.junit.Test

class GuideLineTest {
    @Test
    fun all_three_bays_read_as_one_order_of_work() {
        assertEquals("Work down the list: handle 2, answer 3, then clear 28.", guideLine(2, 3, 28))
        assertEquals("Work down the list: handle 1, answer 1, then clear 1.", guideLine(1, 1, 1))
    }

    @Test
    fun a_bay_at_zero_drops_out_of_the_sentence() {
        assertEquals("Answer 3, then clear 28.", guideLine(0, 3, 28))
        assertEquals("Handle 2, then clear 28.", guideLine(2, 0, 28))
        assertEquals("Handle 2, then answer 3.", guideLine(2, 3, 0))
    }

    @Test
    fun one_job_left_is_one_clause() {
        assertEquals("Handle 2.", guideLine(2, 0, 0))
        assertEquals("Answer 3.", guideLine(0, 3, 0))
        assertEquals("Handle 1.", guideLine(1, 0, 0))
    }

    @Test
    fun nothing_needing_you_says_so_instead_of_giving_orders() {
        assertEquals("All handled. 28 can go when you're ready.", guideLine(0, 0, 28))
        assertEquals("All handled. 1 can go when you're ready.", guideLine(0, 0, 1))
    }

    @Test
    fun an_empty_bay_is_all_clear() {
        assertEquals("All clear.", guideLine(0, 0, 0))
    }
}
