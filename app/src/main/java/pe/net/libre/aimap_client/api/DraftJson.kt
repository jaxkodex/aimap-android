package pe.net.libre.aimap_client.api

import org.json.JSONObject
import java.time.Instant

/** Reply detection from `GET /messages/{id}`, next to labels. */
data class Reply(
    val needed: Boolean,
    val reason: String?,
)

/** One message of a thread from `GET /messages/{id}/thread`. */
data class ThreadMessage(
    val messageId: Long,
    val sender: String,
    val fromEmail: String?,
    val subject: String?,
    val sentAt: Instant?,
    val fromRecipient: Boolean,
    val excerpt: String?,
)

/** The thread holding a message, from `GET /messages/{id}/thread`. */
data class Thread(
    val messageId: Long,
    val threadId: String,
    val messages: List<ThreadMessage>,
)

/** The draft from `POST /messages/{id}/draft`. */
data class Draft(
    val messageId: Long,
    val needsReply: Boolean,
    val replyReason: String?,
    val to: List<String>,
    val cc: List<String>,
    val subject: String,
    val body: String,
    val model: String,
    val usedMessageIds: List<Long>,
    val createdAt: Instant,
)

/** Parses `reply` from `GET /messages/{id}`. */
fun parseReply(o: JSONObject): Reply = Reply(
    needed = o.optBoolean("needed"),
    reason = o.string("reason"),
)

/** Parses `GET /messages/{id}/thread`. */
fun parseThread(json: String): Thread {
    val o = JSONObject(json)
    return Thread(
        messageId = o.getLong("message_id"),
        threadId = o.getString("thread_id"),
        messages = o.optJSONArray("messages").objects().map {
            ThreadMessage(
                messageId = it.getLong("message_id"),
                sender = it.string("sender") ?: "(unknown)",
                fromEmail = it.string("from_email"),
                subject = it.string("subject"),
                sentAt = it.instant("sent_at"),
                fromRecipient = it.optBoolean("from_recipient"),
                excerpt = it.string("excerpt"),
            )
        },
    )
}

/** Parses `POST /messages/{id}/draft`. */
fun parseDraft(json: String): Draft {
    val o = JSONObject(json)
    return Draft(
        messageId = o.getLong("message_id"),
        needsReply = o.optBoolean("needs_reply"),
        replyReason = o.string("reply_reason"),
        to = o.optJSONArray("to").strings(),
        cc = o.optJSONArray("cc").strings(),
        subject = o.getString("subject"),
        body = o.getString("body"),
        model = o.getString("model"),
        usedMessageIds = o.optJSONArray("used_message_ids").longs(),
        createdAt = o.instant("created_at") ?: Instant.now(),
    )
}

private fun org.json.JSONArray?.longs(): List<Long> =
    if (this == null) emptyList() else List(length()) { getLong(it) }
