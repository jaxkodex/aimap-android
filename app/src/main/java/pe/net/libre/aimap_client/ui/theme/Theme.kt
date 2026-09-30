package pe.net.libre.aimap_client.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

/** One custom design language on both platforms, so no dynamic colour. */
@Composable
fun AimapTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkAimapColors else LightAimapColors
    val scheme = (if (darkTheme) darkColorScheme() else lightColorScheme()).copy(
        primary = colors.holderYellow,
        onPrimary = colors.ink,
        background = colors.console,
        onBackground = colors.onConsole,
        surface = colors.console,
        onSurface = colors.onConsole,
    )
    CompositionLocalProvider(LocalAimapColors provides colors) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}

object AimapTheme {
    val colors: AimapColors
        @Composable @ReadOnlyComposable get() = LocalAimapColors.current
}
