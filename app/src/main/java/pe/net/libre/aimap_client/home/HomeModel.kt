package pe.net.libre.aimap_client.home

import java.time.Instant

/** The body of `GET /home` (see aimap-service `src/aimap/home.py`). */
data class Home(
    val brief: Brief,
    val actNow: List<Card>,
    val waiting: List<Card>,
    val sorted: List<SortedGroup>,
)

data class Brief(
    val new: Int,
    val actNow: Int,
    val waiting: Int,
    val sorted: Int,
    val unclassified: Int,
    val text: String,
    val sortedAt: Instant?,
    val byHour: List<HourCount>,
)

/** Messages that arrived in one hour of today, per Home section. */
data class HourCount(
    val hour: Int,
    val actNow: Int = 0,
    val waiting: Int = 0,
    val sorted: Int = 0,
    val unclassified: Int = 0,
) {
    val total get() = actNow + waiting + sorted + unclassified
}

data class Card(
    val messageId: Long,
    val account: String,
    val profile: String?,
    val sender: String,
    val subject: String?,
    val sentAt: Instant?,
    val unread: Boolean,
    val actionBucket: String?,
    val importance: String?,
    val priority: Double,
    val needsReview: Boolean,
    val reasons: List<String>,
    /** Only set on cards in [Home.waiting]. */
    val waitingSince: Instant? = null,
)

data class SortedGroup(
    val name: String,
    val count: Int,
    val unread: Int,
    val latestAt: Instant?,
    val summary: String,
)
