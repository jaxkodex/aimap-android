package pe.net.libre.aimap_client.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class MessageJsonTest {
    // Shaped like a real GET /messages/{id} response; all content is synthetic.
    private val message = """
        {
          "message_id": 41, "account": "me@example.com", "rfc822_message_id": "<a@northwind.example>",
          "from_email": "priya@northwind.example", "sender": "Priya Nair",
          "subject": "Interview: Senior Data Engineer", "sent_at": "2026-09-29T10:12:00+00:00",
          "in_reply_to": null, "bulk": false, "unread": true, "flagged": false,
          "labels": {"importance": "high", "action_bucket": "reply", "tags": ["recruiting"], "insight": null,
                     "needs_review": true, "priority": 0.82, "signals": {"real_person": 0.9},
                     "classified_at": "2026-09-29T10:14:00+00:00",
                     "reasons": ["Asks you to do something", "Written to you by a person"]},
          "mailboxes": [{"mailbox": "INBOX", "flags": ["\\Flagged"]}, {"mailbox": "Archive", "flags": []}]
        }
    """.trimIndent()

    @Test
    fun parses_the_message_and_its_labels() {
        val m = parseMessage(message)
        assertEquals(41L, m.messageId)
        assertEquals("Priya Nair", m.sender)
        assertEquals("priya@northwind.example", m.fromEmail)
        assertEquals(Instant.parse("2026-09-29T10:12:00Z"), m.sentAt)
        assertTrue(m.unread)
        assertFalse(m.bulk)
        assertNull(m.inReplyTo)

        val labels = requireNotNull(m.labels)
        assertEquals("reply", labels.actionBucket)
        assertEquals(listOf("recruiting"), labels.tags)
        assertTrue(labels.needsReview)
        assertEquals(0.82, labels.priority, 1e-9)
        assertEquals(2, labels.reasons.size)
        assertEquals(Instant.parse("2026-09-29T10:14:00Z"), labels.classifiedAt)

        assertEquals(listOf("INBOX", "Archive"), m.mailboxes.map { it.name })
        assertEquals(listOf("\\Flagged"), m.mailboxes.first().flags)
    }

    @Test
    fun unclassified_message_has_no_labels() {
        val json = """
            {"message_id": 7, "account": "me@example.com", "sender": null, "from_email": null,
             "subject": null, "sent_at": null, "labels": null, "mailboxes": []}
        """.trimIndent()
        val m = parseMessage(json)
        assertNull(m.labels)
        assertNull(m.subject)
        assertNull(m.sentAt)
        assertEquals("(unknown)", m.sender)
        assertEquals(emptyList<Mailbox>(), m.mailboxes)
        assertFalse(m.unread)
    }

    @Test
    fun state_is_optional() {
        assertNull(parseMessage(message).state)
        assertEquals(MessageState.Handled, parseMessage(withState("\"handled\"")).state)
        assertEquals(MessageState.Later, parseMessage(withState("\"later\"")).state)
        assertNull(parseMessage(withState("null")).state)
        assertNull(parseMessage(withState("\"snoozed\"")).state)
    }

    @Test
    fun parses_the_body() {
        assertEquals(
            "Thanks for applying.\nCould you send a few times?",
            parseMessageBody("""{"message_id": 41, "text": "Thanks for applying.\nCould you send a few times?"}"""),
        )
        assertEquals("", parseMessageBody("""{"message_id": 41, "text": null}"""))
    }

    private fun withState(state: String) = message.replaceFirst("{", """{"state": $state,""")
}
