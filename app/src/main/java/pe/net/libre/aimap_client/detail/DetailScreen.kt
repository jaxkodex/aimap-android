package pe.net.libre.aimap_client.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import pe.net.libre.aimap_client.api.MessageState
import pe.net.libre.aimap_client.home.Bay
import pe.net.libre.aimap_client.home.Lucide
import pe.net.libre.aimap_client.ui.theme.AimapTheme
import pe.net.libre.aimap_client.ui.theme.BodyFont
import pe.net.libre.aimap_client.ui.theme.CallFont
import pe.net.libre.aimap_client.ui.theme.DataFont

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

/**
 * The read screen from the Pencil design ("Detail — read"): the message's strip pinned
 * over the letter on paper. The foot is deliberately slim, so the reply tray can take
 * its place once there is a write API.
 */
@Composable
fun DetailScreen(
    read: Read,
    modifier: Modifier = Modifier,
    back: String = "Bay",
    onBack: () -> Unit = {},
    onRetry: () -> Unit = {},
    onRetryBody: () -> Unit = {},
    onAction: (Long, DetailAction) -> Unit = { _, _ -> },
) {
    val c = AimapTheme.colors
    Column(modifier.fillMaxSize().background(c.console)) {
        TopBar(back, onBack)
        when (read) {
            Read.Loading -> Skeleton()
            Read.Gone -> Notice(
                "That message is gone.",
                "aimap has no message with that number any more. The bay will have moved on too.",
                listOf("Back to the $back" to onBack),
            )

            is Read.Failed -> Notice("Couldn't read this message.", read.text, listOf("Try again" to onRetry))
            is Read.Ready -> {
                PinnedStrip(read.ui)
                Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())) {
                    Letter(read.ui, read.body, onRetryBody)
                }
                Foot(read.ui, onAction)
            }
        }
    }
}

@Composable
private fun TopBar(back: String, onBack: () -> Unit) {
    val c = AimapTheme.colors
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(start = 6.dp, end = Gutter, top = 4.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(48.dp).clip(Plate).clickable(role = Role.Button, onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Lucide.ArrowLeft, "Back to the $back", Modifier.size(24.dp), tint = c.onConsole)
        }
        Text(back, style = style(CallFont, 24, 800, c.onConsole))
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

private fun Modifier.leftRule(color: Color) = drawBehind {
    drawLine(color, Offset(0f, 0f), Offset(0f, size.height), 1.dp.toPx())
}

private fun Modifier.topRule(color: Color) = drawBehind {
    drawLine(color, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx())
}

/** The message's own strip, the one the bay showed, pinned above the letter. */
@Composable
private fun PinnedStrip(ui: DetailUi) {
    val c = AimapTheme.colors
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Gutter).height(IntrinsicSize.Min)
            .clip(Plate).background(c.paper)
            .semantics(mergeDescendants = true) {},
    ) {
        Column(
            Modifier.widthIn(min = 60.dp).fillMaxHeight().background(holder(ui.bay))
                .padding(start = 10.dp, top = 10.dp, end = 8.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                ui.top,
                style = if (ui.topIsAge) style(DataFont, 20, 800, c.ink) else style(CallFont, 15, 800, c.ink),
                softWrap = false,
            )
            Text(ui.bottom, Modifier.padding(top = 10.dp), style = style(DataFont, 11, color = c.ink))
        }
        Column(
            Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp), Alignment.CenterVertically) {
                Text(ui.sender, Modifier.weight(1f), style = style(CallFont, 22, 800, c.ink))
                Text(ui.account, style = style(DataFont, 11, color = c.inkMuted))
            }
            Text(ui.subject, style = style(BodyFont, 16, color = c.ink, lineHeight = 1.22f))
            if (ui.fields.isNotEmpty()) Fields(ui.fields, ui.unsure)
        }
    }
}

/**
 * Every stored reason, side by side while they fit and wrapped when they do not. Each field
 * after the first on a line is ruled off from the one before it, so no line starts with a rule:
 * the layout measures the rules and leaves the ones it does not need unplaced.
 */
