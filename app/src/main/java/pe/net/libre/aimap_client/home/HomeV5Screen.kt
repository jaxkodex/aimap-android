package pe.net.libre.aimap_client.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pe.net.libre.aimap_client.ui.theme.AimapTheme
import pe.net.libre.aimap_client.ui.theme.BodyFont
import pe.net.libre.aimap_client.ui.theme.CallFont
import pe.net.libre.aimap_client.ui.theme.DataFont

/** How much of the track the Can go block takes next to one queued message. */
private const val CAN_GO_WEIGHT = 2f

/**
 * The Bay, v5: one queue instead of three racks. The message in hand is the only one opened out,
 * with its keys; the rest wait in order under it, and the piles are the single last step.
 */
@Composable
fun HomeV5Screen(
    ui: HomeUi,
    modifier: Modifier = Modifier,
    onOpen: (messageId: Long) -> Unit = {},
    onAction: (messageId: Long, action: HomeAction) -> Unit = { _, _ -> },
    onArchiveAll: () -> Unit = {},
    onAvatar: () -> Unit = {},
) {
    val c = AimapTheme.colors
    val q = remember(ui) { ui.toQueue() }
    // Tapping a row in Up next brings it to Now. The queue keeps its order; only the hand moves.
    var picked by rememberSaveable { mutableStateOf(0L) }
    val at = q.steps.indexOfFirst { it.messageId == picked }.coerceAtLeast(0)
    val now = q.steps.getOrNull(at)

    Column(modifier.fillMaxSize().background(c.console)) {
        LazyColumn(Modifier.weight(1f).statusBarsPadding()) {
            item { ConsoleHeader(ui.header, onAvatar) }
            item { QueueHead(q, at) }
            if (now != null) {
                item {
                    BaySection(now.bay, nowTitle(now.bay), "", top = 16.dp, bottom = 4.dp) {
                        NowCard(now, onOpen, onAction)
                    }
                }
            }
            // Everything still in line keeps the number it has in the queue.
            val next = q.steps.withIndex().filter { it.index != at }
            if (next.isNotEmpty()) {
                item {
                    BaySection(null, "Up next", "in order", top = 18.dp, bottom = 4.dp) {
                        Rack(next) { (i, s) -> QueueRow(s, i + 1) { picked = s.messageId } }
                    }
                }
            }
            if (q.piles.isNotEmpty()) {
                item {
                    BaySection(Bay.CanGo, "Then clear", q.canGoLegend.orEmpty(), top = 18.dp, bottom = 4.dp) {
                        PileSheet(q.piles, q.canGo)
                    }
                }
            }
            item {
                Box(Modifier.fillMaxWidth().padding(start = Gutter, top = 14.dp, end = Gutter)) {
                    UnsortedSlot(ui.unsorted)
                }
            }
            item {
                Text(
                    ui.footer,
                    Modifier.fillMaxWidth().padding(start = Gutter, top = 10.dp, end = Gutter, bottom = 14.dp),
                    style = style(DataFont, 11, color = c.onConsoleMuted),
                )
            }
        }
        ArchiveDock(ui.archiveCount, onArchiveAll)
        TabBar()
    }
}

/** What is left to do, how to do it, and where in the queue you are. */
@Composable
private fun QueueHead(q: QueueUi, at: Int) {
    val c = AimapTheme.colors
    Column(
        Modifier.fillMaxWidth().padding(start = Gutter, top = 6.dp, end = Gutter),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(q.head, style = style(CallFont, 26, 800, c.onConsole, lineHeight = 1.05f))
        SplitRow(Modifier.fillMaxWidth(), gap = 10.dp) {
            Text(q.note, style = style(BodyFont, 15, color = c.onConsoleMuted, lineHeight = 1.25f))
            if (q.steps.isNotEmpty()) {
                Text(queuePosition(at, q.steps.size), style = style(DataFont, 13, color = c.onConsoleMuted))
            }
        }
        // With nothing queued and no pile left there is no track to draw.
        if (q.steps.isNotEmpty() || q.canGo > 0) QueueTrack(q, at)
        if (q.legend != null || q.canGoLegend != null) {
            SplitRow(Modifier.fillMaxWidth().padding(top = 2.dp), gap = 10.dp) {
                Text(q.legend.orEmpty(), style = style(BodyFont, 13, color = c.onConsoleMuted))
                Text(q.canGoLegend.orEmpty(), style = style(BodyFont, 13, color = c.onConsoleMuted))
            }
        }
    }
}

