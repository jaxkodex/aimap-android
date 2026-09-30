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
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
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

private val Plate = RoundedCornerShape(2.dp)
private val Gutter = 20.dp

private fun style(font: FontFamily, size: Int, weight: Int = 400, color: Color, lineHeight: Float? = null) = TextStyle(
    fontFamily = font,
    fontSize = size.sp,
    fontWeight = FontWeight(weight),
    color = color,
    lineHeight = lineHeight?.em ?: TextStyle.Default.lineHeight,
    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
)

/** The Bay: the home screen from the Pencil design (app-v3.pen, "Home — full scroll"). */
@Composable
fun HomeScreen(
    ui: HomeUi,
    modifier: Modifier = Modifier,
    onOpen: (Long) -> Unit = {},
    onAction: (StripUi) -> Unit = {},
    onArchivePile: (PileUi) -> Unit = {},
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
            item { Traffic(ui.traffic, scrubHour) { scrubHour = it } }
            item {
                BaySection(Bay.NeedsYou, "Needs you", "by priority", top = 14.dp, bottom = 4.dp) {
                    ui.needsYou.filter(inHour).forEach { Strip(it, onOpen, onAction) }
                }
            }
            item {
                BaySection(Bay.Waiting, "Waiting on you", "longest first", top = 16.dp, bottom = 8.dp) {
                    ui.waiting.filter(inHour).forEach { Strip(it, onOpen, onAction) }
                }
            }
            item {
                BaySection(Bay.CanGo, "Can go", "biggest pile first", top = 16.dp, bottom = 24.dp, gap = 10.dp) {
                    ui.piles.forEach { Pile(it, onArchivePile) }
                    UnsortedSlot(ui.unsorted)
                    Text(
                        ui.footer,
                        Modifier.fillMaxWidth().padding(top = 12.dp).wrapContentWidth(),
                        style = style(DataFont, 11, color = c.onConsoleMuted),
                    )
                }
            }
        }
        ArchiveDock(ui.archiveCount, onArchiveAll)
        TabBar()
    }
}

