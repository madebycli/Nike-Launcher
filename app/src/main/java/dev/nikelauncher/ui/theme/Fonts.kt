// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.theme

import android.content.Context
import android.graphics.Typeface
import androidx.compose.ui.text.font.FontFamily
import dev.nikelauncher.R
import dev.nikelauncher.data.AppFont
import java.io.File

fun AppFont.toFontFamily(): FontFamily = when (this) {
    AppFont.GOOGLE_SANS_FLEX -> GoogleSansFlexFamily
    AppFont.SYSTEM -> FontFamily.Default
    AppFont.NIAGARA -> WidgetTextFamily
    AppFont.SANS_SERIF -> FontFamily.SansSerif
    AppFont.SERIF -> FontFamily.Serif
    AppFont.MONOSPACE -> FontFamily.Monospace
    // Without the file this cannot be resolved here; callers that have it use fontFamilyOf.
    AppFont.CUSTOM -> FontFamily.Default
}

/**
 * The typeface to draw with, including one the user supplied.
 *
 * The file is loaded once per path rather than per text: Typeface.createFromFile parses the
 * whole font, and the app list asks for this on every row. A file that will not parse falls
 * back to the default rather than failing — a font picked months ago may since have gone.
 */
fun fontFamilyOf(font: AppFont, fontFile: String?): FontFamily {
    if (font != AppFont.CUSTOM) return font.toFontFamily()
    val path = fontFile ?: return FontFamily.Default
    return customFontCache.getOrPut(path) {
        runCatching { FontFamily(Typeface.createFromFile(File(path))) }.getOrDefault(FontFamily.Default)
    }
}

private val customFontCache = mutableMapOf<String, FontFamily>()

/** The same choice as a platform typeface, for the views a widget is drawn with. */
fun widgetTypeface(font: AppFont, fontFile: String?, context: Context): Typeface = when (font) {
    AppFont.GOOGLE_SANS_FLEX -> WidgetTypefaces.get(context, R.font.google_sans_flex)
    AppFont.SYSTEM -> Typeface.DEFAULT
    AppFont.NIAGARA -> WidgetTypefaces.get(context, R.font.nunito_sans_vf)
    AppFont.SANS_SERIF -> Typeface.SANS_SERIF
    AppFont.SERIF -> Typeface.SERIF
    AppFont.MONOSPACE -> Typeface.MONOSPACE
    AppFont.CUSTOM -> fontFile?.let { path ->
        customTypefaceCache.getOrPut(path) {
            runCatching { Typeface.createFromFile(File(path)) }.getOrDefault(Typeface.DEFAULT)
        }
    } ?: Typeface.DEFAULT
}

private val customTypefaceCache = mutableMapOf<String, Typeface>()
