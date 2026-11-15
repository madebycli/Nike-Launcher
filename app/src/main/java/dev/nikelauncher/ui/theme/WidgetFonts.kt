// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.theme

import android.content.Context
import android.graphics.Typeface
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import dev.nikelauncher.R

/** Niagara's text family; bundled so previews and the home screen use the same metrics. */
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
val WidgetTextFamily = FontFamily(
    Font(R.font.nunito_sans_vf, weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.nunito_sans_vf, weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.nunito_sans_vf, weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.nunito_sans_vf, weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600))),
)

/** AniSync's default family, including its rounded variable-font axis. */
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
val GoogleSansFlexFamily = FontFamily(
    listOf(400, 500, 600, 700).map { weight ->
        Font(R.font.google_sans_flex, weight = FontWeight(weight),
            variationSettings = FontVariation.Settings(FontVariation.weight(weight),
                FontVariation.Setting("ROND", 100f), FontVariation.Setting("opsz", 14f)))
    }
)

internal object WidgetTypefaces {
    private val cached = mutableMapOf<Pair<Int, Int>, Typeface>()
    fun get(context: Context, resource: Int, weight: Int = 400): Typeface =
        cached.getOrPut(resource to weight) {
            if (android.os.Build.VERSION.SDK_INT >= 28)
                Typeface.create(context.resources.getFont(resource), weight, false)
            else Typeface.create(context.resources.getFont(resource),
                if (weight >= 600) Typeface.BOLD else Typeface.NORMAL)
        }
}
