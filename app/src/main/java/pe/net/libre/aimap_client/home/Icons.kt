package pe.net.libre.aimap_client.home

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** The lucide icons the designs use (ISC licence), as 24x24 2px strokes. Tint when drawing. */
object Lucide {
    val Check = lucide("check", "M20 6 9 17l-5-5")
    val ArrowLeft = lucide("arrow-left", "m12 19-7-7 7-7", "M19 12H5")
    val Clock = lucide("clock", "M12 6v6l4 2", "M22 12a10 10 0 1 1-20 0 10 10 0 1 1 20 0")
    val Reply = lucide("reply", "M9 17 4 12l5-5", "M20 18v-2a4 4 0 0 0-4-4H4")
    val Archive = lucide(
        "archive",
        "M3 3h18a1 1 0 0 1 1 1v3a1 1 0 0 1-1 1H3a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1z",
        "M4 8v11a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8",
        "M10 12h4",
    )
    val ShieldCheck = lucide(
        "shield-check",
        "M20 13c0 5-3.5 7.5-7.66 8.95a1 1 0 0 1-.67-.01C7.5 20.5 4 18 4 13V6a1 1 0 0 1 1-1c2 0 4.5-1.2 6.24-2.72a1.17 1.17 0 0 1 1.52 0C14.51 3.81 17 5 19 5a1 1 0 0 1 1 1z",
        "m9 12 2 2 4-4",
    )
    val ChevronDown = lucide("chevron-down", "m6 9 6 6 6-6")
    val ChevronUp = lucide("chevron-up", "m18 15-6-6-6 6")
    val Loader = lucide(
        "loader",
        "M12 2v4", "m16.2 7.8 2.9-2.9", "M18 12h4", "m16.2 16.2 2.9 2.9",
        "M12 18v4", "m4.9 19.1 2.9-2.9", "M2 12h4", "m4.9 4.9 2.9 2.9",
    )
    val LayoutList = lucide(
        "layout-list",
        "M4 3h5a1 1 0 0 1 1 1v5a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1z",
        "M4 14h5a1 1 0 0 1 1 1v5a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1v-5a1 1 0 0 1 1-1z",
        "M14 4h7", "M14 9h7", "M14 15h7", "M14 20h7",
    )
    val Inbox = lucide(
        "inbox",
        "M22 12h-6l-2 3h-4l-2-3H2",
        "M5.45 5.11 2 12v6a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-6l-3.45-6.89A2 2 0 0 0 16.76 4H7.24a2 2 0 0 0-1.79 1.11z",
    )
    val SlidersHorizontal = lucide(
        "sliders-horizontal",
        "M21 4h-7", "M10 4H3", "M21 12h-9", "M8 12H3", "M21 20h-5", "M12 20H3",
        "M14 2v4", "M8 10v4", "M16 18v4",
    )

    private fun lucide(name: String, vararg paths: String): ImageVector {
        val b = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
        for (d in paths) {
            b.addPath(
                pathData = PathParser().parsePathString(d).toNodes(),
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
        return b.build()
    }
}
