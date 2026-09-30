package pe.net.libre.aimap_client.api

import org.json.JSONArray
import org.json.JSONObject
import pe.net.libre.aimap_client.home.Brief
import pe.net.libre.aimap_client.home.Card
import pe.net.libre.aimap_client.home.Home
import pe.net.libre.aimap_client.home.HourCount
import pe.net.libre.aimap_client.home.SortedGroup
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeParseException

/** An inbox the API watches, from `GET /accounts`. */
data class Account(val address: String, val profile: String, val messages: Int, val unread: Int)

/** Parses `GET /home`. Field names follow aimap-service `home.py`. */
fun parseHome(json: String): Home {
    val o = JSONObject(json)
    val b = o.getJSONObject("brief")
    return Home(
        brief = Brief(
            new = b.getInt("new"),
            actNow = b.getInt("act_now"),
            waiting = b.getInt("waiting"),
            sorted = b.getInt("sorted"),
            unclassified = b.getInt("unclassified"),
            text = b.getString("text"),
            sortedAt = b.instant("sorted_at"),
            byHour = b.optJSONArray("by_hour").objects().map {
                HourCount(
                    hour = it.getInt("hour"),
                    actNow = it.optInt("act_now"),
                    waiting = it.optInt("waiting"),
                    sorted = it.optInt("sorted"),
                    unclassified = it.optInt("unclassified"),
                )
            },
        ),
        actNow = o.optJSONArray("act_now").objects().map(::card),
        waiting = o.optJSONArray("waiting").objects().map(::card),
        sorted = o.optJSONArray("sorted").objects().map {
            SortedGroup(
                name = it.getString("name"),
                count = it.getInt("count"),
                unread = it.optInt("unread"),
                latestAt = it.instant("latest_at"),
                summary = it.optString("summary"),
            )
        },
    )
}

/** Parses `GET /accounts`. */
fun parseAccounts(json: String): List<Account> =
    JSONObject(json).optJSONArray("accounts").objects().map {
        Account(it.getString("address"), it.optString("profile"), it.optInt("messages"), it.optInt("unread"))
    }

private fun card(o: JSONObject) = Card(
    messageId = o.getLong("message_id"),
    account = o.getString("account"),
    profile = o.string("profile"),
    sender = o.string("sender") ?: "(unknown)",
    subject = o.string("subject"),
    sentAt = o.instant("sent_at"),
    unread = o.optBoolean("unread"),
    actionBucket = o.string("action_bucket"),
    importance = o.string("importance"),
    priority = o.optDouble("priority", 0.0),
    needsReview = o.optBoolean("needs_review"),
    reasons = o.optJSONArray("reasons")?.let { a -> List(a.length()) { a.getString(it) } } ?: emptyList(),
    waitingSince = o.instant("waiting_since"),
)

private fun JSONArray?.objects(): List<JSONObject> = if (this == null) emptyList() else List(length()) { getJSONObject(it) }

/** A string, or null when the key is missing or JSON null (optString would say "null"). */
private fun JSONObject.string(key: String): String? = if (isNull(key)) null else getString(key)

/** An ISO 8601 timestamp. One without an offset is taken as UTC. */
private fun JSONObject.instant(key: String): Instant? = string(key)?.let {
    try {
        OffsetDateTime.parse(it).toInstant()
    } catch (_: DateTimeParseException) {
        LocalDateTime.parse(it).toInstant(ZoneOffset.UTC)
    }
}