/** One segment per queued message, coloured by its bay, then the Can go block at the far end. */
@Composable
private fun QueueTrack(q: QueueUi, at: Int) {
    val c = AimapTheme.colors
    val reading = listOfNotNull(
        if (q.steps.isNotEmpty()) queuePosition(at, q.steps.size) else null,
        q.legend,
        q.canGoLegend,
    ).joinToString(" · ")
    Row(
        Modifier.fillMaxWidth().height(11.dp).padding(top = 4.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "The queue"
                stateDescription = reading
            },
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        q.steps.forEachIndexed { i, s ->
            val fill = holder(s.bay)
            Box(
                Modifier.weight(1f).fillMaxHeight()
                    .background(if (i == at) fill else fill.copy(alpha = 0.55f), Plate)
                    .then(if (i == at) Modifier.border(1.5.dp, c.onConsole, Plate) else Modifier)
            )
        }
        if (q.canGo > 0) {
            Box(Modifier.weight(CAN_GO_WEIGHT).fillMaxHeight().background(c.trafficCango, Plate))
        }
    }
}

/** The message in hand: the whole strip opened out, with the keys for its one job on the foot. */
@Composable
private fun NowCard(s: StripUi, onOpen: (Long) -> Unit, onAction: (Long, HomeAction) -> Unit) {
    val c = AimapTheme.colors
    Column(Modifier.fillMaxWidth().clip(Plate).background(c.paper)) {
        Row(
            Modifier.fillMaxWidth().height(IntrinsicSize.Min)
                .clickable(onClickLabel = "Open") { onOpen(s.messageId) },
        ) {
            NowHolder(s)
            Column(
                Modifier.weight(1f).padding(start = 14.dp, top = 12.dp, end = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                SplitRow(Modifier.fillMaxWidth(), gap = 10.dp) {
                    Text(s.sender, style = style(CallFont, 24, 800, c.ink))
                    Text(s.account, style = style(DataFont, 11, color = c.inkMuted))
                }
                Text(s.subject, style = style(BodyFont, 17, color = c.ink, lineHeight = 1.25f))
                if (s.cocked) {
                    Text(
                        "Unsure: does this need a reply?",
                        style = style(CallFont, 15, 700, c.ink, lineHeight = 1.2f),
                    )
                }
                if (s.reasons.isNotEmpty()) Why(s.reasons)
            }
        }
        NowKeys(s, onOpen, onAction)
    }
}

@Composable
private fun NowHolder(s: StripUi) {
    val c = AimapTheme.colors
    Column(
        Modifier.widthIn(min = 60.dp).fillMaxHeight().background(holder(s.bay))
            .padding(start = 10.dp, top = 12.dp, end = 8.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            Modifier.widthIn(min = 42.dp).heightIn(min = 22.dp)
                .then(if (s.greased) Modifier.greaseRing(if (s.topIsAge) 56.dp else 60.dp) else Modifier),
            contentAlignment = Alignment.TopStart,
        ) {
            Text(
                s.top,
                style = if (s.topIsAge) style(DataFont, 20, 800, c.ink) else style(CallFont, 15, 800, c.ink),
                softWrap = false,
            )
        }
        Text(
            s.bottom,
            style = if (s.bottomIsWord) style(CallFont, 13, 600, c.ink) else style(DataFont, 13, color = c.ink),
        )
    }
}

/** Every reason the agent stored, one ruled line each. */
@Composable
private fun Why(reasons: List<String>) {
    val c = AimapTheme.colors
    Column(Modifier.fillMaxWidth().padding(top = 2.dp)) {
        Text("Why it's here", style = style(CallFont, 14, 700, c.ink))
        reasons.forEach { reason ->
            Text(
                reason,
                Modifier.fillMaxWidth().padding(top = 7.dp, bottom = 6.dp),
                style = style(BodyFont, 15, color = c.inkMuted, lineHeight = 1.2f),
            )
            Box(Modifier.fillMaxWidth().height(1.dp).background(c.paperRule))
        }
    }
}

/** The keys on the foot of the card: the outcome first and widest, then Open and Later. */
@Composable
private fun NowKeys(s: StripUi, onOpen: (Long) -> Unit, onAction: (Long, HomeAction) -> Unit) {
    val c = AimapTheme.colors
    val bay = holder(s.bay)
    Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp)) {
        KeyBar(gap = 10.dp) {
            s.keys.forEach { key ->
                when (key) {
                    StripKey.Handled ->
                        QueueKey("Handled", Lucide.Check, bay) { onAction(s.messageId, HomeAction.Handled) }
                    StripKey.ItWasMe ->
                        QueueKey("It was me", Lucide.ShieldCheck, bay) { onAction(s.messageId, HomeAction.Handled) }
                    StripKey.Reply ->
                        QueueKey("Reply", Lucide.Reply, bay) { onOpen(s.messageId) }
                    // No API for either answer yet, so both read but neither acts.
                    StripKey.NeedsReply -> QueueKey("Needs a reply", Lucide.Reply, c.holderBlue, onClick = null)
                    StripKey.CanGo -> QueueKey("Can go", Lucide.Archive, c.holderCango, onClick = null)
                }
            }
            QueueKey("Open", Lucide.MailOpen, fill = null) { onOpen(s.messageId) }
            QueueKey("Later", Lucide.SkipForward, fill = null) { onAction(s.messageId, HomeAction.Later) }
        }
    }
}

