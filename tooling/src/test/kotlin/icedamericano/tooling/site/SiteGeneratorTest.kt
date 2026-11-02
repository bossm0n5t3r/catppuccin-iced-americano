package icedamericano.tooling.site

import icedamericano.tooling.palette.parsePalette
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.io.TempDir

class SiteGeneratorTest {
    @TempDir lateinit var directory: Path

    private fun fixture(): JsonObject =
        Json.parseToJsonElement(
            checkNotNull(javaClass.getResourceAsStream("/iced-americano.json"))
                .bufferedReader()
                .use { it.readText() }
        ) as JsonObject

    @Test
    fun metadataIsEscapedAndNotReinterpretedAsTemplateSyntax() {
        val root = fixture()
        val changes = root.getValue("changes_from_latte") as JsonObject
        val blue = changes.getValue("blue") as JsonObject
        val reason = "<script>alert(\"blue\")</script> & 'safe' @@status@@"
        val modified =
            JsonObject(
                root +
                    mapOf(
                        "version" to JsonPrimitive("<beta> & \"1\""),
                        "changes_from_latte" to
                            JsonObject(
                                changes +
                                    ("blue" to
                                        JsonObject(blue + ("reason" to JsonPrimitive(reason))))
                            ),
                    )
            )
        val html = renderSite(parsePalette(modified.toString()))
        assertTrue(html.contains("v&lt;beta&gt; &amp; &quot;1&quot;"))
        assertTrue(
            html
                .replace(Regex("\\s+"), " ")
                .contains(
                    "&lt;script&gt;alert(&quot;blue&quot;)&lt;/script&gt; &amp; &#x27;safe&#x27; @@status@@"
                )
        )
        assertFalse(html.contains("<script>"))
    }

    @Test
    fun staleAndMissingChecksPreserveOutputUntilExplicitGeneration() {
        val source = directory.resolve("palette.json")
        val output = directory.resolve("nested/index.html")
        val root = fixture()
        Files.writeString(source, root.toString())
        assertFalse(generateSite(source, output, check = true))
        assertFalse(Files.exists(output.parent))
        generateSite(source, output, check = false)
        assertTrue(generateSite(source, output, check = true))
        val previous = Files.readString(output)
        val colors = root.getValue("colors") as JsonObject
        Files.writeString(
            source,
            JsonObject(
                    root + ("colors" to JsonObject(colors + ("blue" to JsonPrimitive("#112233"))))
                )
                .toString(),
        )
        assertFalse(generateSite(source, output, check = true))
        assertEquals(previous, Files.readString(output))
        generateSite(source, output, check = false)
        assertTrue(Files.readString(output).contains("--blue: #112233;"))
        assertTrue(generateSite(source, output, check = true))
    }

    @Test
    fun customHtmlIsIgnoredByChecksAndPreservedWhenPaletteChanges() {
        val source = directory.resolve("palette.json")
        val output = directory.resolve("index.html")
        val root = fixture()
        Files.writeString(source, root.toString())
        generateSite(source, output, check = false)
        val customized =
            Files.readString(output)
                .replace("<h1>", "<h1 data-custom=\"keep\">")
                .replace("Calm, not washed out.", "My custom headline.")
                .replace("body { margin: 0; }", "body {\n    margin: 1rem;\n}")
        Files.writeString(output, customized)
        assertTrue(generateSite(source, output, check = true))
        assertEquals(customized, Files.readString(output))

        val colors = root.getValue("colors") as JsonObject
        Files.writeString(
            source,
            JsonObject(
                    root + ("colors" to JsonObject(colors + ("blue" to JsonPrimitive("#112233"))))
                )
                .toString(),
        )
        assertFalse(generateSite(source, output, check = true))
        assertEquals(customized, Files.readString(output))
        generateSite(source, output, check = false)
        assertEquals(customized.replace("#1D62ED", "#112233"), Files.readString(output))
        assertTrue(generateSite(source, output, check = true))
    }

    @Test
    fun depthControlsGeneratedIndentationWithoutChangingMarkersOrSurroundings() {
        val html =
            """
            before
            <!-- palette:cards:start depth=2 -->
            stale
            <!-- palette:cards:end -->
            after
            """
                .trimIndent()
        val actual =
            updateSiteRegions(html, mapOf("cards" to "<li>\n    <code>#112233</code>\n</li>"))
        assertEquals(
            "before\n<!-- palette:cards:start depth=2 -->\n" +
                "        <li>\n            <code>#112233</code>\n        </li>\n" +
                "<!-- palette:cards:end -->\nafter",
            actual,
        )
        val crlf = html.replace("\n", "\r\n")
        assertEquals(
            actual.replace("\n", "\r\n"),
            updateSiteRegions(crlf, mapOf("cards" to "<li>\n    <code>#112233</code>\n</li>")),
        )
    }

    @Test
    fun invalidMarkersFailWithoutWriting() {
        val source = directory.resolve("palette.json")
        val output = directory.resolve("index.html")
        val root = fixture()
        Files.writeString(source, root.toString())
        val html = renderSite(parsePalette(root.toString()))
        val start = "<!-- palette:cards:start depth=3 -->"
        val end = "<!-- palette:cards:end -->"
        for (invalid in
            listOf(
                html.replace(start, ""),
                html.replace(end, ""),
                html.replace(start, "$start\n$start"),
                html.replace(start, "SWAP_MARKER").replace(end, start).replace("SWAP_MARKER", end),
                html.replace(end, "<!-- palette:rows:end -->"),
                html.replace(start, "<!-- palette:unknown:start depth=3 -->"),
                html.replace(start, "<!-- palette:cards:start depth=invalid -->"),
                html.replace(start, "<!-- palette:cards:start depth=99999999999 -->"),
                html.replace(start, "/* palette:cards:start depth=3 */"),
            )) {
            Files.writeString(output, invalid)
            assertFailsWith<IllegalArgumentException> { generateSite(source, output, check = true) }
            assertFailsWith<IllegalArgumentException> {
                generateSite(source, output, check = false)
            }
            assertEquals(invalid, Files.readString(output))
        }
    }
}
