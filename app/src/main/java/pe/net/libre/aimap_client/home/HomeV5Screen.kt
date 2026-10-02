package pe.net.libre.aimap_client.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pe.net.libre.aimap_client.ui.theme.AimapTheme
import pe.net.libre.aimap_client.ui.theme.BodyFont
import pe.net.libre.aimap_client.ui.theme.CallFont

/**
 * The Bay, v5: the queue layout, not built yet. The header stays, so the account sheet is one tap
 * away and you can switch back to v4.
 */
@Composable
fun HomeV5Screen(ui: HomeUi, modifier: Modifier = Modifier, onAvatar: () -> Unit = {}) {
    val c = AimapTheme.colors
    Column(modifier.fillMaxSize().background(c.console)) {
        Box(Modifier.statusBarsPadding()) { ConsoleHeader(ui.header, onAvatar) }
        Column(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        ) {
            Text("Queue layout coming next", style = style(CallFont, 22, 800, c.onConsole))
            Text(
                "Pick v4 under Home layout in the account sheet to go back.",
                style = style(BodyFont, 15, color = c.onConsoleMuted, lineHeight = 1.25f),
            )
        }
        TabBar()
    }
}

@Preview(name = "Home v5 · dark", widthDp = 390, heightDp = 844)
@Composable
private fun HomeV5DarkPreview() {
    AimapTheme(darkTheme = true) {
        HomeV5Screen(SampleHome.home.toUi(SampleHome.now, accounts = 3, initials = "AR"))
    }
}

@Preview(name = "Home v5 · light", widthDp = 390, heightDp = 844)
@Composable
private fun HomeV5LightPreview() {
    AimapTheme(darkTheme = false) {
        HomeV5Screen(SampleHome.home.toUi(SampleHome.now, accounts = 3, initials = "AR"))
    }
}
