package pe.net.libre.aimap_client.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import pe.net.libre.aimap_client.ui.theme.AimapTheme
import pe.net.libre.aimap_client.ui.theme.BodyFont
import pe.net.libre.aimap_client.ui.theme.CallFont
import pe.net.libre.aimap_client.ui.theme.DataFont
import kotlin.math.roundToInt

internal val Plate = RoundedCornerShape(2.dp)
internal val Gutter = 20.dp

/** The seam between two strips in a rack, and between two plates in the bar. */
private val Seam = 1.dp
private val PlateSeam = 2.dp

internal fun style(font: FontFamily, size: Int, weight: Int = 400, color: Color, lineHeight: Float? = null) = TextStyle(
    fontFamily = font,
    fontSize = size.sp,
    fontWeight = FontWeight(weight),
    color = color,
    lineHeight = lineHeight?.em ?: TextStyle.Default.lineHeight,
    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
)

/** The Bay, v4: one rack per bay, and the job named on a key inside each strip. */
@Composable
fun HomeScreen(
    ui: HomeUi,
    modifier: Modifier = Modifier,
    onOpen: (messageId: Long) -> Unit = {},
    onAction: (messageId: Long, action: HomeAction) -> Unit = { _, _ -> },
    onArchiveAll: () -> Unit = {},
    onAvatar: () -> Unit = {},
) {
    val c = AimapTheme.colors
    val list = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var scrubHour by remember { mutableStateOf<Int?>(null) }
    val inHour = { s: StripUi -> scrubHour == null || s.hourToday == scrubHour }
    val bayIndex = mapOf(Bay.NeedsYou to 3, Bay.Waiting to 4, Bay.CanGo to 5)

    Column(modifier.fillMaxSize().background(c.console)) {
        LazyColumn(Modifier.weight(1f).statusBarsPadding(), state = list) {
            item { ConsoleHeader(ui.header, onAvatar) }
            item {
                BayPlates(ui.plates) { bay -> scope.launch { list.animateScrollToItem(bayIndex.getValue(bay)) } }
            }
            item {
                Text(
                    ui.guide,
                    Modifier.fillMaxWidth().padding(start = Gutter, top = 12.dp, end = Gutter),
                    style = style(BodyFont, 15, color = c.onConsole, lineHeight = 1.25f),
                )
            }
            item {
                BaySection(Bay.NeedsYou, "Needs you", "handle these first", top = 18.dp, bottom = 4.dp) {
                    Rack(ui.needsYou.filter(inHour)) { Strip(it, onOpen, onAction) }
                }
            }
            item {
                BaySection(Bay.Waiting, "Waiting on you", "reply, oldest first", top = 18.dp, bottom = 4.dp) {
                    Rack(ui.waiting.filter(inHour)) { Strip(it, onOpen, onAction) }
                }
            }
            item {
                BaySection(Bay.CanGo, "Can go", "glance, then archive", top = 18.dp, bottom = 4.dp, gap = 10.dp) {
                    Rack(ui.piles) { Pile(it) }
                    UnsortedSlot(ui.unsorted)
                }
            }
            item { Traffic(ui.traffic, scrubHour) { scrubHour = it } }
            item {
                Text(
                    ui.footer,
                    Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 14.dp).wrapContentWidth(),
                    style = style(DataFont, 11, color = c.onConsoleMuted),
                )
            }
        }
        ArchiveDock(ui.archiveCount, onArchiveAll)
        TabBar()
    }
}

@Composable
internal fun ConsoleHeader(h: HeaderUi, onAvatar: () -> Unit) {
    val c = AimapTheme.colors
    Row(
        Modifier.fillMaxWidth().padding(start = Gutter, top = 4.dp, end = Gutter, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier.height(44.dp).border(1.dp, c.rail, Plate).clickable(onClickLabel = "Switch inbox") {}
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(h.account, style = style(CallFont, 18, 700, c.onConsole))
            Text("${h.accountCount}", style = style(DataFont, 12, color = c.onConsoleMuted))
            Icon(Lucide.ChevronDown, null, Modifier.size(18.dp), tint = c.onConsoleMuted)
        }
        // The sort readout gives up its room first: the avatar is the way into the account sheet.
        Box(Modifier.weight(1f).clipToBounds(), contentAlignment = Alignment.CenterEnd) {
            h.lastSort?.let {
                Text(it, style = style(DataFont, 12, color = c.onConsoleMuted), softWrap = false)
            }
        }
        Box(
            Modifier.size(44.dp).border(1.5.dp, c.onConsoleMuted, CircleShape)
                .clip(CircleShape).clickable(onClickLabel = "Account", onClick = onAvatar),
            contentAlignment = Alignment.Center,
        ) {
            Text(h.initials, style = style(CallFont, 16, 700, c.onConsole))
        }
    }
}

