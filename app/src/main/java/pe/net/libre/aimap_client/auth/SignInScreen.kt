package pe.net.libre.aimap_client.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.net.libre.aimap_client.ui.theme.AimapTheme
import pe.net.libre.aimap_client.ui.theme.BodyFont
import pe.net.libre.aimap_client.ui.theme.CallFont

/**
 * The console with one amber plate: sign in with Google. [onSample] is set in debug builds only,
 * and opens the Bay on sample data when no account can sign in.
 */
@Composable
fun SignInScreen(busy: Boolean, error: String?, onSample: (() -> Unit)? = null, onSignIn: () -> Unit) {
    val c = AimapTheme.colors
    Box(Modifier.fillMaxSize().background(c.console).safeDrawingPadding().padding(horizontal = 20.dp)) {
        Column(Modifier.align(Alignment.Center).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("aimap", style = TextStyle(fontFamily = CallFont, fontWeight = FontWeight(800), fontSize = 36.sp, color = c.onConsole))
            Text(
                "Sign in with the Google account your aimap allows.",
                style = TextStyle(fontFamily = BodyFont, fontSize = 15.sp, color = c.onConsoleMuted),
            )
            Box(
                Modifier.padding(top = 12.dp).fillMaxWidth().heightIn(min = 56.dp)
                    .background(if (busy) c.holderCango else c.holderYellow, RoundedCornerShape(3.dp))
                    .clickable(enabled = !busy, role = Role.Button, onClick = onSignIn),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (busy) "Signing in…" else "Sign in with Google",
                    style = TextStyle(fontFamily = CallFont, fontWeight = FontWeight(800), fontSize = 18.sp, color = c.ink),
                )
            }
            error?.let { Text(it, style = TextStyle(fontFamily = BodyFont, fontSize = 14.sp, color = c.onConsole)) }
            onSample?.let { open ->
                Box(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .clickable(role = Role.Button, onClickLabel = "Open the sample bay", onClick = open),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Open the sample bay",
                        style = TextStyle(fontFamily = CallFont, fontWeight = FontWeight(700), fontSize = 16.sp, color = c.onConsoleMuted),
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun SignInPreview() {
    AimapTheme(darkTheme = true) { SignInScreen(busy = false, error = null) {} }
}
