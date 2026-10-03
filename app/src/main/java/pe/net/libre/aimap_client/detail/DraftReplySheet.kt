package pe.net.libre.aimap_client.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import pe.net.libre.aimap_client.api.Draft
import pe.net.libre.aimap_client.home.Lucide
import pe.net.libre.aimap_client.ui.theme.AimapTheme
import pe.net.libre.aimap_client.ui.theme.BodyFont
import pe.net.libre.aimap_client.ui.theme.CallFont
import pe.net.libre.aimap_client.ui.theme.DataFont

private val Plate = RoundedCornerShape(2.dp)
private val Gutter = 20.dp

private fun style(font: FontFamily, size: Int, weight: Int = 400, color: androidx.compose.ui.graphics.Color, lineHeight: Float? = null) = TextStyle(
    fontFamily = font,
    fontSize = size.sp,
    fontWeight = FontWeight(weight),
    color = color,
    lineHeight = lineHeight?.em ?: TextStyle.Default.lineHeight,
    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
)

/** What the draft sheet is showing. */
sealed interface DraftState {
    /** Working on the draft, the sheet shows a skeleton. */
    data object Loading : DraftState

    /** The model returned a draft. */
    data class Ready(val draft: Draft, val instructions: String, val mailAppError: String? = null) : DraftState

    /** The call failed: say so, keep the instructions, and let Regenerate try again. */
    data class Failed(val text: String, val instructions: String) : DraftState
}

/**
 * The draft reply sheet: the proposed draft (selectable), an editable instructions field,
 * and Copy, Open in mail app, and Regenerate keys.
 */
@Composable
fun DraftReplySheet(
    state: DraftState,
    onClose: () -> Unit,
    onRegenerate: (String) -> Unit,
    onMailAppError: (String?) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val c = AimapTheme.colors
    Column(
        modifier
            .fillMaxWidth()
            .background(c.underSheet)
            .navigationBarsPadding()
            .padding(horizontal = Gutter, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Draft a reply", style = style(CallFont, 22, 800, c.ink))
            Box(
                Modifier.size(32.dp).clip(Plate).clickable(role = Role.Button, onClick = onClose)
                    .semantics { contentDescription = "Close the draft sheet" },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Lucide.X, null, Modifier.size(20.dp), tint = c.ink)
            }
        }
        when (state) {
            DraftState.Loading -> DraftSkeleton()
            is DraftState.Ready -> DraftReady(state.draft, state.instructions, state.mailAppError, onRegenerate, onMailAppError)
            is DraftState.Failed -> DraftFailed(state.text, state.instructions, onRegenerate)
        }
    }
}

@Composable
private fun DraftSkeleton() {
    val c = AimapTheme.colors
    Column(
        Modifier.fillMaxWidth()
            .heightIn(min = 200.dp)
            .clip(Plate)
            .background(c.paper)
            .padding(14.dp)
            .semantics { contentDescription = "Working on the draft" },
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        listOf(1f, 0.96f, 0.88f, 0.6f, 0.9f, 0.5f).forEach { width ->
            Box(Modifier.fillMaxWidth(width).heightIn(min = 12.dp).background(c.inkMuted.copy(alpha = 0.16f), Plate))
        }
    }
}