@Composable
internal fun holder(bay: Bay) = AimapTheme.colors.let {
    when (bay) {
        Bay.NeedsYou -> it.holderYellow
        Bay.Waiting -> it.holderBlue
        Bay.CanGo -> it.holderCango
    }
}

/** The three counts as one bar: console seams between the plates, rounded only outside. */
@Composable
private fun BayPlates(counts: Map<Bay, Int>, onPlate: (Bay) -> Unit) {
    val c = AimapTheme.colors
    val labels = mapOf(Bay.NeedsYou to "Needs you", Bay.Waiting to "Waiting\non you", Bay.CanGo to "Can go")
    Row(
        Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(horizontal = Gutter).clip(Plate),
    ) {
        Bay.entries.forEachIndexed { i, bay ->
            if (i > 0) Box(Modifier.width(PlateSeam).fillMaxHeight().background(c.console))
            Column(
                Modifier.weight(1f).fillMaxHeight().heightIn(min = 94.dp).background(holder(bay))
                    .clickable(onClickLabel = "Jump to bay") { onPlate(bay) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("${counts[bay] ?: 0}", style = style(DataFont, 36, 700, c.ink, lineHeight = 1f))
                Text(labels.getValue(bay), style = style(CallFont, 18, 700, c.ink, lineHeight = 1.05f))
            }
        }
    }
}

/** Today's arrivals per hour. The whole row is one scrub control that snaps to the hour. */
@Composable
private fun Traffic(t: TrafficUi, scrubHour: Int?, onScrub: (Int?) -> Unit) {
    val c = AimapTheme.colors
    val now = t.columns.firstOrNull { it.isNow }?.hour ?: LAST_HOUR
    val scrubbed = t.columns.firstOrNull { it.hour == scrubHour }
    val readout = scrubbed?.let { "%02d:00 · %d arrived".format(it.hour, it.arrived) } ?: t.total
    fun hourAt(x: Float, width: Int) =
        (FIRST_HOUR + (x / width * t.columns.size).toInt()).coerceIn(FIRST_HOUR, now)

    Column(
        Modifier.fillMaxWidth().padding(start = Gutter, top = 20.dp, end = Gutter, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Bottom) {
            Text("Today's traffic", style = style(CallFont, 18, 700, c.onConsole))
            Text(readout, style = style(DataFont, 11, color = if (scrubbed != null) c.onConsole else c.onConsoleMuted))
        }
        Row(
            Modifier.fillMaxWidth().height(60.dp)
                .pointerInput(t.columns.size, now) {
                    detectHorizontalDragGestures(
                        onDragStart = { onScrub(hourAt(it.x, size.width)) },
                        onDragEnd = { onScrub(null) },
                        onDragCancel = { onScrub(null) },
                    ) { change, _ -> onScrub(hourAt(change.position.x, size.width)) }
                }
                .semantics(mergeDescendants = true) {
                    contentDescription = "Today's traffic"
                    stateDescription = readout
                    progressBarRangeInfo = ProgressBarRangeInfo(
                        (scrubHour ?: now).toFloat(), FIRST_HOUR.toFloat()..now.toFloat(), now - FIRST_HOUR - 1,
                    )
                    setProgress { v ->
                        val h = v.roundToInt().coerceIn(FIRST_HOUR, now)
                        onScrub(if (h == now) null else h)
                        true
                    }
                },
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            t.columns.forEach { col ->
                val outlined = if (scrubHour != null) col.hour == scrubHour else col.isNow
                Column(
                    Modifier.weight(1f).fillMaxHeight()
                        .then(if (outlined) Modifier.border(1.5.dp, c.onConsole, Plate) else Modifier)
                        .padding(horizontal = 2.dp, vertical = 3.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.Bottom),
                ) {
                    if (col.isFuture) {
                        Box(Modifier.fillMaxWidth().height(2.dp).background(c.rail))
                    }
                    col.blocks.asReversed().forEach { bay ->
                        val fill = if (bay == Bay.CanGo) c.trafficCango else holder(bay)
                        Box(Modifier.fillMaxWidth().height(5.dp).background(fill))
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            t.columns.forEach { col ->
                Box(Modifier.weight(1f).height(14.dp), contentAlignment = Alignment.Center) {
                    col.axisLabel?.let {
                        val isNow = col.isNow
                        Text(
                            it,
                            Modifier.wrapContentWidth(unbounded = true),
                            style = style(DataFont, 10, if (isNow) 700 else 400, if (isNow) c.onConsole else c.onConsoleMuted),
                            softWrap = false,
                        )
                    }
                }
            }
        }
    }
}

/** A section head over its rack: the bay's chip, if it has one, then the title and its note. */
@Composable
internal fun BaySection(
    bay: Bay?, title: String, note: String, top: Dp, bottom: Dp, gap: Dp = 8.dp,
    content: @Composable () -> Unit,
) {
    val c = AimapTheme.colors
    Column(
        Modifier.fillMaxWidth().padding(start = Gutter, top = top, end = Gutter, bottom = bottom),
        verticalArrangement = Arrangement.spacedBy(gap),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(bottom = 2.dp).semantics(mergeDescendants = true) {},
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (bay != null) Box(Modifier.size(14.dp).background(holder(bay), Plate))
            Text(title, style = style(CallFont, 22, 800, c.onConsole))
            Box(Modifier.weight(1f).height(1.dp).background(c.rail))
            Text(note, style = style(CallFont, 14, 600, c.onConsoleMuted))
        }
        content()
    }
}

/**
 * Two slots on one line, the second against the right edge. When the first cannot say its piece
 * on a single line in the space left over, the second drops below it instead of crowding it.
 * With one child there is nothing to split, so it just sits at the start.
 */
@Composable
internal fun SplitRow(modifier: Modifier, gap: Dp, content: @Composable () -> Unit) {
    Layout(content, modifier) { measurables, constraints ->
        val width = constraints.maxWidth
        // Free height: a key keeps its touch target even when the strip is measured to fit.
        val loose = Constraints(maxWidth = width)
        if (measurables.size < 2) {
            val only = measurables.firstOrNull()?.measure(loose)
            return@Layout layout(width, only?.height ?: 0) { only?.place(0, 0) }
        }
        val gapPx = gap.roundToPx()
        val tail = measurables[1].measure(loose)
        val room = width - tail.width - gapPx
        val beside = measurables[0].maxIntrinsicWidth(Constraints.Infinity) <= room
        val head = measurables[0].measure(loose.copy(maxWidth = (if (beside) room else width).coerceAtLeast(0)))
        if (beside) {
            val height = maxOf(head.height, tail.height)
            layout(width, height) {
                head.place(0, (height - head.height) / 2)
                tail.place(width - tail.width, (height - tail.height) / 2)
            }
        } else {
            layout(width, head.height + gapPx + tail.height) {
                head.place(0, 0)
                tail.place(0, head.height + gapPx)
            }
        }
    }
}

/** Keys in a row that spills onto the next line when the row runs out of width. */
@Composable
private fun WrapRow(gap: Dp, content: @Composable () -> Unit) {
    Layout(content) { measurables, constraints ->
        val gapPx = gap.roundToPx()
        val keys = measurables.map { it.measure(Constraints(maxWidth = constraints.maxWidth)) }
        var x = 0
        var y = 0
        var lineHeight = 0
        var width = 0
        val spots = keys.map { key ->
            if (x > 0 && x + key.width > constraints.maxWidth) {
                y += lineHeight + gapPx
                x = 0
                lineHeight = 0
            }
            val spot = x to y
            x += key.width + gapPx
            lineHeight = maxOf(lineHeight, key.height)
            width = maxOf(width, x - gapPx)
            spot
        }
        layout(width, y + lineHeight) {
            keys.forEachIndexed { i, key -> key.place(spots[i].first, spots[i].second) }
        }
    }
}

/** One rack per bay: strips edge to edge over a rail-coloured seam, rounded only outside. */
@Composable
internal fun <T> Rack(items: List<T>, row: @Composable (T) -> Unit) {
    val c = AimapTheme.colors
    Column(Modifier.fillMaxWidth().clip(Plate)) {
        items.forEachIndexed { i, item ->
            if (i > 0) Box(Modifier.fillMaxWidth().height(Seam).background(c.rail))
            row(item)
        }
    }
}

private fun Modifier.leftRule(color: Color) = drawBehind {
    drawLine(color, Offset(0f, 0f), Offset(0f, size.height), 1.dp.toPx())
}

private fun Modifier.topRule(color: Color) = drawBehind {
    drawLine(color, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx())
}

/** A paper strip in its holder: holder end, then the body with the job on a key. */
@Composable
private fun Strip(s: StripUi, onOpen: (Long) -> Unit, onAction: (Long, HomeAction) -> Unit) {
    val c = AimapTheme.colors
    val strip = @Composable { modifier: Modifier ->
        Row(
            modifier.fillMaxWidth().height(IntrinsicSize.Min).background(c.paper)
                .clickable(onClickLabel = "Open") { onOpen(s.messageId) },
        ) {
            HolderEnd(s)
            StripBody(s, Modifier.weight(1f), onOpen, onAction)
        }
    }
    if (s.cocked) {
        // needs_review: cocked out of line, out of the rack. The angle is the signal.
        Box(Modifier.fillMaxWidth().padding(start = 10.dp, top = 6.dp, end = 6.dp, bottom = 10.dp)) {
            strip(Modifier.graphicsLayer { rotationZ = 1.5f })
        }
    } else {
        strip(Modifier)
    }
}

@Composable
private fun HolderEnd(s: StripUi) {
    val c = AimapTheme.colors
    Column(
        Modifier.widthIn(min = 60.dp).fillMaxHeight().background(holder(s.bay))
            .padding(start = 10.dp, top = 10.dp, end = 8.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        val topStyle = if (s.topIsAge) style(DataFont, 20, 800, c.ink) else style(CallFont, 15, 800, c.ink)
        Box(
            Modifier.widthIn(min = 42.dp).heightIn(min = 22.dp)
                .then(if (s.greased) Modifier.greaseRing(if (s.topIsAge) 56.dp else 60.dp) else Modifier),
            contentAlignment = Alignment.TopStart,
        ) {
            Text(s.top, style = topStyle, softWrap = false)
        }
        val bottomStyle =
            if (s.bottomIsWord) style(CallFont, 13, 600, c.ink) else style(DataFont, 11, color = c.ink)
        Text(s.bottom, style = bottomStyle)
    }
}

private val GreasePath = PathParser()
    .parsePathString("M5 15c-3-8 8-13 20-12 12 1 20 7 16 15-4 7-22 9-32 5-6-3-5-11 3-16")
    .toPath()

/** The agent's urgency pencil: an open hand-drawn loop with an ink keyline. */
@Composable
internal fun Modifier.greaseRing(width: Dp): Modifier {
    val c = AimapTheme.colors
    return drawWithContent {
        drawContent()
        val w = width.toPx()
        val h = 38.dp.toPx()
        val path = androidx.compose.ui.graphics.Path().apply {
            addPath(GreasePath)
            transform(Matrix().apply { scale(w / 46f, h / 28f) })
        }
        translate(-13.dp.toPx(), -8.dp.toPx()) {
            drawPath(path, c.ink, style = Stroke(3.6.dp.toPx(), cap = StrokeCap.Round))
            drawPath(path, c.grease, style = Stroke(2.4.dp.toPx(), cap = StrokeCap.Round))
        }
    }
}

@Composable
private fun StripBody(
    s: StripUi, modifier: Modifier, onOpen: (Long) -> Unit, onAction: (Long, HomeAction) -> Unit,
) {
    val c = AimapTheme.colors
    Column(modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp), Alignment.CenterVertically) {
            Text(s.sender, Modifier.weight(1f), style = style(CallFont, 20, 800, c.ink))
            // Pushed to later: the strip sits at the end of its rack and says why.
            if (s.later) Text("LATER", style = style(DataFont, 10, 700, c.inkMuted), softWrap = false)
            Text(s.account, style = style(DataFont, 11, color = c.inkMuted))
        }
        Text(s.subject, style = style(BodyFont, 15, color = c.ink, lineHeight = 1.22f))
        SplitRow(Modifier.fillMaxWidth().padding(top = 2.dp), gap = 8.dp) {
            if (s.reason != null) Reasons(s)
            WrapRow(gap = 8.dp) {
                s.keys.forEach { StripKeyButton(it, s, onOpen, onAction) }
            }
        }
    }
}

@Composable
private fun Reasons(s: StripUi) {
    val c = AimapTheme.colors
    val reasonStyle = style(CallFont, 14, 600, if (s.cocked) c.ink else c.inkMuted)
    Row(Modifier.height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
        Text(s.reason.orEmpty(), Modifier.padding(end = 8.dp, top = 2.dp, bottom = 2.dp), style = reasonStyle)
        if (s.moreReasons > 0) {
            Text(
                "+${s.moreReasons}",
                Modifier.fillMaxHeight().leftRule(c.paperRule).padding(horizontal = 8.dp, vertical = 2.dp),
                style = reasonStyle,
            )
        }
    }
}

@Composable
private fun StripKeyButton(
    key: StripKey, s: StripUi, onOpen: (Long) -> Unit, onAction: (Long, HomeAction) -> Unit,
) {
    val c = AimapTheme.colors
    val bay = holder(s.bay)
    when (key) {
        StripKey.Handled -> Key("Handled", Lucide.Check, bay) { onAction(s.messageId, HomeAction.Handled) }
        StripKey.ItWasMe -> Key("It was me", Lucide.ShieldCheck, bay) { onAction(s.messageId, HomeAction.Handled) }
        StripKey.Reply -> Key("Reply", Lucide.Reply, bay) { onOpen(s.messageId) }
        StripKey.NeedsReply -> Key("Needs a reply", Lucide.Reply, c.holderBlue, onClick = null)
        StripKey.CanGo -> Key("Can go", Lucide.Archive, c.holderCango, onClick = null)
    }
}

/**
 * The job, named and filled with the bay colour: 40dp of key inside a 48dp touch target.
 * A null [onClick] means the API cannot do it yet, so the key reads but does not act.
 */
@Composable
private fun Key(label: String, icon: ImageVector, fill: Color, onClick: (() -> Unit)?) {
    val c = AimapTheme.colors
    Box(
        Modifier.heightIn(min = 48.dp).then(
            if (onClick != null) Modifier.clickable(role = Role.Button, onClickLabel = label, onClick = onClick)
            else Modifier.semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = label
                stateDescription = "Coming soon"
                disabled()
            }
        ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier.heightIn(min = 40.dp).background(if (onClick != null) fill else fill.copy(alpha = 0.6f), Plate)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null, Modifier.size(20.dp), tint = c.ink)
            Text(label, style = style(CallFont, 15, 700, c.ink), softWrap = false)
        }
    }
}

