package pe.net.libre.aimap_client.ui.theme

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import pe.net.libre.aimap_client.R

private val Weights = listOf(400, 600, 700, 800)

@OptIn(ExperimentalTextApi::class)
private fun variable(res: Int) = FontFamily(
    Weights.map { w ->
        Font(res, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))
    }
)

/** Callsigns, titles and labels. */
val CallFont = variable(R.font.sofia_sans_condensed)

/** Subjects and sender summaries. */
val BodyFont = variable(R.font.sofia_sans)

/** Every count, age and time (the Printed Data Rule). */
val DataFont = variable(R.font.azeret_mono)