@Composable
private fun ConsoleHeader(h: HeaderUi, onAvatar: () -> Unit) {
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
        Spacer(Modifier.weight(1f))
        h.lastSort?.let { Text(it, style = style(DataFont, 12, color = c.onConsoleMuted)) }
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
private fun holder(bay: Bay) = AimapTheme.colors.let {
    when (bay) {
        Bay.NeedsYou -> it.holderYellow
        Bay.Waiting -> it.holderBlue
        Bay.CanGo -> it.holderCango
    }
}

@Composable
private fun BayPlates(counts: Map<Bay, Int>, onPlate: (Bay) -> Unit) {
    val labels = mapOf(Bay.NeedsYou to "Needs you", Bay.Waiting to "Waiting\non you", Bay.CanGo to "Can go")
    Row(
        Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(horizontal = Gutter),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Bay.entries.forEach { bay ->
            Column(
                Modifier.weight(1f).fillMaxHeight().heightIn(min = 94.dp).background(holder(bay), Plate)
                    .clickable(onClickLabel = "Jump to bay") { onPlate(bay) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                val ink = AimapTheme.colors.ink
                Text("${counts[bay] ?: 0}", style = style(DataFont, 36, 700, ink, lineHeight = 1f))
                Text(labels.getValue(bay), style = style(CallFont, 18, 700, ink, lineHeight = 1.05f))
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
        Modifier.fillMaxWidth().padding(start = Gutter, top = 14.dp, end = Gutter, bottom = 4.dp),
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

@Composable
private fun BaySection(
    bay: Bay, title: String, note: String, top: Dp, bottom: Dp, gap: Dp = 8.dp,
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
            Box(Modifier.size(14.dp).background(holder(bay), Plate))
            Text(title, style = style(CallFont, 22, 800, c.onConsole))
            Box(Modifier.weight(1f).height(1.dp).background(c.rail))
            Text(note, style = style(DataFont, 11, color = c.onConsoleMuted))
        }
        content()
    }
}

private fun Modifier.leftRule(color: Color) = drawBehind {
    drawLine(color, Offset(0f, 0f), Offset(0f, size.height), 1.dp.toPx())
}

private fun Modifier.topRule(color: Color) = drawBehind {
    drawLine(color, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx())
}

/** A paper strip in its holder: holder end, body, action end. */
@Composable
private fun Strip(s: StripUi, onOpen: (Long) -> Unit, onAction: (StripUi) -> Unit) {
    val c = AimapTheme.colors
    val largeText = LocalDensity.current.fontScale >= 1.3f
    val strip = @Composable { modifier: Modifier ->
        Column(modifier.fillMaxWidth().clip(Plate).background(c.paper).clickable(onClickLabel = "Open") { onOpen(s.messageId) }) {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                HolderEnd(s)
                StripBody(s, Modifier.weight(1f))
                if (!largeText) ActionEnd(s, Modifier.width(56.dp).fillMaxHeight().leftRule(c.paperRule), onAction)
            }
            if (largeText) ActionEnd(s, Modifier.fillMaxWidth().heightIn(min = 48.dp).topRule(c.paperRule), onAction)
        }
    }
    if (s.cocked) {
        // needs_review: cocked out of line. The angle is the signal.
        Box(Modifier.fillMaxWidth().padding(start = 10.dp, top = 4.dp, end = 6.dp, bottom = 8.dp)) {
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
            Modifier.widthIn(min = 42.dp).height(22.dp)
                .then(if (s.greased) Modifier.greaseRing(if (s.topIsAge) 56.dp else 60.dp) else Modifier),
            contentAlignment = Alignment.TopStart,
        ) {
            Text(s.top, style = topStyle, softWrap = false)
        }
        Text(s.bottom, style = style(DataFont, 11, color = c.ink))
    }
}

private val GreasePath = PathParser()
    .parsePathString("M5 15c-3-8 8-13 20-12 12 1 20 7 16 15-4 7-22 9-32 5-6-3-5-11 3-16")
    .toPath()

/** The agent's urgency pencil: an open hand-drawn loop with an ink keyline. */
@Composable
private fun Modifier.greaseRing(width: Dp): Modifier {
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
private fun StripBody(s: StripUi, modifier: Modifier) {
    val c = AimapTheme.colors
    Column(modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp), Alignment.CenterVertically) {
            Text(s.sender, Modifier.weight(1f), style = style(CallFont, 20, 800, c.ink))
            Text(s.account, style = style(DataFont, 11, color = c.inkMuted))
        }
        Text(s.subject, style = style(BodyFont, 15, color = c.ink, lineHeight = 1.22f))
        if (s.reason != null) {
            Row(Modifier.padding(top = 6.dp).height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
                val reasonStyle = style(CallFont, 14, 600, if (s.cocked) c.ink else c.inkMuted)
                Text(s.reason, Modifier.weight(1f, fill = false).padding(end = 8.dp, top = 2.dp, bottom = 2.dp), style = reasonStyle)
                if (s.moreReasons > 0) {
                    Text(
                        "+${s.moreReasons}",
                        Modifier.fillMaxHeight().leftRule(c.paperRule).padding(horizontal = 8.dp, vertical = 2.dp),
                        style = reasonStyle,
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionEnd(s: StripUi, modifier: Modifier, onAction: (StripUi) -> Unit) {
    val c = AimapTheme.colors
    val (icon, label) = when (s.action) {
        StripAction.Done -> Lucide.Check to "Done"
        StripAction.Reply -> Lucide.Reply to "Reply"
    }
    Column(
        modifier.clickable(role = Role.Button) { onAction(s) },
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, Modifier.size(22.dp), tint = c.ink)
        Text(label, style = style(CallFont, 13, 700, c.ink))
    }
}

/** A sorted group: a short strip over 1 to 2 under-sheet edges. */
@Composable
private fun Pile(p: PileUi, onArchive: (PileUi) -> Unit) {
    val c = AimapTheme.colors
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).height(IntrinsicSize.Min).clip(Plate).background(c.paper)) {
            Box(Modifier.widthIn(min = 60.dp).fillMaxHeight().background(c.holderCango), contentAlignment = Alignment.Center) {
                Text("${p.count}", style = style(DataFont, 22, 700, c.ink))
            }
            Column(
                Modifier.weight(1f).fillMaxHeight().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
            ) {
                Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp), Alignment.CenterVertically) {
                    Text(p.name, Modifier.weight(1f), style = style(CallFont, 19, 800, c.ink))
                    p.latest?.let { Text(it, style = style(DataFont, 11, color = c.inkMuted)) }
                }
                Text(p.summary, style = style(BodyFont, 14, color = c.inkMuted))
            }
            Box(
                Modifier.width(56.dp).fillMaxHeight().leftRule(c.paperRule)
                    .clickable(role = Role.Button, onClickLabel = "Archive ${p.name}") { onArchive(p) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Lucide.Archive, "Archive ${p.name}", Modifier.size(22.dp), tint = c.ink)
            }
        }
        repeat(p.underSheets) { i ->
            Box(
                Modifier.fillMaxWidth().padding(horizontal = (6 * (i + 1)).dp).height(3.dp)
                    .background(
                        if (i == 0) c.underSheet else c.underSheetDeep,
                        RoundedCornerShape(bottomStart = 2.dp, bottomEnd = 2.dp),
                    ),
            )
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
private fun UnsortedSlot(count: Int) {
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
private fun ArchiveDock(count: Int, onArchiveAll: () -> Unit) {
    val c = AimapTheme.colors
    val haptics = LocalHapticFeedback.current
    val progress = remember { Animatable(0f) }
    var confirm by rememberSaveable { mutableStateOf(false) }
    val label = "Archive $count"

    Box(Modifier.fillMaxWidth().background(c.consoleDeep).topRule(c.rail).padding(horizontal = Gutter, vertical = 10.dp)) {
        Row(
            Modifier.fillMaxWidth().height(56.dp).background(c.holderCango, RoundedCornerShape(3.dp))
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
                Text("Hold to archive $count that can go", style = style(CallFont, 18, 800, c.ink))
                Text("Archived, not deleted · undo in Activity", style = style(BodyFont, 13, color = c.ink))
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
private fun TabBar() {
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

@Preview(name = "Home · dark", widthDp = 390, heightDp = 1752)
@Composable
private fun HomeDarkPreview() {
    AimapTheme(darkTheme = true) {
        HomeScreen(SampleHome.home.toUi(SampleHome.now, accounts = 3, initials = "AR"))
    }
}

@Preview(name = "Home · light", widthDp = 390, heightDp = 844)
@Composable
private fun HomeLightPreview() {
    AimapTheme(darkTheme = false) {
        HomeScreen(SampleHome.home.toUi(SampleHome.now, accounts = 3, initials = "AR"))
    }
}
