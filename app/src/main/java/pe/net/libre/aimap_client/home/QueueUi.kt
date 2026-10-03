package pe.net.libre.aimap_client.home

/**
 * The v5 Bay reads the same `/home` as v4, but as one line of work: everything that needs you
 * or waits on you, in the order the agent ranked it, and the piles as the single last step.
 */
data class QueueUi(
    /** act_now first, then waiting, both in API order. */
    val steps: List<StripUi>,
    /** "5 to work, then clear 28". */
    val head: String,
    val note: String,
    /** "2 need you · 3 waiting on you", or null when nothing is queued. */
    val legend: String?,
    /** "28 can go", or null when no pile can. */
    val canGoLegend: String?,
    val piles: List<PileUi>,
    val canGo: Int,
)

fun HomeUi.toQueue(): QueueUi {
    val steps = needsYou + waiting
    return QueueUi(
        steps = steps,
        head = queueHead(steps.size, archiveCount),
        note = queueNote(steps.size, archiveCount, header.lastSort),
        legend = queueLegend(needsYou.size, waiting.size),
        canGoLegend = if (archiveCount > 0) "$archiveCount can go" else null,
        piles = piles,
        canGo = archiveCount,
    )
}

/** The head over the queue: the work first, then what is left over for the end. */
fun queueHead(toWork: Int, canGo: Int): String = when {
    toWork > 0 && canGo > 0 -> "$toWork to work, then clear $canGo"
    toWork > 0 -> "$toWork to work"
    canGo > 0 -> "All worked. $canGo can go."
    else -> "All clear."
}

/**
 * The line under the head: how to work the queue. With nothing left at all there is no work to
 * explain, so it reports when the agent last sorted instead.
 */
fun queueNote(toWork: Int, canGo: Int, lastSort: String?): String = when {
    toWork > 0 -> "One at a time. Most urgent first."
    canGo > 0 -> "Nothing needs you. Clear the piles when you're ready."
    lastSort != null -> "Nothing in the queue · $lastSort"
    else -> "Nothing in the queue."
}

/** What the coloured segments of the track stand for. */
fun queueLegend(needsYou: Int, waiting: Int): String? {
    val parts = buildList {
        if (needsYou > 0) add("$needsYou need${if (needsYou == 1) "s" else ""} you")
        if (waiting > 0) add("$waiting waiting on you")
    }
    return parts.joinToString(" · ").ifEmpty { null }
}

/** The head of the Now section names the bay of the message in hand. */
fun nowTitle(bay: Bay): String = when (bay) {
    Bay.Waiting -> "Now · Waiting on you"
    else -> "Now · Needs you"
}

/** "1 / 5": which step of the queue is in hand. */
fun queuePosition(index: Int, size: Int): String = "${index + 1} / $size"

/** The note on the pile sheet, under the piles themselves. */
fun pileNote(canGo: Int): String =
    "Tap a pile to look inside or keep something. Hold the bar below to archive all $canGo."
