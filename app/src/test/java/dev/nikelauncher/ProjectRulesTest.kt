// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Keeps the rules in AGENTS.md from eroding. The allow-lists below hold what predates the rules;
 * they only ever shrink. A file leaves a list when it is rebuilt, and nothing may join one.
 */
class ProjectRulesTest {

    private val sources: List<File> = File("src/main/java").walkTopDown().filter { it.extension == "kt" }.toList()

    private fun relative(file: File) = file.path.removePrefix("src/main/java/dev/nikelauncher/")

    @Test
    fun `sources were found`() {
        assertTrue("run from the app module", sources.size > 20)
    }

    @Test
    fun `no hand-drawn rounded shapes outside the allow-list`() {
        val offenders = sources
            .filter { "RoundedCornerShape(" in it.readText() && relative(it) !in LEGACY_ROUNDED_SHAPES }
            .map(::relative)
        assertTrue("Use the Material shape tokens or component defaults instead: $offenders", offenders.isEmpty())
    }

    @Test
    fun `no source file outgrows the size cap`() {
        val offenders = sources.mapNotNull { file ->
            val lines = file.readLines().size
            val cap = LEGACY_FILE_CAPS[relative(file)] ?: MAX_LINES
            if (lines > cap) "${relative(file)}: $lines > $cap" else null
        }
        assertTrue("Split these files: $offenders", offenders.isEmpty())
    }

    @Test
    fun `no user-visible string literals in composables`() {
        val literal = Regex("""\bText\(\s*(text\s*=\s*)?"[^"]*[A-Za-z]{3}""")
        val offenders = sources
            .filter { literal.containsMatchIn(it.readText()) && relative(it) !in LEGACY_TEXT_LITERALS }
            .map(::relative)
        assertTrue("Move the text to strings.xml: $offenders", offenders.isEmpty())
    }

    @Test
    fun `source and docs are english`() {
        val german = Regex("[äöüßÄÖÜ]")
        val files = sources + File("..").walkTopDown()
            .onEnter { it.name !in setOf("build", ".git", ".gradle", "res") }
            .filter { it.extension in setOf("md", "kts", "yml", "toml") }
            .toList()
        val offenders = files
            .filter { german.containsMatchIn(it.readText()) && it.name !in GERMAN_ALLOWED }
            .map { it.path }
        assertTrue("Everything outside res/values-<lang> is English: $offenders", offenders.isEmpty())
    }

    private companion object {
        const val MAX_LINES = 400

        /** Index letters are data, not prose. */
        val GERMAN_ALLOWED = setOf("AppListModel.kt")

        val LEGACY_ROUNDED_SHAPES = setOf(
            "ui/common/IconPickerScreen.kt",
            "ui/applist/BandEditOverlay.kt",
            "media/NowPlayingWidget.kt",
            "widget/WidgetSlot.kt",
        )

        val LEGACY_TEXT_LITERALS = emptySet<String>()

        val LEGACY_FILE_CAPS = mapOf(
            "ui/applist/AppListScreen.kt" to 540,
            "data/AppRepository.kt" to 745,
                    )
    }
}
