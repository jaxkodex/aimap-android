package pe.net.libre.aimap_client.home

import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/** The three bays, ranked by attention. Each owns one holder colour. */
enum class Bay { NeedsYou, Waiting, CanGo }

/** What a key on a strip asks the app to do with that message. */
enum class HomeAction { Handled, Later, Undo }

/** The labelled keys a strip can carry. Only [Handled], [ItWasMe] and [Reply] are wired. */
enum class StripKey { Handled, ItWasMe, Reply, NeedsReply, CanGo }

data class HomeUi(
    val header: HeaderUi,
    val plates: Map<Bay, Int>,
    /** The order of work, in one line, under the plates. */
    val guide: String,
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
    /** The arrival time, or the word "waiting" when the holder counts the wait. */
    val bottom: String,
    val bottomIsWord: Boolean,
    val greased: Boolean,
    val sender: String,
    val account: String,
    val subject: String,
    val reason: String?,
    val moreReasons: Int,
    /** needs_review: the strip is cocked out of line. */
    val cocked: Boolean,
    val keys: List<StripKey>,
    /** Hour of today it arrived, for the traffic scrub; null if not today. */
    val hourToday: Int?,
)

/** A sorted group: the count, the latest arrival time and who sent them. */
data class PileUi(val name: String, val count: Int, val latest: String?, val summary: String)

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
        guide = guideLine(brief.actNow, brief.waiting, brief.sorted),
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
                bottom = "waiting",
                bottomIsWord = true,
                greased = c.messageId == longestWait,
            )
        },
        piles = sorted.map { g -> PileUi(g.name, g.count, g.latestAt?.let { stamp(it, now) }, g.summary) },
        unsorted = brief.unclassified,
        footer = "Last $days days · $accounts inbox${if (accounts == 1) "" else "es"} · nothing hidden",
        archiveCount = brief.sorted,
    )
}

/**
 * The line under the plates: the order of work, read off the counts. A bay at zero drops out of
 * the sentence, and when nothing needs you the line says so instead of giving orders.
 */
fun guideLine(needsYou: Int, waiting: Int, canGo: Int): String {
    val jobs = buildList {
        if (needsYou > 0) add("handle $needsYou")
        if (waiting > 0) add("answer $waiting")
        if (canGo > 0) add("clear $canGo")
    }
    return when {
        jobs.isEmpty() -> "All clear."
        needsYou == 0 && waiting == 0 -> "All handled. $canGo can go when you're ready."
        jobs.size == 3 -> "Work down the list: ${jobs.inOrder()}."
        else -> "${jobs.inOrder().replaceFirstChar { it.uppercase() }}."
    }
}

/** "handle 2, answer 3, then clear 28": the last job is the one you get to last. */
private fun List<String>.inOrder() =
    if (size == 1) first() else dropLast(1).joinToString(", ") + ", then " + last()

private fun strip(c: Card, bay: Bay, now: ZonedDateTime): StripUi {
    val sent = c.sentAt?.atZone(now.zone)
    return StripUi(
        messageId = c.messageId,
        bay = bay,
        top = "",
        topIsAge = false,
        bottom = c.sentAt?.let { stamp(it, now) } ?: "",
        bottomIsWord = false,
        greased = false,
        sender = c.sender,
        account = c.profile ?: c.account.substringBefore('@'),
        subject = c.subject ?: "(no subject)",
        reason = if (c.needsReview) "Unsure: does this need a reply?" else c.reasons.firstOrNull(),
        moreReasons = if (c.needsReview) 0 else (c.reasons.size - 1).coerceAtLeast(0),
        cocked = c.needsReview,
        keys = keys(c, bay),
        hourToday = sent?.takeIf { it.toLocalDate() == now.toLocalDate() }?.hour,
    )
}

/** A strip the agent is unsure about offers both answers; the rest offer the one job to do. */
private fun keys(c: Card, bay: Bay): List<StripKey> = when {
    c.needsReview -> listOf(StripKey.NeedsReply, StripKey.CanGo)
    bay == Bay.Waiting -> listOf(StripKey.Reply)
    c.actionBucket == "verify" -> listOf(StripKey.ItWasMe)
    else -> listOf(StripKey.Handled)
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

private fun clock(at: Instant, now: ZonedDateTime) = Clock.format(at.atZone(now.zone))

private fun age(since: Instant?, now: ZonedDateTime): Duration =
    if (since == null) Duration.ZERO else Duration.between(since, now.toInstant()).coerceAtLeast(Duration.ZERO)
