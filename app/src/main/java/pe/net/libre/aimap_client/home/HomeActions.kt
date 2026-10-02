package pe.net.libre.aimap_client.home

import pe.net.libre.aimap_client.api.MessageState
import java.time.Instant

/**
 * What an action does to the Bay, before the API has answered. Pure, so the optimistic change and
 * the rollback are the same code read forwards and backwards, and so the order matches
 * aimap-service `home.build`: a message pushed to later keeps its section but sorts after every
 * other card, and the one marked later most recently sorts last.
 *
 * A message that is not on the Bay (handled elsewhere, or sorted) changes nothing.
 */

/** Highest priority first, then the newest, as `act_now` comes off the API. */
private val ByPriority = compareByDescending<Card> { it.priority }
    .thenByDescending { it.sentAt ?: Instant.EPOCH }

/** The longest wait first, as `waiting` comes off the API. */
private val ByWait = compareBy<Card> { it.sentAt ?: Instant.EPOCH }

/** Which section a message sits in, or null when it is not on the Bay. */
private enum class Section { ActNow, Waiting }

/**
 * [messageId] handled: it leaves `act_now` and `waiting`, its plate counts one less, and the brief
 * counts one more handled today.
 */
fun Home.handled(messageId: Long): Home {
    val section = section(messageId) ?: return this
    return copy(
        brief = brief.copy(
            actNow = brief.actNow.lessOne(section == Section.ActNow),
            waiting = brief.waiting.lessOne(section == Section.Waiting),
            handledToday = brief.handledToday + 1,
        ),
        actNow = actNow.without(messageId),
        waiting = waiting.without(messageId),
    )
}

/** [messageId] pushed to later: it stays in its section, at the end of it. */
fun Home.later(messageId: Long): Home = restated(messageId, MessageState.Later)

/** [messageId] back to normal: no state, and back in its section's own order. */
fun Home.normal(messageId: Long): Home = restated(messageId, null)

private fun Home.restated(messageId: Long, state: MessageState?): Home {
    if (section(messageId) == null) return this
    return copy(
        actNow = actNow.restate(messageId, state).ordered(ByPriority),
        waiting = waiting.restate(messageId, state).ordered(ByWait),
    )
}

private fun Home.section(messageId: Long): Section? = when {
    actNow.any { it.messageId == messageId } -> Section.ActNow
    waiting.any { it.messageId == messageId } -> Section.Waiting
    else -> null
}

/** The restated card goes last, which is where [ordered] leaves a card marked later. */
private fun List<Card>.restate(messageId: Long, state: MessageState?): List<Card> {
    val card = firstOrNull { it.messageId == messageId } ?: return this
    return without(messageId) + card.copy(state = state)
}

/** Later cards keep the order they were marked in; the rest take the section's order. */
private fun List<Card>.ordered(order: Comparator<Card>): List<Card> {
    val (later, live) = partition { it.state == MessageState.Later }
    return live.sortedWith(order) + later
}

private fun List<Card>.without(messageId: Long) = filterNot { it.messageId == messageId }

private fun Int.lessOne(really: Boolean) = if (really) (this - 1).coerceAtLeast(0) else this
