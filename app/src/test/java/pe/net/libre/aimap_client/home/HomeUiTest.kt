package pe.net.libre.aimap_client.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeUiTest {
    private val now = SampleHome.now
    private val ui = SampleHome.home.toUi(now, accounts = 3, initials = "AR")

    @Test
    fun plates_and_header_come_from_the_brief() {
        assertEquals(mapOf(Bay.NeedsYou to 2, Bay.Waiting to 3, Bay.CanGo to 28), ui.plates)
        assertEquals("sorted 14:03", ui.header.lastSort)
        assertEquals(28, ui.archiveCount)
        assertEquals("Last 7 days · 3 inboxes · all strips shown", ui.footer)
    }

    @Test
    fun needs_you_strips_carry_the_bucket_word_and_grease_only_act() {
        val (act, check) = ui.needsYou
        assertEquals("ACT", act.top)
        assertTrue(act.greased)
        assertEquals("CHECK", check.top)
        assertFalse(check.greased)
        assertEquals("08:41", act.bottom)
        assertEquals("Mentions a deadline", act.reason)
        assertEquals(2, act.moreReasons)
        assertEquals("work", act.account)
        assertEquals(8, act.hourToday)
    }

    @Test
    fun waiting_strips_show_the_age_and_grease_the_longest_wait() {
        assertEquals(listOf("4d", "1d", "4h"), ui.waiting.map { it.top })
        assertEquals(listOf(true, false, false), ui.waiting.map { it.greased })
        assertEquals(listOf("Fri", "Mon", "09:14"), ui.waiting.map { it.bottom })
        assertTrue(ui.waiting.all { it.action == StripAction.Reply })
        assertNull(ui.waiting.first().hourToday)
    }

    @Test
    fun needs_review_is_cocked_and_asks_the_question() {
        val tomas = ui.waiting[1]
        assertTrue(tomas.cocked)
        assertEquals("Unsure: written to you by a person?", tomas.reason)
        assertEquals(0, tomas.moreReasons)
    }

    @Test
    fun traffic_covers_six_to_ten_with_now_and_empty_future() {
        val t = ui.traffic
        assertEquals((6..22).toList(), t.columns.map { it.hour })
        assertEquals("26 arrived since 06:00", t.total)
        val nowCol = t.columns.single { it.isNow }
        assertEquals(14, nowCol.hour)
        assertEquals("NOW", nowCol.axisLabel)
        assertTrue(t.columns.filter { it.hour > 14 }.all { it.isFuture && it.blocks.isEmpty() })
        assertEquals(listOf(Bay.Waiting) + List(5) { Bay.CanGo }, t.columns.single { it.hour == 9 }.blocks)
        assertEquals(listOf("06", "09", "12", "NOW", "15", "18", "21"), t.columns.mapNotNull { it.axisLabel })
    }

    @Test
    fun traffic_caps_blocks_but_keeps_the_count() {
        val col = traffic(listOf(HourCount(10, sorted = 20)), nowHour = 12).columns.single { it.hour == 10 }
        assertEquals(MAX_BLOCKS, col.blocks.size)
        assertEquals(20, col.arrived)
    }

    @Test
    fun piles_get_latest_and_under_sheets() {
        assertEquals(listOf(2, 2, 2, 1, 1), ui.piles.map { it.underSheets })
        assertEquals("latest 13:12", ui.piles.first().latest)
        assertEquals(0, underSheets(1))
    }

    @Test
    fun wait_age_rounds_down_to_the_largest_unit() {
        assertEquals("25m", waitAge(now.minusMinutes(25).toInstant(), now))
        assertEquals("1m", waitAge(now.toInstant(), now))
        assertEquals("23h", waitAge(now.minusHours(23).minusMinutes(59).toInstant(), now))
        assertEquals("2d", waitAge(now.minusDays(2).toInstant(), now))
    }

    @Test
    fun stamp_is_time_then_weekday_then_date() {
        assertEquals("14:07", stamp(now.toInstant(), now))
        assertEquals("Mon", stamp(now.minusDays(1).toInstant(), now))
        assertEquals("22 Sep", stamp(now.minusDays(7).toInstant(), now))
    }
}
