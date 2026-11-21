package icedamericano.tooling.ohmypi

import icedamericano.tooling.color.contrast
import icedamericano.tooling.ghostty.renderGhosttyTheme
import icedamericano.tooling.jetbrains.editorSchemeDerivations
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class OhMyPiGeneratorTest {
    @org.junit.jupiter.api.io.TempDir lateinit var directory: Path

    private fun fixture(): JsonObject =
        Json.parseToJsonElement(
            checkNotNull(javaClass.getResourceAsStream("/iced-americano.json"))
                .bufferedReader()
                .use { it.readText() }
        ) as JsonObject

    private fun withColor(root: JsonObject, token: String, value: JsonElement?): JsonObject {
        val colors = (root.getValue("colors") as JsonObject).toMutableMap()
        if (value == null) colors.remove(token) else colors[token] = value
        return JsonObject(root + ("colors" to JsonObject(colors)))
    }

    private fun render(root: JsonObject): JsonObject =
        Json.parseToJsonElement(renderOhMyPiTheme(root)) as JsonObject

    private fun JsonElement.strings(): Map<String, String> =
        (this as JsonObject).mapValues { (it.value as JsonPrimitive).content }

    @Test
    fun varsCopyEveryPaletteColor() {
        val root = fixture()
        val theme = render(root)
        assertEquals(OH_MY_PI_THEME_NAME, (theme.getValue("name") as JsonPrimitive).content)
        val palette = root.getValue("colors").strings().mapValues { it.value.uppercase() }
        val vars = theme.getValue("vars").strings()
        assertEquals(palette, vars.filterKeys { it in palette })
        assertEquals(derivedVars.keys, vars.keys - palette.keys)
    }

    @Test
    fun typeNameMatchesJetBrains() {
        val palette = fixture().getValue("colors").strings().mapValues { it.value.uppercase() }
        val jetbrains =
            editorSchemeDerivations
                .getValue("attributes.DEFAULT_CLASS_NAME.FOREGROUND")
                .resolve(palette)
        assertEquals(jetbrains.uppercase(), resolveDerivedVars(palette)["typeName"])
    }

    @Test
    fun readableRolesReachAaContrast() {
        val theme = render(fixture())
        val vars = theme.getValue("vars").strings()
        val colors = theme.getValue("colors").strings()
        // Borders, separators and deliberately faint states are not body text.
        val decorative =
            setOf(
                "border",
                "borderMuted",
                "dim",
                "mdCodeBlockBorder",
                "mdQuoteBorder",
                "mdHr",
                "syntaxPunctuation",
                "thinkingOff",
                "thinkingMinimal",
                "statusLineSep",
            )
        for ((role, token) in colors) {
            if (role.endsWith("Bg") || role in decorative) continue
            val backgrounds =
                when {
                    role.startsWith("statusLine") -> listOf("crust")
                    // Tool output and diffs render on the Mantle tool background.
                    role.startsWith("tool") -> listOf("base", "mantle")
                    else -> listOf("base")
                }
            for (background in backgrounds) {
                val ratio = contrast(vars.getValue(token), vars.getValue(background))
                assertTrue(ratio >= 4.5, "$role ($token) on $background: $ratio")
            }
        }
        for (role in listOf("accent", "text")) {
            val ratio =
                contrast(
                    vars.getValue(colors.getValue(role)),
                    vars.getValue(colors["selectedBg"]!!),
                )
            assertTrue(ratio >= 4.5, "$role on selectedBg: $ratio")
        }
        val error = vars.getValue(colors.getValue("error"))
        assertTrue(contrast(error, vars.getValue(colors.getValue("toolErrorBg"))) >= 4.5)
    }

    @Test
    fun rolesReferenceThemeVariables() {
        val theme = render(fixture())
        val vars = theme.getValue("vars").strings().keys
        for (section in listOf("colors", "export")) {
            for ((role, token) in theme.getValue(section).strings()) {
                assertTrue(token.isEmpty() || token in vars, "$section.$role -> $token")
            }
        }
        val colors = theme.getValue("colors").strings()
        for (role in listOf("text", "userMessageText", "customMessageText")) {
            assertEquals("text", colors[role], role)
        }
        val terminal = theme.getValue("terminal") as JsonObject
        for (role in listOf("background", "foreground", "chrome", "widget")) {
            assertTrue((terminal.getValue(role) as JsonPrimitive).content in vars, role)
        }
    }

    @Test
    fun ansiMatchesGhostty() {
        val root = withColor(fixture(), "blue", JsonPrimitive("#aabbcc"))
        val ghostty =
            renderGhosttyTheme(root, "1.0.0")
                .lineSequence()
                .filter { it.startsWith("palette = ") }
                .map { it.substringAfterLast('=') }
                .toList()
        val ansi =
            ((render(root).getValue("terminal") as JsonObject).getValue("ansi") as JsonArray).map {
                (it as JsonPrimitive).content
            }
        assertEquals(16, ansi.size)
        assertEquals(ghostty, ansi)
        assertEquals("#AABBCC", ansi[4])
    }

    @Test
    fun outputIsStablePrettyJson() {
        val output = renderOhMyPiTheme(fixture())
        assertTrue(output.startsWith("{\n  \"name\": "), output.take(40))
        assertTrue(output.endsWith("}\n"))
        assertEquals(output, renderOhMyPiTheme(fixture()))
    }

    @Test
    fun missingCurrentAndStaleChecksDoNotWrite() {
        val source = directory.resolve("palette.json")
        val output = directory.resolve("nested/theme.json")
        val root = fixture()
        Files.writeString(source, root.toString())
        assertEquals(OhMyPiCheckResult.MISSING, checkOhMyPi(source, output))
        assertFalse(Files.exists(output.parent))
        generateOhMyPi(source, output)
        assertEquals(OhMyPiCheckResult.CURRENT, checkOhMyPi(source, output))
        val previous = Files.readAllBytes(output)
        Files.writeString(source, withColor(root, "blue", JsonPrimitive("#112233")).toString())
        assertEquals(OhMyPiCheckResult.STALE, checkOhMyPi(source, output))
        assertContentEquals(previous, Files.readAllBytes(output))
    }

    @Test
    fun invalidPalettePreservesOutput() {
        val root = fixture()
        val source = directory.resolve("palette.json")
        val output = directory.resolve("theme.json")
        val original = "existing output\n".toByteArray()
        Files.write(output, original)
        val invalid = mutableListOf(JsonNull, JsonObject(emptyMap()))
        for (token in listOf("blue", "crust", "lavender")) {
            for (value in listOf(null, JsonNull, JsonPrimitive(42), JsonPrimitive("#123"))) {
                invalid.add(withColor(root, token, value))
            }
        }
        for (content in invalid.map { it.toString() } + "{") {
            Files.writeString(source, content)
            assertFailsWith<IllegalArgumentException> { generateOhMyPi(source, output) }
            assertFailsWith<IllegalArgumentException> { checkOhMyPi(source, output) }
            assertContentEquals(original, Files.readAllBytes(output))
        }
        Files.delete(source)
        assertFailsWith<IOException> { generateOhMyPi(source, output) }
    }

    @Test
    fun inputAliasesAndDirectoryOutputsAreRejected() {
        val source = directory.resolve("palette.json")
        Files.writeString(source, fixture().toString())
        val original = Files.readAllBytes(source)
        assertFailsWith<IllegalArgumentException> { generateOhMyPi(source, source) }
        assertFailsWith<IOException> { generateOhMyPi(source, directory) }
        assertContentEquals(original, Files.readAllBytes(source))
    }
}
