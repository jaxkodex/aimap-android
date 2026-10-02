package pe.net.libre.aimap_client.detail

import pe.net.libre.aimap_client.api.Message
import pe.net.libre.aimap_client.api.MessageState
import pe.net.libre.aimap_client.home.Bay
import pe.net.libre.aimap_client.home.stamp
import pe.net.libre.aimap_client.home.waitAge
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** What the read screen is showing. */
sealed interface Read {
    /** The metadata is still coming: the screen draws a skeleton, never a spinner. */
    data object Loading : Read

    /** The API has no such message (404). */
    data object Gone : Read

    /** The metadata call failed and can be tried again. */
    data class Failed(val text: String) : Read

    data class Ready(val ui: DetailUi, val body: BodyState) : Read
}

/** The body comes from a second call, so the letter fills in after the strip. */
sealed interface BodyState {
    data object Loading : BodyState

    data class Failed(val text: String) : BodyState

    data class Text(val text: String) : BodyState
}

/** The pinned strip and the letter head, from one `GET /messages/{id}`. */
data class DetailUi(
    val messageId: Long,
    val bay: Bay,
    /** The bucket word (ACT, CHECK) or the wait age (4h). */
    val top: String,
    val topIsAge: Boolean,
    /** The time it arrived. */
    val bottom: String,
    val sender: String,
    val account: String,
    val subject: String,
    /** needs_review: the agent is not sure, so the strip says so before its reasons. */
    val unsure: Boolean,
    /** Every stored reason, shown as ruled fields. */
    val reasons: List<String>,
    val from: String,
    val to: String,
    val sent: String,
    val state: MessageState?,
) {
    /** The ruled fields under the subject, the doubt first. */
    val fields: List<String> get() = if (unsure) listOf("Unsure") + reasons else reasons
}

/** The fold: about eight lines of letter, then "Show the full message". */
const val FOLD_LINES = 8

/**
 * How many lines stay under the fold. Counted on the laid-out text, not on the stored one:
 * the API strips blank lines but keeps whatever wrapping the sender used, so one "line"
 * of mail can be a whole paragraph.
 */
fun foldedLines(laidOut: Int, limit: Int = FOLD_LINES): Int = (laidOut - limit).coerceAtLeast(0)

private val Clock = DateTimeFormatter.ofPattern("HH:mm")
private val Day = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH)

/** The word on the holder per action bucket. The waiting bay shows the age instead. */
private val Words = mapOf(
    "act_now" to "ACT",
    "verify" to "CHECK",
    "review" to "REVIEW",
    "skim" to "SKIM",
    "batch_review" to "ALERT",
    "discard" to "DROP",
)

fun Message.toUi(now: ZonedDateTime): DetailUi {
    val bucket = labels?.actionBucket
    val bay = bay(bucket)
    val label = account.substringBefore('@')
    return DetailUi(
        messageId = messageId,
        bay = bay,
        top = if (bay == Bay.Waiting) waitAge(sentAt, now) else Words[bucket] ?: "NEW",
        topIsAge = bay == Bay.Waiting,
        bottom = sentAt?.let { stamp(it, now) } ?: "",
        sender = sender,
        account = label,
        subject = subject ?: "(no subject)",
        unsure = labels?.needsReview == true,
        reasons = labels?.reasons ?: emptyList(),
        from = fromEmail ?: sender,
        to = "you · $label",
        sent = sentAt?.let { sent(it, now) } ?: "unknown",
        state = state,
    )
}

private fun bay(bucket: String?) = when (bucket) {
    "act_now", "verify" -> Bay.NeedsYou
    "reply" -> Bay.Waiting
    else -> Bay.CanGo
}

private fun sent(at: java.time.Instant, now: ZonedDateTime): String {
    val local = at.atZone(now.zone)
    val day = if (local.toLocalDate() == now.toLocalDate()) "today" else Day.format(local)
    return "${Clock.format(local)} · $day"
}
