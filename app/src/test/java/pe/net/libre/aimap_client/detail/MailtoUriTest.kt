package pe.net.libre.aimap_client.detail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.net.libre.aimap_client.api.Draft
import java.net.URLEncoder
import java.time.Instant

/** Mailto URI construction for opening drafts in the mail app. */
class MailtoUriTest {
    @Test
    fun mailto_with_one_recipient() {
        val draft = draft(to = listOf("test@example.com"))
        val uri = buildMailtoUri(draft)
        assertTrue(uri.startsWith("mailto:test@example.com?"))
        assertTrue(uri.contains("subject="))
        assertTrue(uri.contains("body="))
    }

    @Test
    fun mailto_with_multiple_recipients() {
        val draft = draft(to = listOf("one@example.com", "two@example.com"))
        val uri = buildMailtoUri(draft)
        assertTrue(uri.startsWith("mailto:one@example.com,two@example.com?"))
    }

    @Test
    fun mailto_with_cc() {
        val draft = draft(to = listOf("to@example.com"), cc = listOf("cc@example.com"))
        val uri = buildMailtoUri(draft)
        assertTrue(uri.contains("cc=cc%40example.com"))
    }

    @Test
    fun mailto_with_multiple_cc() {
        val draft = draft(to = listOf("to@example.com"), cc = listOf("cc1@example.com", "cc2@example.com"))
        val uri = buildMailtoUri(draft)
        assertTrue(uri.contains("cc=cc1%40example.com%2Ccc2%40example.com"))
    }

    @Test
    fun mailto_without_cc() {
        val draft = draft(to = listOf("to@example.com"), cc = emptyList())
        val uri = buildMailtoUri(draft)
        assertTrue(!uri.contains("cc="))
    }

    @Test
    fun mailto_escapes_special_characters_in_subject() {
        val draft = draft(to = listOf("test@example.com"), subject = "Re: Test & More")
        val uri = buildMailtoUri(draft)
        assertTrue(uri.contains("subject=Re%3A+Test+%26+More"))
    }

    @Test
    fun mailto_escapes_special_characters_in_body() {
        val draft = draft(to = listOf("test@example.com"), body = "Line 1\nLine 2\n\nWith & symbols")
        val uri = buildMailtoUri(draft)
        assertTrue(uri.contains("body="))
        assertTrue(uri.contains("%0A")) // newline
        assertTrue(uri.contains("%26")) // ampersand
    }

    private fun draft(
        to: List<String> = listOf("test@example.com"),
        cc: List<String> = emptyList(),
        subject: String = "Test Subject",
        body: String = "Test body",
    ) = Draft(
        messageId = 1,
        needsReply = true,
        replyReason = null,
        to = to,
        cc = cc,
        subject = subject,
        body = body,
        model = "test-model",
        usedMessageIds = emptyList(),
        createdAt = Instant.now(),
    )

    private fun buildMailtoUri(draft: Draft): String {
        val to = draft.to.joinToString(",")
        val params = mutableListOf<String>()
        params.add("subject=${URLEncoder.encode(draft.subject, "UTF-8")}")
        params.add("body=${URLEncoder.encode(draft.body, "UTF-8")}")
        if (draft.cc.isNotEmpty()) {
            val cc = draft.cc.joinToString(",")
            params.add("cc=${URLEncoder.encode(cc, "UTF-8")}")
        }
        return "mailto:$to?${params.joinToString("&")}"
    }
}
