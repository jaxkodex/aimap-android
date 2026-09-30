package pe.net.libre.aimap_client.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.net.libre.aimap_client.ui.theme.AimapTheme
import pe.net.libre.aimap_client.ui.theme.BodyFont
import pe.net.libre.aimap_client.ui.theme.CallFont

/** The Bay once signed in: the live home, or its first load, or why it failed. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeRoute(state: AppState.SignedIn, onRefresh: () -> Unit, onSignOut: () -> Unit) {
    var account by rememberSaveable { mutableStateOf(false) }
    val home = state.home
    if (home == null || state.forbidden) {
        Notice(
            text = state.error ?: "Reading the bay…",
            loading = state.error == null,
            actions = buildList {
                if (!state.forbidden) add("Try again" to onRefresh)
                add("Sign out" to onSignOut)
            },
        )
    } else {
        PullToRefreshBox(isRefreshing = state.loading, onRefresh = onRefresh) {
            HomeScreen(home, onAvatar = { account = true })
            state.error?.let { ErrorBar(it, onRefresh) }
        }
    }
    if (account) {
        AlertDialog(
            onDismissRequest = { account = false },
            title = { Text(state.email) },
            text = { Text("Signed in to aimap with this Google account.") },
            confirmButton = { TextButton({ account = false; onSignOut() }) { Text("Sign out") } },
            dismissButton = { TextButton({ account = false }) { Text("Close") } },
        )
    }
}

@Composable
private fun Notice(text: String, loading: Boolean, actions: List<Pair<String, () -> Unit>>) {
    val c = AimapTheme.colors
    Box(Modifier.fillMaxSize().background(c.console).safeDrawingPadding().padding(horizontal = 20.dp)) {
        Column(Modifier.align(Alignment.Center).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                if (loading) Icon(Lucide.Loader, null, Modifier.size(18.dp), tint = c.onConsoleMuted)
                Text(text, style = TextStyle(fontFamily = BodyFont, fontSize = 15.sp, color = c.onConsole))
            }
            if (!loading) {
                actions.forEach { (label, action) ->
                    Box(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp).border(1.dp, c.rail, RoundedCornerShape(2.dp))
                            .clickable(role = Role.Button, onClick = action),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(label, style = TextStyle(fontFamily = CallFont, fontWeight = FontWeight(700), fontSize = 16.sp, color = c.onConsole))
                    }
                }
            }
        }
    }
}

/** A refresh failed but the last bay is still on screen. */
@Composable
private fun ErrorBar(text: String, onRetry: () -> Unit) {
    val c = AimapTheme.colors
    Box(
        Modifier.statusBarsPadding().padding(horizontal = 20.dp, vertical = 4.dp).fillMaxWidth()
            .background(c.paper, RoundedCornerShape(2.dp)).clickable(onClickLabel = "Try again", onClick = onRetry)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text("$text Tap to retry.", style = TextStyle(fontFamily = BodyFont, fontSize = 14.sp, color = c.ink))
    }
}