@Composable
private fun DraftReady(
    draft: Draft,
    instructions: String,
    mailAppError: String?,
    onRegenerate: (String) -> Unit,
    onMailAppError: (String?) -> Unit,
) {
    val c = AimapTheme.colors
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SelectionContainer {
            Column(
                Modifier.fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .clip(Plate)
                    .background(c.paper)
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val msgCount = draft.usedMessageIds.size
                val msgWord = if (msgCount == 1) "message" else "messages"
                Text("Read $msgCount $msgWord from this thread", style = style(DataFont, 12, color = c.inkMuted))
                Text("TO: " + draft.to.joinToString(", "), style = style(DataFont, 12, color = c.inkMuted))
                if (draft.cc.isNotEmpty()) {
                    Text("CC: " + draft.cc.joinToString(", "), style = style(DataFont, 12, color = c.inkMuted))
                }
                Text("SUBJECT: ${draft.subject}", style = style(DataFont, 12, color = c.inkMuted))
                Box(Modifier.fillMaxWidth().heightIn(min = 1.dp).background(c.paperRule))
                Text(draft.body, style = style(BodyFont, 16, color = c.ink, lineHeight = 1.42f))
            }
        }
        if (mailAppError != null) {
            Column(
                Modifier.fillMaxWidth().clip(Plate).background(c.paper).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Couldn't open mail app", style = style(CallFont, 18, 700, c.ink))
                Text(mailAppError, style = style(BodyFont, 15, color = c.inkMuted, lineHeight = 1.35f))
            }
        }
        InstructionsField(instructions, onRegenerate)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val clipboard = LocalClipboardManager.current
            DraftKey("Copy", Lucide.Copy, Modifier.weight(1f)) {
                clipboard.setText(AnnotatedString(draft.body))
            }
            val context = LocalContext.current
            DraftKey("Open in mail app", Lucide.Mail, Modifier.weight(1f)) {
                val error = openInMailApp(context, draft)
                onMailAppError(error)
            }
        }
        DraftKey("Regenerate", Lucide.RefreshCw, Modifier.fillMaxWidth()) {
            onRegenerate(instructions)
        }
    }
}

@Composable
private fun DraftFailed(text: String, instructions: String, onRegenerate: (String) -> Unit) {
    val c = AimapTheme.colors
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(
            Modifier.fillMaxWidth().clip(Plate).background(c.paper).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Couldn't draft a reply", style = style(CallFont, 18, 700, c.ink))
            Text(text, style = style(BodyFont, 15, color = c.inkMuted, lineHeight = 1.35f))
        }
        InstructionsField(instructions, onRegenerate)
        DraftKey("Regenerate", Lucide.RefreshCw, Modifier.fillMaxWidth()) {
            onRegenerate(instructions)
        }
    }
}

@Composable
private fun InstructionsField(initialValue: String, onRegenerate: (String) -> Unit) {
    val c = AimapTheme.colors
    var text by remember(initialValue) { mutableStateOf(initialValue) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Instructions (optional)", style = style(CallFont, 14, 600, c.ink))
        BasicTextField(
            value = text,
            onValueChange = { if (it.length <= 2000) text = it },
            modifier = Modifier.fillMaxWidth()
                .heightIn(min = 80.dp)
                .clip(Plate)
                .background(c.paper)
                .border(1.dp, c.paperRule, Plate)
                .padding(12.dp)
                .semantics { contentDescription = "Instructions for the draft" },
            textStyle = style(BodyFont, 15, color = c.ink, lineHeight = 1.35f),
            cursorBrush = SolidColor(c.ink),
        )
    }
}

@Composable
private fun DraftKey(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val c = AimapTheme.colors
    Row(
        modifier
            .heightIn(min = 48.dp)
            .background(c.paper, Plate)
            .border(1.5.dp, c.ink, Plate)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label }
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, Modifier.size(20.dp), tint = c.ink)
        Text(label, style = style(CallFont, 18, 800, c.ink))
    }
}

private fun openInMailApp(context: android.content.Context, draft: Draft): String? {
    return try {
        val uriString = buildMailtoUri(draft)
        val uri = Uri.parse(uriString)
        val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        null
    } catch (e: Exception) {
        "No mail app is installed, or the intent could not be handled. ${e.message.orEmpty()}".trim()
    }
}

@Preview(name = "Draft ready", widthDp = 390)
@Composable
private fun DraftReadyPreview() {
    AimapTheme(darkTheme = true) {
        DraftReplySheet(
            DraftState.Ready(SampleMessage.draft, "Say Tuesday or Wednesday after 15:00 works."),
            onClose = {},
            onRegenerate = {},
            onMailAppError = {},
        )
    }
}

@Preview(name = "Draft loading", widthDp = 390)
@Composable
private fun DraftLoadingPreview() {
    AimapTheme(darkTheme = false) {
        DraftReplySheet(DraftState.Loading, onClose = {}, onRegenerate = {}, onMailAppError = {})
    }
}

@Preview(name = "Draft failed", widthDp = 390)
@Composable
private fun DraftFailedPreview() {
    AimapTheme(darkTheme = true) {
        DraftReplySheet(
            DraftState.Failed("Drafting is not configured.", "Say Tuesday works."),
            onClose = {},
            onRegenerate = {},
            onMailAppError = {},
        )
    }
}
