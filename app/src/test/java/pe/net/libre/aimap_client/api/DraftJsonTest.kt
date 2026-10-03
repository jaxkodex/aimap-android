package pe.net.libre.aimap_client.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Parsing reply, thread and draft payloads from the wire contract. */
class DraftJsonTest {
    @Test
    fun reply_with_both_fields() {
        val json = """{"needed": true, "reason": "A person asked you to reschedule."}"""
        val reply = parseReply(org.json.JSONObject(json))
        assertTrue(reply.needed)
        assertEquals("A person asked you to reschedule.", reply.reason)
    }

    @Test
    fun reply_with_null_reason() {
        val json = """{"needed": true, "reason": null}"""
        val reply = parseReply(org.json.JSONObject(json))
        assertTrue(reply.needed)
        assertNull(reply.reason)
    }

    @Test
    fun reply_with_false_needed() {
        val json = """{"needed": false, "reason": null}"""
        val reply = parseReply(org.json.JSONObject(json))
        assertFalse(reply.needed)
    }

    @Test
    fun thread_with_one_message() {
        val json = """
            {
                "message_id": 4211,
                "thread_id": "9f2c",
                "messages": [
                    {
                        "message_id": 4211,
                        "sender": "Barron, Javier",
                        "from_email": "javier.barron@solera.com",
                        "subject": "RE: Jorge - Solera Interview - SW Mgr",
                        "sent_at": "2025-06-02T00:42:00Z",
                        "from_recipient": false,
                        "excerpt": "Jorge, by any chance can you please re schedule…"
                    }
                ]
            }
        """.trimIndent()
        val thread = parseThread(json)
        assertEquals(4211L, thread.messageId)
        assertEquals("9f2c", thread.threadId)
        assertEquals(1, thread.messages.size)
        val msg = thread.messages[0]
        assertEquals(4211L, msg.messageId)
        assertEquals("Barron, Javier", msg.sender)
        assertEquals("javier.barron@solera.com", msg.fromEmail)
        assertEquals("RE: Jorge - Solera Interview - SW Mgr", msg.subject)
        assertFalse(msg.fromRecipient)
        assertEquals("Jorge, by any chance can you please re schedule…", msg.excerpt)
    }

    @Test
    fun thread_message_with_null_excerpt() {
        val json = """
            {
                "message_id": 100,
                "thread_id": "abc",
                "messages": [
                    {
                        "message_id": 100,
                        "sender": "Someone",
                        "from_email": "someone@example.com",
                        "subject": null,
                        "sent_at": null,
                        "from_recipient": true,
                        "excerpt": null
                    }
                ]
            }
        """.trimIndent()
        val thread = parseThread(json)
        val msg = thread.messages[0]
        assertNull(msg.subject)
        assertNull(msg.sentAt)
        assertTrue(msg.fromRecipient)
        assertNull(msg.excerpt)
    }

    @Test
    fun draft_with_all_fields() {
        val json = """
            {
                "message_id": 4211,
                "needs_reply": true,
                "reply_reason": "A person asked you to reschedule.",
                "to": ["javier.barron@solera.com"],
                "cc": ["hr@solera.com"],
                "subject": "RE: Jorge - Solera Interview - SW Mgr",
                "body": "Hi Javier,\n\nTuesday or Wednesday after 15:00 both work…",
                "model": "deepseek-chat",
                "used_message_ids": [4211, 4188],
                "created_at": "2025-06-02T07:10:00Z"
            }
        """.trimIndent()
        val draft = parseDraft(json)
        assertEquals(4211L, draft.messageId)
        assertTrue(draft.needsReply)
        assertEquals("A person asked you to reschedule.", draft.replyReason)
        assertEquals(listOf("javier.barron@solera.com"), draft.to)
        assertEquals(listOf("hr@solera.com"), draft.cc)
        assertEquals("RE: Jorge - Solera Interview - SW Mgr", draft.subject)
        assertTrue(draft.body.contains("Tuesday or Wednesday"))
        assertEquals("deepseek-chat", draft.model)
        assertEquals(listOf(4211L, 4188L), draft.usedMessageIds)
    }

    @Test
    fun draft_with_empty_cc() {
        val json = """
            {
                "message_id": 100,
                "needs_reply": false,
                "reply_reason": null,
                "to": ["test@example.com"],
                "cc": [],
                "subject": "Test",
                "body": "Body",
                "model": "test-model",
                "used_message_ids": [],
                "created_at": "2025-01-01T00:00:00Z"
            }
        """.trimIndent()
        val draft = parseDraft(json)
        assertFalse(draft.needsReply)
        assertNull(draft.replyReason)
        assertTrue(draft.cc.isEmpty())
        assertTrue(draft.usedMessageIds.isEmpty())
    }

    @Test
    fun build_draft_request_with_instructions() {
        val body = buildDraftRequest("Say Tuesday or Wednesday after 15:00 works.")
        assertTrue(body.contains("instructions"))
        assertTrue(body.contains("Say Tuesday or Wednesday after 15:00 works."))
    }

    @Test
    fun build_draft_request_with_null_instructions() {
        val body = buildDraftRequest(null)
        assertEquals("{}", body)
    }

    @Test
    fun build_draft_request_escapes_json() {
        val body = buildDraftRequest("Say \"Tuesday\" works.\nNew line: \\ / \"")
        val parsed = org.json.JSONObject(body)
        assertEquals("Say \"Tuesday\" works.\nNew line: \\ / \"", parsed.getString("instructions"))
    }

    @Test
    fun build_draft_request_handles_unicode() {
        val body = buildDraftRequest("café 日本語 emoji: 🎉")
        val parsed = org.json.JSONObject(body)
        assertEquals("café 日本語 emoji: 🎉", parsed.getString("instructions"))
    }
}