/** A sorted group: the count and its latest time in the holder, the senders in the body. */
@Composable
private fun Pile(p: PileUi) {
    val c = AimapTheme.colors
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).height(IntrinsicSize.Min).background(c.paper),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            Modifier.widthIn(min = 60.dp).fillMaxHeight().background(c.holderCango).padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("${p.count}", style = style(DataFont, 22, 700, c.ink))
            p.latest?.let { Text(it, style = style(DataFont, 11, color = c.ink), softWrap = false) }
        }
        Column(
            Modifier.weight(1f).padding(start = 12.dp, top = 8.dp, end = 8.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(p.name, style = style(CallFont, 19, 800, c.ink))
            Text(p.summary, style = style(BodyFont, 14, color = c.inkMuted, lineHeight = 1.25f))
        }
        Box(Modifier.padding(end = 12.dp)) {
            Key("Archive ${p.count}", Lucide.Archive, c.holderCango, onClick = null)
        }
    }
}

private fun DrawScope.dashedOutline(color: Color) {
    val w = 1.25.dp.toPx()
    drawRoundRect(
        color,
        topLeft = Offset(w / 2, w / 2),
        size = Size(size.width - w, size.height - w),
        cornerRadius = CornerRadius(2.dp.toPx()),
        style = Stroke(w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))),
    )
}

