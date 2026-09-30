package pe.net.libre.aimap_client.home

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Synthetic sample data from the Pencil home design. Every name, subject and count is made up;
 * none of it is real mail. Used until the app talks to `GET /home`.
 */
object SampleHome {
    val zone: ZoneId = ZoneId.of("Europe/Madrid")
    val now: ZonedDateTime = ZonedDateTime.of(LocalDateTime.of(2026, 9, 29, 14, 7), zone)

    private fun at(daysAgo: Long, hour: Int, minute: Int) =
        now.minusDays(daysAgo).withHour(hour).withMinute(minute).toInstant()

    private fun card(
        id: Long, profile: String, sender: String, subject: String, sentAt: java.time.Instant,
        bucket: String, reasons: List<String>, needsReview: Boolean = false, waiting: Boolean = false,
    ) = Card(
        messageId = id, account = "$profile@example.com", profile = profile, sender = sender,
        subject = subject, sentAt = sentAt, unread = true, actionBucket = bucket, importance = null,
        priority = 0.0, needsReview = needsReview, reasons = reasons,
        waitingSince = if (waiting) sentAt else null,
    )

    val home = Home(
        brief = Brief(
            new = 26, actNow = 2, waiting = 3, sorted = 28, unclassified = 1,
            text = "26 new emails. 2 need you now and 3 people are waiting on a reply.",
            sortedAt = at(0, 14, 3),
            byHour = listOf(
                HourCount(6, sorted = 2),
                HourCount(7, sorted = 4),
                HourCount(8, actNow = 1, sorted = 3),
                HourCount(9, waiting = 1, sorted = 5),
                HourCount(10, sorted = 2),
                HourCount(11, actNow = 1, sorted = 3),
                HourCount(12, sorted = 1),
                HourCount(13, sorted = 2),
                HourCount(14, sorted = 1),
            ),
        ),
        actNow = listOf(
            card(1, "work", "Ana Ruiz", "Countersigned contract needed by Friday", at(0, 8, 41), "act_now",
                listOf("Mentions a deadline", "Asks you to do something", "Written to you by a person")),
            card(2, "personal", "Bank · Security", "New sign-in from Chrome on Windows, Madrid", at(0, 11, 2),
                "verify", listOf("Security event on your account")),
        ),
        waiting = listOf(
            card(3, "work", "Marta Gil", "Can you confirm the venue for the 14th?", at(4, 9, 30), "reply",
                listOf("Written to you by a person"), waiting = true),
            card(4, "studio", "Tomás Vera", "Quick question about the invoice numbering", at(1, 12, 20), "reply",
                listOf("Written to you by a person"), needsReview = true, waiting = true),
            card(5, "personal", "Léa Martin", "Dinner Saturday? Let me know by tonight", at(0, 9, 14), "reply",
                listOf("Written to you by a person", "Mentions a deadline"), waiting = true),
        ),
        sorted = listOf(
            SortedGroup("Newsletters", 9, 9, at(0, 13, 12), "Morning Brew, Stratechery + 4 more"),
            SortedGroup("Receipts", 7, 5, at(0, 12, 40), "Uber, AWS Billing + 2 more"),
            SortedGroup("Alerts", 5, 5, at(0, 14, 1), "GitHub, Google Calendar"),
            SortedGroup("Promotions", 4, 4, at(0, 10, 5), "Zara, Iberia + 1 more"),
            SortedGroup("Can discard", 3, 3, at(0, 8, 12), "LinkedIn, Glovo"),
        ),
    )
}