/**
 * A named key, 48dp tall: filled with its bay colour when it decides the message, outlined when
 * it only moves it. A null [onClick] means the API cannot do it yet, so the key reads but does
 * not act.
 */
@Composable
private fun QueueKey(label: String, icon: ImageVector, fill: Color?, onClick: (() -> Unit)?) {
    val c = AimapTheme.colors
    Box(
        Modifier.heightIn(min = 48.dp)
            .then(
                if (fill != null) Modifier.background(if (onClick != null) fill else fill.copy(alpha = 0.6f), Plate)
                else Modifier.border(1.5.dp, c.ink, Plate)
            )
            .then(
                if (onClick != null) Modifier.clickable(role = Role.Button, onClickLabel = label, onClick = onClick)
                else Modifier.semantics(mergeDescendants = true) {
                    role = Role.Button
                    contentDescription = label
                    stateDescription = "Coming soon"
                    disabled()
                }
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null, Modifier.size(20.dp), tint = c.ink)
            Text(label, style = style(CallFont, 16, 700, c.ink), softWrap = false)
        }
    }
}

/**
 * Keys on one line while they fit, the first one taking whatever room is left over. When they
 * stop fitting the line breaks, so at a large font scale each key gets a line to itself.
 */
@Composable
private fun KeyBar(gap: Dp, content: @Composable () -> Unit) {
    Layout(content) { measurables, constraints ->
        val width = constraints.maxWidth
        val gapPx = gap.roundToPx()
        val want = measurables.map { it.maxIntrinsicWidth(Constraints.Infinity).coerceAtMost(width) }
        val lines = mutableListOf<MutableList<Int>>()
        want.indices.forEach { i ->
            val line = lines.lastOrNull()
            val used = line?.sumOf { want[it] + gapPx } ?: 0
            if (line == null || used + want[i] > width) lines += mutableListOf(i) else line += i
        }
        val placed = arrayOfNulls<Placeable>(measurables.size)
        lines.forEach { line ->
            // The first key on the line fills the width the others leave.
            val slack = width - line.sumOf { want[it] } - gapPx * (line.size - 1)
            line.forEachIndexed { j, i ->
                val w = (want[i] + if (j == 0) slack else 0).coerceAtLeast(0)
                placed[i] = measurables[i].measure(Constraints(minWidth = w, maxWidth = w))
            }
        }
        val heights = lines.map { line -> line.maxOf { placed[it]!!.height } }
        layout(width, heights.sum() + gapPx * (lines.size - 1).coerceAtLeast(0)) {
            var y = 0
            lines.forEachIndexed { li, line ->
                var x = 0
                line.forEach { i ->
                    val key = placed[i]!!
                    key.place(x, y)
                    x += key.width + gapPx
                }
                y += heights[li] + gapPx
            }
        }
    }
}

