package pe.net.libre.aimap_client.detail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.net.libre.aimap_client.api.Labels
import pe.net.libre.aimap_client.api.Message
import pe.net.libre.aimap_client.api.MessageState
import pe.net.libre.aimap_client.home.Bay
import pe.net.libre.aimap_client.home.SampleHome
import java.time.Instant

class DetailUiTest {
    private val now = SampleHome.now

    private fun message(
        bucket: String?,
        sentAt: Instant? = now.minusHours(4).toInstant(),
        reasons: List<String> = emptyList(),
        needsReview: Boolean = false,
        subject: String? = "Interview: Senior Data Engineer",
        state: MessageState? = null,
    ) = Message(
        messageId = 41,
        account = "personal@example.com",
        sender = "Priya Nair",
        fromEmail = "priya@northwind.example",
        subject = subject,
        sentAt = sentAt,
        unread = true,
        flagged = false,
        bulk = false,
        inReplyTo = null,
        labels = bucket?.let {
            Labels(null, it, emptyList(), null, needsReview, 0.8, reasons, null)
        },
        mailboxes = emptyList(),
        state = state,
    )

    @Test
    fun a_reply_sits_in_the_waiting_bay_and_shows_its_age() {
        val ui = message("reply", reasons = listOf("Asks you to do something")).toUi(now)
        assertEquals(Bay.Waiting, ui.bay)
        assertEquals("4h", ui.top)
        assertTrue(ui.topIsAge)
        assertEquals("10:07", ui.bottom)
    }

    @Test
    fun the_other_bays_show_the_bucket_word() {
        assertEquals("ACT", message("act_now").toUi(now).top)
        assertEquals("CHECK", message("verify").toUi(now).top)
        assertEquals(Bay.NeedsYou, message("verify").toUi(now).bay)
        assertEquals("SKIM", message("skim").toUi(now).top)
        assertEquals(Bay.CanGo, message("discard").toUi(now).bay)
        assertFalse(message("act_now").toUi(now).topIsAge)
    }

    @Test
    fun an_unclassified_message_is_new_and_has_no_reasons() {
        val ui = message(null, subject = null).toUi(now)
        assertEquals("NEW", ui.top)
        assertEquals(Bay.CanGo, ui.bay)
        assertEquals("(no subject)", ui.subject)
        assertEquals(emptyList<String>(), ui.fields)
        assertFalse(ui.unsure)
    }

    @Test
    fun every_stored_reason_becomes_a_field_with_doubt_first() {
        val reasons = listOf("Mentions a deadline", "Asks you to do something", "Written to you by a person")
        val ui = message("act_now", reasons = reasons, needsReview = true).toUi(now)
        assertTrue(ui.unsure)
        assertEquals(listOf("Unsure") + reasons, ui.fields)
    }

    @Test
    fun the_letter_head_names_the_account_and_the_time() {
        val ui = message("reply").toUi(now)
        assertEquals("priya@northwind.example", ui.from)
        assertEquals("you · personal", ui.to)
        assertEquals("personal", ui.account)
        assertEquals("10:07 · today", ui.sent)
        assertEquals("14:07 · Mon 28 Sep", message("reply", sentAt = now.minusDays(1).toInstant()).toUi(now).sent)
        assertEquals("unknown", message("reply", sentAt = null).toUi(now).sent)
    }

    @Test
    fun the_state_comes_through_for_the_foot() {
        assertNull(message("reply").toUi(now).state)
        assertEquals(MessageState.Handled, message("reply", state = MessageState.Handled).toUi(now).state)
    }

    @Test
    fun the_fold_counts_the_lines_it_hides() {
        assertEquals(0, foldedLines(FOLD_LINES))
        assertEquals(0, foldedLines(3))
        assertEquals(6, foldedLines(FOLD_LINES + 6))
    }

    @Test
    fun the_sample_message_matches_the_design() {
        val ready = SampleMessage.read
        assertEquals("4h", ready.ui.top)
        assertEquals("Priya Nair", ready.ui.sender)
        assertEquals(listOf("Asks you to do something", "Written to you by a person"), ready.ui.fields)
        // Like the API's own output: one line per paragraph, no blank lines, and long enough to fold.
        val body = (ready.body as BodyState.Text).text
        assertEquals(6, body.lines().size)
        assertTrue(body.lines().none { it.isBlank() })
        assertTrue(body.length > 400)
    }
}
