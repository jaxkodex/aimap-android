package pe.net.libre.aimap_client.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.net.libre.aimap_client.home.initials
import java.time.Instant

class HomeJsonTest {
    // Shaped like a real GET /home response; all content is synthetic.
    private val home = """
        {
          "brief": {"new": 3, "act_now": 1, "waiting": 1, "sorted": 1, "unclassified": 0,
                    "handled_today": 2, "text": "3 new emails.", "sorted_at": "2026-09-30T12:03:00Z",
                    "by_hour": [{"hour": 8, "act_now": 1, "waiting": 0, "sorted": 2, "unclassified": 0}]},
          "act_now": [{"message_id": 11, "account": "me@example.com", "profile": null, "sender": "Ana Ruiz",
                       "from_email": "ana@example.com", "subject": null, "sent_at": "2026-09-30T08:41:00+00:00",
                       "unread": true, "action_bucket": "act_now", "importance": "high", "priority": 0.9,
                       "needs_review": false, "reasons": ["Mentions a deadline", "Asks you to do something"],
                       "state": null}],
          "waiting": [{"message_id": 12, "account": "me@example.com", "profile": "work", "sender": "Marta Gil",
                       "from_email": null, "subject": "Venue?", "sent_at": "2026-09-26T09:30:00.123456+00:00",
                       "unread": false, "action_bucket": "reply", "importance": null, "priority": 0.4,
                       "needs_review": true, "reasons": [], "waiting_since": "2026-09-26T09:30:00.123456+00:00",
                       "state": "later"}],
          "sorted": [{"name": "Receipts", "count": 7, "unread": 5, "latest_at": null, "summary": "Uber + 1 more"}]
        }
    """.trimIndent()

    @Test
    fun parses_the_brief() {
        val b = parseHome(home).brief
        assertEquals(listOf(3, 1, 1, 1, 0), listOf(b.new, b.actNow, b.waiting, b.sorted, b.unclassified))
        assertEquals(2, b.handledToday)
        assertEquals(Instant.parse("2026-09-30T12:03:00Z"), b.sortedAt)
        assertEquals(3, b.byHour.single().total)
    }

    @Test
    fun a_brief_without_handled_today_counts_none() {
        assertEquals(0, parseHome(home.replace("\"handled_today\": 2,", "")).brief.handledToday)
    }

    @Test
    fun parses_cards_with_nulls_and_offsets() {
        val h = parseHome(home)
        val act = h.actNow.single()
        assertEquals(11L, act.messageId)
        assertNull(act.profile)
        assertNull(act.subject)
        assertEquals(Instant.parse("2026-09-30T08:41:00Z"), act.sentAt)
        assertEquals(2, act.reasons.size)
        assertFalse(act.needsReview)

        val wait = h.waiting.single()
        assertEquals("work", wait.profile)
        assertTrue(wait.needsReview)
        assertEquals(Instant.parse("2026-09-26T09:30:00.123456Z"), wait.waitingSince)
    }

    @Test
    fun parses_the_cards_state() {
        val h = parseHome(home)
        assertNull(h.actNow.single().state)
        assertEquals(MessageState.Later, h.waiting.single().state)
    }

    @Test
    fun parses_sorted_groups() {
        val g = parseHome(home).sorted.single()
        assertEquals("Receipts", g.name)
        assertEquals(7, g.count)
        assertNull(g.latestAt)
    }

    @Test
    fun parses_accounts() {
        val json = """{"accounts": [{"address": "me@example.com", "profile": "default", "messages": 4, "unread": 3}]}"""
        assertEquals(listOf(Account("me@example.com", "default", 4, 3)), parseAccounts(json))
    }

    @Test
    fun initials_from_name_or_email() {
        assertEquals("AR", initials("Ana María Ruiz", "a@example.com"))
        assertEquals("AN", initials("ana", null))
        assertEquals("M", initials(null, "me@example.com"))
    }
}
