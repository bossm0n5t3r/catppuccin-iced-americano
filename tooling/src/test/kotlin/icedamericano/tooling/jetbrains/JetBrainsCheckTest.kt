package icedamericano.tooling.jetbrains

import icedamericano.tooling.palette.Palette
import icedamericano.tooling.palette.parsePalette
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import org.junit.jupiter.api.io.TempDir

class JetBrainsCheckTest {
    @TempDir lateinit var directory: Path

    private fun resourceText(name: String) =
        checkNotNull(javaClass.getResourceAsStream(name)).bufferedReader().use { it.readText() }

    private fun palette(): Palette = parsePalette(resourceText("/iced-americano.json"))

    private fun themes(edit: (name: String, text: String) -> String = { _, text -> text }): Path {
        for (name in uiThemeFiles + EDITOR_SCHEME_FILE) {
            Files.writeString(directory.resolve(name), edit(name, resourceText("/jetbrains/$name")))
        }
        return directory
    }

    private fun replacing(file: String, old: String, new: String) = { name: String, text: String ->
        if (name != file) text
        else {
            assertTrue(old in text, "$file must contain $old")
            text.replace(old, new)
        }
    }

    @Test
    fun whiskersMixMatchesCatppuccinRounding() {
        // Expected values rendered by catppuccin-whiskers 2.9.0.
        assertEquals("#cbdddf", whiskersMix("#036B65", "#EFF1F5", 0.15))
        assertEquals("#e1bbbf", whiskersMix("#B7191A", "#EFF1F5", 0.25))
        assertEquals("#5f2938", whiskersMix("#25334D", "#B7191A", 0.6))
        assertEquals("#fc7222", whiskersMix("#FE640B", "#EFF1F5", 0.9))
        assertEquals("#1d62ed4d", Opacity("blue", 0.3).resolve(mapOf("blue" to "#1D62ED")))
    }

    @Test
    fun currentThemesMatchPalette() {
        assertEquals(emptyList(), checkJetBrains(palette(), themes()))
    }

    @Test
    fun staleDerivedColorIsReported() {
        val problems =
            checkJetBrains(
                palette(),
                themes(replacing("IcedAmericano.theme.json", "#cbdddf", "#cbdee3")),
            )
        assertEquals(
            listOf(
                "IcedAmericano.theme.json: ui.FileColor.Green is #cbdee3, expected #cbdddf " +
                    "(mix(green, base, 0.15))"
            ),
            problems,
        )
    }

    @Test
    fun unregisteredColorIsReported() {
        val problems =
            checkJetBrains(
                palette(),
                themes(
                    replacing(
                        EDITOR_SCHEME_FILE,
                        """<option name="ADDED_LINES_COLOR" value="036b65"/>""",
                        """<option name="ADDED_LINES_COLOR" value="123456"/>""",
                    )
                ),
            )
        assertEquals(
            listOf(
                "IcedAmericano.xml: colors.ADDED_LINES_COLOR uses #123456, " +
                    "which is not a palette color or registered derivation"
            ),
            problems,
        )
    }

    @Test
    fun paletteTokenMismatchIsReported() {
        val problems =
            checkJetBrains(
                palette(),
                themes(
                    replacing(
                        "IcedAmericano-islands.theme.json",
                        """"text": "#25334d"""",
                        """"text": "#4c4f69"""",
                    )
                ),
            )
        assertTrue(
            "IcedAmericano-islands.theme.json: colors.text is #4c4f69, expected #25334d" in
                problems,
            problems.joinToString("\n"),
        )
    }

    @Test
    fun malformedEditorSchemeIsInvalidInput() {
        for ((scheme, message) in
            listOf(
                "<scheme><colors>" to "IcedAmericano.xml: XML document structures",
                "<scheme><colors/></scheme>" to "IcedAmericano.xml: missing <attributes> section",
            )) {
            val directory = themes { name, text ->
                if (name == EDITOR_SCHEME_FILE) scheme else text
            }
            val exception =
                assertFailsWith<IllegalArgumentException> { checkJetBrains(palette(), directory) }
            assertTrue(exception.message!!.startsWith(message), exception.message)
        }
    }

    @Test
    fun missingDerivedColorIsReported() {
        val problems =
            checkJetBrains(
                palette(),
                themes(
                    replacing(
                        EDITOR_SCHEME_FILE,
                        """<option name="VCS_ANNOTATIONS_COLOR_5" value="dae2f4"/>""",
                        "",
                    )
                ),
            )
        assertEquals(
            listOf(
                "IcedAmericano.xml: missing derived color colors.VCS_ANNOTATIONS_COLOR_5 " +
                    "(mix(blue, base, 0.1))"
            ),
            problems,
        )
    }
}