/** Mail the agent has not sorted yet. Always visible, never hidden. */
@Composable
internal fun UnsortedSlot(count: Int) {
    val c = AimapTheme.colors
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).drawBehind { dashedOutline(c.onConsoleMuted) }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Lucide.Loader, null, Modifier.size(18.dp), tint = c.onConsoleMuted)
        Text(
            if (count == 0) "Everything is sorted." else "$count not sorted yet. The agent is still reading ${if (count == 1) "it" else "them"}.",
            Modifier.weight(1f),
            style = style(BodyFont, 14, color = c.onConsoleMuted),
        )
    }
}

/** Hold the bar for about 600 ms to archive every pile. Archived, never deleted. */
@Composable
internal fun ArchiveDock(count: Int, onArchiveAll: () -> Unit) {
    val c = AimapTheme.colors
    val haptics = LocalHapticFeedback.current
    val progress = remember { Animatable(0f) }
    var confirm by rememberSaveable { mutableStateOf(false) }
    val label = "Archive all $count"

    Box(Modifier.fillMaxWidth().background(c.consoleDeep).topRule(c.rail).padding(horizontal = Gutter, vertical = 10.dp)) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp).background(c.holderCango, RoundedCornerShape(3.dp))
                .drawBehind {
                    drawRoundRect(
                        c.ink.copy(alpha = 0.14f),
                        size = Size(size.width * progress.value, size.height),
                        cornerRadius = CornerRadius(3.dp.toPx()),
                    )
                }
                .pointerInput(count) {
                    detectTapGestures(onPress = {
                        coroutineScope {
                            val hold = launch {
                                progress.animateTo(1f, tween(600, easing = LinearEasing))
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onArchiveAll()
                            }
                            tryAwaitRelease()
                            hold.cancel()
                            progress.animateTo(0f, tween(150))
                        }
                    })
                }
                .semantics(mergeDescendants = true) {
                    role = Role.Button
                    onClick(label) { confirm = true; true }
                }
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(48.dp).background(c.ink, Plate), contentAlignment = Alignment.Center) {
                Icon(Lucide.Archive, null, Modifier.size(22.dp), tint = c.holderCango)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text("Hold to archive all $count", style = style(CallFont, 18, 800, c.ink))
                Text("Archived, not deleted. Undo in Activity.", style = style(BodyFont, 13, color = c.ink))
            }
        }
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("$label that can go?") },
            text = { Text("They are archived, not deleted. Undo lives in Activity.") },
            confirmButton = { TextButton({ confirm = false; onArchiveAll() }) { Text(label) } },
            dismissButton = { TextButton({ confirm = false }) { Text("Cancel") } },
        )
    }
}

