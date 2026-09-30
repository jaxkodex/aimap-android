package pe.net.libre.aimap_client.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** The Strip Bay palette from DESIGN.md. Both modes share every token name. */
@Immutable
data class AimapColors(
    val console: Color,
    val consoleDeep: Color,
    val rail: Color,
    val paper: Color,
    val ink: Color,
    val inkMuted: Color,
    val paperRule: Color,
    val onConsole: Color,
    val onConsoleMuted: Color,
    val holderYellow: Color,
    val holderBlue: Color,
    val holderCango: Color,
    val trafficCango: Color,
    val grease: Color,
    val underSheet: Color,
    val underSheetDeep: Color,
    val isDark: Boolean,
)

val DarkAimapColors = AimapColors(
    console = Color(0xFF1D2024),
    consoleDeep = Color(0xFF151719),
    rail = Color(0xFF363B41),
    paper = Color(0xFFF1F2EE),
    ink = Color(0xFF15191D),
    inkMuted = Color(0xFF555D66),
    paperRule = Color(0x2E15191D),
    onConsole = Color(0xFFECEDEE),
    onConsoleMuted = Color(0xFF9EA4AA),
    holderYellow = Color(0xFFF0B43C),
    holderBlue = Color(0xFF8DB0D6),
    holderCango = Color(0xFF8E979D),
    trafficCango = Color(0xFF6C747A),
    grease = Color(0xFFE0402F),
    underSheet = Color(0xFFC9CCC6),
    underSheetDeep = Color(0xFF9DA29E),
    isDark = true,
)

val LightAimapColors = DarkAimapColors.copy(
    console = Color(0xFFDCDFDB),
    consoleDeep = Color(0xFFCDD1CC),
    rail = Color(0xFFA9AFAA),
    paper = Color(0xFFFFFFFF),
    onConsole = Color(0xFF15191D),
    onConsoleMuted = Color(0xFF4A525A),
    holderBlue = Color(0xFF9DBEE3),
    holderCango = Color(0xFFB4BCC1),
    trafficCango = Color(0xFF6A737A),
    underSheet = Color(0xFFC3C8C3),
    underSheetDeep = Color(0xFFA9AFAA),
    isDark = false,
)

val LocalAimapColors = staticCompositionLocalOf { DarkAimapColors }