@Composable
private fun Fields(fields: List<String>, unsure: Boolean) {
    val c = AimapTheme.colors
    val density = LocalDensity.current
    val gap = with(density) { 9.dp.roundToPx() }
    val lineGap = with(density) { 4.dp.roundToPx() }
    Layout(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        content = {
            fields.forEachIndexed { i, field ->
                if (i > 0) Box(Modifier.width(1.dp).height(15.dp).background(c.paperRule))
                Text(
                    field,
                    style = style(CallFont, 14, 600, if (i == 0 && unsure) c.ink else c.inkMuted),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
    ) { measurables, constraints ->
        // The children run field, (rule, field), (rule, field)...
        val width = constraints.maxWidth
        val items = measurables.map { it.measure(Constraints(maxWidth = width)) }
        val placed = mutableListOf<Triple<Placeable, Int, Int>>()
        var x = 0
        var y = 0
        var line = 0
        var i = 0
        while (i < items.size) {
            val first = i == 0
            val rule = if (first) null else items[i]
            val field = items[if (first) i else i + 1]
            val needed = field.width + if (rule == null) 0 else rule.width + 2 * gap
            if (x > 0 && x + needed > width) {
                x = 0
                y += line + lineGap
                line = 0
            } else if (rule != null) {
                placed += Triple(rule, x + gap, y + (field.height - rule.height) / 2)
                x += rule.width + 2 * gap
            }
            placed += Triple(field, x, y)
            x += field.width
            line = maxOf(line, field.height)
            i += if (first) 1 else 2
        }
        layout(width, y + line) { placed.forEach { (p, px, py) -> p.place(px, py) } }
    }
}

/** The letter on paper: who it came from, then the body, folded until asked. */
@Composable
private fun Letter(ui: DetailUi, body: BodyState, onRetryBody: () -> Unit) {
    val c = AimapTheme.colors
    Column(
        Modifier.fillMaxWidth().padding(start = Gutter, top = 8.dp, end = Gutter, bottom = 24.dp)
            .clip(Plate).background(c.paper),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Field("FROM", ui.from)
            Field("TO", ui.to)
            Field("SENT", ui.sent)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.paperRule))
        when (body) {
            BodyState.Loading -> BodySkeleton()
            is BodyState.Failed -> BodyFailed(body.text, onRetryBody)
            is BodyState.Text -> Body(body.text)
        }
    }
}

@Composable
private fun Field(label: String, value: String) {
    val c = AimapTheme.colors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(label, Modifier.width(44.dp), style = style(DataFont, 11, color = c.inkMuted))
        Text(value, Modifier.weight(1f), style = style(DataFont, 13, color = c.ink, lineHeight = 1.3f))
    }
}

/** The letter itself, cut at the fold until someone asks for the rest. */
@Composable
private fun Body(text: String) {
    val c = AimapTheme.colors
    val letter = text.trim()
    if (letter.isEmpty()) {
        Text(
            "No text in this message.",
            Modifier.fillMaxWidth().padding(14.dp),
            style = style(BodyFont, 15, color = c.inkMuted),
        )
        return
    }
    var open by rememberSaveable(letter) { mutableStateOf(false) }
    val letterStyle = style(BodyFont, 16, color = c.ink, lineHeight = 1.42f)
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val width = with(LocalDensity.current) { (maxWidth - 28.dp).roundToPx() }
        val folded = remember(letter, width) {
            foldedLines(measurer.measure(letter, letterStyle, constraints = Constraints(maxWidth = width)).lineCount)
        }
        Column(Modifier.fillMaxWidth()) {
            Text(
                letter,
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp),
                style = letterStyle,
                maxLines = if (open) Int.MAX_VALUE else FOLD_LINES,
                overflow = TextOverflow.Clip,
            )
            if (folded > 0) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(c.paperRule))
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 52.dp)
                        .clickable(role = Role.Button) { open = !open }
                        .padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        if (open) "Fold the message back" else "Show the full message",
                        style = style(CallFont, 17, 700, c.ink),
                    )
                    Text(
                        if (open) "" else "+$folded lines",
                        Modifier.weight(1f),
                        style = style(DataFont, 12, color = c.inkMuted),
                    )
                    Icon(if (open) Lucide.ChevronUp else Lucide.ChevronDown, null, Modifier.size(22.dp), tint = c.ink)
                }
            }
        }
    }
}

@Composable
private fun BodyFailed(text: String, onRetry: () -> Unit) {
    val c = AimapTheme.colors
    Column(
        Modifier.fillMaxWidth().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(text, style = style(BodyFont, 15, color = c.ink, lineHeight = 1.35f))
        Key("Try again", null, fill = c.paper, outline = c.ink, modifier = Modifier.fillMaxWidth(), onClick = onRetry)
    }
}

/** Paper bars where the text will be: a skeleton, never a spinner in the middle. */
@Composable
private fun BodySkeleton() {
    val c = AimapTheme.colors
    Column(
        Modifier.fillMaxWidth().padding(14.dp).semantics(mergeDescendants = true) { stateDescription = "Reading" },
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        listOf(1f, 0.96f, 0.88f, 0.5f).forEach { width ->
            Box(Modifier.fillMaxWidth(width).height(12.dp).background(c.inkMuted.copy(alpha = 0.16f), Plate))
        }
    }
}

