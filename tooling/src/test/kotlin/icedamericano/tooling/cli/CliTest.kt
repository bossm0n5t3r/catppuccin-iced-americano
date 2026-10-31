package icedamericano.tooling.cli

import java.io.PrintWriter
import java.io.StringWriter
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class CliTest {
    private fun invoke(vararg args: String): Int =
        runCli(arrayOf(*args), PrintWriter(StringWriter()), PrintWriter(StringWriter()))

    private fun withPalette(text: String, action: (String) -> Unit) {
        val path = Files.createTempFile("cli-palette", ".json")
        try {
            Files.writeString(path, text)
            action(path.toString())
        } finally {
            Files.deleteIfExists(path)
        }
    }

    private fun fixture(): JsonObject =
        Json.parseToJsonElement(
            checkNotNull(javaClass.getResourceAsStream("/iced-americano.json"))
                .bufferedReader()
                .use { it.readText() }
        ) as JsonObject

    @Test
    fun requiredContrastFailureUsesExitOne() {
        val root = fixture()
        val colors = root.getValue("colors") as JsonObject
        val changed =
            JsonObject(
                root + ("colors" to JsonObject(colors + ("blue" to colors.getValue("base"))))
            )
        withPalette(changed.toString()) { path -> assertEquals(1, invoke("validate", path)) }
    }

    @Test
    fun invalidColorUsesExitOneForEitherCommand() {
        val root = fixture()
        val colors = root.getValue("colors") as JsonObject
        val changed =
            JsonObject(
                root + ("colors" to JsonObject(colors + ("blue" to JsonPrimitive("#GGGGGG"))))
            )
        withPalette(changed.toString()) { path ->
            assertEquals(1, invoke("validate", path))
            assertEquals(1, invoke("cvd", path))
        }
    }

    @Test
    fun malformedSchemaAndJsonUseExitTwo() {
        for (text in listOf("{", "[]", "{}")) {
            withPalette(text) { path -> assertEquals(2, invoke("validate", path)) }
        }
    }

    @Test
    fun invalidUsageAndMissingFileUseExitTwo() {
        assertEquals(2, invoke())
        assertEquals(2, invoke("unknown"))
        assertEquals(2, invoke("validate", "one", "two"))
        val directory = Files.createTempDirectory("missing-palette")
        try {
            assertEquals(2, invoke("validate", directory.resolve("missing.json").toString()))
        } finally {
            Files.deleteIfExists(directory)
        }
    }
}