@Composable
internal fun TabBar() {
    val c = AimapTheme.colors
    Row(Modifier.fillMaxWidth().background(c.consoleDeep).navigationBarsPadding().height(58.dp)) {
        Tab("Bay", Lucide.LayoutList, active = true)
        Tab("All mail", Lucide.Inbox, active = false)
        Tab("Rules", Lucide.SlidersHorizontal, active = false)
    }
}

@Composable
private fun RowScope.Tab(label: String, icon: ImageVector, active: Boolean) {
    val c = AimapTheme.colors
    val color = if (active) c.onConsole else c.onConsoleMuted
    Column(
        Modifier.weight(1f).fillMaxHeight()
            .then(
                if (active) Modifier.drawBehind { drawRect(c.holderYellow, size = Size(size.width, 3.dp.toPx())) }
                else Modifier
            )
            .clickable(role = Role.Tab) {},
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, Modifier.size(22.dp), tint = color)
        Text(label, style = style(CallFont, 13, if (active) 800 else 600, color))
    }
}

@Preview(name = "Home v4 · dark", widthDp = 390, heightDp = 1810)
@Composable
private fun HomeDarkPreview() {
    AimapTheme(darkTheme = true) {
        HomeScreen(SampleHome.home.toUi(SampleHome.now, accounts = 3, initials = "AR"))
    }
}

@Preview(name = "Home v4 · light", widthDp = 390, heightDp = 844)
@Composable
private fun HomeLightPreview() {
    AimapTheme(darkTheme = false) {
        HomeScreen(SampleHome.home.toUi(SampleHome.now, accounts = 3, initials = "AR"))
    }
}

@Preview(name = "Home v4 · all clear", widthDp = 390, heightDp = 844)
@Composable
private fun HomeClearPreview() {
    AimapTheme(darkTheme = true) {
        HomeScreen(SampleHome.clear.toUi(SampleHome.now, accounts = 1, initials = "AR"))
    }
}