/** The first load: the strip and the letter in outline, in place, so nothing jumps. */
@Composable
private fun Skeleton() {
    val c = AimapTheme.colors
    Column(Modifier.fillMaxWidth().semantics(mergeDescendants = true) { stateDescription = "Reading the message" }) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Gutter).height(IntrinsicSize.Min)
                .clip(Plate).background(c.paper),
        ) {
            Box(Modifier.width(60.dp).fillMaxHeight().background(c.rail))
            Column(
                Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(Modifier.fillMaxWidth(0.55f).height(16.dp).background(c.inkMuted.copy(alpha = 0.22f), Plate))
                Box(Modifier.fillMaxWidth(0.9f).height(12.dp).background(c.inkMuted.copy(alpha = 0.16f), Plate))
            }
        }
        Column(
            Modifier.fillMaxWidth().padding(start = Gutter, top = 8.dp, end = Gutter).clip(Plate).background(c.paper),
        ) {
            BodySkeleton()
        }
    }
}

/** The whole screen could not be read: say so on paper, with one way out. */
@Composable
private fun Notice(title: String, text: String, keys: List<Pair<String, () -> Unit>>) {
    val c = AimapTheme.colors
    Column(
        Modifier.fillMaxWidth().padding(horizontal = Gutter).clip(Plate).background(c.paper).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(title, style = style(CallFont, 22, 800, c.ink))
        Text(text, style = style(BodyFont, 15, color = c.inkMuted, lineHeight = 1.35f))
        keys.forEach { (label, action) ->
            Key(label, null, fill = c.paper, outline = c.ink, modifier = Modifier.fillMaxWidth(), onClick = action)
        }
    }
}

/**
 * The foot: two keys and nothing else. Keeping it one row high leaves the space the
 * reply tray needs, so swapping it in means replacing this composable.
 */
@Composable
private fun Foot(ui: DetailUi, onAction: (Long, DetailAction) -> Unit) {
    val c = AimapTheme.colors
    Row(
        Modifier.fillMaxWidth().background(c.consoleDeep).topRule(c.rail).navigationBarsPadding()
            .padding(horizontal = Gutter, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Key(
            "Handled",
            Lucide.Check,
            fill = holder(ui.bay),
            outline = null,
            current = ui.state == MessageState.Handled,
            modifier = Modifier.weight(1f),
        ) { onAction(ui.messageId, DetailAction.Handled) }
        Key(
            "Later",
            Lucide.Clock,
            fill = c.paper,
            outline = c.ink,
            current = ui.state == MessageState.Later,
            modifier = Modifier.weight(1f),
        ) { onAction(ui.messageId, DetailAction.Later) }
    }
}

@Composable
private fun Key(
    label: String,
    icon: ImageVector?,
    fill: Color,
    outline: Color?,
    modifier: Modifier = Modifier,
    current: Boolean = false,
    onClick: () -> Unit,
) {
    val c = AimapTheme.colors
    Row(
        modifier.heightIn(min = 48.dp).background(fill, Plate)
            .then(
                when {
                    current -> Modifier.border(2.5.dp, c.ink, Plate)
                    outline != null -> Modifier.border(1.5.dp, outline, Plate)
                    else -> Modifier
                }
            )
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { if (current) stateDescription = "set" }
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let { Icon(it, null, Modifier.size(20.dp), tint = c.ink) }
        Text(label, style = style(CallFont, 18, 800, c.ink))
    }
}

@Preview(name = "Read · dark", widthDp = 390, heightDp = 844)
@Composable
private fun ReadDarkPreview() {
    AimapTheme(darkTheme = true) { DetailScreen(SampleMessage.read) }
}

@Preview(name = "Read · light", widthDp = 390, heightDp = 844)
@Composable
private fun ReadLightPreview() {
    AimapTheme(darkTheme = false) { DetailScreen(SampleMessage.read) }
}

@Preview(name = "Read · loading", widthDp = 390, heightDp = 844)
@Composable
private fun ReadLoadingPreview() {
    AimapTheme(darkTheme = true) { DetailScreen(Read.Loading) }
}

@Preview(name = "Read · body failed", widthDp = 390, heightDp = 844)
@Composable
private fun ReadBodyFailedPreview() {
    AimapTheme(darkTheme = false) {
        DetailScreen(SampleMessage.read.copy(body = BodyState.Failed("The body is not in the bucket any more.")))
    }
}

@Preview(name = "Read · gone", widthDp = 390, heightDp = 844)
@Composable
private fun ReadGonePreview() {
    AimapTheme(darkTheme = true) { DetailScreen(Read.Gone) }
}
