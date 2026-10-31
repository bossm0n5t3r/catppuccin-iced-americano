package icedamericano.tooling.ghostty

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
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.io.TempDir

class GhosttyGeneratorTest {
    @TempDir lateinit var directory: Path

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

    private fun parsed(theme: String): Pair<Map<String, String>, Map<Int, String>> {
        val scalars = mutableMapOf<String, String>()
        val ansi = mutableMapOf<Int, String>()
        for (line in theme.lineSequence().filter { it.isNotEmpty() && !it.startsWith("#") }) {
            val (key, value) = line.split(" = ", limit = 2)
            if (key == "palette") {
                val (index, color) = value.split('=', limit = 2)
                ansi[index.toInt()] = color
            } else scalars[key] = value
        }
        return scalars to ansi
    }

    @Test
    fun productionRolesAndCentralPaletteChanges() {
        val root = fixture()
        val (scalars, ansi) = parsed(renderGhosttyTheme(root))
        assertEquals(
            mapOf(
                "background" to "#EFF1F5",
                "foreground" to "#25334D",
                "cursor-color" to "#1D62ED",
                "cursor-text" to "#EFF1F5",
                "selection-background" to "#BCC0CC",
                "selection-foreground" to "#25334D",
            ),
            scalars,
        )
        assertEquals(
            listOf(
                    "#25334D",
                    "#D20F39",
                    "#006F80",
                    "#DF8E1D",
                    "#1D62ED",
                    "#EA76CB",
                    "#179299",
                    "#454D65",
                    "#686B81",
                    "#D20F39",
                    "#006F80",
                    "#DF8E1D",
                    "#1D62ED",
                    "#EA76CB",
                    "#179299",
                    "#25334D",
                )
                .withIndex()
                .associate { it.index to it.value },
            ansi,
        )
        val modified = withColor(root, "blue", JsonPrimitive("#aabbcc"))
        val (changedScalars, changedAnsi) = parsed(renderGhosttyTheme(modified))
        assertEquals("#AABBCC", changedScalars["cursor-color"])
        assertEquals("#AABBCC", changedAnsi[4])
        assertEquals("#AABBCC", changedAnsi[12])
        assertEquals(ansi[1], changedAnsi[1])
        assertEquals(ansi[9], changedAnsi[9])
    }

    @Test
    fun unrelatedMetadataIsNotValidated() {
        val root = withColor(fixture(), "unused", JsonNull)
        val minimal =
            JsonObject(
                mapOf(
                    "colors" to root.getValue("colors"),
                    "version" to JsonPrimitive("custom"),
                    "status" to JsonPrimitive("experimental"),
                    "semantics" to JsonNull,
                )
            )
        assertEquals(parsed(renderGhosttyTheme(root)), parsed(renderGhosttyTheme(minimal)))
    }

    @Test
    fun missingCurrentAndStaleChecksDoNotWrite() {
        val source = directory.resolve("palette.json")
        val output = directory.resolve("nested/theme")
        val root = fixture()
        Files.writeString(source, root.toString())
        assertEquals(GhosttyCheckResult.MISSING, checkGhostty(source, output))
        assertFalse(Files.exists(output.parent))
        generateGhostty(source, output)
        assertEquals(GhosttyCheckResult.CURRENT, checkGhostty(source, output))
        val previous = Files.readAllBytes(output)
        Files.writeString(source, withColor(root, "blue", JsonPrimitive("#112233")).toString())
        assertEquals(GhosttyCheckResult.STALE, checkGhostty(source, output))
        assertContentEquals(previous, Files.readAllBytes(output))
        generateGhostty(source, output)
        assertEquals(GhosttyCheckResult.CURRENT, checkGhostty(source, output))
        assertEquals("#112233", parsed(Files.readString(output)).second[4])
    }

    @Test
    fun invalidPalettePreservesOutput() {
        val root = fixture()
        val source = directory.resolve("palette.json")
        val output = directory.resolve("theme")
        val original = "existing output\n".toByteArray()
        Files.write(output, original)
        val invalid = mutableListOf(JsonNull, JsonObject(emptyMap()))
        for (value in
            listOf(
                null,
                JsonNull,
                JsonPrimitive(42),
                JsonPrimitive("#GGGGGG"),
                JsonPrimitive("#123"),
            )) {
            invalid.add(withColor(root, "blue", value))
        }
        for (field in listOf("version", "status")) {
            for (value in
                listOf(
                    null,
                    JsonNull,
                    JsonPrimitive(42),
                    JsonPrimitive(""),
                    JsonPrimitive("  "),
                    JsonPrimitive("bad\rvalue"),
                    JsonPrimitive("bad\nvalue"),
                )) {
                val fields = root.toMutableMap()
                if (value == null) fields.remove(field) else fields[field] = value
                invalid.add(JsonObject(fields))
            }
        }
        val contents = invalid.map { it.toString() } + "{"
        for (content in contents) {
            Files.writeString(source, content)
            assertFailsWith<IllegalArgumentException> { generateGhostty(source, output) }
            assertFailsWith<IllegalArgumentException> { checkGhostty(source, output) }
            assertContentEquals(original, Files.readAllBytes(output))
        }
        Files.delete(source)
        assertFailsWith<IOException> { generateGhostty(source, output) }
        assertFailsWith<IOException> { checkGhostty(source, output) }
        assertContentEquals(original, Files.readAllBytes(output))
    }

    @Test
    fun inputAliasesAndDirectoryOutputsAreRejected() {
        val source = directory.resolve("palette.json")
        Files.writeString(source, fixture().toString())
        val original = Files.readAllBytes(source)
        val hardlink = Files.createLink(directory.resolve("hardlink"), source)
        val symlink = Files.createSymbolicLink(directory.resolve("symlink"), source)
        for (output in
            listOf(source, directory.resolve(".").resolve("palette.json"), hardlink, symlink)) {
            assertFailsWith<IllegalArgumentException> { generateGhostty(source, output) }
            assertFailsWith<IllegalArgumentException> { checkGhostty(source, output) }
            assertContentEquals(original, Files.readAllBytes(source))
        }
        assertFailsWith<IOException> { generateGhostty(source, directory) }
        assertFailsWith<IOException> { checkGhostty(source, directory) }
        assertTrue(Files.isDirectory(directory))
        assertContentEquals(original, Files.readAllBytes(source))
    }
}