/** A queued message waiting its turn: no keys, just its place in the line. Tap to bring it up. */
@Composable
private fun QueueRow(s: StripUi, position: Int, onPick: () -> Unit) {
    val c = AimapTheme.colors
    val row = @Composable { modifier: Modifier ->
        Row(
            modifier.fillMaxWidth().height(IntrinsicSize.Min).background(c.paper)
                .clickable(onClickLabel = "Bring to Now", onClick = onPick),
        ) {
            Column(
                Modifier.widthIn(min = 60.dp).fillMaxHeight().background(holder(s.bay))
                    .padding(start = 10.dp, top = 8.dp, end = 8.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    s.top,
                    style = if (s.topIsAge) style(DataFont, 20, 800, c.ink) else style(CallFont, 15, 800, c.ink),
                    softWrap = false,
                )
                Text("$position", style = style(DataFont, 11, color = c.ink))
            }
            Column(
                Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(s.sender, style = style(CallFont, 20, 800, c.ink))
                Text(s.subject, style = style(BodyFont, 15, color = c.ink, lineHeight = 1.22f))
                if (s.cocked) {
                    Text(
                        "Unsure: does this need a reply? You'll decide when it's up.",
                        style = style(CallFont, 14, 600, c.ink, lineHeight = 1.15f),
                    )
                }
            }
        }
    }
    // needs_review: cocked out of line. The angle says the agent is not sure about this one.
    if (s.cocked) row(Modifier.graphicsLayer { rotationZ = -1.2f }) else row(Modifier)
}

/** The last step, as one sheet: every pile, and what the bar under it does. */
@Composable
private fun PileSheet(piles: List<PileUi>, canGo: Int) {
    val c = AimapTheme.colors
    Column(Modifier.fillMaxWidth().clip(Plate).background(c.paper)) {
        piles.forEachIndexed { i, p ->
            if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(c.paperRule))
            Row(
                // Looking inside a pile needs an API that is not there yet.
                Modifier.fillMaxWidth().heightIn(min = 56.dp)
                    .semantics(mergeDescendants = true) {
                        role = Role.Button
                        contentDescription = "${p.name}, ${p.count}"
                        stateDescription = "Coming soon"
                        disabled()
                    }
                    .padding(start = 14.dp, top = 10.dp, end = 10.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("${p.count}", Modifier.widthIn(min = 20.dp), style = style(DataFont, 18, 700, c.ink))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(p.name, style = style(CallFont, 19, 800, c.ink))
                    Text(p.summary, style = style(BodyFont, 14, color = c.inkMuted, lineHeight = 1.25f))
                }
                Icon(Lucide.ChevronRight, null, Modifier.size(20.dp), tint = c.inkMuted)
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.paperRule))
        Text(
            pileNote(canGo),
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            style = style(BodyFont, 14, color = c.inkMuted, lineHeight = 1.3f),
        )
    }
}

/** The sample queue with the message the agent is unsure about first in line. */
private val reviewFirst = SampleHome.home.let { h ->
    h.copy(
        brief = h.brief.copy(actNow = 0, waiting = 2),
        actNow = emptyList(),
        waiting = h.waiting.filter { it.needsReview } + h.waiting.filterNot { it.needsReview }.take(1),
    )
}

@Preview(name = "Home v5 · dark", widthDp = 390, heightDp = 1580)
@Composable
private fun HomeV5DarkPreview() {
    AimapTheme(darkTheme = true) {
        HomeV5Screen(SampleHome.home.toUi(SampleHome.now, accounts = 3, initials = "AR"))
    }
}

@Preview(name = "Home v5 · light", widthDp = 390, heightDp = 1580)
@Composable
private fun HomeV5LightPreview() {
    AimapTheme(darkTheme = false) {
        HomeV5Screen(SampleHome.home.toUi(SampleHome.now, accounts = 3, initials = "AR"))
    }
}

@Preview(name = "Home v5 · unsure at Now", widthDp = 390, heightDp = 844)
@Composable
private fun HomeV5ReviewPreview() {
    AimapTheme(darkTheme = true) {
        HomeV5Screen(reviewFirst.toUi(SampleHome.now, accounts = 3, initials = "AR"))
    }
}

@Preview(name = "Home v5 · all clear", widthDp = 390, heightDp = 844)
@Composable
private fun HomeV5ClearPreview() {
    AimapTheme(darkTheme = true) {
        HomeV5Screen(SampleHome.clear.toUi(SampleHome.now, accounts = 1, initials = "AR"))
    }
}
