package pe.net.libre.aimap_client.home

import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/** The three bays, ranked by attention. Each owns one holder colour. */
enum class Bay { NeedsYou, Waiting, CanGo }

enum class StripAction { Done, Reply }

data class HomeUi(
    val header: HeaderUi,
    val plates: Map<Bay, Int>,
    val traffic: TrafficUi,
    val needsYou: List<StripUi>,
    val waiting: List<StripUi>,
    val piles: List<PileUi>,
    val unsorted: Int,
    val footer: String,
    val archiveCount: Int,
)

data class HeaderUi(val account: String, val accountCount: Int, val lastSort: String?, val initials: String)

data class TrafficUi(val columns: List<HourColumn>, val total: String)

data class HourColumn(
    val hour: Int,
    /** Bottom to top. */
    val blocks: List<Bay>,
    val arrived: Int,
    val isNow: Boolean,
    val isFuture: Boolean,
    val axisLabel: String?,
)

data class StripUi(
    val messageId: Long,
    val bay: Bay,
    /** The bucket word (ACT, CHECK) or the wait age (4d). */
    val top: String,
    val topIsAge: Boolean,
    /** The time today, or the day before that. */
    val bottom: String,
    val greased: Boolean,
    val sender: String,
    val account: String,
    val subject: String,
    val reason: String?,
    val moreReasons: Int,
    /** needs_review: the strip is cocked out of line. */
    val cocked: Boolean,
    val action: StripAction,
    /** Hour of today it arrived, for the traffic scrub; null if not today. */
    val hourToday: Int?,
)

data class PileUi(val name: String, val count: Int, val latest: String?, val summary: String, val underSheets: Int)

const val FIRST_HOUR = 6
const val LAST_HOUR = 22
const val MAX_BLOCKS = 8

private val Clock = DateTimeFormatter.ofPattern("HH:mm")
private val Weekday = DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH)
private val Day = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)

fun Home.toUi(now: ZonedDateTime, accounts: Int, initials: String, days: Int = 7): HomeUi {
    val longestWait = waiting.maxByOrNull { age(it.waitingSince ?: it.sentAt, now) }?.messageId
    return HomeUi(
        header = HeaderUi("All inboxes", accounts, brief.sortedAt?.let { "sorted " + clock(it, now) }, initials),
        plates = mapOf(Bay.NeedsYou to brief.actNow, Bay.Waiting to brief.waiting, Bay.CanGo to brief.sorted),
        traffic = traffic(brief.byHour, now.hour),
        needsYou = actNow.map { c ->
            strip(c, Bay.NeedsYou, now).copy(
                top = if (c.actionBucket == "act_now") "ACT" else "CHECK",
                greased = c.actionBucket == "act_now",
            )
        },
        waiting = waiting.map { c ->
            strip(c, Bay.Waiting, now).copy(
                top = waitAge(c.waitingSince ?: c.sentAt, now),
                topIsAge = true,
                greased = c.messageId == longestWait,
                action = StripAction.Reply,
            )
        },
        piles = sorted.map { g ->
            PileUi(g.name, g.count, g.latestAt?.let { "latest " + stamp(it, now) }, g.summary, underSheets(g.count))
        },
        unsorted = brief.unclassified,
        footer = "Last $days days · $accounts inbox${if (accounts == 1) "" else "es"} · all strips shown",
        archiveCount = brief.sorted,
    )
}

private fun strip(c: Card, bay: Bay, now: ZonedDateTime): StripUi {
    val sent = c.sentAt?.atZone(now.zone)
    val reason = when {
        c.needsReview && c.reasons.isNotEmpty() -> "Unsure: " + c.reasons.first().replaceFirstChar { it.lowercase() } + "?"
        c.needsReview -> "Unsure about this one"
        else -> c.reasons.firstOrNull()
    }
    return StripUi(
        messageId = c.messageId,
        bay = bay,
        top = "",
        topIsAge = false,
        bottom = c.sentAt?.let { stamp(it, now) } ?: "",
        greased = false,
        sender = c.sender,
        account = c.profile ?: c.account.substringBefore('@'),
        subject = c.subject ?: "(no subject)",
        reason = reason,
        moreReasons = (c.reasons.size - 1).coerceAtLeast(0),
        cocked = c.needsReview,
        action = StripAction.Done,
        hourToday = sent?.takeIf { it.toLocalDate() == now.toLocalDate() }?.hour,
    )
}

fun traffic(byHour: List<HourCount>, nowHour: Int): TrafficUi {
    val hours = byHour.associateBy { it.hour }
    val columns = (FIRST_HOUR..LAST_HOUR).map { h ->
        val c = hours[h] ?: HourCount(h)
        val blocks = List(c.actNow) { Bay.NeedsYou } + List(c.waiting) { Bay.Waiting } + List(c.sorted) { Bay.CanGo }
        HourColumn(
            hour = h,
            blocks = if (h > nowHour) emptyList() else blocks.take(MAX_BLOCKS),
            arrived = c.total,
            isNow = h == nowHour,
            isFuture = h > nowHour,
            axisLabel = when {
                h == nowHour -> "NOW"
                (h - FIRST_HOUR) % 3 == 0 -> "%02d".format(h)
                else -> null
            },
        )
    }
    val total = columns.filterNot { it.isFuture }.sumOf { it.arrived }
    return TrafficUi(columns, "$total arrived since %02d:00".format(FIRST_HOUR))
}

/** "4d", "6h", "25m": how long someone has been waiting. */
fun waitAge(since: Instant?, now: ZonedDateTime): String {
    val d = age(since, now)
    return when {
        d.toHours() >= 24 -> "${d.toDays()}d"
        d.toMinutes() >= 60 -> "${d.toHours()}h"
        else -> "${d.toMinutes().coerceAtLeast(1)}m"
    }
}

/** The time if it is today, the weekday within the last week, the date before that. */
fun stamp(at: Instant, now: ZonedDateTime): String {
    val local = at.atZone(now.zone)
    val days = ChronoUnit.DAYS.between(local.toLocalDate(), now.toLocalDate())
    return when {
        days == 0L -> Clock.format(local)
        days in 1..6 -> Weekday.format(local)
        else -> Day.format(local)
    }
}

/** A pile shows up to two under-sheet edges: one from 2 strips, two from 5. */
fun underSheets(count: Int) = when {
    count >= 5 -> 2
    count >= 2 -> 1
    else -> 0
}

private fun clock(at: Instant, now: ZonedDateTime) = Clock.format(at.atZone(now.zone))

private fun age(since: Instant?, now: ZonedDateTime): Duration =
    if (since == null) Duration.ZERO else Duration.between(since, now.toInstant()).coerceAtLeast(Duration.ZERO)
