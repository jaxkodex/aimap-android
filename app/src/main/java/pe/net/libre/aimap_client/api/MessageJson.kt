package pe.net.libre.aimap_client.api

import org.json.JSONObject
import java.time.Instant

/** One message from `GET /messages/{id}`. Field names follow aimap-service `inbox.get_message`. */
data class Message(
    val messageId: Long,
    val account: String,
    /** The account's profile name ("work"), the label Home cards use. Null from older services. */
    val profile: String? = null,
    val sender: String,
    val fromEmail: String?,
    val subject: String?,
    val sentAt: Instant?,
    val unread: Boolean,
    val flagged: Boolean,
    /** It carries list headers: a newsletter or a notification, not a letter. */
    val bulk: Boolean,
    val inReplyTo: String?,
    /** Null until the agent has read the message. */
    val labels: Labels?,
    val mailboxes: List<Mailbox>,
    /** What was already done with it. The API leaves it out until actions are stored. */
    val state: MessageState?,
    /** Reply detection from the service. Absent from older services. */
    val reply: Reply? = null,
)

/** The latest classification, with the reasons the API builds from its signals. */
data class Labels(
    val importance: String?,
    val actionBucket: String?,
    val tags: List<String>,
    val insight: String?,
    val needsReview: Boolean,
    val priority: Double,
    val reasons: List<String>,
    val classifiedAt: Instant?,
)

/** Where a copy of the message sits, with the IMAP flags seen at ingestion. */
data class Mailbox(val name: String, val flags: List<String>)

/** `state` on `GET /messages/{id}`: null, "handled" or "later". An unknown word reads as null. */
enum class MessageState(val wire: String) {
    Handled("handled"),
    Later("later"),
    ;

    companion object {
        fun of(wire: String?): MessageState? = entries.firstOrNull { it.wire == wire }
    }
}

/** The answer to `POST /messages/{id}/actions`: what the message's state is now. */
data class ActionState(val messageId: Long, val state: MessageState?, val changedAt: Instant?)

/** Parses `POST /messages/{id}/actions`. Undo answers with both fields null. */
fun parseAction(json: String): ActionState {
    val o = JSONObject(json)
    return ActionState(
        messageId = o.getLong("message_id"),
        state = MessageState.of(o.string("state")),
        changedAt = o.instant("changed_at"),
    )
}

/** Parses `GET /messages/{id}`. */
fun parseMessage(json: String): Message {
    val o = JSONObject(json)
    return Message(
        messageId = o.getLong("message_id"),
        account = o.getString("account"),
        profile = o.string("profile"),
        sender = o.string("sender") ?: "(unknown)",
        fromEmail = o.string("from_email"),
        subject = o.string("subject"),
        sentAt = o.instant("sent_at"),
        unread = o.optBoolean("unread"),
        flagged = o.optBoolean("flagged"),
        bulk = o.optBoolean("bulk"),
        inReplyTo = o.string("in_reply_to"),
        labels = o.optJSONObject("labels")?.let(::labels),
        mailboxes = o.optJSONArray("mailboxes").objects().map {
            Mailbox(it.string("mailbox").orEmpty(), it.optJSONArray("flags").strings())
        },
        state = MessageState.of(o.string("state")),
        reply = o.optJSONObject("reply")?.let(::parseReply),
    )
}

/** Parses `GET /messages/{id}/body`: the text the API read from the bucket for that request. */
fun parseMessageBody(json: String): String = JSONObject(json).string("text").orEmpty()

private fun labels(o: JSONObject) = Labels(
    importance = o.string("importance"),
    actionBucket = o.string("action_bucket"),
    tags = o.optJSONArray("tags").strings(),
    insight = o.string("insight"),
    needsReview = o.optBoolean("needs_review"),
    priority = o.optDouble("priority", 0.0),
    reasons = o.optJSONArray("reasons").strings(),
    classifiedAt = o.instant("classified_at"),
)
